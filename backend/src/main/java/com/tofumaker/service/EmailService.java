package com.tofumaker.service;

import com.tofumaker.config.EmailConfig;
import com.tofumaker.entity.EmailLog;
import com.tofumaker.entity.EmailTemplate;
import com.tofumaker.repository.EmailLogRepository;
import com.tofumaker.repository.EmailTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private EmailTemplateRepository templateRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 단순 텍스트 이메일 전송
     */
    @Async
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String subject, String text) {
        return sendSimpleEmail(to, null, subject, text, null, null);
    }

    /**
     * 단순 텍스트 이메일 전송 (상세 정보 포함)
     */
    @Async
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String toName, String subject, 
                                                     String text, Long userId, String eventType) {
        if (!emailConfig.isEnabled()) {
            logger.warn("Email service is disabled");
            return CompletableFuture.completedFuture(false);
        }

        EmailLog emailLog = new EmailLog(to, subject, null);
        emailLog.setToName(toName);
        emailLog.setUserId(userId);
        emailLog.setEventType(eventType);
        emailLog.setTextContent(text);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailConfig.getFromEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            
            emailLog.markAsSent();
            logger.info("Simple email sent successfully to: {}", to);
            
        } catch (MailException e) {
            emailLog.markAsFailed(e.getMessage());
            logger.error("Failed to send simple email to: {}", to, e);
        } finally {
            emailLogRepository.save(emailLog);
        }

        return CompletableFuture.completedFuture(emailLog.getStatus() == EmailLog.EmailStatus.SENT);
    }

    /**
     * HTML 이메일 전송
     */
    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String htmlContent) {
        return sendHtmlEmail(to, null, subject, htmlContent, null, null, null);
    }

    /**
     * HTML 이메일 전송 (상세 정보 포함)
     */
    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String toName, String subject, 
                                                   String htmlContent, String textContent, 
                                                   Long userId, String eventType) {
        if (!emailConfig.isEnabled()) {
            logger.warn("Email service is disabled");
            return CompletableFuture.completedFuture(false);
        }

        EmailLog emailLog = new EmailLog(to, subject, null);
        emailLog.setToName(toName);
        emailLog.setUserId(userId);
        emailLog.setEventType(eventType);
        emailLog.setHtmlContent(htmlContent);
        emailLog.setTextContent(textContent);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            try {
                helper.setFrom(emailConfig.getFromEmail(), emailConfig.getFromName());
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom(emailConfig.getFromEmail());
                logger.warn("Failed to set from name, using email only: {}", e.getMessage());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            
            if (htmlContent != null && textContent != null) {
                helper.setText(textContent, htmlContent);
            } else if (htmlContent != null) {
                helper.setText(htmlContent, true);
            } else {
                helper.setText(textContent != null ? textContent : "", false);
            }

            mailSender.send(mimeMessage);
            
            emailLog.markAsSent();
            logger.info("HTML email sent successfully to: {}", to);
            
        } catch (MailException | MessagingException e) {
            emailLog.markAsFailed(e.getMessage());
            logger.error("Failed to send HTML email to: {}", to, e);
        } finally {
            emailLogRepository.save(emailLog);
        }

        return CompletableFuture.completedFuture(emailLog.getStatus() == EmailLog.EmailStatus.SENT);
    }

    /**
     * 템플릿 기반 이메일 전송
     */
    @Async
    public CompletableFuture<Boolean> sendTemplateEmail(String templateCode, String to, 
                                                       Map<String, Object> variables) {
        return sendTemplateEmail(templateCode, to, null, variables, null, null);
    }

    /**
     * 템플릿 기반 이메일 전송 (상세 정보 포함)
     */
    @Async
    public CompletableFuture<Boolean> sendTemplateEmail(String templateCode, String to, String toName,
                                                       Map<String, Object> variables, Long userId, 
                                                       String eventType) {
        if (!emailConfig.isEnabled()) {
            logger.warn("Email service is disabled");
            return CompletableFuture.completedFuture(false);
        }

        Optional<EmailTemplate> templateOpt = templateRepository.findByTemplateCodeAndActiveTrue(templateCode);
        if (!templateOpt.isPresent()) {
            logger.error("Email template not found: {}", templateCode);
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateOpt.get();
        EmailLog emailLog = new EmailLog(to, template.getSubject(), templateCode);
        emailLog.setToName(toName);
        emailLog.setUserId(userId);
        emailLog.setEventType(eventType);

        try {
            // Thymeleaf 컨텍스트 생성
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            // 기본 변수 추가
            context.setVariable("toName", toName != null ? toName : to);
            context.setVariable("fromName", emailConfig.getFromName());

            // 템플릿 처리
            String processedSubject = template.getSubject();
            String processedHtmlContent = null;
            String processedTextContent = null;

            if (template.getHtmlContent() != null) {
                processedHtmlContent = templateEngine.process("email/" + templateCode + "_html", context);
            }
            if (template.getTextContent() != null) {
                processedTextContent = templateEngine.process("email/" + templateCode + "_text", context);
            }

            // 템플릿이 없는 경우 DB의 내용 사용
            if (processedHtmlContent == null && template.getHtmlContent() != null) {
                processedHtmlContent = processTemplate(template.getHtmlContent(), variables);
            }
            if (processedTextContent == null && template.getTextContent() != null) {
                processedTextContent = processTemplate(template.getTextContent(), variables);
            }

            emailLog.setHtmlContent(processedHtmlContent);
            emailLog.setTextContent(processedTextContent);

            // 이메일 전송
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            try {
                helper.setFrom(emailConfig.getFromEmail(), emailConfig.getFromName());
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom(emailConfig.getFromEmail());
                logger.warn("Failed to set from name, using email only: {}", e.getMessage());
            }
            helper.setTo(to);
            helper.setSubject(processedSubject);

            if (processedHtmlContent != null && processedTextContent != null) {
                helper.setText(processedTextContent, processedHtmlContent);
            } else if (processedHtmlContent != null) {
                helper.setText(processedHtmlContent, true);
            } else if (processedTextContent != null) {
                helper.setText(processedTextContent, false);
            }

            mailSender.send(mimeMessage);
            
            emailLog.markAsSent();
            logger.info("Template email sent successfully to: {} using template: {}", to, templateCode);
            
        } catch (MailException | MessagingException e) {
            emailLog.markAsFailed(e.getMessage());
            logger.error("Failed to send template email to: {} using template: {}", to, templateCode, e);
        } finally {
            emailLogRepository.save(emailLog);
        }

        return CompletableFuture.completedFuture(emailLog.getStatus() == EmailLog.EmailStatus.SENT);
    }

    /**
     * 실패한 이메일 재시도
     */
    @Async
    public void retryFailedEmails() {
        List<EmailLog> failedEmails = emailLogRepository.findFailedEmailsForRetry(MAX_RETRY_COUNT);
        
        for (EmailLog emailLog : failedEmails) {
            emailLog.markAsRetry();
            emailLogRepository.save(emailLog);
            
            try {
                boolean success;
                if (emailLog.getTemplateCode() != null) {
                    // 템플릿 이메일 재시도는 복잡하므로 단순 HTML 이메일로 재시도
                    success = sendHtmlEmail(
                        emailLog.getToEmail(),
                        emailLog.getToName(),
                        emailLog.getSubject(),
                        emailLog.getHtmlContent(),
                        emailLog.getTextContent(),
                        emailLog.getUserId(),
                        emailLog.getEventType()
                    ).get();
                } else {
                    success = sendHtmlEmail(
                        emailLog.getToEmail(),
                        emailLog.getToName(),
                        emailLog.getSubject(),
                        emailLog.getHtmlContent(),
                        emailLog.getTextContent(),
                        emailLog.getUserId(),
                        emailLog.getEventType()
                    ).get();
                }
                
                if (success) {
                    emailLog.markAsSent();
                    logger.info("Email retry successful for: {}", emailLog.getToEmail());
                } else {
                    emailLog.markAsFailed("Retry failed");
                }
                
            } catch (Exception e) {
                emailLog.markAsFailed("Retry exception: " + e.getMessage());
                logger.error("Email retry failed for: {}", emailLog.getToEmail(), e);
            }
            
            emailLogRepository.save(emailLog);
        }
    }

    /**
     * 이메일 로그 조회
     */
    @Transactional(readOnly = true)
    public Page<EmailLog> getEmailLogs(Pageable pageable) {
        return emailLogRepository.findAll(pageable);
    }

    /**
     * 상태별 이메일 로그 조회
     */
    @Transactional(readOnly = true)
    public Page<EmailLog> getEmailLogsByStatus(EmailLog.EmailStatus status, Pageable pageable) {
        return emailLogRepository.findByStatusOrderBySentAtDesc(status, pageable);
    }

    /**
     * 사용자별 이메일 로그 조회
     */
    @Transactional(readOnly = true)
    public Page<EmailLog> getEmailLogsByUser(Long userId, Pageable pageable) {
        return emailLogRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
    }

    /**
     * 이메일 통계 조회
     */
    @Transactional(readOnly = true)
    public EmailStatistics getEmailStatistics() {
        List<Object[]> statusStats = emailLogRepository.getEmailStatsByStatus();
        List<Object[]> templateStats = emailLogRepository.getEmailStatsByTemplate();
        
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        Long successCount = emailLogRepository.countSuccessfulEmailsInPeriod(weekAgo, LocalDateTime.now());
        Long failedCount = emailLogRepository.countFailedEmailsInPeriod(weekAgo, LocalDateTime.now());
        
        return new EmailStatistics(statusStats, templateStats, successCount, failedCount);
    }

    /**
     * 간단한 템플릿 처리 (변수 치환)
     */
    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }

    /**
     * 이메일 통계 클래스
     */
    public static class EmailStatistics {
        private final List<Object[]> statusStats;
        private final List<Object[]> templateStats;
        private final Long weeklySuccessCount;
        private final Long weeklyFailedCount;

        public EmailStatistics(List<Object[]> statusStats, List<Object[]> templateStats, 
                              Long weeklySuccessCount, Long weeklyFailedCount) {
            this.statusStats = statusStats;
            this.templateStats = templateStats;
            this.weeklySuccessCount = weeklySuccessCount;
            this.weeklyFailedCount = weeklyFailedCount;
        }

        public List<Object[]> getStatusStats() {
            return statusStats;
        }

        public List<Object[]> getTemplateStats() {
            return templateStats;
        }

        public Long getWeeklySuccessCount() {
            return weeklySuccessCount;
        }

        public Long getWeeklyFailedCount() {
            return weeklyFailedCount;
        }

        public double getSuccessRate() {
            long total = weeklySuccessCount + weeklyFailedCount;
            return total > 0 ? (double) weeklySuccessCount / total * 100 : 0.0;
        }
    }
} 