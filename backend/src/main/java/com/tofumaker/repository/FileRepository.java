package com.tofumaker.repository;

import com.tofumaker.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    /**
     * 활성 상태인 파일 조회
     */
    List<FileEntity> findByActiveTrue();

    /**
     * 활성 상태인 파일 페이징 조회
     */
    Page<FileEntity> findByActiveTrue(Pageable pageable);

    /**
     * 특정 사용자가 업로드한 파일 조회
     */
    List<FileEntity> findByUploadedByAndActiveTrue(Long uploadedBy);

    /**
     * 특정 사용자가 업로드한 파일 페이징 조회
     */
    Page<FileEntity> findByUploadedByAndActiveTrue(Long uploadedBy, Pageable pageable);

    /**
     * 파일 확장자로 검색
     */
    List<FileEntity> findByFileExtensionAndActiveTrue(String fileExtension);

    /**
     * 파일명으로 검색 (원본 파일명 기준)
     */
    @Query("SELECT f FROM FileEntity f WHERE f.originalFileName LIKE %:fileName% AND f.active = true")
    List<FileEntity> findByOriginalFileNameContaining(@Param("fileName") String fileName);

    /**
     * 파일명으로 검색 (원본 파일명 기준) - 페이징
     */
    @Query("SELECT f FROM FileEntity f WHERE f.originalFileName LIKE %:fileName% AND f.active = true")
    Page<FileEntity> findByOriginalFileNameContaining(@Param("fileName") String fileName, Pageable pageable);

    /**
     * 특정 기간 내 업로드된 파일 조회
     */
    @Query("SELECT f FROM FileEntity f WHERE f.uploadedAt BETWEEN :startDate AND :endDate AND f.active = true")
    List<FileEntity> findByUploadedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 파일 크기 범위로 검색
     */
    @Query("SELECT f FROM FileEntity f WHERE f.fileSize BETWEEN :minSize AND :maxSize AND f.active = true")
    List<FileEntity> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * 저장된 파일명으로 조회
     */
    Optional<FileEntity> findByStoredFileNameAndActiveTrue(String storedFileName);

    /**
     * 콘텐츠 타입으로 검색
     */
    List<FileEntity> findByContentTypeAndActiveTrue(String contentType);

    /**
     * 파일 통계 - 총 파일 수
     */
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.active = true")
    Long countActiveFiles();

    /**
     * 파일 통계 - 총 파일 크기
     */
    @Query("SELECT SUM(f.fileSize) FROM FileEntity f WHERE f.active = true")
    Long getTotalFileSize();

    /**
     * 사용자별 파일 수
     */
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.uploadedBy = :userId AND f.active = true")
    Long countByUploadedBy(@Param("userId") Long userId);
} 