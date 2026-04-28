package com.hl.hlaicodemother.manager.websocket;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private AppChatWebSocketHandler appChatWebSocketHandler;

    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(appChatWebSocketHandler, "/ws/app/chat")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
