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
            log.info("[REISSUE] ===== 토큰 재발급 시작 =====");
            String refreshToken = null;

            Cookie[] cookies = request.getCookies();
            log.info("[REISSUE] 쿠키 개수: {}", cookies != null ? cookies.length : 0);

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    log.info("[REISSUE] 쿠키 발견 - name: {}", cookie.getName());
                    if (cookie.getName().equals("refresh-token")) {
                        refreshToken = cookie.getValue();
                        log.info("[REISSUE] refresh-token 쿠키 발견: {}", refreshToken);
                    }
                }
            }

            // Refresh 토큰이 비어있는지 검증
            if (refreshToken == null) {
                log.error("[REISSUE] refresh-token 쿠키가 존재하지 않습니다.");
                sendErrorResponse(response, "리프레시 토큰이 존재하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            // Refresh 토큰 만료 여부 검증
            log.info("[REISSUE] 토큰 만료 여부 검증 시작");
            try {
                jwtUtil.isExpired(refreshToken);
                log.info("[REISSUE] 토큰 만료 검증 통과");
            } catch (ExpiredJwtException e) {
                log.error("[REISSUE] 토큰 만료됨: {}", e.getMessage());
                sendErrorResponse(response, "리프레시 토큰이 만료되었습니다.", "REFRESH_EXPIRED");
                return;
            }

            // Refresh 토큰 타입 검증
            log.info("[REISSUE] 토큰 타입 검증 시작");
            JwtTokenType tokenType = jwtUtil.getTokenType(refreshToken);
            log.info("[REISSUE] 추출된 토큰 타입: {}", tokenType);

            if (tokenType != JwtTokenType.REFRESH) {
                log.error("[REISSUE] 토큰 타입 불일치 - 기대: REFRESH, 실제: {}", tokenType);
                sendErrorResponse(response, "토큰 타입이 리프레시 타입과 일치하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            // DB에 Refresh 토큰이 존재하는지 검증
            String uid = jwtUtil.getUid(refreshToken);
            log.info("[REISSUE] 추출된 uid: {}", uid);

            Object redisRefreshToken = redisTemplate.opsForValue().get(uid);
            log.info("[REISSUE] Redis에서 조회한 토큰: {}", redisRefreshToken);
            log.info("[REISSUE] 쿠키에서 받은 토큰: {}", refreshToken);

            if (redisRefreshToken == null) {
                log.error("[REISSUE] Redis에 uid '{}'에 대한 토큰이 존재하지 않습니다.", uid);
                sendErrorResponse(response, "서버에 일치하는 리프레시 토큰이 존재하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            if (!redisRefreshToken.toString().equals(refreshToken)) {
                log.error("[REISSUE] 토큰 불일치 - Redis 토큰과 쿠키 토큰이 다릅니다.");
                log.error("[REISSUE] Redis: {}", redisRefreshToken);
                log.error("[REISSUE] Cookie: {}", refreshToken);
                sendErrorResponse(response, "서버에 일치하는 리프레시 토큰이 존재하지 않습니다.", "REFRESH_INVALID");
                return;
            }

            log.info("[REISSUE] Refresh 토큰 검증 완료");

            String role = jwtUtil.getRole(refreshToken);
            log.info("[REISSUE] 추출된 role: {}", role);

            log.info("[REISSUE] 새로운 토큰 생성 시작 - uid: {}, role: {}", uid, role);
            String newAccessToken = jwtUtil.createJwt(JwtTokenType.ACCESS, uid, role, 3_600_000L); // 1시간
            String newRefreshToken = jwtUtil.createJwt(JwtTokenType.REFRESH, uid, role, 86_400_000L);
            log.info("[REISSUE] 새로운 Access Token 생성 완료");
            log.info("[REISSUE] 새로운 Refresh Token 생성 완료");
            log.info("[REISSUE] 새 Access Token: {}", newAccessToken);
            log.info("[REISSUE] 새 Refresh Token: {}", newRefreshToken);

            // Redis에 기존에 존재하는 Refresh 토큰 삭제
            log.info("[REISSUE] Redis에서 기존 토큰 삭제 시작 - uid: {}", uid);
            redisTemplate.delete(String.valueOf(uid));
            log.info("[REISSUE] Redis에서 기존 Refresh 토큰 삭제 완료");

            // Redis에 Refresh 토큰 저장
            String redisKey = String.valueOf(uid);
            log.info("[REISSUE] Redis에 새로운 Refresh 토큰 저장 시작 - key: {}", redisKey);
            redisTemplate.opsForValue().set(redisKey, newRefreshToken, 86_400_000L, TimeUnit.MILLISECONDS);
            log.info("[REISSUE] Redis에 새로운 Refresh 토큰 저장 완료");

            ApiResponse<?> apiResponse = ApiResponse.success();

            String jsonApiResponse = objectMapper.writeValueAsString(apiResponse);

            log.info("[REISSUE] 응답 헤더에 access-token 추가");
            response.setHeader("access-token", newAccessToken);
            log.info("[REISSUE] 응답 쿠키에 refresh-token 추가");
            response.addCookie(createCookie("refresh-token", newRefreshToken));

            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            response.getWriter().write(jsonApiResponse);

            log.info("[REISSUE] ===== 토큰 재발급 성공 =====");

        } catch (Exception e) {
            log.error("[REISSUE] ===== 토큰 재발급 실패 =====");
            log.error("[REISSUE] 에러 타입: {}", e.getClass().getName());
            log.error("[REISSUE] 에러 메시지: {}", e.getMessage());
            log.error("[REISSUE] 상세 스택 트레이스: ", e);
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
