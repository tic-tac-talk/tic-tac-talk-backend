package com.khi.ragservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ChatRagRequestDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.enums.ReportState;
import com.khi.ragservice.enums.SourceType;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    public void initializeReport(String user1Id, String user1Name, String user2Id, String user2Name) {
        log.info("[RAG][INIT] ===== START: Initializing PENDING report =====");
        log.info("[RAG][INIT] Input parameters - user1Id: '{}', user1Name: '{}', user2Id: '{}', user2Name: '{}'",
                user1Id, user1Name, user2Id, user2Name);

        ConversationReport entity = new ConversationReport();
        entity.setUser1Id(user1Id);
        entity.setUser1Name(user1Name);
        entity.setUser2Id(user2Id);
        entity.setUser2Name(user2Name);
        entity.setTitle("생성 중...");
        entity.setState(ReportState.PENDING);
        entity.setSourceType(SourceType.VOICE);
        // chatData와 reportCards는 null로 둠 (nullable=true로 변경했음)

        log.info("[RAG][INIT] Creating entity with state: PENDING, sourceType: VOICE");
        ConversationReport savedEntity = conversationReportRepository.save(entity);

        log.info("[RAG][INIT] ===== SUCCESS: Created PENDING report with id: {} =====", savedEntity.getId());
        log.info("[RAG][INIT] Saved entity details - id: {}, user1Id: '{}', user2Id: '{}', state: {}",
                savedEntity.getId(), savedEntity.getUser1Id(), savedEntity.getUser2Id(), savedEntity.getState());
    }

    /**
     * Chat-Service 전용: PENDING 상태의 빈 리포트를 동기적으로 생성 (컨트롤러에서 호출)
     * 프론트엔드가 즉시 "생성 중..." 상태를 조회할 수 있도록 즉시 커밋
     */
    @Transactional
    public void createPendingChatReportSync(ChatRagRequestDto requestDto) {
        log.info("[RAG][CHAT][PENDING] ===== Creating PENDING report (SYNC) =====");
        log.info("[RAG][CHAT][PENDING] reportId: {}, user1Id: '{}', user2Id: '{}'",
                requestDto.getReportId(), requestDto.getUser1Id(), requestDto.getUser2Id());

        // user1Name, user2Name 추출
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

        conversationReportRepository.upsertReport(
                requestDto.getReportId(),
                requestDto.getUser1Id(),
                user1Name,
                requestDto.getUser2Id(),
                user2Name,
                "생성 중...", // PENDING 상태의 제목
                "[]", // 빈 chatData
                "[]", // 빈 reportCards
                ReportState.PENDING.name(),
                SourceType.CHAT.name(),
                false); // isNameUpdated = false

        log.info("[RAG][CHAT][PENDING] ===== PENDING report committed to DB =====");
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
            gptInput.put("user1_id", user1Id);
            gptInput.put("user2_id", user2Id);
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
            // user1Id와 user2Id 순서에 관계없이 찾기 위해 유연한 쿼리 사용
            log.info("[RAG][ANALYZE] ===== Searching for existing PENDING report =====");
            log.info("[RAG][ANALYZE] Search parameters - user1Id: '{}', user2Id: '{}', state: PENDING", user1Id,
                    user2Id);

            Optional<ConversationReport> pendingReportOpt = conversationReportRepository
                    .findFirstByUserIdsAndStateOrderByCreatedAtDesc(
                            user1Id, user2Id, ReportState.PENDING.name());

            if (pendingReportOpt.isPresent()) {
                ConversationReport found = pendingReportOpt.get();
                log.info("[RAG][ANALYZE] ===== FOUND existing PENDING report =====");
                log.info(
                        "[RAG][ANALYZE] Found report details - id: {}, user1Id: '{}', user2Id: '{}', state: {}, createdAt: {}",
                        found.getId(), found.getUser1Id(), found.getUser2Id(), found.getState(), found.getCreatedAt());
            } else {
                log.warn("[RAG][ANALYZE] ===== NO PENDING report found =====");
                log.warn("[RAG][ANALYZE] This means either:");
                log.warn("[RAG][ANALYZE]   1. initializeReport was not called (expected for Chat-Service)");
                log.warn("[RAG][ANALYZE]   2. user1Id/user2Id don't match the PENDING report");
                log.warn("[RAG][ANALYZE]   3. The PENDING report was already updated to COMPLETED");
            }

            ConversationReport savedEntity;
            if (pendingReportOpt.isPresent()) {
                // Voice-Service 시나리오: PENDING 리포트가 있으면 업데이트
                ConversationReport existingReport = pendingReportOpt.get();
                log.info("[RAG][ANALYZE] ===== UPDATE MODE: Updating existing PENDING report =====");
                log.info("[RAG][ANALYZE] BEFORE update - id: {}, state: {}, title: '{}'",
                        existingReport.getId(), existingReport.getState(), existingReport.getTitle());

                existingReport.setTitle(reportTitle);
                existingReport.setChatData(chatMessages);
                existingReport.setReportCards(reportCards);
                existingReport.setState(ReportState.COMPLETED);
                existingReport.setSourceType(SourceType.VOICE);
                existingReport.setIsNameUpdated(false);

                log.info(
                        "[RAG][ANALYZE] Saving updated report with new title: '{}', chatData size: {}, reportCards size: {}",
                        reportTitle, chatMessages.size(), reportCards.size());
                savedEntity = conversationReportRepository.save(existingReport);

                log.info("[RAG][ANALYZE] ===== UPDATE SUCCESS =====");
                log.info("[RAG][ANALYZE] AFTER update - id: {}, state: {}, title: '{}'",
                        savedEntity.getId(), savedEntity.getState(), savedEntity.getTitle());
            } else {
                // Chat-Service 시나리오: PENDING 리포트가 없으면 새로 생성
                log.info("[RAG][ANALYZE] ===== CREATE MODE: Creating new COMPLETED report =====");
                log.info("[RAG][ANALYZE] Creating with user1Id: '{}', user2Id: '{}', title: '{}'",
                        user1Id, user2Id, reportTitle);

                ConversationReport entity = new ConversationReport();
                entity.setUser1Id(user1Id);
                entity.setUser2Id(user2Id);
                entity.setTitle(reportTitle);
                entity.setChatData(chatMessages);
                entity.setReportCards(reportCards);
                entity.setState(ReportState.COMPLETED);
                entity.setSourceType(SourceType.VOICE);
                entity.setIsNameUpdated(false);

                savedEntity = conversationReportRepository.save(entity);

                log.info("[RAG][ANALYZE] ===== CREATE SUCCESS =====");
                log.info("[RAG][ANALYZE] Created new report - id: {}, user1Id: '{}', user2Id: '{}', state: {}",
                        savedEntity.getId(), savedEntity.getUser1Id(), savedEntity.getUser2Id(),
                        savedEntity.getState());
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
                    savedEntity.getState(),
                    savedEntity.getSourceType(),
                    savedEntity.getIsNameUpdated());

        } catch (Exception e) {
            log.error("[RAG] error", e);
            throw new RuntimeException("Failed to generate RAG response", e);
        }
    }

    /**
     * Chat-Service 전용: reportId를 지정하여 보고서 생성 (비동기)
     * PENDING 보고서는 이미 생성되어 있으므로 바로 분석 시작
     */
    @Async
    @Transactional
    public void analyzeConversationWithReportIdAsync(ChatRagRequestDto requestDto) {

        final int K = 3;
        final long t0 = System.nanoTime();
        log.info("[RAG][CHAT] ===== START: Chat-Service Report Generation (ASYNC) =====");
        log.info("[RAG][CHAT] Input parameters - reportId: {}, user1Id: '{}', user2Id: '{}', messages: {}",
                requestDto.getReportId(), requestDto.getUser1Id(), requestDto.getUser2Id(),
                requestDto.getChatData().size());

        try {
            // user1Name, user2Name 추출 (chatData의 메시지에서)
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
            log.info("[RAG][CHAT] Extracted names - user1Name: '{}', user2Name: '{}'", user1Name, user2Name);

            // RAG 분석 시작
            ensureTrgmReady(dataSource);

            // Perform individual RAG search for each message
            List<Map<String, Object>> messagesWithRag = new ArrayList<>();
            for (int i = 0; i < requestDto.getChatData().size(); i++) {
                ChatMessageDto message = requestDto.getChatData().get(i);
                log.info("[RAG] Processing message {}/{}: {}", i + 1, requestDto.getChatData().size(),
                        message.getMessage());

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
            log.info("[RAG] done (sparse) | messages={} | {} ms", requestDto.getChatData().size(),
                    (t1 - t0) / 1_000_000);

            Map<String, Object> gptInput = new LinkedHashMap<>();
            gptInput.put("user1_id", requestDto.getUser1Id());
            gptInput.put("user2_id", requestDto.getUser2Id());
            gptInput.put("messages_with_rag", messagesWithRag);

            log.info("[RAG] GPT Input - messages_with_rag: {}", messagesWithRag);

            String inputJson = objectMapper.writeValueAsString(gptInput);
            String gptResponseJson = gptService.generateReport(inputJson);

            // Parse GPT response
            @SuppressWarnings("unchecked")
            Map<String, Object> gptResponse = objectMapper.readValue(gptResponseJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            String reportTitle = (String) gptResponse.get("report_title");
            log.info("[RAG][CHAT] GPT analysis completed - reportTitle: '{}'", reportTitle);

            List<Map<String, Object>> reportCardsRaw = (List<Map<String, Object>>) gptResponse.get("report_cards");
            String reportCardsJson = objectMapper.writeValueAsString(reportCardsRaw);
            List<ReportCardDto> reportCards = objectMapper.readValue(
                    reportCardsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReportCardDto.class));
            log.info("[RAG][CHAT] Parsed {} report cards from GPT response", reportCards.size());

            // user1Name, user2Name은 이미 메서드 시작 부분에서 추출됨

            // reportId를 직접 지정하여 저장 (네이티브 쿼리 사용하여 JPA IDENTITY 전략 충돌 회피)
            log.info("[RAG][CHAT] ===== Updating report to COMPLETED state =====");
            log.info("[RAG][CHAT] Upsert parameters - reportId: {}, user1Id: '{}', user2Id: '{}', title: '{}'",
                    requestDto.getReportId(), requestDto.getUser1Id(), requestDto.getUser2Id(), reportTitle);

            String chatDataJson = objectMapper.writeValueAsString(requestDto.getChatData());

            // PENDING → COMPLETED 상태로 업데이트 (분석 결과 포함)
            conversationReportRepository.upsertReport(
                    requestDto.getReportId(),
                    requestDto.getUser1Id(),
                    user1Name,
                    requestDto.getUser2Id(),
                    user2Name,
                    reportTitle, // GPT가 생성한 제목으로 변경
                    chatDataJson,
                    reportCardsJson,
                    ReportState.COMPLETED.name(), // PENDING → COMPLETED
                    SourceType.CHAT.name(),
                    true); // isNameUpdated = true for chat reports

            log.info("[RAG][CHAT] Upsert completed successfully");

            // 저장된 엔티티를 다시 조회
            log.info("[RAG][CHAT] Retrieving saved report with reportId: {}", requestDto.getReportId());
            ConversationReport savedEntity = conversationReportRepository.findById(requestDto.getReportId())
                    .orElseThrow(() -> new RuntimeException(
                            "Failed to save report with reportId: " + requestDto.getReportId()));

            log.info("[RAG][CHAT] ===== SUCCESS: Report saved =====");
            log.info("[RAG][CHAT] Saved entity details - id: {}, user1Id: '{}', user2Id: '{}', title: '{}', state: {}",
                    savedEntity.getId(), savedEntity.getUser1Id(), savedEntity.getUser2Id(),
                    savedEntity.getTitle(), savedEntity.getState());

        } catch (Exception e) {
            log.error("[RAG][CHAT] Failed to generate RAG response for reportId: {}", requestDto.getReportId(), e);
            // 비동기 메서드이므로 예외를 던지지 않고 로그만 남김
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