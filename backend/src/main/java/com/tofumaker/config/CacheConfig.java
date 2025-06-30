package com.tofumaker.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON 직렬화 설정
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // String 직렬화 설정
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key 직렬화
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // value 직렬화
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON 직렬화 설정
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 기본 TTL 30분
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 사용자 정보 캐시 (1시간)
        cacheConfigurations.put("users", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 세션 캐시 (12시간)
        cacheConfigurations.put("sessions", defaultCacheConfig.entryTtl(Duration.ofHours(12)));
        
        // OpenStack 리소스 캐시 (5분)
        cacheConfigurations.put("openstack-resources", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 설정 정보 캐시 (24시간)
        cacheConfigurations.put("configurations", defaultCacheConfig.entryTtl(Duration.ofHours(24)));
        
        // API 응답 캐시 (10분)
        cacheConfigurations.put("api-responses", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 통계 데이터 캐시 (1시간)
        cacheConfigurations.put("statistics", defaultCacheConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 캐시 키 생성 전략
     */
    public static class CacheKeyGenerator {
        public static String generateKey(String prefix, Object... params) {
            StringBuilder keyBuilder = new StringBuilder(prefix);
            for (Object param : params) {
                keyBuilder.append(":").append(param != null ? param.toString() : "null");
            }
            return keyBuilder.toString();
        }
    }

    /**
     * 캐시 상수 정의
     */
    public static class CacheNames {
        public static final String USERS = "users";
        public static final String SESSIONS = "sessions";
        public static final String OPENSTACK_RESOURCES = "openstack-resources";
        public static final String CONFIGURATIONS = "configurations";
        public static final String API_RESPONSES = "api-responses";
        public static final String STATISTICS = "statistics";
    }
} 