package com.khi.chatservice.presentation.controller;

import com.khi.chatservice.presentation.dto.request.ReportCallbackRequestDto;
import com.khi.chatservice.redis.RagRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/chat/feign/rag")
@RequiredArgsConstructor
public class ChatFeignController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/callback")
    public void receiveRagCallback(@RequestBody ReportCallbackRequestDto request) {
        log.info("[CHAT-SERVICE] Received RAG callback for reportId: {}. Broadcasting via Redis...", request.getReportId());

        RagRedisMessage redisMessage = RagRedisMessage.builder()
                .type(request.getType())
                .reportId(request.getReportId())
                .targetUserIds(request.getTargetUserIds())
                .build();

        redisTemplate.convertAndSend("rag:completion", redisMessage);
    }
}
