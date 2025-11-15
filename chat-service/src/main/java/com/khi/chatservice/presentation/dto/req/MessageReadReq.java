package com.khi.chatservice.presentation.dto.req;

public record MessageReadReq(
    Long roomId,
    Long lastReadMessageId
) {} 