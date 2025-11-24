package com.khi.ragservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final GptService gptService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final ConversationReportRepository conversationReportRepository;

    public ReportSummaryDto analyzeConversation(String user1Id, String user2Id, List<ChatMessageDto> chatMessages) {
        final int K = 5;
        final String queryText = toUtteranceString(chatMessages).trim();
        final long t0 = System.nanoTime();
        log.info("[RAG] start (sparse) | K={} | q.len={}", K, queryText.length());

        try {
            ensureTrgmReady(dataSource);

            final String sqlFiltered = """
                        WITH q AS (SELECT ?::text AS q)
                        SELECT id, text, label, labelid AS label_id,
                               similarity(
                                 (coalesce(text,'')||' '||coalesce(label,'')),
                                 q.q
                               ) AS score
                        FROM rag_items, q
                        WHERE (
                             (coalesce(text,'')||' '||coalesce(label,'')) % q.q
                          OR  coalesce(text,'')  ILIKE '%'||q.q||'%'
                          OR  coalesce(label,'') ILIKE '%'||q.q||'%'
                        )
                        ORDER BY score DESC NULLS LAST
                        LIMIT ?
                    """;

            List<Map<String, Object>> items = runQuery(sqlFiltered, queryText, K);

            if (items.isEmpty()) {
                log.info("[RAG] no hits → fallback to full-table similarity sort");
                final String sqlFallback = """
                            WITH q AS (SELECT ?::text AS q)
                            SELECT id, text, label, labelid AS label_id,
                                   similarity(
                                     (coalesce(text,'')||' '||coalesce(label,'')),
                                     q.q
                                   ) AS score
                            FROM rag_items, q
                            ORDER BY score DESC NULLS LAST
                            LIMIT ?
                        """;
                items = runQuery(sqlFallback, queryText, K);
            }

            long t1 = System.nanoTime();
            log.info("[RAG] done (sparse) | items={} | {} ms", items.size(), (t1 - t0) / 1_000_000);

            Map<String, Object> gptInput = new LinkedHashMap<>();
            gptInput.put("conversation_text", queryText);
            gptInput.put("rag_items", items);

            // Log RAG search results before sending to GPT
            log.info("[RAG] GPT Input - conversation_text: {}", queryText);
            log.info("[RAG] GPT Input - rag_items: {}", items);

            String inputJson = objectMapper.writeValueAsString(gptInput);
            String gptResponseJson = gptService.generateReport(inputJson);

            // Parse GPT response
            @SuppressWarnings("unchecked")
            Map<String, Object> gptResponse = objectMapper.readValue(gptResponseJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            String reportTitle = (String) gptResponse.get("report_title");

            List<Map<String, Object>> reportCardsRaw = (List<Map<String, Object>>) gptResponse.get("report_cards");
            String reportCardsJson = objectMapper.writeValueAsString(reportCardsRaw);
            List<ReportCardDto> reportCards = objectMapper.readValue(
                    reportCardsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReportCardDto.class));

            // Save to database
            ConversationReport entity = new ConversationReport();
            entity.setUser1Id(user1Id);
            entity.setUser2Id(user2Id);
            entity.setTitle(reportTitle);
            entity.setChatData(chatMessages);
            entity.setReportCards(reportCards);
            ConversationReport savedEntity = conversationReportRepository.save(entity);

            log.info("[RAG] Saved response to database for user1Id: {}, user2Id: {}, title: {}",
                    user1Id, user2Id, reportTitle);

            return new ReportSummaryDto(
                    savedEntity.getId(),
                    savedEntity.getUser1Id(),
                    savedEntity.getUser2Id(),
                    savedEntity.getTitle(),
                    savedEntity.getChatData(),
                    savedEntity.getReportCards(),
                    savedEntity.getCreatedAt());

        } catch (Exception e) {
            log.error("[RAG] error", e);
            throw new RuntimeException("Failed to generate RAG response", e);
        }
    }

    private List<Map<String, Object>> runQuery(String sql, String queryText, int k) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, queryText);
            ps.setInt(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("text", rs.getString("text"));
                    m.put("label", rs.getString("label"));
                    m.put("label_id", rs.getInt("label_id"));
                    m.put("score", rs.getDouble("score"));
                    items.add(m);
                }
            }
        }
        return items;
    }

    private String toUtteranceString(List<ChatMessageDto> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (ChatMessageDto msg : chatMessages) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(msg.getName()).append(": ").append(msg.getMessage());
        }
        return sb.toString();
    }

    private void ensureTrgmReady(DataSource ds) {
        try (var con = ds.getConnection(); var st = con.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            st.execute("""
                        CREATE INDEX IF NOT EXISTS idx_rag_items_trgm
                        ON rag_items USING gin (
                          (coalesce(text,'')||' '||coalesce(label,'')) gin_trgm_ops
                        )
                    """);
            st.execute("ANALYZE rag_items");
        } catch (Exception e) {
            log.warn("[rag] pg_trgm prepare failed (continue): {}", e.toString());
        }
    }
}
