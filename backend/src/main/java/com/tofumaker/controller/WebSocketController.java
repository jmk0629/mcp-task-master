package com.tofumaker.controller;

import com.tofumaker.dto.WebSocketMessage;
import com.tofumaker.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 메시지 처리 컨트롤러
 */
@Controller
@Tag(name = "WebSocket", description = "실시간 메시징 API")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * 클라이언트 연결 확인 (Ping)
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    @Operation(summary = "연결 확인", description = "클라이언트와 서버 간 연결 상태를 확인합니다.")
    public WebSocketMessage ping(@Payload WebSocketMessage message, Principal principal) {
        logger.debug("Ping 수신: 사용자={}", principal != null ? principal.getName() : "익명");
        
        return WebSocketMessage.builder()
            .type(WebSocketMessage.MessageType.PONG)
            .content("pong")
            .addData("serverTime", LocalDateTime.now())
            .addData("originalMessage", message.getContent())
            .build();
    }

    /**
     * 채팅 메시지 처리
     */
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    @Operation(summary = "채팅 메시지", description = "채팅 메시지를 전체 사용자에게 브로드캐스트합니다.")
    public WebSocketMessage handleChatMessage(@Payload WebSocketMessage message, 
                                            SimpMessageHeaderAccessor headerAccessor,
                                            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "익명";
            
            logger.info("채팅 메시지 수신: 사용자={}, 내용={}", username, message.getContent());
            
            // 메시지에 발신자 정보 추가
            message.setSenderName(username);
            message.setTimestamp(LocalDateTime.now());
            
            return message;
            
        } catch (Exception e) {
            logger.error("채팅 메시지 처리 실패", e);
            return WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .content("메시지 처리에 실패했습니다.")
                .build();
        }
    }

    /**
     * 개인 메시지 처리
     */
    @MessageMapping("/private")
    @Operation(summary = "개인 메시지", description = "특정 사용자에게 개인 메시지를 전송합니다.")
    public void handlePrivateMessage(@Payload WebSocketMessage message, Principal principal) {
        try {
            String senderName = principal != null ? principal.getName() : "익명";
            
            logger.info("개인 메시지 수신: 발신자={}, 수신자={}, 내용={}", 
                       senderName, message.getRecipientId(), message.getContent());
            
            // 메시지에 발신자 정보 추가
            message.setSenderName(senderName);
            message.setTimestamp(LocalDateTime.now());
            
            // 수신자에게 메시지 전송
            if (message.getRecipientId() != null) {
                messagingTemplate.convertAndSendToUser(
                    message.getRecipientId().toString(),
                    "/queue/private",
                    message
                );
            }
            
        } catch (Exception e) {
            logger.error("개인 메시지 처리 실패", e);
        }
    }

    /**
     * 알림 읽음 처리
     */
    @MessageMapping("/notification/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    public void markNotificationAsRead(@Payload Map<String, Object> payload, Principal principal) {
        try {
            Long notificationId = Long.valueOf(payload.get("notificationId").toString());
            Long userId = principal != null ? Long.valueOf(principal.getName()) : null;
            
            if (userId != null) {
                boolean success = notificationService.markAsRead(notificationId, userId);
                
                WebSocketMessage response = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.NOTIFICATION)
                    .content(success ? "알림이 읽음 처리되었습니다." : "알림 읽음 처리에 실패했습니다.")
                    .recipientId(userId)
                    .addData("notificationId", notificationId)
                    .addData("success", success)
                    .build();
                
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications/response",
                    response
                );
                
                logger.info("알림 읽음 처리: 사용자={}, 알림ID={}, 성공={}", userId, notificationId, success);
            }
            
        } catch (Exception e) {
            logger.error("알림 읽음 처리 실패", e);
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @MessageMapping("/notification/readAll")
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    public void markAllNotificationsAsRead(Principal principal) {
        try {
            Long userId = principal != null ? Long.valueOf(principal.getName()) : null;
            
            if (userId != null) {
                int updatedCount = notificationService.markAllAsRead(userId);
                
                WebSocketMessage response = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.NOTIFICATION)
                    .content("모든 알림이 읽음 처리되었습니다.")
                    .recipientId(userId)
                    .addData("updatedCount", updatedCount)
                    .addData("success", true)
                    .build();
                
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications/response",
                    response
                );
                
                logger.info("모든 알림 읽음 처리: 사용자={}, 업데이트된 개수={}", userId, updatedCount);
            }
            
        } catch (Exception e) {
            logger.error("모든 알림 읽음 처리 실패", e);
        }
    }

    /**
     * 사용자 상태 업데이트
     */
    @MessageMapping("/user/status")
    @SendTo("/topic/user/status")
    @Operation(summary = "사용자 상태 업데이트", description = "사용자의 온라인 상태를 업데이트합니다.")
    public WebSocketMessage updateUserStatus(@Payload WebSocketMessage message, Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "익명";
            
            logger.info("사용자 상태 업데이트: 사용자={}, 상태={}", username, message.getContent());
            
            message.setSenderName(username);
            message.setTimestamp(LocalDateTime.now());
            
            return message;
            
        } catch (Exception e) {
            logger.error("사용자 상태 업데이트 실패", e);
            return WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .content("상태 업데이트에 실패했습니다.")
                .build();
        }
    }

    /**
     * 시스템 메시지 브로드캐스트
     */
    @MessageMapping("/system/broadcast")
    @SendTo("/topic/system")
    @Operation(summary = "시스템 메시지 브로드캐스트", description = "시스템 메시지를 모든 사용자에게 전송합니다.")
    public WebSocketMessage broadcastSystemMessage(@Payload WebSocketMessage message, Principal principal) {
        try {
            // 관리자 권한 확인 (실제 구현에서는 권한 체크 로직 추가)
            String username = principal != null ? principal.getName() : "시스템";
            
            logger.info("시스템 메시지 브로드캐스트: 발신자={}, 내용={}", username, message.getContent());
            
            message.setType(WebSocketMessage.MessageType.SYSTEM);
            message.setSenderName("시스템");
            message.setTimestamp(LocalDateTime.now());
            
            return message;
            
        } catch (Exception e) {
            logger.error("시스템 메시지 브로드캐스트 실패", e);
            return WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .content("시스템 메시지 전송에 실패했습니다.")
                .build();
        }
    }

    /**
     * 파일 업로드 진행률 업데이트
     */
    @MessageMapping("/file/progress")
    @Operation(summary = "파일 업로드 진행률", description = "파일 업로드 진행률을 업데이트합니다.")
    public void updateFileProgress(@Payload WebSocketMessage message, Principal principal) {
        try {
            Long userId = principal != null ? Long.valueOf(principal.getName()) : null;
            
            if (userId != null) {
                message.setType(WebSocketMessage.MessageType.FILE_PROGRESS);
                message.setRecipientId(userId);
                message.setTimestamp(LocalDateTime.now());
                
                messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/file/progress",
                    message
                );
                
                logger.debug("파일 업로드 진행률 업데이트: 사용자={}, 진행률={}%", 
                           userId, message.getData().get("progress"));
            }
            
        } catch (Exception e) {
            logger.error("파일 업로드 진행률 업데이트 실패", e);
        }
    }

    /**
     * 연결 상태 확인
     */
    @MessageMapping("/health")
    @SendToUser("/queue/health")
    @Operation(summary = "연결 상태 확인", description = "WebSocket 연결 상태를 확인합니다.")
    public WebSocketMessage healthCheck(Principal principal) {
        String username = principal != null ? principal.getName() : "익명";
        
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "healthy");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("user", username);
        healthData.put("connections", "active");
        
        return WebSocketMessage.builder()
            .type(WebSocketMessage.MessageType.SYSTEM)
            .content("연결 상태가 정상입니다.")
            .data(healthData)
            .build();
    }
} 