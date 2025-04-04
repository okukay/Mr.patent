package com.d208.mr_patent_backend.global.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 웹소켓 엔드포인트
        registry.addEndpoint("/api/ws/chat")
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS fallback 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 경로: /sub/room/1 (채팅방 1 구독)
        // /sub 라는 접두사만 표현, 그뒤는 자유롭게 표현 가능
        registry.enableSimpleBroker("/sub");

        // 메시지 보낼 때: /pub/chat/message
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
