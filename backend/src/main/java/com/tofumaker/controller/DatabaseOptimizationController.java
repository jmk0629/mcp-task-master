package com.tofumaker.controller;

import com.tofumaker.service.DatabaseOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Database Optimization", description = "데이터베이스 최적화 관리 API (관리자 전용)")
@SecurityRequirement(name = "Bearer Authentication")
public class DatabaseOptimizationController {

    @Autowired
    private DatabaseOptimizationService databaseOptimizationService;

    @Operation(summary = "슬로우 쿼리 분석", description = "실행 시간이 긴 쿼리들을 분석합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬로우 쿼리 분석 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/slow-queries")
    public ResponseEntity<List<Map<String, Object>>> getSlowQueries() {
        List<Map<String, Object>> slowQueries = databaseOptimizationService.analyzeSlowQueries();
        return ResponseEntity.ok(slowQueries);
    }

    /**
     * 인덱스 사용률 분석
     */
    @GetMapping("/index-usage")
    public ResponseEntity<List<Map<String, Object>>> getIndexUsage() {
        List<Map<String, Object>> indexUsage = databaseOptimizationService.analyzeIndexUsage();
        return ResponseEntity.ok(indexUsage);
    }

    /**
     * 테이블 통계 정보
     */
    @GetMapping("/table-statistics")
    public ResponseEntity<List<Map<String, Object>>> getTableStatistics() {
        List<Map<String, Object>> tableStats = databaseOptimizationService.getTableStatistics();
        return ResponseEntity.ok(tableStats);
    }

    /**
     * 데이터베이스 크기 정보
     */
    @GetMapping("/size-info")
    public ResponseEntity<Map<String, Object>> getDatabaseSizeInfo() {
        Map<String, Object> sizeInfo = databaseOptimizationService.getDatabaseSizeInfo();
        return ResponseEntity.ok(sizeInfo);
    }

    /**
     * 연결 풀 상태
     */
    @GetMapping("/connection-pool")
    public ResponseEntity<Map<String, Object>> getConnectionPoolStatus() {
        Map<String, Object> poolStatus = databaseOptimizationService.getConnectionPoolStatus();
        return ResponseEntity.ok(poolStatus);
    }

    /**
     * 인덱스 추천
     */
    @GetMapping("/index-recommendations")
    public ResponseEntity<List<String>> getIndexRecommendations() {
        List<String> recommendations = databaseOptimizationService.recommendIndexes();
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 데이터베이스 통계 업데이트
     */
    @PostMapping("/update-statistics")
    public ResponseEntity<String> updateStatistics() {
        databaseOptimizationService.updateStatistics();
        return ResponseEntity.ok("Database statistics updated successfully");
    }

    /**
     * VACUUM 작업 수행
     */
    @PostMapping("/vacuum/{tableName}")
    public ResponseEntity<String> performVacuum(@PathVariable String tableName) {
        databaseOptimizationService.performVacuum(tableName);
        return ResponseEntity.ok("VACUUM completed for table: " + tableName);
    }

    @Operation(summary = "쿼리 실행 계획 분석", description = "주어진 쿼리의 실행 계획을 분석합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿼리 분석 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 쿼리"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/explain")
    public ResponseEntity<List<Map<String, Object>>> explainQuery(
            @Parameter(description = "분석할 쿼리", required = true) @RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Map<String, Object>> explainResult = databaseOptimizationService.explainQuery(query);
        return ResponseEntity.ok(explainResult);
    }
} 