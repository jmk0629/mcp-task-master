package com.tofumaker.repository;

import com.tofumaker.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    /**
     * 템플릿 코드로 활성 템플릿 조회
     */
    Optional<EmailTemplate> findByTemplateCodeAndActiveTrue(String templateCode);

    /**
     * 활성 템플릿 목록 조회
     */
    List<EmailTemplate> findByActiveTrueOrderByTemplateNameAsc();

    /**
     * 템플릿 이름으로 검색
     */
    @Query("SELECT t FROM EmailTemplate t WHERE t.templateName LIKE %:name% AND t.active = true")
    List<EmailTemplate> findByTemplateNameContaining(@Param("name") String name);

    /**
     * 템플릿 코드 존재 여부 확인
     */
    boolean existsByTemplateCodeAndActiveTrue(String templateCode);

    /**
     * 생성자별 템플릿 조회
     */
    List<EmailTemplate> findByCreatedByAndActiveTrueOrderByCreatedAtDesc(Long createdBy);
} 