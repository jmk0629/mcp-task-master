package com.example.boardstack.controller;

import com.example.boardstack.config.AppProperties;
import com.example.boardstack.config.OpenStackProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private AppProperties appProperties;
    private OpenStackProperties openStackProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping("/")
    public String home() {
        return "Welcome to BoardStack Application!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("applicationName", applicationName);
        info.put("appName", appProperties.getName());
        info.put("version", appProperties.getVersion());
        info.put("description", appProperties.getDescription());
        info.put("debug", appProperties.isDebug());
        info.put("mockData", appProperties.isMockData());
        info.put("openStackEnabled", openStackProperties.getTofumaker().isEnabled());
        info.put("openStackBaseUrl", openStackProperties.getTofumaker().getBaseUrl());
        return info;
    }
} 