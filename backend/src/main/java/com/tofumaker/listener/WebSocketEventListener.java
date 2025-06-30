package com.tofumaker.listener;

import com.tofumaker.dto.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket 이벤트 리스너
 * 사용자 연결/해제 이벤트를 처리하고 상태를 관리합니다.
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // 활성 사용자 세션 관리
    private final ConcurrentMap<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 이벤트 처리
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "익명";

            // 사용자 세션 정보 저장
            UserSession userSession = new UserSession(sessionId, username, LocalDateTime.now());
            activeSessions.put(sessionId, userSession);

            logger.info("WebSocket 연결: 세션ID={}, 사용자={}, 총 연결 수={}", 
                       sessionId, username, activeSessions.size());

            // 연결 알림 브로드캐스트
            WebSocketMessage connectMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.USER_STATUS)
                .content(username + "님이 접속했습니다.")
                .senderName(username)
                .addData("action", "connect")
                .addData("sessionId", sessionId)
                .addData("totalConnections", activeSessions.size())
                .build();

            messagingTemplate.convertAndSend("/topic/user/status", connectMessage);

            // 개인 환영 메시지
            WebSocketMessage welcomeMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.SYSTEM)
                .content("TofuMaker에 오신 것을 환영합니다!")
                .addData("connectedAt", LocalDateTime.now())
                .addData("sessionId", sessionId)
                .build();

            messagingTemplate.convertAndSendToUser(username, "/queue/welcome", welcomeMessage);

        } catch (Exception e) {
            logger.error("WebSocket 연결 이벤트 처리 실패", e);
        }
    }

    /**
     * WebSocket 연결 해제 이벤트 처리
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            // 세션 정보 조회 및 제거
            UserSession userSession = activeSessions.remove(sessionId);
            
            if (userSession != null) {
                String username = userSession.getUsername();
                LocalDateTime connectedAt = userSession.getConnectedAt();
                LocalDateTime disconnectedAt = LocalDateTime.now();

                logger.info("WebSocket 연결 해제: 세션ID={}, 사용자={}, 연결 시간={}, 총 연결 수={}", 
                           sessionId, username, 
                           java.time.Duration.between(connectedAt, disconnectedAt).toMinutes() + "분",
                           activeSessions.size());

                // 연결 해제 알림 브로드캐스트
                WebSocketMessage disconnectMessage = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.USER_STATUS)
                    .content(username + "님이 접속을 종료했습니다.")
                    .senderName(username)
                    .addData("action", "disconnect")
                    .addData("sessionId", sessionId)
                    .addData("totalConnections", activeSessions.size())
                    .addData("connectionDuration", java.time.Duration.between(connectedAt, disconnectedAt).toMinutes())
                    .build();

                messagingTemplate.convertAndSend("/topic/user/status", disconnectMessage);
            } else {
                logger.warn("연결 해제된 세션 정보를 찾을 수 없음: 세션ID={}", sessionId);
            }

        } catch (Exception e) {
            logger.error("WebSocket 연결 해제 이벤트 처리 실패", e);
        }
    }

    /**
     * 활성 세션 수 조회
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 특정 사용자의 활성 세션 조회
     */
    public UserSession getUserSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 모든 활성 세션 조회
     */
    public ConcurrentMap<String, UserSession> getAllActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * 특정 사용자의 모든 세션 조회
     */
    public long getSessionCountByUsername(String username) {
        return activeSessions.values().stream()
                .filter(session -> username.equals(session.getUsername()))
                .count();
    }

    /**
     * 시스템 상태 브로드캐스트
     */
    public void broadcastSystemStatus() {
        try {
            WebSocketMessage statusMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.SYSTEM)
                .content("시스템 상태 업데이트")
                .addData("totalConnections", activeSessions.size())
                .addData("timestamp", LocalDateTime.now())
                .addData("status", "healthy")
                .build();

            messagingTemplate.convertAndSend("/topic/system/status", statusMessage);
            
        } catch (Exception e) {
            logger.error("시스템 상태 브로드캐스트 실패", e);
        }
    }

    /**
     * 사용자 세션 정보 클래스
     */
    public static class UserSession {
        private final String sessionId;
        private final String username;
        private final LocalDateTime connectedAt;

        public UserSession(String sessionId, String username, LocalDateTime connectedAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.connectedAt = connectedAt;
        }

        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public LocalDateTime getConnectedAt() { return connectedAt; }

        @Override
        public String toString() {
            return String.format("UserSession{sessionId='%s', username='%s', connectedAt=%s}", 
                               sessionId, username, connectedAt);
        }
    }
} 