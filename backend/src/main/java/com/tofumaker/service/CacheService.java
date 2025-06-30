package com.tofumaker.service;

import com.tofumaker.config.CacheConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    // 캐시 메트릭
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer cacheOperationTimer;

    public CacheService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cacheHitCounter = Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .tag("cache", "unknown")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .tag("cache", "unknown")
                .register(meterRegistry);
        this.cacheOperationTimer = Timer.builder("cache_operation_duration_seconds")
                .description("Cache operation duration")
                .register(meterRegistry);
    }

    /**
     * 캐시에서 값 조회
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            return cacheOperationTimer.recordCallable(() -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Cache.ValueWrapper wrapper = cache.get(key);
                    if (wrapper != null) {
                        recordCacheHit(cacheName);
                        logger.debug("Cache hit for key: {} in cache: {}", key, cacheName);
                        return type.cast(wrapper.get());
                    }
                }
                recordCacheMiss(cacheName);
                logger.debug("Cache miss for key: {} in cache: {}", key, cacheName);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error getting cache value for key: {} in cache: {}", key, cacheName, e);
            recordCacheMiss(cacheName);
            return null;
        }
    }

    /**
     * 캐시에 값 저장
     */
    public void put(String cacheName, String key, Object value) {
        cacheOperationTimer.record(() -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                logger.debug("Cached value for key: {} in cache: {}", key, cacheName);
            } else {
                logger.warn("Cache not found: {}", cacheName);
            }
        });
    }

    /**
     * 캐시에서 값 삭제
     */
    public void evict(String cacheName, String key) {
        cacheOperationTimer.record(() -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted key: {} from cache: {}", key, cacheName);
            }
        });
    }

    /**
     * 전체 캐시 클리어
     */
    public void clear(String cacheName) {
        cacheOperationTimer.record(() -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.info("Cleared cache: {}", cacheName);
            }
        });
    }

    /**
     * 모든 캐시 클리어
     */
    public void clearAll() {
        cacheOperationTimer.record(() -> {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            for (String cacheName : cacheNames) {
                clear(cacheName);
            }
            logger.info("Cleared all caches");
        });
    }

    /**
     * 캐시 통계 정보 조회
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Redis 정보 조회
        try {
            Object info = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> connection.info("memory"));
            if (info instanceof String) {
                String memoryInfo = (String) info;
                stats.put("redis_memory_info", parseRedisMemoryInfo(memoryInfo));
            }
            
            // 키 개수 조회
            Set<String> keys = redisTemplate.keys("*");
            stats.put("total_keys", keys != null ? keys.size() : 0);
            
            // 캐시별 키 개수
            Map<String, Integer> cacheKeyCounts = new HashMap<>();
            Collection<String> cacheNames = cacheManager.getCacheNames();
            for (String cacheName : cacheNames) {
                Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");
                cacheKeyCounts.put(cacheName, cacheKeys != null ? cacheKeys.size() : 0);
            }
            stats.put("cache_key_counts", cacheKeyCounts);
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 캐시 적중률 조회
     */
    public Map<String, Double> getCacheHitRates() {
        Map<String, Double> hitRates = new HashMap<>();
        
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            double hits = meterRegistry.counter("cache_hits_total", "cache", cacheName).count();
            double misses = meterRegistry.counter("cache_misses_total", "cache", cacheName).count();
            double total = hits + misses;
            
            if (total > 0) {
                hitRates.put(cacheName, (hits / total) * 100);
            } else {
                hitRates.put(cacheName, 0.0);
            }
        }
        
        return hitRates;
    }

    /**
     * 캐시 워밍업 (자주 사용되는 데이터 미리 로드)
     */
    public void warmupCache() {
        logger.info("Starting cache warmup...");
        
        // 여기에 자주 사용되는 데이터를 미리 캐시에 로드하는 로직 구현
        // 예: 설정 정보, 자주 조회되는 사용자 정보 등
        
        logger.info("Cache warmup completed");
    }

    /**
     * TTL 설정
     */
    public void setTtl(String key, long timeout, TimeUnit timeUnit) {
        redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * TTL 조회
     */
    public Long getTtl(String key) {
        return redisTemplate.getExpire(key);
    }

    private void recordCacheHit(String cacheName) {
        Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    private void recordCacheMiss(String cacheName) {
        Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    private Map<String, String> parseRedisMemoryInfo(String memoryInfo) {
        Map<String, String> info = new HashMap<>();
        String[] lines = memoryInfo.split("\r\n");
        
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    info.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return info;
    }
} 