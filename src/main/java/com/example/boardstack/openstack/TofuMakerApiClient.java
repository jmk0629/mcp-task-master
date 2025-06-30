package com.example.boardstack.openstack;

import com.example.boardstack.dto.TofuMakerResponse;
import com.example.boardstack.config.OpenStackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TofuMakerApiClient {

    private final RestTemplate restTemplate;
    private final OpenStackProperties openStackProperties;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * Terraform 템플릿 제출
     */
    public TofuMakerResponse submitTemplate(String templateContent, Map<String, Object> variables) {
        log.info("TofuMaker에 템플릿 제출 시작");
        
        try {
            String url = openStackProperties.getTofumaker().getBaseUrl() + "/jobs";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("template", templateContent);
            requestBody.put("variables", variables != null ? variables : new HashMap<>());
            requestBody.put("action", "apply");
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED || 
                response.getStatusCode() == HttpStatus.OK) {
                
                Map<String, Object> responseBody = response.getBody();
                String jobId = (String) responseBody.get("jobId");
                
                log.info("템플릿 제출 성공, Job ID: {}", jobId);
                
                return TofuMakerResponse.builder()
                        .jobId(jobId)
                        .status(TofuMakerResponse.JobStatus.PENDING)
                        .message("템플릿이 성공적으로 제출되었습니다")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            } else {
                throw new RuntimeException("예상치 못한 응답 코드: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("템플릿 제출 실패: {}", e.getMessage(), e);
            return createErrorResponse(null, "템플릿 제출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 작업 상태 조회
     */
    public TofuMakerResponse getJobStatus(String jobId) {
        log.debug("작업 상태 조회: {}", jobId);
        
        return executeWithRetry(() -> {
            String url = openStackProperties.getTofumaker().getBaseUrl() + "/jobs/" + jobId;
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return mapResponseToTofuMakerResponse(response.getBody());
            } else {
                throw new RuntimeException("작업 상태 조회 실패: " + response.getStatusCode());
            }
        }, "작업 상태 조회");
    }

    /**
     * 작업 로그 조회
     */
    public String getJobLogs(String jobId) {
        log.debug("작업 로그 조회: {}", jobId);
        
        try {
            String url = openStackProperties.getTofumaker().getBaseUrl() + "/jobs/" + jobId + "/logs";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                log.warn("로그 조회 실패: {}", response.getStatusCode());
                return "로그를 가져올 수 없습니다";
            }
            
        } catch (Exception e) {
            log.error("로그 조회 중 오류 발생: {}", e.getMessage(), e);
            return "로그 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 리소스 삭제 (destroy)
     */
    public TofuMakerResponse destroyResources(String jobId) {
        log.info("리소스 삭제 요청: {}", jobId);
        
        try {
            String url = openStackProperties.getTofumaker().getBaseUrl() + "/jobs/" + jobId + "/destroy";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.ACCEPTED) {
                
                Map<String, Object> responseBody = response.getBody();
                String destroyJobId = (String) responseBody.get("jobId");
                
                log.info("리소스 삭제 요청 성공, Destroy Job ID: {}", destroyJobId);
                
                return TofuMakerResponse.builder()
                        .jobId(destroyJobId)
                        .status(TofuMakerResponse.JobStatus.PENDING)
                        .message("리소스 삭제가 요청되었습니다")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            } else {
                throw new RuntimeException("리소스 삭제 요청 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("리소스 삭제 요청 실패: {}", e.getMessage(), e);
            return createErrorResponse(jobId, "리소스 삭제 요청에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 작업 취소
     */
    public TofuMakerResponse cancelJob(String jobId) {
        log.info("작업 취소 요청: {}", jobId);
        
        try {
            String url = openStackProperties.getTofumaker().getBaseUrl() + "/jobs/" + jobId + "/cancel";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("작업 취소 성공: {}", jobId);
                
                return TofuMakerResponse.builder()
                        .jobId(jobId)
                        .status(TofuMakerResponse.JobStatus.CANCELLED)
                        .message("작업이 취소되었습니다")
                        .updatedAt(LocalDateTime.now())
                        .completedAt(LocalDateTime.now())
                        .build();
            } else {
                throw new RuntimeException("작업 취소 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("작업 취소 실패: {}", e.getMessage(), e);
            return createErrorResponse(jobId, "작업 취소에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 작업 완료까지 대기 (폴링)
     */
    public TofuMakerResponse waitForCompletion(String jobId, long timeoutMinutes) {
        log.info("작업 완료 대기 시작: {}, 타임아웃: {}분", jobId, timeoutMinutes);
        
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutMinutes * 60 * 1000;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            TofuMakerResponse response = getJobStatus(jobId);
            
            if (response.getStatus() == TofuMakerResponse.JobStatus.COMPLETED ||
                response.getStatus() == TofuMakerResponse.JobStatus.FAILED ||
                response.getStatus() == TofuMakerResponse.JobStatus.CANCELLED) {
                
                log.info("작업 완료: {}, 상태: {}", jobId, response.getStatus());
                return response;
            }
            
            try {
                Thread.sleep(5000); // 5초마다 폴링
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("대기 중 인터럽트 발생");
                break;
            }
        }
        
        log.warn("작업 완료 대기 타임아웃: {}", jobId);
        return createErrorResponse(jobId, "작업 완료 대기 시간이 초과되었습니다");
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "BoardStack/1.0");
        
        // 인증이 필요한 경우 여기에 추가
        // headers.set("Authorization", "Bearer " + token);
        
        return headers;
    }

    /**
     * API 응답을 TofuMakerResponse로 변환
     */
    private TofuMakerResponse mapResponseToTofuMakerResponse(Map<String, Object> responseBody) {
        String jobId = (String) responseBody.get("jobId");
        String status = (String) responseBody.get("status");
        String message = (String) responseBody.get("message");
        
        TofuMakerResponse.JobStatus jobStatus;
        try {
            jobStatus = TofuMakerResponse.JobStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            log.warn("알 수 없는 작업 상태: {}", status);
            jobStatus = TofuMakerResponse.JobStatus.PENDING;
        }
        
        TofuMakerResponse.TofuMakerResponseBuilder builder = TofuMakerResponse.builder()
                .jobId(jobId)
                .status(jobStatus)
                .message(message)
                .updatedAt(LocalDateTime.now());
        
        // 출력 값 설정
        if (responseBody.containsKey("outputs")) {
            builder.outputs((Map<String, Object>) responseBody.get("outputs"));
        }
        
        // 로그 설정
        if (responseBody.containsKey("logs")) {
            builder.logs((String) responseBody.get("logs"));
        }
        
        // 리소스 정보 설정
        if (responseBody.containsKey("resources")) {
            builder.resources((Map<String, Object>) responseBody.get("resources"));
        }
        
        // 오류 정보 설정
        if (responseBody.containsKey("error")) {
            Map<String, Object> errorMap = (Map<String, Object>) responseBody.get("error");
            TofuMakerResponse.ErrorDetails error = TofuMakerResponse.ErrorDetails.builder()
                    .code((String) errorMap.get("code"))
                    .message((String) errorMap.get("message"))
                    .details((String) errorMap.get("details"))
                    .timestamp(LocalDateTime.now())
                    .build();
            builder.error(error);
        }
        
        return builder.build();
    }

    /**
     * 오류 응답 생성
     */
    private TofuMakerResponse createErrorResponse(String jobId, String errorMessage) {
        return TofuMakerResponse.builder()
                .jobId(jobId)
                .status(TofuMakerResponse.JobStatus.FAILED)
                .message("API 호출 실패")
                .error(TofuMakerResponse.ErrorDetails.builder()
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .build())
                .updatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 재시도 로직이 포함된 실행
     */
    private TofuMakerResponse executeWithRetry(RetryableOperation operation, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                lastException = e;
                if (e.getStatusCode().is4xxClientError()) {
                    // 4xx 오류는 재시도하지 않음
                    break;
                }
                log.warn("{} 실패 (시도 {}/{}): {}", operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("{} 네트워크 오류 (시도 {}/{}): {}", operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
            } catch (Exception e) {
                lastException = e;
                log.error("{} 예상치 못한 오류 (시도 {}/{}): {}", operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                break;
            }
            
            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt); // 지수 백오프
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("{} 최종 실패: {}", operationName, lastException.getMessage());
        return createErrorResponse(null, operationName + " 실패: " + lastException.getMessage());
    }

    /**
     * 재시도 가능한 작업 인터페이스
     */
    @FunctionalInterface
    private interface RetryableOperation {
        TofuMakerResponse execute() throws Exception;
    }
} 