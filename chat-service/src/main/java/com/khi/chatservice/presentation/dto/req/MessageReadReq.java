package com.khi.chatservice.presentation.dto.req;

public record MessageReadReq(
    String roomId,
    Long lastReadMessageId
) {} 