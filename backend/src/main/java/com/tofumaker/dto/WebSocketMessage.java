package com.tofumaker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 메시지 DTO
 */
@Schema(description = "WebSocket 메시지")
public class WebSocketMessage {

    @Schema(description = "메시지 타입", example = "NOTIFICATION")
    private MessageType type;

    @Schema(description = "메시지 내용", example = "새로운 알림이 도착했습니다.")
    private String content;

    @Schema(description = "발신자 ID", example = "1")
    private Long senderId;

    @Schema(description = "발신자 이름", example = "관리자")
    private String senderName;

    @Schema(description = "수신자 ID", example = "2")
    private Long recipientId;

    @Schema(description = "채널/토픽", example = "/topic/notifications")
    private String channel;

    @Schema(description = "추가 데이터")
    private Map<String, Object> data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "전송 시간", example = "2024-01-01 12:00:00")
    private LocalDateTime timestamp;

    // 생성자
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(MessageType type, String content) {
        this();
        this.type = type;
        this.content = content;
    }

    public WebSocketMessage(MessageType type, String content, Long recipientId) {
        this(type, content);
        this.recipientId = recipientId;
    }

    public WebSocketMessage(MessageType type, String content, Long senderId, Long recipientId) {
        this(type, content, recipientId);
        this.senderId = senderId;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        @Schema(description = "알림 메시지")
        NOTIFICATION,
        
        @Schema(description = "채팅 메시지")
        CHAT,
        
        @Schema(description = "시스템 메시지")
        SYSTEM,
        
        @Schema(description = "사용자 상태 변경")
        USER_STATUS,
        
        @Schema(description = "게시글 업데이트")
        BOARD_UPDATE,
        
        @Schema(description = "파일 업로드 진행률")
        FILE_PROGRESS,
        
        @Schema(description = "이메일 전송 상태")
        EMAIL_STATUS,
        
        @Schema(description = "연결 확인")
        PING,
        
        @Schema(description = "연결 응답")
        PONG,
        
        @Schema(description = "오류 메시지")
        ERROR
    }

    /**
     * 빌더 패턴
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WebSocketMessage message = new WebSocketMessage();

        public Builder type(MessageType type) {
            message.type = type;
            return this;
        }

        public Builder content(String content) {
            message.content = content;
            return this;
        }

        public Builder senderId(Long senderId) {
            message.senderId = senderId;
            return this;
        }

        public Builder senderName(String senderName) {
            message.senderName = senderName;
            return this;
        }

        public Builder recipientId(Long recipientId) {
            message.recipientId = recipientId;
            return this;
        }

        public Builder channel(String channel) {
            message.channel = channel;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            message.data = data;
            return this;
        }

        public Builder addData(String key, Object value) {
            if (message.data == null) {
                message.data = new java.util.HashMap<>();
            }
            message.data.put(key, value);
            return this;
        }

        public WebSocketMessage build() {
            return message;
        }
    }
} 