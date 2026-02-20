package com.khi.ragservice.service.event;

import com.khi.ragservice.client.ChatServiceClient;
import com.khi.ragservice.client.VoiceServiceClient;
import com.khi.ragservice.dto.ReportCallbackDto;
import com.khi.ragservice.dto.ReportCompletedEvent;
import com.khi.ragservice.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportCompletedEventListener {

    private final ChatServiceClient chatServiceClient;
    private final VoiceServiceClient voiceServiceClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCompleted(ReportCompletedEvent event) {
        log.info("[RAG] 리포트: {} 분석 완료 이벤트 확인, SourceType: {}", event.getReportId(), event.getSourceType());

        // user 중복 방지
        Set<String> targets = Set.of(
                event.getRequestUserId1(),
                event.getRequestUserId2());

        ReportCallbackDto payload = ReportCallbackDto.builder()
                .type("REPORT_COMPLETED")
                .reportId(event.getReportId())
                .targetUserIds(targets)
                .build();

        for (String userId : targets) {
            log.info("[RAG] 콜백 수신 대기 중인 userId: {}", userId);
        }

        if (event.getSourceType() == SourceType.CHAT) {
            log.info("[RAG] Sending callback to Chat-Service for reportId: {}", event.getReportId());
            chatServiceClient.sendReportCompletedCallback(payload);
        } else if (event.getSourceType() == SourceType.VOICE) {
            log.info("[RAG] Sending callback to Voice-Service for reportId: {}", event.getReportId());
            voiceServiceClient.sendReportCompletedCallback(payload);
        }
    }
}
