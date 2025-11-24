package com.khi.securityservice.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.securityservice.common.api.ApiResponse;
import com.khi.securityservice.core.enumeration.JwtTokenType;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class JwtReissueFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        return !(request.getRequestURI().equals("/security/jwt/reissue")
                && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("JwtReissueFilter 실행");

        try {
            String refreshToken = null;

            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("Refresh-Token")) {
                        refreshToken = cookie.getValue();
                    }
                }
            }

            // Refresh 토큰이 비어있는지 검증
            if (refreshToken == null) {
                sendErrorResponse(response, "리프레시 토큰이 존재하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            // Refresh 토큰 만료 여부 검증
            try {
                jwtUtil.isExpired(refreshToken);
            } catch (ExpiredJwtException e) {
                sendErrorResponse(response, "리프레시 토큰이 만료되었습니다.", "REFRESH_EXPIRED");
                return;
            }

            // Refresh 토큰 타입 검증
            JwtTokenType tokenType = jwtUtil.getTokenType(refreshToken);

            if (tokenType != JwtTokenType.REFRESH) {
                sendErrorResponse(response, "토큰 타입이 리프레시 타입과 일치하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            // DB에 Refresh 토큰이 존재하는지 검증
            String uid = jwtUtil.getUid(refreshToken);

            Object redisRefreshToken = redisTemplate.opsForValue().get(uid);

            if (redisRefreshToken == null || !redisRefreshToken.toString().equals(refreshToken)) {
                sendErrorResponse(response, "서버에 일치하는 리프레시 토큰이 존재하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            log.info("Refresh 토큰 검증 완료");

            String role = jwtUtil.getRole(refreshToken);

            String newAccessToken = jwtUtil.createJwt(JwtTokenType.ACCESS, uid, role, 600_000L);
            String newRefreshToken = jwtUtil.createJwt(JwtTokenType.REFRESH, uid, role, 86_400_000L);

            log.info("새로운 Access, Refresh 토큰 발급 완료");

            // Redis에 기존에 존재하는 Refresh 토큰 삭제
            redisTemplate.delete(String.valueOf(uid));

            log.info("Redis에서 기존 Refresh 토큰 삭제 완료");

            // Redis에 Refresh 토큰 저장
            String redisKey = String.valueOf(uid);

            log.info("Redis에 새로운 Refresh 토큰 저장 완료");

            redisTemplate.opsForValue().set(redisKey, newRefreshToken, 86_400_000L, TimeUnit.MILLISECONDS);

            ApiResponse<?> apiResponse = ApiResponse.success();

            String jsonApiResponse = objectMapper.writeValueAsString(apiResponse);

            response.setHeader("Access-Token", newAccessToken);
            response.addCookie(createCookie("Refresh-Token", newRefreshToken));

            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            response.getWriter().write(jsonApiResponse);

        } catch (Exception e) {
            log.error("JwtReissueFilter error: ", e);
            sendErrorResponse(response, "토큰 재발급 중 오류가 발생했습니다.", "REFRESH_INVALID");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message, String errorCode) throws IOException {
        ApiResponse<?> apiResponse = ApiResponse.error(message, errorCode);
        String jsonApiResponse = objectMapper.writeValueAsString(apiResponse);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(jsonApiResponse);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
