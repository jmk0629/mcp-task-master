package com.tofumaker.service;

import com.tofumaker.dto.WebSocketMessage;
import com.tofumaker.entity.Notification;
import com.tofumaker.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 알림 서비스
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 알림 생성 및 실시간 전송
     */
    @Transactional
    public Notification createNotification(Notification notification) {
        try {
            // 알림 저장
            Notification savedNotification = notificationRepository.save(notification);
            
            // 실시간 알림 전송
            sendRealTimeNotification(savedNotification);
            
            logger.info("알림 생성 완료: ID={}, 수신자={}, 타입={}", 
                       savedNotification.getId(), savedNotification.getRecipientId(), savedNotification.getType());
            
            return savedNotification;
        } catch (Exception e) {
            logger.error("알림 생성 실패", e);
            throw new RuntimeException("알림 생성에 실패했습니다.", e);
        }
    }

    /**
     * 알림 생성 (빌더 패턴)
     */
    @Transactional
    public Notification createNotification(Notification.NotificationType type, String title, 
                                         String content, Long recipientId) {
        Notification notification = new Notification(type, title, content, recipientId);
        return createNotification(notification);
    }

    /**
     * 알림 생성 (상세 정보 포함)
     */
    @Transactional
    public Notification createNotification(Notification.NotificationType type, String title, 
                                         String content, Long recipientId, Long senderId, 
                                         Long referenceId, String referenceType) {
        Notification notification = new Notification(type, title, content, recipientId);
        notification.setSenderId(senderId);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        return createNotification(notification);
    }

    /**
     * 사용자별 알림 조회 (페이징)
     */
    public Page<Notification> getNotificationsByRecipientId(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    /**
     * 사용자별 읽지 않은 알림 조회 (페이징)
     */
    public Page<Notification> getUnreadNotificationsByRecipientId(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId, pageable);
    }

    /**
     * 사용자별 읽지 않은 알림 개수
     */
    public long getUnreadNotificationCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    /**
     * 알림 타입별 조회
     */
    public Page<Notification> getNotificationsByType(Long recipientId, Notification.NotificationType type, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(recipientId, type, pageable);
    }

    /**
     * 기간별 알림 조회
     */
    public Page<Notification> getNotificationsByDateRange(Long recipientId, LocalDateTime startDate, 
                                                         LocalDateTime endDate, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndDateRange(recipientId, startDate, endDate, pageable);
    }

    /**
     * 알림 상세 조회
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long recipientId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            if (notification.getRecipientId().equals(recipientId)) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
                
                // 실시간으로 읽음 상태 업데이트 전송
                sendNotificationStatusUpdate(recipientId, notificationId, true);
                
                logger.info("알림 읽음 처리: ID={}, 수신자={}", notificationId, recipientId);
                return true;
            }
        }
        return false;
    }

    /**
     * 여러 알림 읽음 처리
     */
    @Transactional
    public int markAsRead(List<Long> notificationIds, Long recipientId) {
        int updatedCount = notificationRepository.markAsReadByIds(notificationIds, recipientId, LocalDateTime.now());
        
        if (updatedCount > 0) {
            // 실시간으로 읽음 상태 업데이트 전송
            sendBulkNotificationStatusUpdate(recipientId, notificationIds, true);
            logger.info("알림 일괄 읽음 처리: 개수={}, 수신자={}", updatedCount, recipientId);
        }
        
        return updatedCount;
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long recipientId) {
        int updatedCount = notificationRepository.markAllAsReadByRecipientId(recipientId, LocalDateTime.now());
        
        if (updatedCount > 0) {
            // 실시간으로 전체 읽음 상태 업데이트 전송
            sendAllNotificationsReadUpdate(recipientId);
            logger.info("모든 알림 읽음 처리: 개수={}, 수신자={}", updatedCount, recipientId);
        }
        
        return updatedCount;
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long recipientId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            if (notification.getRecipientId().equals(recipientId)) {
                notificationRepository.delete(notification);
                
                // 실시간으로 삭제 알림 전송
                sendNotificationDeleteUpdate(recipientId, notificationId);
                
                logger.info("알림 삭제: ID={}, 수신자={}", notificationId, recipientId);
                return true;
            }
        }
        return false;
    }

    /**
     * 사용자별 알림 통계
     */
    public Map<String, Long> getNotificationStats(Long recipientId) {
        List<Object[]> stats = notificationRepository.getNotificationStatsByRecipientId(recipientId);
        Map<String, Long> result = new HashMap<>();
        
        for (Object[] stat : stats) {
            Notification.NotificationType type = (Notification.NotificationType) stat[0];
            Long count = (Long) stat[1];
            result.put(type.name(), count);
        }
        
        return result;
    }

    /**
     * 최근 알림 조회 (N일간)
     */
    public List<Notification> getRecentNotifications(Long recipientId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentNotifications(recipientId, since);
    }

    /**
     * 실시간 알림 전송
     */
    @Async
    public CompletableFuture<Void> sendRealTimeNotification(Notification notification) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .content(notification.getContent())
                .senderId(notification.getSenderId())
                .recipientId(notification.getRecipientId())
                .addData("notificationId", notification.getId())
                .addData("notificationType", notification.getType())
                .addData("title", notification.getTitle())
                .addData("referenceId", notification.getReferenceId())
                .addData("referenceType", notification.getReferenceType())
                .build();

            // 개인 알림 전송
            messagingTemplate.convertAndSendToUser(
                notification.getRecipientId().toString(),
                "/queue/notifications",
                message
            );

            // 전체 알림 채널에도 전송 (선택적)
            messagingTemplate.convertAndSend("/topic/notifications", message);

            logger.debug("실시간 알림 전송 완료: 수신자={}", notification.getRecipientId());
            
        } catch (Exception e) {
            logger.error("실시간 알림 전송 실패: 수신자={}", notification.getRecipientId(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 알림 상태 업데이트 전송
     */
    private void sendNotificationStatusUpdate(Long recipientId, Long notificationId, boolean isRead) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .content("알림 상태가 업데이트되었습니다.")
                .recipientId(recipientId)
                .addData("action", "status_update")
                .addData("notificationId", notificationId)
                .addData("isRead", isRead)
                .build();

            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications/status",
                message
            );
        } catch (Exception e) {
            logger.error("알림 상태 업데이트 전송 실패", e);
        }
    }

    /**
     * 일괄 알림 상태 업데이트 전송
     */
    private void sendBulkNotificationStatusUpdate(Long recipientId, List<Long> notificationIds, boolean isRead) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .content("알림 상태가 일괄 업데이트되었습니다.")
                .recipientId(recipientId)
                .addData("action", "bulk_status_update")
                .addData("notificationIds", notificationIds)
                .addData("isRead", isRead)
                .build();

            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications/status",
                message
            );
        } catch (Exception e) {
            logger.error("일괄 알림 상태 업데이트 전송 실패", e);
        }
    }

    /**
     * 전체 알림 읽음 업데이트 전송
     */
    private void sendAllNotificationsReadUpdate(Long recipientId) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .content("모든 알림이 읽음 처리되었습니다.")
                .recipientId(recipientId)
                .addData("action", "mark_all_read")
                .build();

            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications/status",
                message
            );
        } catch (Exception e) {
            logger.error("전체 알림 읽음 업데이트 전송 실패", e);
        }
    }

    /**
     * 알림 삭제 업데이트 전송
     */
    private void sendNotificationDeleteUpdate(Long recipientId, Long notificationId) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .content("알림이 삭제되었습니다.")
                .recipientId(recipientId)
                .addData("action", "delete")
                .addData("notificationId", notificationId)
                .build();

            messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications/status",
                message
            );
        } catch (Exception e) {
            logger.error("알림 삭제 업데이트 전송 실패", e);
        }
    }

    /**
     * 만료된 알림 정리 (스케줄링)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시 실행
    @Transactional
    public void cleanupExpiredNotifications() {
        try {
            int deletedCount = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
            logger.info("만료된 알림 정리 완료: 삭제된 개수={}", deletedCount);
        } catch (Exception e) {
            logger.error("만료된 알림 정리 실패", e);
        }
    }

    /**
     * 오래된 알림 정리
     */
    @Transactional
    public int cleanupOldNotifications(Long recipientId, int daysOld) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        return notificationRepository.deleteOldNotificationsByRecipientId(recipientId, before);
    }
} 