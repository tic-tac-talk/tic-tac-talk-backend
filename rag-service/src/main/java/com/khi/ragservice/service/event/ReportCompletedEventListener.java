package com.khi.ragservice.service.event;

import com.khi.ragservice.dto.ReportCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportCompletedEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onReportCompleted(ReportCompletedEvent event) {
        Map<String, Object> payload = Map.of(
          "type","REPORT_COMPLETED",
          "reportId", event.getReportId()
        );

        // user 중복 방지
        Set<String> targets = Set.of(
                event.getRequestUserId1(),
                event.getRequestUserId2()
        );

        for (String userId : targets) {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notify",
                    payload
            );
        }
    }
}
