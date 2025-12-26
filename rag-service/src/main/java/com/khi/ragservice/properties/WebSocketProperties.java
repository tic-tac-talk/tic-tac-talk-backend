package com.khi.ragservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tictactalk.websocket")
public class WebSocketProperties {
    private String endpoint = "/ws-report";
}
