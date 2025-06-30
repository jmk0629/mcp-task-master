package com.example.boardstack.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenStackDeployRequest {

    @NotBlank(message = "VM 이름은 필수입니다")
    @Size(min = 3, max = 50, message = "VM 이름은 3-50자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "VM 이름은 영문, 숫자, 하이픈, 언더스코어만 사용 가능합니다")
    private String vmName;

    @NotBlank(message = "인스턴스 타입은 필수입니다")
    private String instanceType;

    @NotBlank(message = "이미지 ID는 필수입니다")
    private String imageId;

    @NotBlank(message = "네트워크 ID는 필수입니다")
    private String networkId;

    @NotBlank(message = "보안 그룹은 필수입니다")
    private String securityGroup;

    @NotBlank(message = "키페어는 필수입니다")
    private String keyPair;

    @NotNull(message = "디스크 크기는 필수입니다")
    @Min(value = 10, message = "디스크 크기는 최소 10GB 이상이어야 합니다")
    @Max(value = 1000, message = "디스크 크기는 최대 1000GB 이하여야 합니다")
    private Integer diskSize;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    // 추가 네트워크 설정
    private List<String> additionalNetworks;

    // 추가 보안 그룹
    private List<String> additionalSecurityGroups;

    // 사용자 데이터 (cloud-init 스크립트 등)
    @Size(max = 16384, message = "사용자 데이터는 16KB를 초과할 수 없습니다")
    private String userData;

    // 메타데이터
    private Map<String, String> metadata;

    // 가용 영역
    private String availabilityZone;

    // 부팅 볼륨 타입
    @Builder.Default
    private String volumeType = "standard";

    // 부팅 볼륨 삭제 옵션
    @Builder.Default
    private Boolean deleteOnTermination = true;

    // 요청자 정보
    @NotBlank(message = "요청자는 필수입니다")
    @Size(max = 100, message = "요청자는 100자를 초과할 수 없습니다")
    private String requestedBy;

    // 프로젝트/테넌트 ID
    private String projectId;

    // 태그
    private Map<String, String> tags;

    // 자동 IP 할당 여부
    @Builder.Default
    private Boolean autoAssignFloatingIp = false;

    // 백업 설정
    @Builder.Default
    private Boolean enableBackup = false;

    // 모니터링 설정
    @Builder.Default
    private Boolean enableMonitoring = true;
} 