package com.khi.ragservice.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Slf4j
@Component
public class WsSessionListener {

    // ws 연결 시 로그 출력
    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

        if (accessor == null) return;

        Object userId = accessor.getSessionAttributes().get("userId");

        log.info("[RAG WS] userName = {} 과 ws 연결됨", userId);
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

        if (accessor == null) return;

        Object userId = accessor.getSessionAttributes().get("userId");
        String destination = accessor.getDestination();

        log.info("[RAG WS]");
        log.info(" - userId : {}", userId);
        log.info(" - destination : {}", destination);
    }

    // ws 연결 해제 시 로그 출력
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        log.info("[RAG WS] principal = {} 과 ws 연결 해제됨.", sessionId);
    }
}
