package com.tofumaker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates")
@Schema(description = "이메일 템플릿")
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "템플릿 ID", example = "1")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "템플릿 코드", example = "WELCOME_EMAIL")
    private String templateCode;

    @Column(nullable = false)
    @Schema(description = "템플릿 이름", example = "환영 이메일")
    private String templateName;

    @Column(nullable = false)
    @Schema(description = "이메일 제목", example = "TofuMaker에 오신 것을 환영합니다!")
    private String subject;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "HTML 템플릿 내용")
    private String htmlContent;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "텍스트 템플릿 내용")
    private String textContent;

    @Column
    @Schema(description = "템플릿 설명", example = "신규 사용자 가입 시 전송되는 환영 이메일")
    private String description;

    @Column(nullable = false)
    @Schema(description = "활성 상태", example = "true")
    private Boolean active = true;

    @Column(nullable = false)
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    @Column
    @Schema(description = "생성자 ID", example = "1")
    private Long createdBy;

    @Column
    @Schema(description = "수정자 ID", example = "1")
    private Long updatedBy;

    // Constructors
    public EmailTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public EmailTemplate(String templateCode, String templateName, String subject, 
                        String htmlContent, String textContent) {
        this();
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.subject = subject;
        this.htmlContent = htmlContent;
        this.textContent = textContent;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
} 