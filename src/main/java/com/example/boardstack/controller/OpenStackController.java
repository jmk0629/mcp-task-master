package com.example.boardstack.controller;

import com.example.boardstack.dto.OpenStackDeployRequest;
import com.example.boardstack.dto.OpenStackDeployResponse;
import com.example.boardstack.service.OpenStackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/openstack")
@RequiredArgsConstructor
@Slf4j
public class OpenStackController {

    private final OpenStackService openStackService;

    /**
     * VM 배포 요청
     * POST /api/openstack/deploy
     */
    @PostMapping("/deploy")
    public ResponseEntity<OpenStackDeployResponse> deployVm(@Valid @RequestBody OpenStackDeployRequest request) {
        log.info("VM 배포 요청: {}", request.getVmName());
        
        try {
            OpenStackDeployResponse response = openStackService.deployVm(request);
            
            if (response.getStatus() == OpenStackDeployResponse.DeploymentStatus.PENDING) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("VM 배포 요청 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("VM 배포 요청 처리 중 오류가 발생했습니다"));
        }
    }

    /**
     * 배포 상태 조회
     * GET /api/openstack/deployments/{deploymentId}
     */
    @GetMapping("/deployments/{deploymentId}")
    public ResponseEntity<OpenStackDeployResponse> getDeploymentStatus(@PathVariable String deploymentId) {
        log.debug("배포 상태 조회: {}", deploymentId);
        
        try {
            OpenStackDeployResponse response = openStackService.getDeploymentStatus(deploymentId);
            
            if (response.getStatus() == OpenStackDeployResponse.DeploymentStatus.FAILED && 
                response.getError() != null && 
                "배포 ID를 찾을 수 없습니다".equals(response.getError().getErrorMessage())) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배포 상태 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("배포 상태 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 모든 배포 목록 조회
     * GET /api/openstack/deployments
     */
    @GetMapping("/deployments")
    public ResponseEntity<Map<String, OpenStackDeployResponse>> getAllDeployments() {
        log.debug("모든 배포 목록 조회");
        
        try {
            Map<String, OpenStackDeployResponse> deployments = openStackService.getAllDeployments();
            return ResponseEntity.ok(deployments);
            
        } catch (Exception e) {
            log.error("배포 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * VM 삭제 (리소스 정리)
     * DELETE /api/openstack/deployments/{deploymentId}
     */
    @DeleteMapping("/deployments/{deploymentId}")
    public ResponseEntity<OpenStackDeployResponse> destroyVm(@PathVariable String deploymentId) {
        log.info("VM 삭제 요청: {}", deploymentId);
        
        try {
            OpenStackDeployResponse response = openStackService.destroyVm(deploymentId);
            
            if (response.getStatus() == OpenStackDeployResponse.DeploymentStatus.FAILED && 
                response.getError() != null && 
                "배포 ID를 찾을 수 없습니다".equals(response.getError().getErrorMessage())) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("VM 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("VM 삭제 중 오류가 발생했습니다"));
        }
    }

    /**
     * 배포 취소
     * POST /api/openstack/deployments/{deploymentId}/cancel
     */
    @PostMapping("/deployments/{deploymentId}/cancel")
    public ResponseEntity<OpenStackDeployResponse> cancelDeployment(@PathVariable String deploymentId) {
        log.info("배포 취소 요청: {}", deploymentId);
        
        try {
            OpenStackDeployResponse response = openStackService.cancelDeployment(deploymentId);
            
            if (response.getStatus() == OpenStackDeployResponse.DeploymentStatus.FAILED && 
                response.getError() != null && 
                "배포 ID를 찾을 수 없습니다".equals(response.getError().getErrorMessage())) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배포 취소 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("배포 취소 중 오류가 발생했습니다"));
        }
    }

    /**
     * 헬스 체크
     * GET /api/openstack/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("OpenStack 서비스 헬스 체크");
        
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "OpenStack Integration",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("헬스 체크 실패: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "OpenStack Integration",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    /**
     * 오류 응답 생성 헬퍼 메서드
     */
    private OpenStackDeployResponse createErrorResponse(String errorMessage) {
        return OpenStackDeployResponse.failed(null, 
            OpenStackDeployResponse.ErrorInfo.builder()
                .errorCode("CONTROLLER_ERROR")
                .errorMessage(errorMessage)
                .occurredAt(java.time.LocalDateTime.now())
                .build());
    }
} 