package com.tofumaker.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseOptimizationService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private final Timer queryAnalysisTimer;

    public DatabaseOptimizationService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queryAnalysisTimer = Timer.builder("database_query_analysis_duration_seconds")
                .description("Database query analysis duration")
                .register(meterRegistry);
    }

    /**
     * 슬로우 쿼리 분석
     */
    public List<Map<String, Object>> analyzeSlowQueries() {
        try {
            return queryAnalysisTimer.recordCallable(() -> {
                List<Map<String, Object>> slowQueries = new ArrayList<>();
                
                try {
                    // PostgreSQL의 pg_stat_statements 확장을 사용하여 슬로우 쿼리 분석
                    String query = "SELECT " +
                        "query, " +
                        "calls, " +
                        "total_time, " +
                        "mean_time, " +
                        "rows, " +
                        "100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent " +
                        "FROM pg_stat_statements " +
                        "WHERE mean_time > 100 " +  // 100ms 이상인 쿼리
                        "ORDER BY mean_time DESC " +
                        "LIMIT 20";
                    
                    List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
                    slowQueries.addAll(results);
                    
                } catch (Exception e) {
                    logger.warn("pg_stat_statements extension not available, using alternative method");
                    // pg_stat_statements가 없는 경우 대안 방법
                    slowQueries = getAlternativeSlowQueryAnalysis();
                }
                
                return slowQueries;
            });
        } catch (Exception e) {
            logger.error("Error analyzing slow queries", e);
            return new ArrayList<>();
        }
    }

    /**
     * 인덱스 사용률 분석
     */
    public List<Map<String, Object>> analyzeIndexUsage() {
        String query = "SELECT " +
            "schemaname, " +
            "tablename, " +
            "indexname, " +
            "idx_tup_read, " +
            "idx_tup_fetch, " +
            "idx_scan, " +
            "CASE " +
                "WHEN idx_scan = 0 THEN 'Unused' " +
                "WHEN idx_scan < 10 THEN 'Low Usage' " +
                "ELSE 'Active' " +
            "END as usage_status " +
            "FROM pg_stat_user_indexes " +
            "ORDER BY idx_scan ASC";
        
        return jdbcTemplate.queryForList(query);
    }

    /**
     * 테이블 통계 정보 조회
     */
    public List<Map<String, Object>> getTableStatistics() {
        String query = "SELECT " +
            "schemaname, " +
            "tablename, " +
            "n_tup_ins as inserts, " +
            "n_tup_upd as updates, " +
            "n_tup_del as deletes, " +
            "n_live_tup as live_tuples, " +
            "n_dead_tup as dead_tuples, " +
            "last_vacuum, " +
            "last_autovacuum, " +
            "last_analyze, " +
            "last_autoanalyze " +
            "FROM pg_stat_user_tables " +
            "ORDER BY n_live_tup DESC";
        
        return jdbcTemplate.queryForList(query);
    }

    /**
     * 데이터베이스 크기 정보
     */
    public Map<String, Object> getDatabaseSizeInfo() {
        Map<String, Object> sizeInfo = new HashMap<>();
        
        // 전체 데이터베이스 크기
        String dbSizeQuery = "SELECT pg_size_pretty(pg_database_size(current_database())) as database_size";
        String dbSize = jdbcTemplate.queryForObject(dbSizeQuery, String.class);
        sizeInfo.put("database_size", dbSize);
        
        // 테이블별 크기
        String tableSizeQuery = "SELECT " +
            "tablename, " +
            "pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size " +
            "FROM pg_tables " +
            "WHERE schemaname = 'public' " +
            "ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC";
        
        List<Map<String, Object>> tableSizes = jdbcTemplate.queryForList(tableSizeQuery);
        sizeInfo.put("table_sizes", tableSizes);
        
        return sizeInfo;
    }

    /**
     * 연결 풀 상태 조회
     */
    public Map<String, Object> getConnectionPoolStatus() {
        Map<String, Object> poolStatus = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // HikariCP 정보 조회
            if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                String activeConnectionsQuery = "SELECT count(*) as active_connections " +
                    "FROM pg_stat_activity " +
                    "WHERE state = 'active'";
                
                Integer activeConnections = jdbcTemplate.queryForObject(activeConnectionsQuery, Integer.class);
                poolStatus.put("active_connections", activeConnections);
                
                String totalConnectionsQuery = "SELECT count(*) as total_connections " +
                    "FROM pg_stat_activity";
                
                Integer totalConnections = jdbcTemplate.queryForObject(totalConnectionsQuery, Integer.class);
                poolStatus.put("total_connections", totalConnections);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting connection pool status", e);
            poolStatus.put("error", e.getMessage());
        }
        
        return poolStatus;
    }

    /**
     * 인덱스 추천
     */
    public List<String> recommendIndexes() {
        List<String> recommendations = new ArrayList<>();
        
        try {
            // 자주 사용되는 WHERE 절 컬럼 분석
            List<Map<String, Object>> tableStats = getTableStatistics();
            
            for (Map<String, Object> table : tableStats) {
                String tableName = (String) table.get("tablename");
                
                // 기본적인 인덱스 추천 로직
                if ("boards".equals(tableName)) {
                    recommendations.add("CREATE INDEX CONCURRENTLY idx_boards_author ON boards(author);");
                    recommendations.add("CREATE INDEX CONCURRENTLY idx_boards_created_at ON boards(created_at);");
                    recommendations.add("CREATE INDEX CONCURRENTLY idx_boards_title_gin ON boards USING gin(to_tsvector('english', title));");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating index recommendations", e);
        }
        
        return recommendations;
    }

    /**
     * 데이터베이스 통계 업데이트
     */
    public void updateStatistics() {
        try {
            logger.info("Updating database statistics...");
            jdbcTemplate.execute("ANALYZE");
            logger.info("Database statistics updated successfully");
        } catch (Exception e) {
            logger.error("Error updating database statistics", e);
        }
    }

    /**
     * VACUUM 작업 수행
     */
    public void performVacuum(String tableName) {
        try {
            logger.info("Performing VACUUM on table: {}", tableName);
            String vacuumQuery = "VACUUM ANALYZE " + tableName;
            jdbcTemplate.execute(vacuumQuery);
            logger.info("VACUUM completed for table: {}", tableName);
        } catch (Exception e) {
            logger.error("Error performing VACUUM on table: {}", tableName, e);
        }
    }

    /**
     * 쿼리 실행 계획 분석
     */
    public List<Map<String, Object>> explainQuery(String query) {
        List<Map<String, Object>> explainResult = new ArrayList<>();
        
        try {
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query;
            List<Map<String, Object>> results = jdbcTemplate.queryForList(explainQuery);
            explainResult.addAll(results);
        } catch (Exception e) {
            logger.error("Error explaining query: {}", query, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            explainResult.add(error);
        }
        
        return explainResult;
    }

    private List<Map<String, Object>> getAlternativeSlowQueryAnalysis() {
        // pg_stat_statements가 없는 경우의 대안 방법
        List<Map<String, Object>> alternatives = new ArrayList<>();
        
        // 현재 실행 중인 쿼리 분석
        String currentQueriesQuery = "SELECT " +
            "query, " +
            "state, " +
            "query_start, " +
            "now() - query_start as duration " +
            "FROM pg_stat_activity " +
            "WHERE state = 'active' " +
            "AND query NOT LIKE '%pg_stat_activity%' " +
            "ORDER BY query_start";
        
        try {
            alternatives = jdbcTemplate.queryForList(currentQueriesQuery);
        } catch (Exception e) {
            logger.error("Error getting alternative slow query analysis", e);
        }
        
        return alternatives;
    }
} 