package com.tofumaker.service;

import com.tofumaker.config.EmailConfig;
import com.tofumaker.entity.EmailLog;
import com.tofumaker.entity.EmailTemplate;
import com.tofumaker.repository.EmailLogRepository;
import com.tofumaker.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private EmailTemplate testTemplate;
    private EmailLog testEmailLog;

    @BeforeEach
    void setUp() {
        testTemplate = new EmailTemplate(
            "WELCOME_EMAIL",
            "환영 이메일",
            "TofuMaker에 오신 것을 환영합니다!",
            "<h1>환영합니다, {{userName}}님!</h1><p>TofuMaker에 가입해주셔서 감사합니다.</p>",
            "환영합니다, {{userName}}님! TofuMaker에 가입해주셔서 감사합니다."
        );
        testTemplate.setId(1L);

        testEmailLog = new EmailLog("test@example.com", "Test Subject", "WELCOME_EMAIL");
        testEmailLog.setId(1L);
        testEmailLog.setToName("Test User");
        testEmailLog.setUserId(1L);

        // EmailConfig 모킹
        when(emailConfig.isEnabled()).thenReturn(true);
        when(emailConfig.getFromEmail()).thenReturn("noreply@tofumaker.com");
        when(emailConfig.getFromName()).thenReturn("TofuMaker");
    }

    @Test
    void sendSimpleEmail_Success() throws Exception {
        // Given
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(testEmailLog);

        // When
        CompletableFuture<Boolean> result = emailService.sendSimpleEmail(
            "test@example.com", "Test User", "Test Subject", "Test Content", 1L, "TEST_EVENT");

        // Then
        assertTrue(result.get());
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(emailLogRepository).save(any(EmailLog.class));
    }

    @Test
    void sendSimpleEmail_EmailDisabled_ReturnsFalse() throws Exception {
        // Given
        when(emailConfig.isEnabled()).thenReturn(false);

        // When
        CompletableFuture<Boolean> result = emailService.sendSimpleEmail(
            "test@example.com", "Test Subject", "Test Content");

        // Then
        assertFalse(result.get());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendHtmlEmail_Success() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(testEmailLog);

        // When
        CompletableFuture<Boolean> result = emailService.sendHtmlEmail(
            "test@example.com", "Test Subject", "<h1>Test HTML Content</h1>");

        // Then
        assertTrue(result.get());
        verify(mailSender).send(any(MimeMessage.class));
        verify(emailLogRepository).save(any(EmailLog.class));
    }

    @Test
    void sendTemplateEmail_Success() throws Exception {
        // Given
        when(templateRepository.findByTemplateCodeAndActiveTrue("WELCOME_EMAIL"))
            .thenReturn(Optional.of(testTemplate));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(testEmailLog);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "Test User");

        // When
        CompletableFuture<Boolean> result = emailService.sendTemplateEmail(
            "WELCOME_EMAIL", "test@example.com", "Test User", variables, 1L, "USER_REGISTRATION");

        // Then
        assertTrue(result.get());
        verify(templateRepository).findByTemplateCodeAndActiveTrue("WELCOME_EMAIL");
        verify(mailSender).send(any(MimeMessage.class));
        verify(emailLogRepository).save(any(EmailLog.class));
    }

    @Test
    void sendTemplateEmail_TemplateNotFound_ReturnsFalse() throws Exception {
        // Given
        when(templateRepository.findByTemplateCodeAndActiveTrue("NONEXISTENT_TEMPLATE"))
            .thenReturn(Optional.empty());

        // When
        CompletableFuture<Boolean> result = emailService.sendTemplateEmail(
            "NONEXISTENT_TEMPLATE", "test@example.com", new HashMap<>());

        // Then
        assertFalse(result.get());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void getEmailLogs_Success() {
        // Given
        List<EmailLog> logs = Arrays.asList(testEmailLog);
        Page<EmailLog> page = new PageImpl<>(logs);
        Pageable pageable = PageRequest.of(0, 10);
        when(emailLogRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<EmailLog> result = emailService.getEmailLogs(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testEmailLog, result.getContent().get(0));
    }

    @Test
    void getEmailLogsByStatus_Success() {
        // Given
        List<EmailLog> logs = Arrays.asList(testEmailLog);
        Page<EmailLog> page = new PageImpl<>(logs);
        Pageable pageable = PageRequest.of(0, 10);
        when(emailLogRepository.findByStatusOrderBySentAtDesc(EmailLog.EmailStatus.SENT, pageable))
            .thenReturn(page);

        // When
        Page<EmailLog> result = emailService.getEmailLogsByStatus(EmailLog.EmailStatus.SENT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testEmailLog, result.getContent().get(0));
    }

    @Test
    void getEmailLogsByUser_Success() {
        // Given
        List<EmailLog> logs = Arrays.asList(testEmailLog);
        Page<EmailLog> page = new PageImpl<>(logs);
        Pageable pageable = PageRequest.of(0, 10);
        when(emailLogRepository.findByUserIdOrderBySentAtDesc(1L, pageable)).thenReturn(page);

        // When
        Page<EmailLog> result = emailService.getEmailLogsByUser(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testEmailLog, result.getContent().get(0));
    }

    @Test
    void getEmailStatistics_Success() {
        // Given
        List<Object[]> statusStats = Arrays.asList(
            new Object[]{EmailLog.EmailStatus.SENT, 10L},
            new Object[]{EmailLog.EmailStatus.FAILED, 2L}
        );
        List<Object[]> templateStats = Arrays.asList(
            new Object[]{"WELCOME_EMAIL", 5L},
            new Object[]{"NOTIFICATION_EMAIL", 3L}
        );

        when(emailLogRepository.getEmailStatsByStatus()).thenReturn(statusStats);
        when(emailLogRepository.getEmailStatsByTemplate()).thenReturn(templateStats);
        when(emailLogRepository.countSuccessfulEmailsInPeriod(any(), any())).thenReturn(8L);
        when(emailLogRepository.countFailedEmailsInPeriod(any(), any())).thenReturn(2L);

        // When
        EmailService.EmailStatistics result = emailService.getEmailStatistics();

        // Then
        assertNotNull(result);
        assertEquals(statusStats, result.getStatusStats());
        assertEquals(templateStats, result.getTemplateStats());
        assertEquals(8L, result.getWeeklySuccessCount());
        assertEquals(2L, result.getWeeklyFailedCount());
        assertEquals(80.0, result.getSuccessRate(), 0.01);
    }

    @Test
    void retryFailedEmails_Success() {
        // Given
        EmailLog failedEmail = new EmailLog("failed@example.com", "Failed Subject", null);
        failedEmail.setStatus(EmailLog.EmailStatus.FAILED);
        failedEmail.setHtmlContent("<p>Failed content</p>");
        failedEmail.setTextContent("Failed content");

        List<EmailLog> failedEmails = Arrays.asList(failedEmail);
        when(emailLogRepository.findFailedEmailsForRetry(3)).thenReturn(failedEmails);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(failedEmail);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.retryFailedEmails();

        // Then
        verify(emailLogRepository).findFailedEmailsForRetry(3);
        verify(emailLogRepository, atLeast(2)).save(any(EmailLog.class)); // 재시도 마킹 + 결과 저장
    }

    @Test
    void emailStatistics_SuccessRate_ZeroTotal() {
        // Given
        EmailService.EmailStatistics statistics = new EmailService.EmailStatistics(
            new ArrayList<>(), new ArrayList<>(), 0L, 0L);

        // When
        double successRate = statistics.getSuccessRate();

        // Then
        assertEquals(0.0, successRate, 0.01);
    }

    @Test
    void emailStatistics_SuccessRate_CalculatedCorrectly() {
        // Given
        EmailService.EmailStatistics statistics = new EmailService.EmailStatistics(
            new ArrayList<>(), new ArrayList<>(), 7L, 3L);

        // When
        double successRate = statistics.getSuccessRate();

        // Then
        assertEquals(70.0, successRate, 0.01);
    }

    @Test
    void processTemplate_VariableSubstitution() throws Exception {
        // Given - 리플렉션을 사용하여 private 메서드 테스트
        java.lang.reflect.Method method = EmailService.class.getDeclaredMethod(
            "processTemplate", String.class, Map.class);
        method.setAccessible(true);

        String template = "Hello {{userName}}, welcome to {{appName}}!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "John");
        variables.put("appName", "TofuMaker");

        // When
        String result = (String) method.invoke(emailService, template, variables);

        // Then
        assertEquals("Hello John, welcome to TofuMaker!", result);
    }

    @Test
    void processTemplate_NullTemplate_ReturnsNull() throws Exception {
        // Given
        java.lang.reflect.Method method = EmailService.class.getDeclaredMethod(
            "processTemplate", String.class, Map.class);
        method.setAccessible(true);

        // When
        String result = (String) method.invoke(emailService, null, new HashMap<>());

        // Then
        assertNull(result);
    }

    @Test
    void processTemplate_NullVariables_ReturnsOriginal() throws Exception {
        // Given
        java.lang.reflect.Method method = EmailService.class.getDeclaredMethod(
            "processTemplate", String.class, Map.class);
        method.setAccessible(true);

        String template = "Hello {{userName}}!";

        // When
        String result = (String) method.invoke(emailService, template, null);

        // Then
        assertEquals(template, result);
    }
} 