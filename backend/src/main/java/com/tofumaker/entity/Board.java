package com.tofumaker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "board")
@Schema(description = "게시판 엔티티")
public class Board {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "게시글 ID", example = "1")
    private Long id;
    
    @Column(nullable = false, length = 200)
    @Schema(description = "게시글 제목", example = "안녕하세요", maxLength = 200)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "게시글 내용", example = "게시글 내용입니다.")
    private String content;
    
    @Column(nullable = false, length = 100)
    @Schema(description = "작성자", example = "홍길동", maxLength = 100)
    private String author;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
    
    @Column(name = "view_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Schema(description = "조회수", example = "0")
    private Long viewCount = 0L;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    @Schema(description = "활성 상태", example = "true")
    private Boolean active = true;
    
    // 기본 생성자
    public Board() {}
    
    // 생성자
    public Board(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getter와 Setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 