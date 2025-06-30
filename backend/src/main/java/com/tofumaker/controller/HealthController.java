package com.tofumaker.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "시스템 상태 확인 API")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private final Counter healthCheckCounter;
    private final Timer healthCheckTimer;

    public HealthController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.healthCheckCounter = Counter.builder("health_check_requests_total")
                .description("Total number of health check requests")
                .register(meterRegistry);
        this.healthCheckTimer = Timer.builder("health_check_duration_seconds")
                .description("Health check request duration")
                .register(meterRegistry);
    }

    @Operation(summary = "전체 시스템 상태 확인", description = "데이터베이스, Redis 등 모든 컴포넌트의 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 확인 성공",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        try {
            return healthCheckTimer.recordCallable(() -> {
                healthCheckCounter.increment();
                
                Map<String, Object> healthStatus = new HashMap<>();
                healthStatus.put("status", "UP");
                healthStatus.put("timestamp", LocalDateTime.now());
                healthStatus.put("application", "TofuMaker Backend");
                healthStatus.put("version", "1.0.0");
                
                Map<String, Object> components = new HashMap<>();
                
                // Database 상태 확인
                components.put("database", checkDatabaseHealth());
                
                // Redis 상태 확인
                components.put("redis", checkRedisHealth());
                
                // 전체 상태 결정
                boolean allHealthy = components.values().stream()
                        .allMatch(component -> {
                            if (component instanceof Map) {
                                return "UP".equals(((Map<?, ?>) component).get("status"));
                            }
                            return false;
                        });
                
                if (!allHealthy) {
                    healthStatus.put("status", "DOWN");
                }
                
                healthStatus.put("components", components);
                
                return ResponseEntity.ok(healthStatus);
            });
        } catch (Exception e) {
            healthCheckCounter.increment();
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "DOWN");
            errorStatus.put("timestamp", LocalDateTime.now());
            errorStatus.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorStatus);
        }
    }

    @Operation(summary = "준비 상태 확인", description = "애플리케이션이 요청을 처리할 준비가 되었는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "준비 상태 확인 성공",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readinessStatus = new HashMap<>();
        
        boolean databaseReady = isDatabaseReady();
        boolean redisReady = isRedisReady();
        
        readinessStatus.put("status", (databaseReady && redisReady) ? "READY" : "NOT_READY");
        readinessStatus.put("timestamp", LocalDateTime.now());
        readinessStatus.put("checks", Map.of(
                "database", databaseReady ? "READY" : "NOT_READY",
                "redis", redisReady ? "READY" : "NOT_READY"
        ));
        
        return ResponseEntity.ok(readinessStatus);
    }

    @Operation(summary = "생존 상태 확인", description = "애플리케이션이 살아있는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생존 상태 확인 성공",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> livenessStatus = new HashMap<>();
        livenessStatus.put("status", "ALIVE");
        livenessStatus.put("timestamp", LocalDateTime.now());
        livenessStatus.put("uptime", getUptime());
        
        return ResponseEntity.ok(livenessStatus);
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("validationQuery", "SELECT 1");
        } catch (SQLException e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> redisHealth = new HashMap<>();
        try {
            String testKey = "health_check_" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", 10);
            String value = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            redisHealth.put("status", "test".equals(value) ? "UP" : "DOWN");
            redisHealth.put("operation", "SET/GET/DELETE test");
        } catch (Exception e) {
            redisHealth.put("status", "DOWN");
            redisHealth.put("error", e.getMessage());
        }
        return redisHealth;
    }

    private boolean isDatabaseReady() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isRedisReady() {
        try {
            redisTemplate.opsForValue().get("readiness_check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getUptime() {
        long uptimeMillis = System.currentTimeMillis() - 
                java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%d hours, %d minutes, %d seconds", 
                hours, minutes % 60, seconds % 60);
    }
} 