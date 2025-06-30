package com.example.boardstack.service;

import com.example.boardstack.dto.OpenStackDeployRequest;
import com.example.boardstack.dto.OpenStackDeployResponse;
import com.example.boardstack.dto.TofuMakerResponse;
import com.example.boardstack.openstack.TofuMakerApiClient;
import com.example.boardstack.template.TfTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenStackService {

    private final TfTemplateManager templateManager;
    private final TofuMakerApiClient tofuMakerApiClient;
    
    // 배포 상태를 메모리에 저장 (실제 환경에서는 데이터베이스 사용)
    private final Map<String, OpenStackDeployResponse> deploymentCache = new ConcurrentHashMap<>();

    /**
     * VM 배포 요청 처리
     */
    public OpenStackDeployResponse deployVm(OpenStackDeployRequest request) {
        log.info("VM 배포 요청 처리 시작: {}", request.getVmName());
        
        try {
            // 1. 배포 ID 생성
            String deploymentId = generateDeploymentId();
            
            // 2. 초기 응답 생성 및 캐시 저장
            OpenStackDeployResponse initialResponse = OpenStackDeployResponse.pending(
                deploymentId, request.getVmName(), request.getRequestedBy());
            deploymentCache.put(deploymentId, initialResponse);
            
            // 3. 비동기로 실제 배포 처리
            CompletableFuture.runAsync(() -> processDeploymentAsync(deploymentId, request));
            
            log.info("VM 배포 요청 접수 완료: {}, 배포 ID: {}", request.getVmName(), deploymentId);
            return initialResponse;
            
        } catch (Exception e) {
            log.error("VM 배포 요청 처리 실패: {}", e.getMessage(), e);
            return createErrorResponse(null, "배포 요청 처리에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 배포 상태 조회
     */
    public OpenStackDeployResponse getDeploymentStatus(String deploymentId) {
        log.debug("배포 상태 조회: {}", deploymentId);
        
        OpenStackDeployResponse cachedResponse = deploymentCache.get(deploymentId);
        if (cachedResponse == null) {
            log.warn("배포 ID를 찾을 수 없음: {}", deploymentId);
            return createErrorResponse(deploymentId, "배포 ID를 찾을 수 없습니다");
        }
        
        // 진행 중인 작업의 경우 최신 상태 확인
        if (cachedResponse.getStatus() == OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS) {
            updateDeploymentStatus(deploymentId);
            cachedResponse = deploymentCache.get(deploymentId);
        }
        
        return cachedResponse;
    }

    /**
     * VM 삭제 (리소스 정리)
     */
    public OpenStackDeployResponse destroyVm(String deploymentId) {
        log.info("VM 삭제 요청: {}", deploymentId);
        
        try {
            OpenStackDeployResponse deployment = deploymentCache.get(deploymentId);
            if (deployment == null) {
                return createErrorResponse(deploymentId, "배포 ID를 찾을 수 없습니다");
            }
            
            if (deployment.getInstanceId() == null) {
                return createErrorResponse(deploymentId, "삭제할 인스턴스가 없습니다");
            }
            
            // TofuMaker API를 통해 리소스 삭제 요청
            TofuMakerResponse destroyResponse = tofuMakerApiClient.destroyResources(deployment.getInstanceId());
            
            if (destroyResponse.getStatus() == TofuMakerResponse.JobStatus.PENDING ||
                destroyResponse.getStatus() == TofuMakerResponse.JobStatus.RUNNING) {
                
                // 삭제 진행 중 상태로 업데이트
                OpenStackDeployResponse updatedResponse = deployment.toBuilder()
                        .status(OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS)
                        .statusMessage("리소스 삭제 진행 중")
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                deploymentCache.put(deploymentId, updatedResponse);
                
                // 비동기로 삭제 완료 대기
                CompletableFuture.runAsync(() -> waitForDestroyCompletion(deploymentId, destroyResponse.getJobId()));
                
                return updatedResponse;
            } else {
                return createErrorResponse(deploymentId, "리소스 삭제 요청에 실패했습니다");
            }
            
        } catch (Exception e) {
            log.error("VM 삭제 실패: {}", e.getMessage(), e);
            return createErrorResponse(deploymentId, "VM 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 모든 배포 목록 조회
     */
    public Map<String, OpenStackDeployResponse> getAllDeployments() {
        log.debug("모든 배포 목록 조회");
        return new HashMap<>(deploymentCache);
    }

    /**
     * 배포 취소
     */
    public OpenStackDeployResponse cancelDeployment(String deploymentId) {
        log.info("배포 취소 요청: {}", deploymentId);
        
        try {
            OpenStackDeployResponse deployment = deploymentCache.get(deploymentId);
            if (deployment == null) {
                return createErrorResponse(deploymentId, "배포 ID를 찾을 수 없습니다");
            }
            
            if (deployment.getStatus() != OpenStackDeployResponse.DeploymentStatus.PENDING &&
                deployment.getStatus() != OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS) {
                return createErrorResponse(deploymentId, "취소할 수 없는 상태입니다: " + deployment.getStatus());
            }
            
            // TofuMaker 작업이 있는 경우 취소 요청
            if (deployment.getInstanceId() != null) {
                tofuMakerApiClient.cancelJob(deployment.getInstanceId());
            }
            
            // 취소 상태로 업데이트
            OpenStackDeployResponse cancelledResponse = deployment.toBuilder()
                    .status(OpenStackDeployResponse.DeploymentStatus.CANCELLED)
                    .statusMessage("배포가 취소되었습니다")
                    .updatedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();
            
            deploymentCache.put(deploymentId, cancelledResponse);
            
            log.info("배포 취소 완료: {}", deploymentId);
            return cancelledResponse;
            
        } catch (Exception e) {
            log.error("배포 취소 실패: {}", e.getMessage(), e);
            return createErrorResponse(deploymentId, "배포 취소에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 비동기 배포 처리
     */
    private void processDeploymentAsync(String deploymentId, OpenStackDeployRequest request) {
        try {
            log.info("비동기 배포 처리 시작: {}", deploymentId);
            
            // 1. 진행 중 상태로 업데이트
            updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS, 
                "Terraform 템플릿 생성 중");
            
            // 2. Terraform 템플릿 생성
            String terraformTemplate = templateManager.generateTerraformTemplate(request);
            
            // 3. 변수 맵 생성
            Map<String, Object> variables = createVariablesMap(request);
            
            // 4. TofuMaker에 템플릿 제출
            updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS, 
                "TofuMaker에 배포 요청 중");
            
            TofuMakerResponse submitResponse = tofuMakerApiClient.submitTemplate(terraformTemplate, variables);
            
            if (submitResponse.getStatus() == TofuMakerResponse.JobStatus.PENDING ||
                submitResponse.getStatus() == TofuMakerResponse.JobStatus.RUNNING) {
                
                // 5. 인스턴스 ID 업데이트
                OpenStackDeployResponse currentResponse = deploymentCache.get(deploymentId);
                OpenStackDeployResponse updatedResponse = currentResponse.toBuilder()
                        .instanceId(submitResponse.getJobId())
                        .statusMessage("VM 생성 진행 중")
                        .updatedAt(LocalDateTime.now())
                        .build();
                deploymentCache.put(deploymentId, updatedResponse);
                
                // 6. 완료까지 대기
                TofuMakerResponse finalResponse = tofuMakerApiClient.waitForCompletion(
                    submitResponse.getJobId(), 30); // 30분 타임아웃
                
                // 7. 최종 결과 처리
                handleDeploymentCompletion(deploymentId, finalResponse);
                
            } else {
                // 제출 실패
                updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.FAILED, 
                    "템플릿 제출에 실패했습니다: " + submitResponse.getMessage());
            }
            
        } catch (Exception e) {
            log.error("비동기 배포 처리 실패: {}", e.getMessage(), e);
            updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.FAILED, 
                "배포 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 배포 완료 처리
     */
    private void handleDeploymentCompletion(String deploymentId, TofuMakerResponse tofuResponse) {
        OpenStackDeployResponse currentResponse = deploymentCache.get(deploymentId);
        
        if (tofuResponse.getStatus() == TofuMakerResponse.JobStatus.COMPLETED) {
            // 성공 처리
            OpenStackDeployResponse.VmInfo vmInfo = extractVmInfo(tofuResponse);
            
            OpenStackDeployResponse completedResponse = currentResponse.toBuilder()
                    .status(OpenStackDeployResponse.DeploymentStatus.COMPLETED)
                    .statusMessage("VM 배포가 성공적으로 완료되었습니다")
                    .vmInfo(vmInfo)
                    .updatedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();
            
            deploymentCache.put(deploymentId, completedResponse);
            log.info("배포 완료: {}", deploymentId);
            
        } else {
            // 실패 처리
            String errorMessage = tofuResponse.getError() != null ? 
                tofuResponse.getError().getMessage() : "알 수 없는 오류";
            
            OpenStackDeployResponse.ErrorInfo errorInfo = OpenStackDeployResponse.ErrorInfo.builder()
                    .errorCode("DEPLOYMENT_FAILED")
                    .errorMessage(errorMessage)
                    .detailMessage(tofuResponse.getLogs())
                    .occurredAt(LocalDateTime.now())
                    .build();
            
            OpenStackDeployResponse failedResponse = currentResponse.toBuilder()
                    .status(OpenStackDeployResponse.DeploymentStatus.FAILED)
                    .statusMessage("VM 배포에 실패했습니다")
                    .error(errorInfo)
                    .updatedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();
            
            deploymentCache.put(deploymentId, failedResponse);
            log.error("배포 실패: {}, 오류: {}", deploymentId, errorMessage);
        }
    }

    /**
     * TofuMaker 응답에서 VM 정보 추출
     */
    private OpenStackDeployResponse.VmInfo extractVmInfo(TofuMakerResponse tofuResponse) {
        Map<String, Object> outputs = tofuResponse.getOutputs();
        if (outputs == null) {
            return null;
        }
        
        return OpenStackDeployResponse.VmInfo.builder()
                .instanceId((String) outputs.get("instance_id"))
                .privateIp((String) outputs.get("private_ip"))
                .publicIp((String) outputs.get("public_ip"))
                .status("ACTIVE")
                .launchedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 삭제 완료 대기
     */
    private void waitForDestroyCompletion(String deploymentId, String destroyJobId) {
        try {
            TofuMakerResponse destroyResult = tofuMakerApiClient.waitForCompletion(destroyJobId, 15);
            
            OpenStackDeployResponse currentResponse = deploymentCache.get(deploymentId);
            if (currentResponse != null) {
                if (destroyResult.getStatus() == TofuMakerResponse.JobStatus.COMPLETED) {
                    // 삭제 완료
                    OpenStackDeployResponse deletedResponse = currentResponse.toBuilder()
                            .status(OpenStackDeployResponse.DeploymentStatus.COMPLETED)
                            .statusMessage("리소스가 성공적으로 삭제되었습니다")
                            .updatedAt(LocalDateTime.now())
                            .completedAt(LocalDateTime.now())
                            .build();
                    deploymentCache.put(deploymentId, deletedResponse);
                } else {
                    // 삭제 실패
                    updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.FAILED,
                        "리소스 삭제에 실패했습니다");
                }
            }
        } catch (Exception e) {
            log.error("삭제 완료 대기 중 오류: {}", e.getMessage(), e);
            updateDeploymentStatus(deploymentId, OpenStackDeployResponse.DeploymentStatus.FAILED,
                "삭제 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 배포 상태 업데이트
     */
    private void updateDeploymentStatus(String deploymentId) {
        OpenStackDeployResponse deployment = deploymentCache.get(deploymentId);
        if (deployment != null && deployment.getInstanceId() != null) {
            TofuMakerResponse tofuResponse = tofuMakerApiClient.getJobStatus(deployment.getInstanceId());
            
            // TofuMaker 상태를 배포 상태로 변환
            OpenStackDeployResponse.DeploymentStatus newStatus = mapTofuStatusToDeploymentStatus(tofuResponse.getStatus());
            
            if (newStatus != deployment.getStatus()) {
                OpenStackDeployResponse updatedResponse = deployment.toBuilder()
                        .status(newStatus)
                        .statusMessage(tofuResponse.getMessage())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                if (newStatus == OpenStackDeployResponse.DeploymentStatus.COMPLETED ||
                    newStatus == OpenStackDeployResponse.DeploymentStatus.FAILED) {
                    updatedResponse = updatedResponse.toBuilder()
                            .completedAt(LocalDateTime.now())
                            .build();
                }
                
                deploymentCache.put(deploymentId, updatedResponse);
            }
        }
    }

    /**
     * 배포 상태 업데이트 (상태와 메시지 지정)
     */
    private void updateDeploymentStatus(String deploymentId, 
                                      OpenStackDeployResponse.DeploymentStatus status, 
                                      String message) {
        OpenStackDeployResponse currentResponse = deploymentCache.get(deploymentId);
        if (currentResponse != null) {
            OpenStackDeployResponse updatedResponse = currentResponse.toBuilder()
                    .status(status)
                    .statusMessage(message)
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            deploymentCache.put(deploymentId, updatedResponse);
        }
    }

    /**
     * TofuMaker 상태를 배포 상태로 매핑
     */
    private OpenStackDeployResponse.DeploymentStatus mapTofuStatusToDeploymentStatus(TofuMakerResponse.JobStatus tofuStatus) {
        switch (tofuStatus) {
            case PENDING:
            case RUNNING:
                return OpenStackDeployResponse.DeploymentStatus.IN_PROGRESS;
            case COMPLETED:
                return OpenStackDeployResponse.DeploymentStatus.COMPLETED;
            case FAILED:
                return OpenStackDeployResponse.DeploymentStatus.FAILED;
            case CANCELLED:
                return OpenStackDeployResponse.DeploymentStatus.CANCELLED;
            default:
                return OpenStackDeployResponse.DeploymentStatus.PENDING;
        }
    }

    /**
     * 변수 맵 생성
     */
    private Map<String, Object> createVariablesMap(OpenStackDeployRequest request) {
        Map<String, Object> variables = new HashMap<>();
        
        // 기본 변수들
        variables.put("vm_name", request.getVmName());
        variables.put("instance_type", request.getInstanceType());
        variables.put("disk_size", request.getDiskSize());
        variables.put("requested_by", request.getRequestedBy());
        
        // 메타데이터가 있는 경우 추가
        if (request.getMetadata() != null) {
            variables.putAll(request.getMetadata());
        }
        
        return variables;
    }

    /**
     * 배포 ID 생성
     */
    private String generateDeploymentId() {
        return "deploy-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 오류 응답 생성
     */
    private OpenStackDeployResponse createErrorResponse(String deploymentId, String errorMessage) {
        return OpenStackDeployResponse.failed(deploymentId, 
            OpenStackDeployResponse.ErrorInfo.builder()
                .errorCode("SERVICE_ERROR")
                .errorMessage(errorMessage)
                .occurredAt(LocalDateTime.now())
                .build());
    }
} 