package com.tofumaker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 알림 엔티티
 */
@Entity
@Table(name = "notifications")
@Schema(description = "알림 정보")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "알림 타입", example = "BOARD_COMMENT")
    private NotificationType type;

    @Column(nullable = false, length = 200)
    @Schema(description = "알림 제목", example = "새로운 댓글이 달렸습니다")
    private String title;

    @Column(nullable = false, length = 1000)
    @Schema(description = "알림 내용", example = "게시글 '제목'에 새로운 댓글이 달렸습니다.")
    private String content;

    @Column(name = "recipient_id", nullable = false)
    @Schema(description = "수신자 ID", example = "1")
    private Long recipientId;

    @Column(name = "sender_id")
    @Schema(description = "발신자 ID", example = "2")
    private Long senderId;

    @Column(name = "reference_id")
    @Schema(description = "참조 ID (게시글 ID, 댓글 ID 등)", example = "10")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    @Schema(description = "참조 타입", example = "BOARD")
    private String referenceType;

    @Column(name = "is_read", nullable = false)
    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead = false;

    @Column(name = "read_at")
    @Schema(description = "읽은 시간")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    @Schema(description = "만료 시간")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // 기본 만료 시간: 30일
            expiresAt = createdAt.plusDays(30);
        }
    }

    // 생성자
    public Notification() {}

    public Notification(NotificationType type, String title, String content, Long recipientId) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.recipientId = recipientId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { 
        this.isRead = isRead;
        if (isRead && readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /**
     * 알림 타입 열거형
     */
    public enum NotificationType {
        @Schema(description = "게시글 댓글")
        BOARD_COMMENT,
        
        @Schema(description = "게시글 좋아요")
        BOARD_LIKE,
        
        @Schema(description = "새 게시글")
        NEW_BOARD,
        
        @Schema(description = "시스템 알림")
        SYSTEM,
        
        @Schema(description = "사용자 멘션")
        USER_MENTION,
        
        @Schema(description = "파일 업로드 완료")
        FILE_UPLOAD_COMPLETE,
        
        @Schema(description = "이메일 전송 완료")
        EMAIL_SENT,
        
        @Schema(description = "일반 알림")
        GENERAL
    }
} 