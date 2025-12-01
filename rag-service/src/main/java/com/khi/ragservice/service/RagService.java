package com.khi.ragservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ChatRagRequestDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.enums.ReportState;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final GptService gptService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final ConversationReportRepository conversationReportRepository;

    /**
     * 빈 보고서를 초기화하여 PENDING 상태로 저장
     * voice-service가 음성 파일을 받자마자 호출하는 메서드
     */
    @Transactional
    public void initializeReport(String user1Id, String user2Id) {
        log.info("[RAG] Initializing report for user1Id: {}, user2Id: {}", user1Id, user2Id);

        ConversationReport entity = new ConversationReport();
        entity.setUser1Id(user1Id);
        entity.setUser2Id(user2Id);
        entity.setTitle("생성 중...");
        entity.setState(ReportState.PENDING);
        // chatData와 reportCards는 null로 둠 (nullable=true로 변경했음)

        ConversationReport savedEntity = conversationReportRepository.save(entity);

        log.info("[RAG] Initialized report with id: {} in PENDING state", savedEntity.getId());
    }

    @Transactional
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

            // 먼저 PENDING 상태의 리포트를 찾아서 업데이트 시도
            // Voice-Service: initializeReport로 먼저 생성한 PENDING 리포트가 있으면 업데이트
            // Chat-Service: PENDING 리포트 없이 바로 호출하면 새로 생성
            Optional<ConversationReport> pendingReportOpt = conversationReportRepository
                    .findFirstByUser1IdAndUser2IdAndStateOrderByCreatedAtDesc(
                            user1Id, user2Id, ReportState.PENDING);

            ConversationReport savedEntity;
            if (pendingReportOpt.isPresent()) {
                // Voice-Service 시나리오: PENDING 리포트가 있으면 업데이트
                ConversationReport existingReport = pendingReportOpt.get();
                log.info("[RAG] Found existing PENDING report with id: {}, updating to COMPLETED (Voice-Service flow)",
                        existingReport.getId());

                existingReport.setTitle(reportTitle);
                existingReport.setChatData(chatMessages);
                existingReport.setReportCards(reportCards);
                existingReport.setState(ReportState.COMPLETED);

                savedEntity = conversationReportRepository.save(existingReport);
                log.info("[RAG] Updated existing report id: {} to COMPLETED state", savedEntity.getId());
            } else {
                // Chat-Service 시나리오: PENDING 리포트가 없으면 새로 생성
                log.info("[RAG] No PENDING report found, creating new COMPLETED report (Chat-Service flow)");
                ConversationReport entity = new ConversationReport();
                entity.setUser1Id(user1Id);
                entity.setUser2Id(user2Id);
                entity.setTitle(reportTitle);
                entity.setChatData(chatMessages);
                entity.setReportCards(reportCards);
                entity.setState(ReportState.COMPLETED);
                savedEntity = conversationReportRepository.save(entity);
                log.info("[RAG] Created new report with id: {}", savedEntity.getId());
            }

            log.info("[RAG] Saved response to database for user1Id: {}, user2Id: {}, title: {}",
                    user1Id, user2Id, reportTitle);

            return new ReportSummaryDto(
                    savedEntity.getId(),
                    savedEntity.getUser1Id(),
                    savedEntity.getUser1Name(),
                    savedEntity.getUser2Id(),
                    savedEntity.getUser2Name(),
                    savedEntity.getTitle(),
                    savedEntity.getChatData(),
                    savedEntity.getReportCards(),
                    savedEntity.getCreatedAt(),
                    savedEntity.getState());

        } catch (Exception e) {
            log.error("[RAG] error", e);
            throw new RuntimeException("Failed to generate RAG response", e);
        }
    }

    /**
     * Chat-Service 전용: reportId를 지정하여 보고서 생성
     */
    @Transactional
    public ReportSummaryDto analyzeConversationWithReportId(ChatRagRequestDto requestDto) {


        final int K = 3;
        final long t0 = System.nanoTime();
        log.info("[RAG] start with reportId: {} | K={} | messages={}", requestDto.getReportId(), K, requestDto.getChatData().size());

        try {
            ensureTrgmReady(dataSource);

            // Perform individual RAG search for each message
            List<Map<String, Object>> messagesWithRag = new ArrayList<>();
            for (int i = 0; i < requestDto.getChatData().size(); i++) {
                ChatMessageDto message = requestDto.getChatData().get(i);
                log.info("[RAG] Processing message {}/{}: {}", i + 1, requestDto.getChatData().size(), message.getMessage());

                List<Map<String, Object>> ragItems = searchRagForMessage(message.getMessage(), K);

                log.info("[RAG] Message {}/{} - found {} RAG items for: \"{}\"",
                        i + 1, requestDto.getChatData().size(), ragItems.size(), message.getMessage());
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
            log.info("[RAG] done (sparse) | messages={} | {} ms", requestDto.getChatData().size(), (t1 - t0) / 1_000_000);

            Map<String, Object> gptInput = new LinkedHashMap<>();
            gptInput.put("messages_with_rag", messagesWithRag);

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

            // user1Name, user2Name 추출 (chatData의 첫 번째 메시지에서)
            String user1Name = null;
            String user2Name = null;
            if (!requestDto.getChatData().isEmpty()) {
                for (ChatMessageDto msg : requestDto.getChatData()) {
                    if (msg.getUserId().equals(requestDto.getUser1Id())) {
                        user1Name = msg.getName();
                    }
                    if (msg.getUserId().equals(requestDto.getUser2Id())) {
                        user2Name = msg.getName();
                    }
                    if (user1Name != null && user2Name != null) {
                        break;
                    }
                }
            }

            // reportId를 직접 지정하여 엔티티 생성
            ConversationReport entity = new ConversationReport();
            entity.setId(requestDto.getReportId());  // reportId 직접 설정
            entity.setUser1Id(requestDto.getUser1Id());
            entity.setUser1Name(user1Name);
            entity.setUser2Id(requestDto.getUser2Id());
            entity.setUser2Name(user2Name);
            entity.setTitle(reportTitle);
            entity.setChatData(requestDto.getChatData());
            entity.setReportCards(reportCards);
            entity.setState(ReportState.COMPLETED);

            ConversationReport savedEntity = conversationReportRepository.save(entity);
            log.info("[RAG] Created report with specified reportId: {}", savedEntity.getId());

            return new ReportSummaryDto(
                    savedEntity.getId(),
                    savedEntity.getUser1Id(),
                    savedEntity.getUser1Name(),
                    savedEntity.getUser2Id(),
                    savedEntity.getUser2Name(),
                    savedEntity.getTitle(),
                    savedEntity.getChatData(),
                    savedEntity.getReportCards(),
                    savedEntity.getCreatedAt(),
                    savedEntity.getState());

        } catch (Exception e) {
            log.error("[RAG] error for reportId: {}", requestDto.getReportId(), e);
            throw new RuntimeException("Failed to generate RAG response with reportId: " + requestDto.getReportId(), e);
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
