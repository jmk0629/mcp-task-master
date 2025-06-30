# TofuMaker 성능 최적화 가이드

## 개요

TofuMaker 애플리케이션의 성능 최적화를 위한 종합적인 가이드입니다. Redis 캐싱, 데이터베이스 최적화, API 응답 시간 개선, 메모리 사용량 최적화 등을 다룹니다.

## 1. Redis 캐싱 시스템

### 1.1 캐시 구성

TofuMaker는 다음과 같은 캐시 전략을 사용합니다:

- **사용자 정보 캐시**: 1시간 TTL
- **세션 캐시**: 12시간 TTL  
- **OpenStack 리소스 캐시**: 5분 TTL
- **설정 정보 캐시**: 24시간 TTL
- **API 응답 캐시**: 10분 TTL
- **통계 데이터 캐시**: 1시간 TTL

### 1.2 캐시 사용법

#### 서비스 레벨 캐싱

```java
@Service
public class BoardService {
    
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'all_boards'")
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }
    
    @CacheEvict(value = CacheConfig.CacheNames.API_RESPONSES, allEntries = true)
    public Board createBoard(Board board) {
        return boardRepository.save(board);
    }
}
```

#### 프로그래밍 방식 캐시 관리

```java
@Autowired
private CacheService cacheService;

// 캐시에서 조회
Board board = cacheService.get("api-responses", "board_1", Board.class);

// 캐시에 저장
cacheService.put("api-responses", "board_1", board);

// 캐시 삭제
cacheService.evict("api-responses", "board_1");
```

### 1.3 캐시 모니터링

캐시 성능은 다음 API를 통해 모니터링할 수 있습니다:

- `GET /api/cache/statistics` - 캐시 통계 정보
- `GET /api/cache/hit-rates` - 캐시 적중률
- `DELETE /api/cache/{cacheName}` - 특정 캐시 클리어
- `POST /api/cache/warmup` - 캐시 워밍업

## 2. 데이터베이스 최적화

### 2.1 쿼리 최적화

#### 슬로우 쿼리 분석

```bash
# 슬로우 쿼리 조회
curl -X GET http://localhost:8080/api/database/slow-queries
```

#### 인덱스 사용률 분석

```bash
# 인덱스 사용률 확인
curl -X GET http://localhost:8080/api/database/index-usage
```

#### 인덱스 추천

시스템이 자동으로 추천하는 인덱스:

```sql
-- 게시판 테이블 최적화
CREATE INDEX CONCURRENTLY idx_boards_author ON boards(author);
CREATE INDEX CONCURRENTLY idx_boards_created_at ON boards(created_at);
CREATE INDEX CONCURRENTLY idx_boards_title_gin ON boards USING gin(to_tsvector('english', title));
```

### 2.2 연결 풀 최적화

HikariCP 설정 (application-production.yml):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 20000
      validation-timeout: 5000
      leak-detection-threshold: 60000
```

### 2.3 데이터베이스 유지보수

#### 통계 업데이트

```bash
# 데이터베이스 통계 업데이트
curl -X POST http://localhost:8080/api/database/update-statistics
```

#### VACUUM 작업

```bash
# 특정 테이블 VACUUM
curl -X POST http://localhost:8080/api/database/vacuum/boards
```

## 3. API 응답 시간 개선

### 3.1 캐싱 전략

- **읽기 전용 데이터**: `@Cacheable` 사용
- **자주 변경되는 데이터**: 짧은 TTL 설정
- **사용자별 데이터**: 사용자 ID를 키에 포함

### 3.2 비동기 처리

무거운 작업은 비동기로 처리:

```java
@Async
public CompletableFuture<String> processHeavyTask() {
    // 무거운 작업 수행
    return CompletableFuture.completedFuture("결과");
}
```

### 3.3 페이징 및 제한

대량 데이터 조회 시 페이징 적용:

```java
public Page<Board> getBoards(Pageable pageable) {
    return boardRepository.findAll(pageable);
}
```

## 4. 메모리 사용량 최적화

### 4.1 JVM 튜닝

프로덕션 환경 JVM 옵션:

```bash
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/tofumaker/
```

### 4.2 캐시 메모리 관리

Redis 메모리 정책 설정:

```redis
# redis.conf
maxmemory 1gb
maxmemory-policy allkeys-lru
```

### 4.3 메모리 모니터링

메모리 사용량은 다음을 통해 모니터링:

- Prometheus 메트릭: `jvm_memory_used_bytes`
- Actuator 엔드포인트: `/actuator/metrics/jvm.memory.used`

## 5. 부하 테스트

### 5.1 JMeter 테스트 실행

#### Linux/macOS

```bash
chmod +x scripts/run-performance-tests.sh
./scripts/run-performance-tests.sh
```

#### Windows

```powershell
.\scripts\run-performance-tests.ps1
```

### 5.2 테스트 시나리오

기본 테스트 시나리오:

- **동시 사용자**: 50명
- **램프업 시간**: 30초
- **반복 횟수**: 10회
- **테스트 대상**: 헬스체크, 게시판 API, 캐시 통계

### 5.3 성능 기준

- **평균 응답 시간**: < 1초
- **성공률**: > 95%
- **처리량**: > 100 TPS
- **캐시 적중률**: > 80%

## 6. 모니터링 및 알림

### 6.1 주요 메트릭

- **응답 시간**: `http_request_duration_seconds`
- **캐시 적중률**: `cache_hits_total` / (`cache_hits_total` + `cache_misses_total`)
- **데이터베이스 연결**: `database_connections_active`
- **메모리 사용량**: `jvm_memory_used_bytes`

### 6.2 Grafana 대시보드

성능 모니터링을 위한 주요 패널:

1. **API 응답 시간 추이**
2. **캐시 적중률**
3. **데이터베이스 성능**
4. **JVM 메모리 사용량**
5. **에러율 추이**

### 6.3 알림 설정

Alertmanager 규칙:

```yaml
groups:
- name: performance
  rules:
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, http_request_duration_seconds_bucket) > 1
    for: 5m
    annotations:
      summary: "API 응답 시간이 높습니다"
      
  - alert: LowCacheHitRate
    expr: (cache_hits_total / (cache_hits_total + cache_misses_total)) < 0.8
    for: 10m
    annotations:
      summary: "캐시 적중률이 낮습니다"
```

## 7. 최적화 체크리스트

### 7.1 개발 단계

- [ ] 적절한 캐싱 전략 적용
- [ ] 데이터베이스 쿼리 최적화
- [ ] 페이징 및 제한 구현
- [ ] 비동기 처리 적용

### 7.2 배포 전

- [ ] 부하 테스트 실행
- [ ] 성능 기준 충족 확인
- [ ] 메모리 사용량 검증
- [ ] 캐시 워밍업 실행

### 7.3 운영 단계

- [ ] 성능 메트릭 모니터링
- [ ] 정기적인 데이터베이스 유지보수
- [ ] 캐시 적중률 확인
- [ ] 알림 규칙 검토

## 8. 트러블슈팅

### 8.1 일반적인 성능 문제

#### 높은 응답 시간

1. 캐시 적중률 확인
2. 슬로우 쿼리 분석
3. 데이터베이스 연결 풀 상태 확인
4. JVM 메모리 사용량 확인

#### 메모리 부족

1. 힙 덤프 분석
2. 캐시 메모리 사용량 확인
3. 메모리 릭 검사
4. GC 로그 분석

#### 데이터베이스 성능 저하

1. 인덱스 사용률 분석
2. VACUUM 작업 실행
3. 통계 정보 업데이트
4. 연결 풀 설정 검토

### 8.2 성능 분석 도구

- **JProfiler**: JVM 프로파일링
- **VisualVM**: 메모리 및 CPU 분석
- **pgAdmin**: PostgreSQL 성능 분석
- **Redis CLI**: Redis 성능 모니터링

## 9. 참고 자료

- [Spring Boot Caching](https://spring.io/guides/gs/caching/)
- [Redis Best Practices](https://redis.io/topics/memory-optimization)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html) 