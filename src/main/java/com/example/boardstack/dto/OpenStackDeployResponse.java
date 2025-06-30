package com.example.boardstack.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OpenStackDeployResponse {

    // 배포 요청 ID
    private String deploymentId;

    // VM 인스턴스 ID
    private String instanceId;

    // VM 이름
    private String vmName;

    // 배포 상태
    private DeploymentStatus status;

    // 상태 메시지
    private String statusMessage;

    // 생성 시간
    private LocalDateTime createdAt;

    // 마지막 업데이트 시간
    private LocalDateTime updatedAt;

    // 완료 시간
    private LocalDateTime completedAt;

    // VM 정보
    private VmInfo vmInfo;

    // 네트워크 정보
    private List<NetworkInfo> networks;

    // 오류 정보 (실패 시)
    private ErrorInfo error;

    // 요청자
    private String requestedBy;

    // 메타데이터
    private Map<String, String> metadata;

    // 배포 상태 열거형
    public enum DeploymentStatus {
        PENDING("대기중"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨");

        private final String description;

        DeploymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // VM 정보 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VmInfo {
        private String instanceId;
        private String instanceType;
        private String imageId;
        private String keyPair;
        private Integer diskSize;
        private String volumeType;
        private String availabilityZone;
        private String publicIp;
        private String privateIp;
        private String status;
        private LocalDateTime launchedAt;
    }

    // 네트워크 정보 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NetworkInfo {
        private String networkId;
        private String networkName;
        private String subnetId;
        private String ipAddress;
        private String macAddress;
        private Boolean isPrimary;
    }

    // 오류 정보 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorInfo {
        private String errorCode;
        private String errorMessage;
        private String detailMessage;
        private LocalDateTime occurredAt;
    }

    // 정적 팩토리 메서드들
    public static OpenStackDeployResponse pending(String deploymentId, String vmName, String requestedBy) {
        return OpenStackDeployResponse.builder()
                .deploymentId(deploymentId)
                .vmName(vmName)
                .status(DeploymentStatus.PENDING)
                .statusMessage("배포 요청이 접수되었습니다")
                .requestedBy(requestedBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static OpenStackDeployResponse inProgress(String deploymentId, String statusMessage) {
        return OpenStackDeployResponse.builder()
                .deploymentId(deploymentId)
                .status(DeploymentStatus.IN_PROGRESS)
                .statusMessage(statusMessage)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static OpenStackDeployResponse completed(String deploymentId, String instanceId, VmInfo vmInfo) {
        return OpenStackDeployResponse.builder()
                .deploymentId(deploymentId)
                .instanceId(instanceId)
                .status(DeploymentStatus.COMPLETED)
                .statusMessage("배포가 성공적으로 완료되었습니다")
                .vmInfo(vmInfo)
                .updatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static OpenStackDeployResponse failed(String deploymentId, ErrorInfo error) {
        return OpenStackDeployResponse.builder()
                .deploymentId(deploymentId)
                .status(DeploymentStatus.FAILED)
                .statusMessage("배포에 실패했습니다")
                .error(error)
                .updatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }
} 