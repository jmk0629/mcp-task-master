package com.tofumaker.repository;

import com.tofumaker.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /**
     * 상태별 이메일 로그 조회
     */
    List<EmailLog> findByStatusOrderBySentAtDesc(EmailLog.EmailStatus status);

    /**
     * 상태별 이메일 로그 페이징 조회
     */
    Page<EmailLog> findByStatusOrderBySentAtDesc(EmailLog.EmailStatus status, Pageable pageable);

    /**
     * 사용자별 이메일 로그 조회
     */
    Page<EmailLog> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    /**
     * 수신자 이메일로 조회
     */
    Page<EmailLog> findByToEmailOrderBySentAtDesc(String toEmail, Pageable pageable);

    /**
     * 템플릿 코드별 조회
     */
    List<EmailLog> findByTemplateCodeOrderBySentAtDesc(String templateCode);

    /**
     * 이벤트 타입별 조회
     */
    List<EmailLog> findByEventTypeOrderBySentAtDesc(String eventType);

    /**
     * 기간별 이메일 로그 조회
     */
    @Query("SELECT e FROM EmailLog e WHERE e.sentAt BETWEEN :startDate AND :endDate ORDER BY e.sentAt DESC")
    List<EmailLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * 재시도가 필요한 이메일 조회 (실패한 이메일 중 재시도 횟수가 제한 미만)
     */
    @Query("SELECT e FROM EmailLog e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.sentAt ASC")
    List<EmailLog> findFailedEmailsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * 이메일 전송 통계 - 상태별 개수
     */
    @Query("SELECT e.status, COUNT(e) FROM EmailLog e GROUP BY e.status")
    List<Object[]> getEmailStatsByStatus();

    /**
     * 이메일 전송 통계 - 일별 전송 개수
     */
    @Query("SELECT DATE(e.sentAt), COUNT(e) FROM EmailLog e WHERE e.sentAt >= :startDate GROUP BY DATE(e.sentAt) ORDER BY DATE(e.sentAt)")
    List<Object[]> getDailyEmailStats(@Param("startDate") LocalDateTime startDate);

    /**
     * 템플릿별 전송 통계
     */
    @Query("SELECT e.templateCode, COUNT(e) FROM EmailLog e WHERE e.templateCode IS NOT NULL GROUP BY e.templateCode ORDER BY COUNT(e) DESC")
    List<Object[]> getEmailStatsByTemplate();

    /**
     * 최근 전송된 이메일 조회
     */
    List<EmailLog> findTop10ByOrderBySentAtDesc();

    /**
     * 특정 기간 내 성공한 이메일 개수
     */
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = 'SENT' AND e.sentAt BETWEEN :startDate AND :endDate")
    Long countSuccessfulEmailsInPeriod(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간 내 실패한 이메일 개수
     */
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = 'FAILED' AND e.sentAt BETWEEN :startDate AND :endDate")
    Long countFailedEmailsInPeriod(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);
} 