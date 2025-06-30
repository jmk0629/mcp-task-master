package com.example.boardstack.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenStack 연동 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "openstack")
@Getter
@Setter
public class OpenStackProperties {

    private Tofumaker tofumaker = new Tofumaker();
    private Templates templates = new Templates();

    @Getter
    @Setter
    public static class Tofumaker {
        private String baseUrl;
        private int timeout = 30000;
        private boolean enabled = false;
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Templates {
        private String basePath;
    }
} 