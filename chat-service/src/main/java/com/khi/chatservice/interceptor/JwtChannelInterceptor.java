package com.khi.chatservice.interceptor;

import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.domain.entity.SocketEventType;
import com.khi.chatservice.presentation.dto.SocketEvent;
import com.khi.chatservice.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final UserClient userClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final SimpMessagingTemplate messagingTemplate;


    public JwtChannelInterceptor(UserClient userClient,  JwtTokenProvider jwtTokenProvider, @Lazy SimpMessagingTemplate messagingTemplate) {
        this.userClient = userClient;
        this.jwtTokenProvider = jwtTokenProvider;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message<?> preSend(@NotNull Message<?> msg, @NotNull MessageChannel ch) {
        log.info("🔍 JwtChannelInterceptor 실행됨");

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(msg);
        log.info("📩 STOMP Command: {}", acc.getCommand());

        if (StompCommand.CONNECT.equals(acc.getCommand())) {

            log.info("CONNECT received");

            String authHeader = acc.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Authorization header required");
            }

            // JWT 파싱
            String token = authHeader.substring(7);
            String userId = jwtTokenProvider.getUserIdFromToken(token);

            log.info("WebSocket 인증 성공: userId={}", userId);

            // UserInfo 조회
            try {
                UserInfo user = userClient.getUserInfo(userId);
                log.info("👤 UserDetails 로드 완료 - username: {}", UserInfo.getNickname(user));
            } catch (Exception e) {
                log.error("⚠️ UserInfo 조회 실패: {}", userId, e);
                throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
            }

            Principal userPrincipal = () -> userId;
            acc.setUser(userPrincipal);

            acc.getSessionAttributes().put("userId", userId);
            acc.getSessionAttributes().put("token", token);

            log.info("✅ WebSocket 인증 완료");
            log.info("   - Principal name: {}", userPrincipal.getName());
            log.info("   - Session userId: {}", userId);

        } else if (StompCommand.DISCONNECT.equals(acc.getCommand())) {
            log.info("🔌 WebSocket DISCONNECT");
        } else if (StompCommand.SEND.equals(acc.getCommand())) {
            log.debug("📤 STOMP SEND: {}", acc.getDestination());

            String token = (String) acc.getSessionAttributes().get("token");
            String userId = (String) acc.getSessionAttributes().get("userId");

            if (token != null && jwtTokenProvider.isTokenExpired(token)) {
                log.warn("⚠️ Token expired for user: {}", userId);
                sendTokenExpiredEvent(userId);
                return null;
            }
        } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            log.info("📥 STOMP SUBSCRIBE: {}", acc.getDestination());
        } else if (StompCommand.UNSUBSCRIBE.equals(acc.getCommand())) {
            log.info("📤 STOMP UNSUBSCRIBE: {}", acc.getDestination());
        }

        return msg;
    }

    private void sendTokenExpiredEvent(String userId) {
        String topicDestination = "/topic/user-room-updates/" + userId;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.TOKEN_EXPIRED,
                            new java.util.HashMap<String, String>() {{
                                put("message", "액세스 토큰이 만료되었습니다.");
                            }})
            );
            log.info("TOKEN_EXPIRED sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send TOKEN_EXPIRED to user {}: {}", userId, e.getMessage());
        }
    }
}