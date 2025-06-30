package com.tofumaker.service;

import com.tofumaker.entity.Notification;
import com.tofumaker.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private Long testUserId;
    private Long testNotificationId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testNotificationId = 1L;
        
        testNotification = new Notification();
        testNotification.setId(testNotificationId);
        testNotification.setType(Notification.NotificationType.BOARD_COMMENT);
        testNotification.setTitle("테스트 알림");
        testNotification.setContent("테스트 알림 내용");
        testNotification.setRecipientId(testUserId);
        testNotification.setSenderId(2L);
        testNotification.setReferenceId(10L);
        testNotification.setReferenceType("BOARD");
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createNotification(testNotification);

        // Then
        assertNotNull(result);
        assertEquals(testNotification.getId(), result.getId());
        assertEquals(testNotification.getTitle(), result.getTitle());
        assertEquals(testNotification.getContent(), result.getContent());
        assertEquals(testNotification.getRecipientId(), result.getRecipientId());
        
        verify(notificationRepository).save(testNotification);
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), anyString(), any());
        verify(messagingTemplate).convertAndSend(anyString(), any());
    }

    @Test
    void testCreateNotificationWithBuilder() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createNotification(
                Notification.NotificationType.BOARD_COMMENT,
                "테스트 알림",
                "테스트 알림 내용",
                testUserId
        );

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotificationWithDetails() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createNotification(
                Notification.NotificationType.BOARD_COMMENT,
                "테스트 알림",
                "테스트 알림 내용",
                testUserId,
                2L,
                10L,
                "BOARD"
        );

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testGetNotificationsByRecipientId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getNotificationsByRecipientId(testUserId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testNotification.getId(), result.getContent().get(0).getId());
        
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    void testGetUnreadNotificationsByRecipientId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getUnreadNotificationsByRecipientId(testUserId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).getIsRead());
        
        verify(notificationRepository).findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    void testGetUnreadNotificationCount() {
        // Given
        long expectedCount = 5L;
        when(notificationRepository.countByRecipientIdAndIsReadFalse(testUserId))
                .thenReturn(expectedCount);

        // When
        long result = notificationService.getUnreadNotificationCount(testUserId);

        // Then
        assertEquals(expectedCount, result);
        verify(notificationRepository).countByRecipientIdAndIsReadFalse(testUserId);
    }

    @Test
    void testGetNotificationsByType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                testUserId, Notification.NotificationType.BOARD_COMMENT, pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getNotificationsByType(
                testUserId, Notification.NotificationType.BOARD_COMMENT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(Notification.NotificationType.BOARD_COMMENT, result.getContent().get(0).getType());
        
        verify(notificationRepository).findByRecipientIdAndTypeOrderByCreatedAtDesc(
                testUserId, Notification.NotificationType.BOARD_COMMENT, pageable);
    }

    @Test
    void testGetNotificationsByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientIdAndDateRange(testUserId, startDate, endDate, pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getNotificationsByDateRange(
                testUserId, startDate, endDate, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(notificationRepository).findByRecipientIdAndDateRange(testUserId, startDate, endDate, pageable);
    }

    @Test
    void testGetNotificationById() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.of(testNotification));

        // When
        Optional<Notification> result = notificationService.getNotificationById(testNotificationId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testNotification.getId(), result.get().getId());
        
        verify(notificationRepository).findById(testNotificationId);
    }

    @Test
    void testGetNotificationById_NotFound() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.empty());

        // When
        Optional<Notification> result = notificationService.getNotificationById(testNotificationId);

        // Then
        assertFalse(result.isPresent());
        
        verify(notificationRepository).findById(testNotificationId);
    }

    @Test
    void testMarkAsRead_Success() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(testNotification);

        // When
        boolean result = notificationService.markAsRead(testNotificationId, testUserId);

        // Then
        assertTrue(result);
        assertTrue(testNotification.getIsRead());
        assertNotNull(testNotification.getReadAt());
        
        verify(notificationRepository).findById(testNotificationId);
        verify(notificationRepository).save(testNotification);
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testMarkAsRead_NotFound() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.empty());

        // When
        boolean result = notificationService.markAsRead(testNotificationId, testUserId);

        // Then
        assertFalse(result);
        
        verify(notificationRepository).findById(testNotificationId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsRead_WrongUser() {
        // Given
        testNotification.setRecipientId(999L); // 다른 사용자 ID
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.of(testNotification));

        // When
        boolean result = notificationService.markAsRead(testNotificationId, testUserId);

        // Then
        assertFalse(result);
        
        verify(notificationRepository).findById(testNotificationId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsReadBulk() {
        // Given
        List<Long> notificationIds = Arrays.asList(1L, 2L, 3L);
        int expectedUpdatedCount = 3;
        
        when(notificationRepository.markAsReadByIds(eq(notificationIds), eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(expectedUpdatedCount);

        // When
        int result = notificationService.markAsRead(notificationIds, testUserId);

        // Then
        assertEquals(expectedUpdatedCount, result);
        
        verify(notificationRepository).markAsReadByIds(eq(notificationIds), eq(testUserId), any(LocalDateTime.class));
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testMarkAllAsRead() {
        // Given
        int expectedUpdatedCount = 5;
        
        when(notificationRepository.markAllAsReadByRecipientId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(expectedUpdatedCount);

        // When
        int result = notificationService.markAllAsRead(testUserId);

        // Then
        assertEquals(expectedUpdatedCount, result);
        
        verify(notificationRepository).markAllAsReadByRecipientId(eq(testUserId), any(LocalDateTime.class));
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testDeleteNotification_Success() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.of(testNotification));

        // When
        boolean result = notificationService.deleteNotification(testNotificationId, testUserId);

        // Then
        assertTrue(result);
        
        verify(notificationRepository).findById(testNotificationId);
        verify(notificationRepository).delete(testNotification);
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testDeleteNotification_NotFound() {
        // Given
        when(notificationRepository.findById(testNotificationId))
                .thenReturn(Optional.empty());

        // When
        boolean result = notificationService.deleteNotification(testNotificationId, testUserId);

        // Then
        assertFalse(result);
        
        verify(notificationRepository).findById(testNotificationId);
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void testGetNotificationStats() {
        // Given
        List<Object[]> mockStats = Arrays.asList(
                new Object[]{Notification.NotificationType.BOARD_COMMENT, 5L},
                new Object[]{Notification.NotificationType.BOARD_LIKE, 3L},
                new Object[]{Notification.NotificationType.SYSTEM, 2L}
        );
        
        when(notificationRepository.getNotificationStatsByRecipientId(testUserId))
                .thenReturn(mockStats);

        // When
        Map<String, Long> result = notificationService.getNotificationStats(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(5L, result.get("BOARD_COMMENT"));
        assertEquals(3L, result.get("BOARD_LIKE"));
        assertEquals(2L, result.get("SYSTEM"));
        
        verify(notificationRepository).getNotificationStatsByRecipientId(testUserId);
    }

    @Test
    void testGetRecentNotifications() {
        // Given
        int days = 7;
        List<Notification> notifications = Arrays.asList(testNotification);
        
        when(notificationRepository.findRecentNotifications(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getRecentNotifications(testUserId, days);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotification.getId(), result.get(0).getId());
        
        verify(notificationRepository).findRecentNotifications(eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    void testCleanupOldNotifications() {
        // Given
        int daysOld = 30;
        int expectedDeletedCount = 10;
        
        when(notificationRepository.deleteOldNotificationsByRecipientId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(expectedDeletedCount);

        // When
        int result = notificationService.cleanupOldNotifications(testUserId, daysOld);

        // Then
        assertEquals(expectedDeletedCount, result);
        
        verify(notificationRepository).deleteOldNotificationsByRecipientId(eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredNotifications() {
        // Given
        int expectedDeletedCount = 15;
        
        when(notificationRepository.deleteExpiredNotifications(any(LocalDateTime.class)))
                .thenReturn(expectedDeletedCount);

        // When
        notificationService.cleanupExpiredNotifications();

        // Then
        verify(notificationRepository).deleteExpiredNotifications(any(LocalDateTime.class));
    }

    @Test
    void testCreateNotification_Exception() {
        // Given
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.createNotification(testNotification);
        });
        
        verify(notificationRepository).save(testNotification);
    }
} 