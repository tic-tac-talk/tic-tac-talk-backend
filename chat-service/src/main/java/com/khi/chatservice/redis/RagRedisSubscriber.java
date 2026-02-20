package com.khi.chatservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.chatservice.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RagRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseService sseService;

    public RagRedisSubscriber(@Qualifier("redisObjectMapper") ObjectMapper objectMapper,
                              SseService sseService) {
        this.objectMapper = objectMapper;
        this.sseService = sseService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            RagRedisMessage ragMessage = objectMapper.readValue(body, RagRedisMessage.class);

            log.info("[CHAT-SERVICE SSE] Received report completion from Redis PubSub for reportId: {}", ragMessage.getReportId());
            
            // Forward the notification to our active SSE connections
            // SseService will gracefully handle it if the client isn't connected to *this* specific pod.
            sseService.notifyReportCompleted(ragMessage.getReportId());

        } catch (Exception e) {
            log.error("Failed to process RAG Redis message: {}", e.getMessage(), e);
        }
    }
}
