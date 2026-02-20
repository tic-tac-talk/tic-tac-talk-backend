package com.khi.chatservice.presentation.controller;

import com.khi.chatservice.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/chat/sse")
@RequiredArgsConstructor
public class ChatSseController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe/{reportId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable("reportId") Long reportId) {
        log.info("[CHAT-SERVICE] Received SSE subscription for reportId: {}", reportId);
        return sseService.subscribe(reportId);
    }
}
