package com.khi.chatservice.interceptor;

import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.common.exception.type.WebSocketAuthException;
import com.khi.chatservice.util.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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


    public JwtChannelInterceptor(UserClient userClient,  JwtTokenProvider jwtTokenProvider) {
        this.userClient = userClient;
        this.jwtTokenProvider = jwtTokenProvider;
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
                log.error("[NO TOKEN] Authorization header required");
                throw new WebSocketAuthException("토큰이 없습니다.", "ACCESS_INVALID");
            }

            // JWT 검증 및 파싱
            String token = authHeader.substring(7);
            final String userId;

            try {
                userId = jwtTokenProvider.getUserIdFromToken(token);
                log.info("[JWT VALID] userId: {}", userId);

            } catch (ExpiredJwtException e) {
                log.error("[JWT EXPIRED] 만료된 토큰입니다. message: {}", e.getMessage());
                throw new WebSocketAuthException(e, "만료된 토큰입니다.", "ACCESS_EXPIRED");
            } catch (Exception e) {
                log.error("[JWT INVALID] 유효하지 않은 토큰입니다. message: {}", e.getMessage());
                throw new WebSocketAuthException(e, "유효하지 않은 토큰입니다.", "ACCESS_INVALID");
            }

            // UserInfo 조회
            try {
                UserInfo user = userClient.getUserInfo(userId);
                log.info("👤 UserDetails 로드 완료 - username: {}", UserInfo.getNickname(user));
            } catch (Exception e) {
                log.error("⚠️ UserInfo 조회 실패: {}", userId, e);
                throw new WebSocketAuthException(e, "유효하지 않은 사용자입니다.", "USER_INVALID");
            }

            Principal userPrincipal = () -> userId;
            acc.setUser(userPrincipal);

            acc.getSessionAttributes().put("userId", userId);

            log.info("✅ WebSocket 인증 완료");
            log.info("   - Principal name: {}", userPrincipal.getName());
            log.info("   - Session userId: {}", userId);

        } else if (StompCommand.DISCONNECT.equals(acc.getCommand())) {
            log.info("🔌 WebSocket DISCONNECT");
        } else if (StompCommand.SEND.equals(acc.getCommand())) {
            log.debug("📤 STOMP SEND: {}", acc.getDestination());
        } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
            log.info("📥 STOMP SUBSCRIBE: {}", acc.getDestination());
        } else if (StompCommand.UNSUBSCRIBE.equals(acc.getCommand())) {
            log.info("📤 STOMP UNSUBSCRIBE: {}", acc.getDestination());
        }

        return msg;
    }
}