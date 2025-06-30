package com.tofumaker.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter cacheHitCounter;

    @Mock
    private Counter cacheMissCounter;

    @Mock
    private Timer cacheOperationTimer;

    @Mock
    private Cache cache;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        // Mock MeterRegistry to return mock counters and timers
        when(meterRegistry.counter(anyString())).thenReturn(cacheHitCounter);
        when(meterRegistry.timer(anyString())).thenReturn(cacheOperationTimer);
        
        // Initialize CacheService with mocked dependencies
        cacheService = new CacheService(cacheManager, redisTemplate, meterRegistry);
    }

    @Test
    void get_WhenCacheHit_ShouldReturnValueAndIncrementHitCounter() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String expectedValue = "testValue";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key, String.class)).thenReturn(expectedValue);

        // When
        String actualValue = cacheService.get(cacheName, key, String.class);

        // Then
        assertEquals(expectedValue, actualValue);
        verify(cacheHitCounter, times(1)).increment();
        verify(cacheMissCounter, never()).increment();
    }

    @Test
    void get_WhenCacheMiss_ShouldReturnNullAndIncrementMissCounter() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(key, String.class)).thenReturn(null);

        // When
        String actualValue = cacheService.get(cacheName, key, String.class);

        // Then
        assertNull(actualValue);
        verify(cacheMissCounter, times(1)).increment();
        verify(cacheHitCounter, never()).increment();
    }

    @Test
    void put_ShouldStoreValueInCache() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.put(cacheName, key, value);

        // Then
        verify(cache, times(1)).put(key, value);
    }

    @Test
    void evict_ShouldRemoveValueFromCache() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.evict(cacheName, key);

        // Then
        verify(cache, times(1)).evict(key);
    }

    @Test
    void clear_ShouldClearEntireCache() {
        // Given
        String cacheName = "testCache";
        
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        // When
        cacheService.clear(cacheName);

        // Then
        verify(cache, times(1)).clear();
    }

    @Test
    void clearAll_ShouldClearAllCaches() {
        // Given
        String[] cacheNames = {"cache1", "cache2", "cache3"};
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList(cacheNames));
        
        for (String cacheName : cacheNames) {
            when(cacheManager.getCache(cacheName)).thenReturn(cache);
        }

        // When
        cacheService.clearAll();

        // Then
        verify(cache, times(cacheNames.length)).clear();
    }

    @Test
    void getCacheStatistics_ShouldReturnStatisticsMap() {
        // Given
        when(cacheHitCounter.count()).thenReturn(100.0);
        when(cacheMissCounter.count()).thenReturn(20.0);

        // When
        Map<String, Object> statistics = cacheService.getCacheStatistics();

        // Then
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("cacheHits"));
        assertTrue(statistics.containsKey("cacheMisses"));
        assertTrue(statistics.containsKey("hitRate"));
        assertEquals(100.0, statistics.get("cacheHits"));
        assertEquals(20.0, statistics.get("cacheMisses"));
    }

    @Test
    void getCacheHitRates_ShouldReturnHitRatesForAllCaches() {
        // Given
        String[] cacheNames = {"cache1", "cache2"};
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList(cacheNames));

        // When
        Map<String, Double> hitRates = cacheService.getCacheHitRates();

        // Then
        assertNotNull(hitRates);
        assertEquals(cacheNames.length, hitRates.size());
        for (String cacheName : cacheNames) {
            assertTrue(hitRates.containsKey(cacheName));
        }
    }

    @Test
    void getTtl_ShouldReturnTimeToLive() {
        // Given
        String key = "testKey";
        Long expectedTtl = 3600L;
        
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(expectedTtl);

        // When
        Long actualTtl = cacheService.getTtl(key);

        // Then
        assertEquals(expectedTtl, actualTtl);
        verify(redisTemplate, times(1)).getExpire(key, TimeUnit.SECONDS);
    }

    @Test
    void warmupCache_ShouldExecuteWithoutErrors() {
        // Given
        // No specific setup needed for warmup test

        // When & Then
        assertDoesNotThrow(() -> cacheService.warmupCache());
    }
} 