package com.khi.securityservice.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.securityservice.common.api.ApiResponse;
import com.khi.securityservice.core.enumeration.JwtTokenType;
import com.khi.securityservice.core.principal.SecurityUserPrincipal;
import com.khi.securityservice.core.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        log.info("LoginSuccessHandler 실행");

        SecurityUserPrincipal userDetails = (SecurityUserPrincipal) authentication.getPrincipal();

        String uid = userDetails.getName();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();

        String accessToken = jwtUtil.createJwt(JwtTokenType.ACCESS, uid, role, 10_000L); // 10초
        String refreshToken = jwtUtil.createJwt(JwtTokenType.REFRESH, uid, role, 86_400_000L);

        redisTemplate.opsForValue().set(uid, refreshToken, 86_400_000L, TimeUnit.MILLISECONDS);

        log.info("Redis에 Refresh 토큰 저장 완료");

        String targetUrl = UriComponentsBuilder.fromUriString("https://tictactalk.studio/oauth/callback")
                .queryParam("access-token", accessToken)
                .build()
                .toUriString();

        addCookie(response, "refresh-token", refreshToken);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void addCookie(HttpServletResponse response, String key, String value) {

        ResponseCookie cookie = ResponseCookie.from(key, value)
                .maxAge(24 * 60 * 60)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
