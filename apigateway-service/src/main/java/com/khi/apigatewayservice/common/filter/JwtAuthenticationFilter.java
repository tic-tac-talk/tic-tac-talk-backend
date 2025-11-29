package com.khi.apigatewayservice.common.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.apigatewayservice.common.api.ApiResponse;
import com.khi.apigatewayservice.common.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 로깅 필터 다음에 실행
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/signup",
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/security/jwt/reissue",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui/index.css",
            "/swagger-ui/swagger-ui-bundle.js",
            "/swagger-ui/swagger-ui-standalone-preset.js",
            "/swagger-ui/swagger-initializer.js",
            "/v3/api-docs/swagger-config",
            "/security/test",
            "/rag/feign/receive",
            "/reports",
            "/rag/receive",
            "/api/v1/voice/transcribe",
            "/api/v1/voice/callback",
            "/oauth2/authorization/kakao",
            "login/oauth2/code/kakao",
            "/api/v1/rag/feign/receive",
            "/api/v1/chat/swagger-ui",
            "/api/v1/chat/v3/api-docs",
            "/api/v1/chat/ws-chat",
            "/api/v1/chat/ws-chat/"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            log.info("[PUBLIC PATH] {}", path);
            return chain.filter(exchange);
        }

        String token = extractToken(request);

        if (token == null) {
            log.error("[NO TOKEN] {}", path);
            return onError(exchange, "토큰이 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            log.info("[JWT VALID] userId: {}, path: {}", userId, path);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            log.error("[JWT INVALID] {}", e.getMessage());
            return onError(exchange, "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.contains(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            ApiResponse<?> apiResponse = ApiResponse.error(message);
            String errorJson = objectMapper.writeValueAsString(apiResponse);

            return response.writeWith(
                    Mono.just(response.bufferFactory().wrap(errorJson.getBytes())));
        } catch (JsonProcessingException e) {
            log.error("[JSON PROCESSING ERROR] {}", e.getMessage());
            String fallbackJson = String.format(
                    "{\"status\":\"error\",\"message\":\"%s\",\"data\":null}",
                    message);
            return response.writeWith(
                    Mono.just(response.bufferFactory().wrap(fallbackJson.getBytes())));
        }
    }
}

