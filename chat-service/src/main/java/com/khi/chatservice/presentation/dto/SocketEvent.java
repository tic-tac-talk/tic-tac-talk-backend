package com.khi.chatservice.presentation.dto;

import com.khi.chatservice.domain.entity.SocketEventType;

public record SocketEvent<T> (
        SocketEventType type,
        T content
) {
}
