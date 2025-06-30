package com.tofumaker.controller;

import com.tofumaker.entity.Notification;
import com.tofumaker.service.NotificationService;
import com.tofumaker.util.PaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 알림 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "알림 관리 API")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 사용자별 알림 조회 (페이징)
     */
    @Operation(summary = "사용자별 알림 조회", description = "특정 사용자의 알림을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Notification>> getNotificationsByUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 필드", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);
        Page<Notification> notifications = notificationService.getNotificationsByRecipientId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 사용자별 읽지 않은 알림 조회 (페이징)
     */
    @Operation(summary = "읽지 않은 알림 조회", description = "특정 사용자의 읽지 않은 알림을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<Notification>> getUnreadNotificationsByUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 필드", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);
        Page<Notification> notifications = notificationService.getUnreadNotificationsByRecipientId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Operation(summary = "읽지 않은 알림 개수", description = "특정 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {

        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 알림 타입별 조회
     */
    @Operation(summary = "알림 타입별 조회", description = "특정 타입의 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<Page<Notification>> getNotificationsByType(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "알림 타입", required = true)
            @PathVariable Notification.NotificationType type,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 필드", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);
        Page<Notification> notifications = notificationService.getNotificationsByType(userId, type, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 기간별 알림 조회
     */
    @Operation(summary = "기간별 알림 조회", description = "특정 기간의 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<Page<Notification>> getNotificationsByDateRange(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "시작 날짜", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 필드", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);
        Page<Notification> notifications = notificationService.getNotificationsByDateRange(
                userId, startDate, endDate, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 상세 조회
     */
    @Operation(summary = "알림 상세 조회", description = "특정 알림의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(
            @Parameter(description = "알림 ID", required = true)
            @PathVariable Long id) {

        Optional<Notification> notification = notificationService.getNotificationById(id);
        return notification.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 알림 생성
     */
    @Operation(summary = "알림 생성", description = "새로운 알림을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @Parameter(description = "알림 정보", required = true)
            @RequestBody Notification notification) {

        Notification createdNotification = notificationService.createNotification(notification);
        return ResponseEntity.ok(createdNotification);
    }

    /**
     * 간단한 알림 생성
     */
    @Operation(summary = "간단한 알림 생성", description = "기본 정보로 알림을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = Notification.class)))
    })
    @PostMapping("/simple")
    public ResponseEntity<Notification> createSimpleNotification(
            @Parameter(description = "알림 타입", required = true)
            @RequestParam Notification.NotificationType type,
            @Parameter(description = "알림 제목", required = true)
            @RequestParam String title,
            @Parameter(description = "알림 내용", required = true)
            @RequestParam String content,
            @Parameter(description = "수신자 ID", required = true)
            @RequestParam Long recipientId,
            @Parameter(description = "발신자 ID")
            @RequestParam(required = false) Long senderId,
            @Parameter(description = "참조 ID")
            @RequestParam(required = false) Long referenceId,
            @Parameter(description = "참조 타입")
            @RequestParam(required = false) String referenceType) {

        Notification notification = notificationService.createNotification(
                type, title, content, recipientId, senderId, referenceId, referenceType);
        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "알림 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId) {

        boolean success = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("success", success, "message", 
                success ? "알림이 읽음 처리되었습니다." : "알림 읽음 처리에 실패했습니다."));
    }

    /**
     * 여러 알림 읽음 처리
     */
    @Operation(summary = "여러 알림 읽음 처리", description = "여러 알림을 한 번에 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공")
    })
    @PutMapping("/read/bulk")
    public ResponseEntity<Map<String, Object>> markAsReadBulk(
            @Parameter(description = "알림 ID 목록", required = true)
            @RequestBody List<Long> notificationIds,
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId) {

        int updatedCount = notificationService.markAsRead(notificationIds, userId);
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount, 
                "message", updatedCount + "개의 알림이 읽음 처리되었습니다."));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공")
    })
    @PutMapping("/user/{userId}/read/all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {

        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount, 
                "message", "모든 알림이 읽음 처리되었습니다."));
    }

    /**
     * 알림 삭제
     */
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "알림 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId) {

        boolean success = notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(Map.of("success", success, "message", 
                success ? "알림이 삭제되었습니다." : "알림 삭제에 실패했습니다."));
    }

    /**
     * 사용자별 알림 통계
     */
    @Operation(summary = "알림 통계", description = "사용자의 알림 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getNotificationStats(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {

        Map<String, Long> stats = notificationService.getNotificationStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 최근 알림 조회
     */
    @Operation(summary = "최근 알림 조회", description = "최근 N일간의 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "조회할 일수", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        List<Notification> notifications = notificationService.getRecentNotifications(userId, days);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 오래된 알림 정리
     */
    @Operation(summary = "오래된 알림 정리", description = "특정 기간 이전의 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정리 성공")
    })
    @DeleteMapping("/user/{userId}/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldNotifications(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "삭제할 알림의 기준 일수", example = "30")
            @RequestParam(defaultValue = "30") int daysOld) {

        int deletedCount = notificationService.cleanupOldNotifications(userId, daysOld);
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount, 
                "message", deletedCount + "개의 오래된 알림이 삭제되었습니다."));
    }
} 