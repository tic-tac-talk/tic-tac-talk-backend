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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;

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

            if (userId == null || userId.isEmpty()) {
                log.error("❌ X-User-Id 헤더가 없음");
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }

            log.info("👤 X-User-Id: {}", userId);

            UserInfo user = userClient.getUserInfo(userId);
            log.info("👤 UserDetails 로드 완료 - username: {}", UserInfo.getName(user));

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            acc.setUser(authToken);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            acc.getSessionAttributes().put("userId", userId);

            log.info("✅ WebSocket 인증 완료");
            log.info("   - Principal name: {}", authToken.getName());
            log.info("   - Session userId: {}", userId);
            log.info("   - 일치 여부: {}", userId.equals(authToken.getName()));

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