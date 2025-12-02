package com.khi.securityservice.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.securityservice.common.api.ApiResponse;
import com.khi.securityservice.core.enumeration.JwtTokenType;
import com.khi.securityservice.core.exception.type.SecurityAuthenticationException;
import com.khi.securityservice.core.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtLogoutFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        return !(request.getRequestURI().equals("/security/logout") && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("JwtLogoutFilter 실행");

        String refreshToken = null;

        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("refresh-token")) {

                refreshToken = cookie.getValue();
            }
        }

        if (refreshToken == null) {

            throw new SecurityAuthenticationException("리프레시 토큰이 존재하지 않습니다.");
        }

        try {

            jwtUtil.isExpired(refreshToken);

        } catch (ExpiredJwtException e) {

            throw new SecurityAuthenticationException("리프레시 토큰이 만료되었습니다.");
        }

        JwtTokenType tokenType = jwtUtil.getTokenType(refreshToken);

        if (tokenType != JwtTokenType.REFRESH) {

            throw new SecurityAuthenticationException("토큰 타입이 리프레시 타입과 일치하지 않습니다.");
        }

        String uid = jwtUtil.getUid(refreshToken);
        log.info("[LOGOUT] 추출된 uid: {}", uid);

        Object redisRefreshToken = redisTemplate.opsForValue().get(uid);
        log.info("[LOGOUT] Redis에서 조회한 토큰: {}", redisRefreshToken);
        log.info("[LOGOUT] 쿠키에서 받은 토큰: {}", refreshToken);

        if (redisRefreshToken == null) {
            log.error("[LOGOUT] Redis에 uid '{}'에 대한 토큰이 존재하지 않습니다.", uid);
            throw new SecurityAuthenticationException("서버에 일치하는 리프레시 토큰이 존재하지 않습니다.");
        }

        if (!redisRefreshToken.toString().equals(refreshToken)) {
            log.error("[LOGOUT] 토큰 불일치 - Redis 토큰과 쿠키 토큰이 다릅니다.");
            log.error("[LOGOUT] Redis: {}", redisRefreshToken);
            log.error("[LOGOUT] Cookie: {}", refreshToken);
            throw new SecurityAuthenticationException("서버에 일치하는 리프레시 토큰이 존재하지 않습니다.");
        }

        log.info("Refresh 토큰 검증 완료");

        redisTemplate.delete(uid);

        log.info("Redis에서 Refresh 토큰 삭제 완료");

        Cookie cookie = new Cookie("refresh-token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");

        ApiResponse<?> apiResponse = ApiResponse.success();

        String jsonApiResponse = objectMapper.writeValueAsString(apiResponse);

        response.addCookie(cookie);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(jsonApiResponse);
    }
}
