package com.example.boardstack.template;

import com.example.boardstack.dto.OpenStackDeployRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TfTemplateManager {

    private static final String TEMPLATE_PATH = "terraform/openstack-vm.tf.template";
    private static final String DEFAULT_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC..."; // 기본 공개키

    /**
     * OpenStack VM 배포용 Terraform 템플릿 생성
     */
    public String generateTerraformTemplate(OpenStackDeployRequest request) {
        log.info("Terraform 템플릿 생성 시작: {}", request.getVmName());
        
        try {
            // 템플릿 파일 읽기
            String templateContent = loadTemplate();
            
            // 파라미터 맵 생성
            Map<String, String> parameters = buildParameterMap(request);
            
            // 템플릿에 파라미터 주입
            String generatedTemplate = replaceParameters(templateContent, parameters);
            
            log.info("Terraform 템플릿 생성 완료: {}", request.getVmName());
            return generatedTemplate;
            
        } catch (Exception e) {
            log.error("Terraform 템플릿 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("템플릿 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 템플릿 파일을 파일 시스템에 저장
     */
    public Path saveTemplateToFile(String templateContent, String fileName) {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "terraform");
            Files.createDirectories(tempDir);
            
            Path templateFile = tempDir.resolve(fileName + ".tf");
            Files.write(templateFile, templateContent.getBytes(StandardCharsets.UTF_8));
            
            log.info("템플릿 파일 저장 완료: {}", templateFile.toString());
            return templateFile;
            
        } catch (IOException e) {
            log.error("템플릿 파일 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("템플릿 파일 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 템플릿 파일 로드
     */
    private String loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 요청 객체로부터 파라미터 맵 생성
     */
    private Map<String, String> buildParameterMap(OpenStackDeployRequest request) {
        Map<String, String> parameters = new HashMap<>();
        
        // 기본 파라미터
        parameters.put("vm_name", escapeString(request.getVmName()));
        parameters.put("instance_type", escapeString(request.getInstanceType()));
        parameters.put("image_name", extractImageName(request.getImageId()));
        parameters.put("network_name", extractNetworkName(request.getNetworkId()));
        parameters.put("security_group", escapeString(request.getSecurityGroup()));
        parameters.put("key_pair", escapeString(request.getKeyPair()));
        parameters.put("disk_size", String.valueOf(request.getDiskSize()));
        parameters.put("volume_type", escapeString(request.getVolumeType()));
        parameters.put("availability_zone", escapeString(request.getAvailabilityZone() != null ? 
            request.getAvailabilityZone() : "nova"));
        parameters.put("delete_on_termination", String.valueOf(request.getDeleteOnTermination()));
        
        // 공개키 설정 (기본값 사용)
        parameters.put("public_key_content", DEFAULT_PUBLIC_KEY);
        
        // 사용자 데이터
        parameters.put("user_data", escapeUserData(request.getUserData()));
        
        // 추가 네트워크 설정
        parameters.put("additional_networks", buildAdditionalNetworks(request.getAdditionalNetworks()));
        
        // 추가 보안 그룹 설정
        parameters.put("additional_security_groups", buildAdditionalSecurityGroups(request.getAdditionalSecurityGroups()));
        
        // 메타데이터 설정
        parameters.put("instance_metadata", buildMetadata(request.getMetadata()));
        parameters.put("volume_metadata", buildVolumeMetadata(request));
        
        // 태그 설정
        parameters.put("instance_tags", buildTags(request.getTags()));
        
        // Floating IP 설정
        parameters.put("floating_ip_block", buildFloatingIpBlock(request.getAutoAssignFloatingIp()));
        parameters.put("floating_ip_output", buildFloatingIpOutput(request.getAutoAssignFloatingIp()));
        
        // 추가 볼륨 설정
        parameters.put("additional_volumes", buildAdditionalVolumes(request));
        
        return parameters;
    }

    /**
     * 템플릿에 파라미터 주입
     */
    private String replaceParameters(String template, Map<String, String> parameters) {
        String result = template;
        
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }

    /**
     * 문자열 이스케이프 처리
     */
    private String escapeString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * 사용자 데이터 이스케이프 처리
     */
    private String escapeUserData(String userData) {
        if (!StringUtils.hasText(userData)) {
            return "# No user data provided";
        }
        return userData.replace("${", "$${"); // Terraform 변수와 충돌 방지
    }

    /**
     * 이미지 ID에서 이미지 이름 추출
     */
    private String extractImageName(String imageId) {
        // 실제 구현에서는 OpenStack API를 호출하여 이미지 이름을 가져와야 함
        // 여기서는 간단히 ID를 이름으로 사용
        return escapeString(imageId);
    }

    /**
     * 네트워크 ID에서 네트워크 이름 추출
     */
    private String extractNetworkName(String networkId) {
        // 실제 구현에서는 OpenStack API를 호출하여 네트워크 이름을 가져와야 함
        // 여기서는 간단히 ID를 이름으로 사용
        return escapeString(networkId);
    }

    /**
     * 추가 네트워크 설정 생성
     */
    private String buildAdditionalNetworks(List<String> additionalNetworks) {
        if (additionalNetworks == null || additionalNetworks.isEmpty()) {
            return "";
        }
        
        return additionalNetworks.stream()
                .map(networkId -> String.format("  network {\n    uuid = \"%s\"\n  }", escapeString(networkId)))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 추가 보안 그룹 설정 생성
     */
    private String buildAdditionalSecurityGroups(List<String> additionalSecurityGroups) {
        if (additionalSecurityGroups == null || additionalSecurityGroups.isEmpty()) {
            return "";
        }
        
        return additionalSecurityGroups.stream()
                .map(sg -> "\"" + escapeString(sg) + "\"")
                .collect(Collectors.joining(",\n    "));
    }

    /**
     * 메타데이터 설정 생성
     */
    private String buildMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "created_by = \"BoardStack\"\n    environment = \"development\"";
        }
        
        return metadata.entrySet().stream()
                .map(entry -> String.format("%s = \"%s\"", 
                    escapeString(entry.getKey()), 
                    escapeString(entry.getValue())))
                .collect(Collectors.joining("\n    "));
    }

    /**
     * 볼륨 메타데이터 설정 생성
     */
    private String buildVolumeMetadata(OpenStackDeployRequest request) {
        Map<String, String> volumeMetadata = new HashMap<>();
        volumeMetadata.put("created_by", "BoardStack");
        volumeMetadata.put("vm_name", request.getVmName());
        volumeMetadata.put("requested_by", request.getRequestedBy());
        
        return buildMetadata(volumeMetadata);
    }

    /**
     * 태그 설정 생성
     */
    private String buildTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "\"BoardStack\",\n    \"OpenStack\"";
        }
        
        return tags.entrySet().stream()
                .map(entry -> String.format("\"%s:%s\"", 
                    escapeString(entry.getKey()), 
                    escapeString(entry.getValue())))
                .collect(Collectors.joining(",\n    "));
    }

    /**
     * Floating IP 블록 생성
     */
    private String buildFloatingIpBlock(Boolean autoAssignFloatingIp) {
        if (autoAssignFloatingIp == null || !autoAssignFloatingIp) {
            return "# Floating IP 할당 안함";
        }
        
        return "resource \"openstack_networking_floatingip_v2\" \"vm_floating_ip\" {\n" +
               "  pool = \"public\"\n" +
               "}\n\n" +
               "resource \"openstack_compute_floatingip_associate_v2\" \"vm_floating_ip_associate\" {\n" +
               "  floating_ip = openstack_networking_floatingip_v2.vm_floating_ip.address\n" +
               "  instance_id = openstack_compute_instance_v2.vm_instance.id\n" +
               "}";
    }

    /**
     * Floating IP 출력 설정 생성
     */
    private String buildFloatingIpOutput(Boolean autoAssignFloatingIp) {
        if (autoAssignFloatingIp == null || !autoAssignFloatingIp) {
            return "null";
        }
        
        return "openstack_networking_floatingip_v2.vm_floating_ip.address";
    }

    /**
     * 추가 볼륨 설정 생성
     */
    private String buildAdditionalVolumes(OpenStackDeployRequest request) {
        // 현재는 추가 볼륨 미지원
        return "# 추가 볼륨 없음";
    }
} 