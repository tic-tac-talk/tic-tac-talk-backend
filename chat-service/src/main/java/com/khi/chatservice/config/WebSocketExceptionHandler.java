package com.khi.chatservice.config;

import com.khi.chatservice.common.exception.type.WebSocketAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class WebSocketExceptionHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof WebSocketAuthException) {
            WebSocketAuthException authException = (WebSocketAuthException) cause;
            log.error("[WebSocket Auth Error] message: {}, errorCode: {}",
                    authException.getMessage(), authException.getErrorCode());

            return prepareErrorMessage(authException.getMessage(), authException.getErrorCode());
        }

        log.error("[WebSocket Error] Unhandled exception: {}", ex.getMessage(), ex);
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> prepareErrorMessage(String message, String errorCode) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(message);
        accessor.setNativeHeader("error-code", errorCode);
        accessor.setLeaveMutable(true);

        String errorMessage = String.format("{\"message\":\"%s\",\"errorCode\":\"%s\"}", message, errorCode);
        byte[] bytes = errorMessage.getBytes(StandardCharsets.UTF_8);

        return MessageBuilder.createMessage(bytes, accessor.getMessageHeaders());
    }
}