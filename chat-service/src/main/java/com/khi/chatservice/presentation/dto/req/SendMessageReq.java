package com.khi.chatservice.presentation.dto.req;

public record SendMessageReq(
        String roomId,
        String message
) {
}
