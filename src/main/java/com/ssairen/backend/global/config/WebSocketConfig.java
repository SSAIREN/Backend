package com.ssairen.backend.global.config;

import com.ssairen.backend.domain.callsession.controller.VictimWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final VictimWebSocketHandler victimWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            VictimWebSocketHandler victimWebSocketHandler,
            @Value("${ssairen.websocket.allowed-origins:*}") String allowedOrigins
    ) {
        this.victimWebSocketHandler = victimWebSocketHandler;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(victimWebSocketHandler, "/ws/v1/victim")
                .setAllowedOrigins(allowedOrigins);
    }
}
