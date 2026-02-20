package com.khi.chatservice.presentation.controller;

import com.khi.chatservice.presentation.dto.request.ReportCallbackRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat/feign/rag")
@RequiredArgsConstructor
public class ChatFeignController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/callback")
    public void receiveRagCallback(@RequestBody ReportCallbackRequestDto request) {
        log.info("[CHAT-SERVICE] Received RAG callback for reportId: {}", request.getReportId());

        Map<String, Object> payload = Map.of(
                "type", request.getType(),
                "reportId", request.getReportId()
        );

        for (String userId : request.getTargetUserIds()) {
            log.info("[CHAT-SERVICE WS] Forwarding report completion to userId: {}", userId);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notify",
                    payload
            );
        }
    }
}
