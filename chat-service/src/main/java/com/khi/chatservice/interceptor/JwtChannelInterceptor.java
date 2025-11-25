package com.khi.chatservice.interceptor;

import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
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

    public JwtChannelInterceptor(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public Message<?> preSend(@NotNull Message<?> msg, @NotNull MessageChannel ch) {
        log.info("🔍 JwtChannelInterceptor 실행됨");

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(msg);
        log.info("📩 STOMP Command: {}", acc.getCommand());

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            log.info("🔗 WebSocket CONNECT 처리 시작");

            String userId = acc.getFirstNativeHeader("X-User-Id");

            // X-User-Id 헤더는 필수 (게이트웨이에서 JWT 검증 후 추가됨)
            if (userId == null || userId.isEmpty()) {
                log.error("❌ X-User-Id 헤더가 없습니다. 인증이 필요합니다.");
                throw new IllegalArgumentException("인증이 필요한 서비스입니다.");
            }

            log.info("👤 X-User-Id: {}", userId);

            // UserInfo 조회
            try {
                UserInfo user = userClient.getUserInfo(userId);
                log.info("👤 UserDetails 로드 완료 - username: {}", UserInfo.getName(user));
            } catch (Exception e) {
                log.error("⚠️ UserInfo 조회 실패: {}", userId, e);
                throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
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