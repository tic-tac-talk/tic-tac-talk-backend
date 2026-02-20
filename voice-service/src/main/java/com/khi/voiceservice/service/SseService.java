package com.khi.voiceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long reportId) {
        // 60 minutes timeout
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        emitters.put(reportId, emitter);

        emitter.onCompletion(() -> {
            log.info("[SSE] Emitter completed for reportId: {}", reportId);
            emitters.remove(reportId);
        });
        emitter.onTimeout(() -> {
            log.warn("[SSE] Emitter timeout for reportId: {}", reportId);
            emitter.complete();
            emitters.remove(reportId);
        });
        emitter.onError((e) -> {
            log.error("[SSE] Emitter error for reportId: {}", reportId, e);
            emitter.completeWithError(e);
            emitters.remove(reportId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully for reportId: " + reportId));
        } catch (IOException e) {
            log.error("[SSE] Failed to send initial connect event for reportId: {}", reportId, e);
            emitter.completeWithError(e);
            emitters.remove(reportId);
        }

        return emitter;
    }

    public void notifyReportCompleted(Long reportId) {
        SseEmitter emitter = emitters.get(reportId);
        if (emitter != null) {
            try {
                log.info("[SSE] Sending REPORT_COMPLETED event for reportId: {}", reportId);
                emitter.send(SseEmitter.event()
                        .name("REPORT_COMPLETED")
                        .data("{\"status\": \"COMPLETED\", \"reportId\": " + reportId + "}"));
                // Once notified, we can complete the emitter
                emitter.complete();
            } catch (IOException e) {
                log.error("[SSE] Failed to send REPORT_COMPLETED event for reportId: {}", reportId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(reportId);
            }
        } else {
            log.warn("[SSE] No active emitter found for reportId: {}", reportId);
        }
    }
}
