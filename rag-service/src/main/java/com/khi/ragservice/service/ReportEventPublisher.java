package com.khi.ragservice.service;

import com.khi.ragservice.dto.ReportCompletedEvent;
import com.khi.ragservice.entity.ConversationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReportEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishReportCompleted(ConversationReport report) {
        eventPublisher.publishEvent(
                new ReportCompletedEvent(
                        report.getId(),
                        report.getUser1Id(),
                        report.getUser2Id(),
                        report.getSourceType()));

        log.info("[RAG] 레포트 분석 완료 이벤트 발생");
    }
}
