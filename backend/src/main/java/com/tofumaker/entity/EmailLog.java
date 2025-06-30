package com.tofumaker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Schema(description = "이메일 전송 로그")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "로그 ID", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "수신자 이메일", example = "user@example.com")
    private String toEmail;

    @Column
    @Schema(description = "수신자 이름", example = "홍길동")
    private String toName;

    @Column(nullable = false)
    @Schema(description = "이메일 제목", example = "환영합니다!")
    private String subject;

    @Column
    @Schema(description = "템플릿 코드", example = "WELCOME_EMAIL")
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "전송 상태", example = "SENT")
    private EmailStatus status;

    @Column
    @Schema(description = "오류 메시지")
    private String errorMessage;

    @Column(nullable = false)
    @Schema(description = "전송 시도 일시")
    private LocalDateTime sentAt;

    @Column
    @Schema(description = "전송 완료 일시")
    private LocalDateTime deliveredAt;

    @Column
    @Schema(description = "재시도 횟수", example = "0")
    private Integer retryCount = 0;

    @Column
    @Schema(description = "관련 사용자 ID", example = "1")
    private Long userId;

    @Column
    @Schema(description = "이벤트 타입", example = "USER_REGISTRATION")
    private String eventType;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "이메일 내용 (HTML)")
    private String htmlContent;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "이메일 내용 (텍스트)")
    private String textContent;

    // Constructors
    public EmailLog() {
        this.sentAt = LocalDateTime.now();
        this.status = EmailStatus.PENDING;
    }

    public EmailLog(String toEmail, String subject, String templateCode) {
        this();
        this.toEmail = toEmail;
        this.subject = subject;
        this.templateCode = templateCode;
    }

    // Email Status Enum
    public enum EmailStatus {
        PENDING,    // 전송 대기
        SENT,       // 전송 완료
        FAILED,     // 전송 실패
        RETRY       // 재시도 중
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }

    public void markAsSent() {
        this.status = EmailStatus.SENT;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsRetry() {
        this.status = EmailStatus.RETRY;
        incrementRetryCount();
    }
} 