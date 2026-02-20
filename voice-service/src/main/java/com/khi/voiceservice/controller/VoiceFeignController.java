package com.khi.voiceservice.controller;

import com.khi.voiceservice.dto.ReportCallbackRequestDto;
import com.khi.voiceservice.redis.VoiceRagRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/voice/feign/rag")
@RequiredArgsConstructor
public class VoiceFeignController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/callback")
    public void receiveRagCallback(@RequestBody ReportCallbackRequestDto request) {
        log.info("[VOICE-SERVICE] Received RAG callback for reportId: {}. Broadcasting via Redis...",
                request.getReportId());

        VoiceRagRedisMessage redisMessage = VoiceRagRedisMessage.builder()
                .type(request.getType())
                .reportId(request.getReportId())
                .targetUserIds(request.getTargetUserIds())
                .build();

        redisTemplate.convertAndSend("voice:rag:completion", redisMessage);
    }
}
