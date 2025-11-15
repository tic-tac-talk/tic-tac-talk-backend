package com.khi.chatservice.presentation.dto.req;

public record SendMessageReq(
        Long roomId,
        String message
) {
}
