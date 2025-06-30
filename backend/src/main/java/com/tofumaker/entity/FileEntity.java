package com.tofumaker.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Schema(description = "파일 정보")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "파일 ID", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "원본 파일명", example = "document.pdf")
    private String originalFileName;

    @Column(nullable = false)
    @Schema(description = "저장된 파일명", example = "20231201_123456_document.pdf")
    private String storedFileName;

    @Column(nullable = false)
    @Schema(description = "파일 경로", example = "/uploads/2023/12/01/")
    private String filePath;

    @Column(nullable = false)
    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long fileSize;

    @Column(nullable = false)
    @Schema(description = "파일 타입", example = "application/pdf")
    private String contentType;

    @Column(nullable = false)
    @Schema(description = "파일 확장자", example = "pdf")
    private String fileExtension;

    @Column(nullable = false)
    @Schema(description = "업로드 일시")
    private LocalDateTime uploadedAt;

    @Column
    @Schema(description = "업로드한 사용자 ID", example = "1")
    private Long uploadedBy;

    @Column
    @Schema(description = "파일 설명", example = "프로젝트 관련 문서")
    private String description;

    @Column(nullable = false)
    @Schema(description = "활성 상태", example = "true")
    private Boolean active = true;

    // Constructors
    public FileEntity() {
        this.uploadedAt = LocalDateTime.now();
    }

    public FileEntity(String originalFileName, String storedFileName, String filePath, 
                     Long fileSize, String contentType, String fileExtension) {
        this();
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
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

    public String getFullPath() {
        return filePath + storedFileName;
    }
} 