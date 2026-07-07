package com.momogo.realtime.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // ws://localhost:8080/ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*"); // CORS 임시 허용
        // TODO: 배포 전 `setAllowedOriginPatterns("https://app.example.com")` 등으로 변경
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 구독(수신)용 프리픽스
        registry.enableSimpleBroker("/sub");

        // 송신(발행)용 프리픽스
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        // JWT 검증 인터셉터 등록
        registration.interceptors(jwtChannelInterceptor);
    }
}
