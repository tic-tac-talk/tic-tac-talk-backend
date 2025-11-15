package com.khi.chatservice.common.exception.type;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
