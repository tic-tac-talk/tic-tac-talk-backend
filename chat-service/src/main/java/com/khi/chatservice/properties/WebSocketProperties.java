package com.khi.chatservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jamjam.websocket")
public class WebSocketProperties {
    private String endpoint = "/ws-chat";
}
