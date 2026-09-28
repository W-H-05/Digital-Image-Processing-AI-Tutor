package com.aitutor.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：教师端学情实时推送
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LearningWebSocketHandler handler;
    private final LearningHandshakeInterceptor interceptor;

    public WebSocketConfig(LearningWebSocketHandler handler, LearningHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/learning")
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns("*");
    }
}
