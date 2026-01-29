package com.khi.chatservice.redis;

import com.khi.chatservice.domain.entity.SocketEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisChatMessage implements Serializable {
    
    private SocketEventType eventType;
    private String destination;
    private Object payload;
}
