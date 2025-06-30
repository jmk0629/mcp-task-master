package com.tofumaker.repository;

import com.tofumaker.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자별 알림 조회 (페이징)
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * 사용자별 알림 조회 (리스트)
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * 사용자별 읽지 않은 알림 조회 (페이징)
     */
    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 조회 (리스트)
     */
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    /**
     * 사용자별 읽지 않은 알림 개수
     */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * 알림 타입별 조회 (페이징)
     */
    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            Long recipientId, Notification.NotificationType type, Pageable pageable);

    /**
     * 알림 타입별 조회 (리스트)
     */
    List<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            Long recipientId, Notification.NotificationType type);

    /**
     * 기간별 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
           "AND n.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndDateRange(
            @Param("recipientId") Long recipientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 참조 ID와 타입으로 알림 조회
     */
    List<Notification> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

    /**
     * 발신자별 알림 조회
     */
    Page<Notification> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    /**
     * 만료된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * 만료된 알림 삭제
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 알림들을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.id IN :notificationIds AND n.recipientId = :recipientId")
    int markAsReadByIds(@Param("notificationIds") List<Long> notificationIds, 
                       @Param("recipientId") Long recipientId, 
                       @Param("readAt") LocalDateTime readAt);

    /**
     * 사용자별 알림 통계
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
           "WHERE n.recipientId = :recipientId " +
           "GROUP BY n.type")
    List<Object[]> getNotificationStatsByRecipientId(@Param("recipientId") Long recipientId);

    /**
     * 최근 N일간의 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
           "AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("recipientId") Long recipientId, 
                                              @Param("since") LocalDateTime since);

    /**
     * 사용자별 알림 삭제 (특정 기간 이전)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId " +
           "AND n.createdAt < :before")
    int deleteOldNotificationsByRecipientId(@Param("recipientId") Long recipientId, 
                                           @Param("before") LocalDateTime before);
} 