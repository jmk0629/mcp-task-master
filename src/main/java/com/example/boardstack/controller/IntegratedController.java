package com.example.boardstack.controller;

import com.example.boardstack.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integrated")
public class IntegratedController {

    @Autowired
    private BoardService boardService;

    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 시스템 상태
        Map<String, Object> system = new HashMap<>();
        system.put("status", "UP");
        system.put("uptime", System.currentTimeMillis() - getStartTime());
        
        Map<String, String> services = new HashMap<>();
        services.put("board", "UP");
        services.put("openstack", "UP");
        system.put("services", services);
        
        status.put("system", system);
        
        // 게시판 통계
        Map<String, Object> boards = new HashMap<>();
        boards.put("total", boardService.getAllBoards().size());
        Map<String, Integer> boardsByStatus = new HashMap<>();
        boardsByStatus.put("active", boardService.getAllBoards().size());
        boards.put("byStatus", boardsByStatus);
        
        status.put("boards", boards);
        
        // 배포 통계 (모의 데이터)
        Map<String, Object> deployments = new HashMap<>();
        deployments.put("total", 0);
        Map<String, Integer> deploymentsByStatus = new HashMap<>();
        deploymentsByStatus.put("COMPLETED", 0);
        deploymentsByStatus.put("PENDING", 0);
        deploymentsByStatus.put("FAILED", 0);
        deployments.put("byStatus", deploymentsByStatus);
        
        status.put("deployments", deployments);
        
        return ResponseEntity.ok(status);
    }
    
    private long getStartTime() {
        // 애플리케이션 시작 시간 (간단한 구현)
        return System.currentTimeMillis() - 60000; // 1분 전으로 가정
    }
} 