package com.khi.chatservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.chatservice.presentation.dto.SocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            RedisChatMessage chatMessage = objectMapper.readValue(body, RedisChatMessage.class);

            SocketEvent<?> socketEvent = new SocketEvent<>(
                    chatMessage.getEventType(),
                    chatMessage.getPayload()
            );

            messagingTemplate.convertAndSend(chatMessage.getDestination(), socketEvent);
            log.debug("Redis subscribe - eventType: {}, destination: {}",
                    chatMessage.getEventType(), chatMessage.getDestination());

        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", e.getMessage(), e);
        }
    }
}
