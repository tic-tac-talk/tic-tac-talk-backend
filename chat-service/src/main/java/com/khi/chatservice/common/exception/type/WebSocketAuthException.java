package com.khi.chatservice.common.exception.type;

import lombok.Getter;

@Getter
public class WebSocketAuthException extends RuntimeException {
    private final String errorCode;

    public WebSocketAuthException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebSocketAuthException(Throwable cause, String errorCode, String message) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}