package com.khi.chatservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RagRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RagRedisSubscriber(SimpMessagingTemplate messagingTemplate,
                              @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            RagRedisMessage ragMessage = objectMapper.readValue(body, RagRedisMessage.class);

            Map<String, Object> payload = Map.of(
                    "type", ragMessage.getType(),
                    "reportId", ragMessage.getReportId()
            );

            for (String userId : ragMessage.getTargetUserIds()) {
                log.info("[CHAT-SERVICE WS] Forwarding report completion to userId (via Redis PubSub): {}", userId);
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/notify",
                        payload
                );
            }

        } catch (Exception e) {
            log.error("Failed to process RAG Redis message: {}", e.getMessage(), e);
        }
    }
}
