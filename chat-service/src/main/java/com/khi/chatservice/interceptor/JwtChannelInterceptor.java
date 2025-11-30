package com.khi.chatservice.interceptor;

import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
                throw new IllegalArgumentException("Authorization header required");
            }

            // JWT 파싱
            String token = authHeader.substring(7);
            String userId = jwtTokenProvider.getUserIdFromToken(token);

            log.info("WebSocket 인증 성공: userId={}", userId);

            // Principal 설정
            acc.setUser(() -> userId);
            acc.getSessionAttributes().put("userId", userId);
        }

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            log.info("🔗 WebSocket CONNECT 처리 시작");

            // WebSocket 핸드셰이크 시 Gateway가 추가한 HTTP 헤더에서 추출
            String userId = null;

            // 1. Native header에서 시도 (STOMP 프레임에 포함된 경우)
            userId = acc.getFirstNativeHeader("X-User-Id");
            log.info("🔍 Native header X-User-Id: {}", userId);

            // 2. Handshake headers에서 추출 (WebSocket upgrade 시 HTTP 헤더)
            if (userId == null || userId.isEmpty()) {
                if (acc.getSessionAttributes() != null) {
                    var handshakeHeaders = acc.getSessionAttributes().get("simpSessionAttributes");
                    log.info("🔍 Handshake session attributes: {}", handshakeHeaders);
                }

                // SimpMessageHeaderAccessor에서 직접 추출
                var messageHeaders = msg.getHeaders();
                log.info("🔍 All message headers: {}", messageHeaders.keySet());

                // nativeHeaders에서 추출
                Object nativeHeaders = messageHeaders.get("nativeHeaders");
                log.info("🔍 Native headers object: {}", nativeHeaders);
            }

            // X-User-Id 헤더는 필수 (게이트웨이에서 JWT 검증 후 추가됨)
//            if (userId == null || userId.isEmpty()) {
//                log.error("❌ X-User-Id 헤더가 없습니다. 인증이 필요합니다.");
//                log.error("❌ Available headers: {}", acc.toNativeHeaderMap());
//                throw new IllegalArgumentException("인증이 필요한 서비스입니다.");
//            }

            log.info("👤 X-User-Id: {}", userId);

            // UserInfo 조회
            try {
                UserInfo user = userClient.getUserInfo(userId);
                log.info("👤 UserDetails 로드 완료 - username: {}", UserInfo.getName(user));
            } catch (Exception e) {
                log.error("⚠️ UserInfo 조회 실패: {}", userId, e);
                throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
            }

            String finalUserId = userId;
            Principal userPrincipal = () -> finalUserId;
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