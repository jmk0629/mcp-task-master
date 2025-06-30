package com.example.boardstack.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TofuMakerResponse {

    // 작업 ID
    private String jobId;

    // 작업 상태
    private JobStatus status;

    // 상태 메시지
    private String message;

    // 생성 시간
    private LocalDateTime createdAt;

    // 마지막 업데이트 시간
    private LocalDateTime updatedAt;

    // 완료 시간
    private LocalDateTime completedAt;

    // Terraform 출력 값들
    private Map<String, Object> outputs;

    // 오류 정보
    private ErrorDetails error;

    // 로그 정보
    private String logs;

    // 리소스 정보
    private Map<String, Object> resources;

    // 작업 상태 열거형
    public enum JobStatus {
        PENDING("대기중"),
        RUNNING("실행중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨");

        private final String description;

        JobStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 오류 세부 정보 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetails {
        private String code;
        private String message;
        private String details;
        private LocalDateTime timestamp;
    }

    // 성공 응답 생성 헬퍼 메서드
    public static TofuMakerResponse success(String jobId, JobStatus status, String message) {
        return TofuMakerResponse.builder()
                .jobId(jobId)
                .status(status)
                .message(message)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 실패 응답 생성 헬퍼 메서드
    public static TofuMakerResponse failure(String jobId, String errorMessage) {
        return TofuMakerResponse.builder()
                .jobId(jobId)
                .status(JobStatus.FAILED)
                .message("작업이 실패했습니다")
                .error(ErrorDetails.builder()
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .build())
                .updatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }
} 