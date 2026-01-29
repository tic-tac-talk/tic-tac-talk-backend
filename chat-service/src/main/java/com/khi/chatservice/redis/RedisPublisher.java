package com.khi.chatservice.redis;

import com.khi.chatservice.domain.entity.SocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;

    public void publish(SocketEventType eventType, String destination, Object payload) {
        RedisChatMessage message = RedisChatMessage.builder()
                .eventType(eventType)
                .destination(destination)
                .payload(payload)
                .build();

        redisTemplate.convertAndSend(chatTopic.getTopic(), message);
        log.debug("Redis publish - eventType: {}, destination: {}", eventType, destination);
    }
}
