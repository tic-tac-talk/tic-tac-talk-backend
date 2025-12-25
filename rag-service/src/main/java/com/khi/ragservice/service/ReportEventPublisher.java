package com.khi.ragservice.service;

import com.khi.ragservice.dto.ReportCompletedEvent;
import com.khi.ragservice.entity.ConversationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ReportEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishReportCompleted(ConversationReport report) {
        eventPublisher.publishEvent(
                new ReportCompletedEvent(
                        report.getId(),
                        report.getUser1Id(),
                        report.getUser2Id()
                )
        );
    }
}
