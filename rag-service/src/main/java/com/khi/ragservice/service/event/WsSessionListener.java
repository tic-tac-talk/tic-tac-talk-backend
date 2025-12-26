package com.khi.ragservice.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
public class WsSessionListener {

    // ws 연결 시 로그 출력
    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        log.info("[RAG WS CONNECT] principal = {} 과 ws 연결됨",
                principal != null ? principal.getName() : "null");
    }

    // ws 연결 해제 시 로그 출력
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        log.info("[RAG WS DISCONNECT] principal = {} 과 ws 연결 해제됨.",
                principal != null ? principal.getName() : "null");
    }
}
