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
        final int K = 3;
        final long t0 = System.nanoTime();
        log.info("[RAG] start (sparse) | K={} | messages={}", K, chatMessages.size());

        try {
            ensureTrgmReady(dataSource);

            // Perform individual RAG search for each message
            List<Map<String, Object>> messagesWithRag = new ArrayList<>();
            for (int i = 0; i < chatMessages.size(); i++) {
                ChatMessageDto message = chatMessages.get(i);
                log.info("[RAG] Processing message {}/{}: {}", i + 1, chatMessages.size(), message.getMessage());

                List<Map<String, Object>> ragItems = searchRagForMessage(message.getMessage(), K);

                // Log detailed RAG results for this message
                log.info("[RAG] Message {}/{} - found {} RAG items for: \"{}\"",
                        i + 1, chatMessages.size(), ragItems.size(), message.getMessage());
                for (int j = 0; j < ragItems.size(); j++) {
                    Map<String, Object> item = ragItems.get(j);
                    log.info("[RAG]   Item {}: score={}, label={}, text={}",
                            j + 1, item.get("score"), item.get("label"), item.get("text"));
                }

                Map<String, Object> messageWithRag = new LinkedHashMap<>();
                messageWithRag.put("userId", message.getUserId());
                messageWithRag.put("name", message.getName());
                messageWithRag.put("message", message.getMessage());
                messageWithRag.put("rag_items", ragItems);

                messagesWithRag.add(messageWithRag);
            }

            long t1 = System.nanoTime();
            log.info("[RAG] done (sparse) | messages={} | {} ms", chatMessages.size(), (t1 - t0) / 1_000_000);

            Map<String, Object> gptInput = new LinkedHashMap<>();
            gptInput.put("messages_with_rag", messagesWithRag);

            // Log RAG search results before sending to GPT
            log.info("[RAG] GPT Input - messages_with_rag: {}", messagesWithRag);

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

    private List<Map<String, Object>> searchRagForMessage(String messageText, int k) throws Exception {
        if (messageText == null || messageText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        final String queryText = messageText.trim();

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

        List<Map<String, Object>> items = runQuery(sqlFiltered, queryText, k);

        if (items.isEmpty()) {
            log.info("[RAG] no hits for message → fallback to full-table similarity sort");
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
            items = runQuery(sqlFallback, queryText, k);
        }

        return items;
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
