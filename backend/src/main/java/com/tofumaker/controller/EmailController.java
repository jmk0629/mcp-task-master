package com.tofumaker.controller;

import com.tofumaker.entity.EmailLog;
import com.tofumaker.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/email")
@Tag(name = "Email", description = "이메일 전송 및 관리 API")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;

    @PostMapping("/send/simple")
    @Operation(summary = "단순 텍스트 이메일 전송", description = "단순한 텍스트 이메일을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 전송 요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> sendSimpleEmail(
            @Parameter(description = "수신자 이메일", required = true)
            @RequestParam String to,
            @Parameter(description = "수신자 이름")
            @RequestParam(required = false) String toName,
            @Parameter(description = "이메일 제목", required = true)
            @RequestParam String subject,
            @Parameter(description = "이메일 내용", required = true)
            @RequestParam String text,
            @Parameter(description = "사용자 ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "이벤트 타입")
            @RequestParam(required = false) String eventType) {

        try {
            CompletableFuture<Boolean> future = emailService.sendSimpleEmail(
                to, toName, subject, text, userId, eventType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email sending request submitted successfully");
            response.put("to", to);
            response.put("subject", subject);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending simple email", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/send/html")
    @Operation(summary = "HTML 이메일 전송", description = "HTML 형식의 이메일을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 전송 요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> sendHtmlEmail(
            @Parameter(description = "수신자 이메일", required = true)
            @RequestParam String to,
            @Parameter(description = "수신자 이름")
            @RequestParam(required = false) String toName,
            @Parameter(description = "이메일 제목", required = true)
            @RequestParam String subject,
            @Parameter(description = "HTML 내용", required = true)
            @RequestParam String htmlContent,
            @Parameter(description = "텍스트 내용")
            @RequestParam(required = false) String textContent,
            @Parameter(description = "사용자 ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "이벤트 타입")
            @RequestParam(required = false) String eventType) {

        try {
            CompletableFuture<Boolean> future = emailService.sendHtmlEmail(
                to, toName, subject, htmlContent, textContent, userId, eventType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HTML email sending request submitted successfully");
            response.put("to", to);
            response.put("subject", subject);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending HTML email", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send HTML email: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/send/template")
    @Operation(summary = "템플릿 이메일 전송", description = "미리 정의된 템플릿을 사용하여 이메일을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 전송 요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> sendTemplateEmail(
            @Parameter(description = "템플릿 코드", required = true)
            @RequestParam String templateCode,
            @Parameter(description = "수신자 이메일", required = true)
            @RequestParam String to,
            @Parameter(description = "수신자 이름")
            @RequestParam(required = false) String toName,
            @Parameter(description = "템플릿 변수 (JSON 형식)")
            @RequestParam(required = false) Map<String, Object> variables,
            @Parameter(description = "사용자 ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "이벤트 타입")
            @RequestParam(required = false) String eventType) {

        try {
            CompletableFuture<Boolean> future = emailService.sendTemplateEmail(
                templateCode, to, toName, variables, userId, eventType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Template email sending request submitted successfully");
            response.put("templateCode", templateCode);
            response.put("to", to);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending template email", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send template email: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/retry-failed")
    @Operation(summary = "실패한 이메일 재시도", description = "전송에 실패한 이메일들을 재시도합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "재시도 요청 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> retryFailedEmails() {
        try {
            emailService.retryFailedEmails();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Failed email retry process started");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrying failed emails", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to start retry process: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/logs")
    @Operation(summary = "이메일 로그 조회", description = "이메일 전송 로그를 페이징으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 로그 조회 성공")
    })
    public ResponseEntity<Page<EmailLog>> getEmailLogs(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 (sentAt, status, toEmail)")
            @RequestParam(defaultValue = "sentAt") String sortBy,
            @Parameter(description = "정렬 방향 (asc, desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EmailLog> logs = emailService.getEmailLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/status/{status}")
    @Operation(summary = "상태별 이메일 로그 조회", description = "특정 상태의 이메일 로그를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태별 이메일 로그 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 상태 값")
    })
    public ResponseEntity<Page<EmailLog>> getEmailLogsByStatus(
            @Parameter(description = "이메일 상태 (PENDING, SENT, FAILED, RETRY)", required = true)
            @PathVariable String status,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {

        try {
            EmailLog.EmailStatus emailStatus = EmailLog.EmailStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());

            Page<EmailLog> logs = emailService.getEmailLogsByStatus(emailStatus, pageable);
            return ResponseEntity.ok(logs);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid email status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "사용자별 이메일 로그 조회", description = "특정 사용자의 이메일 로그를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자별 이메일 로그 조회 성공")
    })
    public ResponseEntity<Page<EmailLog>> getEmailLogsByUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<EmailLog> logs = emailService.getEmailLogsByUser(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @Operation(summary = "이메일 통계", description = "이메일 전송 통계 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 통계 조회 성공")
    })
    public ResponseEntity<EmailService.EmailStatistics> getEmailStatistics() {
        EmailService.EmailStatistics statistics = emailService.getEmailStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/test")
    @Operation(summary = "이메일 테스트", description = "이메일 서비스 테스트를 위한 간단한 이메일을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "테스트 이메일 전송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<?> testEmail(
            @Parameter(description = "테스트 수신자 이메일", required = true)
            @RequestParam String to) {

        try {
            String subject = "TofuMaker 이메일 서비스 테스트";
            String text = "안녕하세요!\n\n이것은 TofuMaker 이메일 서비스의 테스트 메시지입니다.\n\n" +
                         "이 메시지를 받으셨다면 이메일 서비스가 정상적으로 작동하고 있습니다.\n\n" +
                         "감사합니다.\nTofuMaker 팀";

            CompletableFuture<Boolean> future = emailService.sendSimpleEmail(
                to, null, subject, text, null, "EMAIL_TEST");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test email sent successfully");
            response.put("to", to);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending test email", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 