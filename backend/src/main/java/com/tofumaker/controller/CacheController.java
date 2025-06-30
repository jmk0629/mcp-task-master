package com.tofumaker.controller;

import com.tofumaker.service.CacheService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Cache", description = "캐시 관리 API (관리자 전용)")
@SecurityRequirement(name = "Bearer Authentication")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @Operation(summary = "캐시 통계 정보 조회", description = "전체 캐시의 통계 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "캐시 적중률 조회", description = "각 캐시의 적중률을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "적중률 조회 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/hit-rates")
    public ResponseEntity<Map<String, Double>> getCacheHitRates() {
        Map<String, Double> hitRates = cacheService.getCacheHitRates();
        return ResponseEntity.ok(hitRates);
    }

    @Operation(summary = "특정 캐시 클리어", description = "지정된 캐시를 모두 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "캐시 클리어 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<String> clearCache(
            @Parameter(description = "클리어할 캐시명", required = true) @PathVariable String cacheName) {
        cacheService.clear(cacheName);
        return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
    }

    @Operation(summary = "모든 캐시 클리어", description = "모든 캐시를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모든 캐시 클리어 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllCaches() {
        cacheService.clearAll();
        return ResponseEntity.ok("All caches cleared successfully");
    }

    @Operation(summary = "특정 캐시 키 삭제", description = "지정된 캐시에서 특정 키를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "캐시 키 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<String> evictCacheKey(
            @Parameter(description = "캐시명", required = true) @PathVariable String cacheName,
            @Parameter(description = "삭제할 캐시 키", required = true) @PathVariable String key) {
        cacheService.evict(cacheName, key);
        return ResponseEntity.ok("Cache key '" + key + "' evicted from '" + cacheName + "' successfully");
    }

    @Operation(summary = "캐시 워밍업", description = "자주 사용되는 데이터를 미리 캐시에 로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "캐시 워밍업 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/warmup")
    public ResponseEntity<String> warmupCache() {
        cacheService.warmupCache();
        return ResponseEntity.ok("Cache warmup completed");
    }

    @Operation(summary = "캐시 키 TTL 조회", description = "지정된 캐시 키의 남은 생존 시간을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TTL 조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/ttl/{key}")
    public ResponseEntity<Long> getTtl(
            @Parameter(description = "TTL을 조회할 캐시 키", required = true) @PathVariable String key) {
        Long ttl = cacheService.getTtl(key);
        return ResponseEntity.ok(ttl);
    }
} 