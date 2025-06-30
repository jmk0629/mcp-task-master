package com.tofumaker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 * STOMP 프로토콜을 사용하여 실시간 메시징 기능을 제공합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     * 
     * @param config 메시지 브로커 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 수 있는 경로 설정
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // 클라이언트가 메시지를 보낼 때 사용할 경로 접두사
        config.setApplicationDestinationPrefixes("/app");
        
        // 사용자별 개인 메시지를 위한 접두사
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     * 
     * @param registry STOMP 엔드포인트 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (개발 환경용)
                .withSockJS(); // SockJS 폴백 지원
        
        // 순수 WebSocket 연결 (SockJS 없이)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
} 