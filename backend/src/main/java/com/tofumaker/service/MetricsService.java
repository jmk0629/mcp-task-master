package com.tofumaker.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    // 커스텀 메트릭 카운터들
    private final Counter userLoginCounter;
    private final Counter apiRequestCounter;
    private final Counter errorCounter;
    private final Timer databaseQueryTimer;
    private final Timer redisOperationTimer;

    // 게이지 메트릭을 위한 AtomicInteger
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicInteger databaseConnections = new AtomicInteger(0);

    @Autowired
    public MetricsService(MeterRegistry meterRegistry, DataSource dataSource, 
                         RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;

        // 카운터 메트릭 초기화
        this.userLoginCounter = Counter.builder("user_logins_total")
                .description("Total number of user logins")
                .register(meterRegistry);

        this.apiRequestCounter = Counter.builder("api_requests_total")
                .description("Total number of API requests")
                .tag("endpoint", "unknown")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("application_errors_total")
                .description("Total number of application errors")
                .tag("type", "unknown")
                .register(meterRegistry);

        // 타이머 메트릭 초기화
        this.databaseQueryTimer = Timer.builder("database_query_duration_seconds")
                .description("Database query execution time")
                .register(meterRegistry);

        this.redisOperationTimer = Timer.builder("redis_operation_duration_seconds")
                .description("Redis operation execution time")
                .register(meterRegistry);

        // 게이지 메트릭 초기화
        Gauge.builder("active_users_count", activeUsers, AtomicInteger::get)
                .description("Number of currently active users")
                .register(meterRegistry);

        Gauge.builder("database_connections_active", this, MetricsService::getActiveDatabaseConnections)
                .description("Number of active database connections")
                .register(meterRegistry);

        Gauge.builder("redis_memory_usage_bytes", this, MetricsService::getRedisMemoryUsage)
                .description("Redis memory usage in bytes")
                .register(meterRegistry);

        Gauge.builder("jvm_memory_used_bytes", this, MetricsService::getJvmMemoryUsage)
                .description("JVM memory usage in bytes")
                .register(meterRegistry);
    }

    // 사용자 로그인 메트릭 증가
    public void incrementUserLogin() {
        userLoginCounter.increment();
    }

    // API 요청 메트릭 증가
    public void incrementApiRequest(String endpoint) {
        Counter.builder("api_requests_total")
                .description("Total number of API requests")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    // 에러 메트릭 증가
    public void incrementError(String errorType) {
        Counter.builder("application_errors_total")
                .description("Total number of application errors")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }

    // 활성 사용자 수 설정
    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }

    // 데이터베이스 쿼리 시간 측정
    public <T> T recordDatabaseQuery(Timer.Sample sample, DatabaseOperation<T> operation) throws SQLException {
        try {
            T result = operation.execute();
            sample.stop(databaseQueryTimer);
            return result;
        } catch (SQLException e) {
            incrementError("database_error");
            throw e;
        }
    }

    // Redis 작업 시간 측정
    public <T> T recordRedisOperation(RedisOperation<T> operation) {
        try {
            return redisOperationTimer.recordCallable(() -> {
                try {
                    return operation.execute();
                } catch (Exception e) {
                    incrementError("redis_error");
                    throw e;
                }
            });
        } catch (Exception e) {
            incrementError("redis_error");
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    // 활성 데이터베이스 연결 수 조회
    private double getActiveDatabaseConnections() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'")) {
            
            if (resultSet.next()) {
                return resultSet.getDouble(1);
            }
        } catch (SQLException e) {
            // 로그 기록 후 0 반환
            return 0;
        }
        return 0;
    }

    // Redis 메모리 사용량 조회
    private double getRedisMemoryUsage() {
        try {
            Object info = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                return connection.info("memory");
            });
            
            if (info instanceof String) {
                String memoryInfo = (String) info;
                // used_memory 값 파싱
                String[] lines = memoryInfo.split("\r\n");
                for (String line : lines) {
                    if (line.startsWith("used_memory:")) {
                        return Double.parseDouble(line.split(":")[1]);
                    }
                }
            }
        } catch (Exception e) {
            // 로그 기록 후 0 반환
            return 0;
        }
        return 0;
    }

    // JVM 메모리 사용량 조회
    private double getJvmMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    // 함수형 인터페이스 정의
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws SQLException;
    }

    @FunctionalInterface
    public interface RedisOperation<T> {
        T execute() throws Exception;
    }
} 