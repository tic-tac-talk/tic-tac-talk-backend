package com.khi.apigatewayservice.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.logging.Logger;

@Slf4j
@Component
public class WebSocketLoggingFilter implements WebFilter {

    public WebSocketLoggingFilter() {
        log.info("WebSocketLoggingFilter Bean Loaded!!!");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        String upgrade = request.getHeaders().getFirst("Upgrade");

        if ("websocket".equalsIgnoreCase(upgrade)) {
            log.info("====== [WEBSOCKET HANDSHAKE] ======");
            log.info("URL        : {}", request.getURI());
            log.info("Method     : {}", request.getMethod());
            log.info("Headers:");
            request.getHeaders().forEach((k, v) -> log.info("  {}: {}", k, v));
            log.info("==================================");
        }

        return chain.filter(exchange);
    }
}
