package com.tofumaker.service;

import com.tofumaker.config.FileUploadConfig;
import com.tofumaker.entity.FileEntity;
import com.tofumaker.repository.FileRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileUploadConfig fileUploadConfig;

    /**
     * 파일 업로드
     */
    public FileEntity uploadFile(MultipartFile file, Long userId, String description) throws IOException {
        // 파일 유효성 검증
        validateFile(file);

        // 파일 저장 경로 생성
        String uploadPath = createUploadPath();
        String storedFileName = generateStoredFileName(file.getOriginalFilename());
        String fullPath = uploadPath + storedFileName;

        // 디렉토리 생성
        Path uploadDir = Paths.get(fileUploadConfig.getUploadDir() + uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 파일 저장
        Path filePath = uploadDir.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 파일 정보 DB 저장
        FileEntity fileEntity = new FileEntity(
            file.getOriginalFilename(),
            storedFileName,
            uploadPath,
            file.getSize(),
            file.getContentType(),
            FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase()
        );
        fileEntity.setUploadedBy(userId);
        fileEntity.setDescription(description);

        FileEntity savedFile = fileRepository.save(fileEntity);
        logger.info("File uploaded successfully: {} (ID: {})", file.getOriginalFilename(), savedFile.getId());

        return savedFile;
    }

    /**
     * 파일 다운로드
     */
    public Resource downloadFile(Long fileId) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        if (!fileEntity.getActive()) {
            throw new RuntimeException("File is not active: " + fileId);
        }

        Path filePath = Paths.get(fileUploadConfig.getUploadDir() + fileEntity.getFullPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found or not readable: " + fileEntity.getOriginalFileName());
        }
    }

    /**
     * 파일 정보 조회
     */
    @Transactional(readOnly = true)
    public Optional<FileEntity> getFileInfo(Long fileId) {
        return fileRepository.findById(fileId);
    }

    /**
     * 활성 파일 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<FileEntity> getActiveFiles(Pageable pageable) {
        return fileRepository.findByActiveTrue(pageable);
    }

    /**
     * 사용자별 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FileEntity> getUserFiles(Long userId, Pageable pageable) {
        return fileRepository.findByUploadedByAndActiveTrue(userId, pageable);
    }

    /**
     * 파일명으로 검색
     */
    @Transactional(readOnly = true)
    public Page<FileEntity> searchFilesByName(String fileName, Pageable pageable) {
        return fileRepository.findByOriginalFileNameContaining(fileName, pageable);
    }

    /**
     * 파일 삭제 (논리적 삭제)
     */
    public void deleteFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        fileEntity.setActive(false);
        fileRepository.save(fileEntity);
        logger.info("File deleted (logical): {} (ID: {})", fileEntity.getOriginalFileName(), fileId);
    }

    /**
     * 파일 물리적 삭제
     */
    public void deleteFilePhysically(Long fileId) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        // 물리적 파일 삭제
        Path filePath = Paths.get(fileUploadConfig.getUploadDir() + fileEntity.getFullPath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // DB에서 삭제
        fileRepository.delete(fileEntity);
        logger.info("File deleted (physical): {} (ID: {})", fileEntity.getOriginalFileName(), fileId);
    }

    /**
     * 파일 통계 정보
     */
    @Transactional(readOnly = true)
    public FileStatistics getFileStatistics() {
        Long totalFiles = fileRepository.countActiveFiles();
        Long totalSize = fileRepository.getTotalFileSize();
        return new FileStatistics(totalFiles != null ? totalFiles : 0, totalSize != null ? totalSize : 0);
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new RuntimeException("File size exceeds maximum allowed size: " + fileUploadConfig.getMaxFileSize());
        }

        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        if (!Arrays.asList(fileUploadConfig.getAllowedExtensions()).contains(fileExtension)) {
            throw new RuntimeException("File extension not allowed: " + fileExtension);
        }
    }

    /**
     * 업로드 경로 생성 (년/월/일 구조)
     */
    private String createUploadPath() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
    }

    /**
     * 저장될 파일명 생성 (UUID + 타임스탬프 + 원본파일명)
     */
    private String generateStoredFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = FilenameUtils.getExtension(originalFileName);
        String baseName = FilenameUtils.getBaseName(originalFileName);
        
        return String.format("%s_%s_%s.%s", timestamp, uuid, baseName, extension);
    }

    /**
     * 파일 통계 정보 클래스
     */
    public static class FileStatistics {
        private final Long totalFiles;
        private final Long totalSize;

        public FileStatistics(Long totalFiles, Long totalSize) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
        }

        public Long getTotalFiles() {
            return totalFiles;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public String getFormattedSize() {
            if (totalSize == null || totalSize == 0) {
                return "0 B";
            }

            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int unitIndex = 0;
            double size = totalSize.doubleValue();

            while (size >= 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }

            return String.format("%.2f %s", size, units[unitIndex]);
        }
    }
} 