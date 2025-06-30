package com.example.boardstack.controller;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
                
                health.put("status", "UP");
                health.put("poolName", hikariDataSource.getPoolName());
                health.put("activeConnections", poolBean.getActiveConnections());
                health.put("idleConnections", poolBean.getIdleConnections());
                health.put("totalConnections", poolBean.getTotalConnections());
                health.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
                health.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                health.put("minimumIdle", hikariDataSource.getMinimumIdle());
                
                // 연결 테스트
                dataSource.getConnection().close();
                health.put("connectionTest", "SUCCESS");
                
            } else {
                health.put("status", "UP");
                health.put("connectionTest", "SUCCESS");
                dataSource.getConnection().close();
            }
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getApplicationStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("application", "BoardStack");
        status.put("status", "UP");
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }
} 