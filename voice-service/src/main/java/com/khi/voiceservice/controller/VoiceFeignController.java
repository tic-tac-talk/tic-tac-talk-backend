package com.khi.voiceservice.controller;

import com.khi.voiceservice.dto.ReportCallbackRequestDto;
import com.khi.voiceservice.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/voice/feign/rag")
@RequiredArgsConstructor
public class VoiceFeignController {

    private final SseService sseService;

    @PostMapping("/callback")
    public void receiveRagCallback(@RequestBody ReportCallbackRequestDto request) {
        log.info("[VOICE-SERVICE] Received RAG callback for reportId: {}", request.getReportId());

        // Notify through SSE
        sseService.notifyReportCompleted(request.getReportId());
    }
}
