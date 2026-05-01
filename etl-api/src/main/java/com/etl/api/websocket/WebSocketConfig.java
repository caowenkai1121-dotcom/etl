package com.etl.api.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 用于实时日志推送
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SyncLogWebSocketHandler syncLogWebSocketHandler;

    public WebSocketConfig(SyncLogWebSocketHandler syncLogWebSocketHandler) {
        this.syncLogWebSocketHandler = syncLogWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(syncLogWebSocketHandler, "/ws/sync-log")
            .setAllowedOrigins("*");
    }
}
