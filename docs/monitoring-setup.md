# TofuMaker 모니터링 및 로깅 시스템

## 개요

TofuMaker 프로젝트의 종합적인 모니터링 및 로깅 시스템입니다. Prometheus + Grafana를 통한 메트릭 모니터링과 ELK Stack을 통한 로그 분석을 제공합니다.

## 시스템 구성

### 모니터링 스택 (Prometheus + Grafana)

- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 대시보드 및 시각화
- **Alertmanager**: 알림 관리
- **Node Exporter**: 시스템 메트릭 수집
- **cAdvisor**: 컨테이너 메트릭 수집
- **PostgreSQL Exporter**: 데이터베이스 메트릭
- **Redis Exporter**: Redis 메트릭
- **Nginx Exporter**: 웹서버 메트릭

### 로깅 스택 (ELK Stack)

- **Elasticsearch**: 로그 저장 및 검색
- **Logstash**: 로그 처리 및 변환
- **Kibana**: 로그 분석 및 시각화
- **Filebeat**: 로그 수집

## 설치 및 실행

### 1. 사전 요구사항

- Docker 및 Docker Compose 설치
- 최소 4GB RAM 권장 (ELK Stack 포함)

### 2. 시스템 시작

#### Linux/macOS
```bash
chmod +x scripts/start-monitoring.sh
./scripts/start-monitoring.sh
```

#### Windows PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-monitoring.ps1
```

#### 수동 시작
```bash
# 네트워크 생성
docker network create tofumaker-network

# 모니터링 스택 시작
docker-compose -f docker-compose.monitoring.yml up -d

# ELK 스택 시작
docker-compose -f docker-compose.elk.yml up -d
```

### 3. 접속 정보

| 서비스 | URL | 기본 계정 |
|--------|-----|-----------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin123 |
| Kibana | http://localhost:5601 | - |
| Alertmanager | http://localhost:9093 | - |
| Elasticsearch | http://localhost:9200 | - |

## 애플리케이션 메트릭

### 커스텀 메트릭

TofuMaker 백엔드 애플리케이션에서 수집하는 주요 메트릭:

#### 카운터 메트릭
- `user_logins_total`: 사용자 로그인 횟수
- `api_requests_total{endpoint}`: API 요청 횟수 (엔드포인트별)
- `application_errors_total{type}`: 애플리케이션 에러 횟수 (타입별)
- `health_check_requests_total`: 헬스체크 요청 횟수

#### 타이머 메트릭
- `database_query_duration_seconds`: 데이터베이스 쿼리 실행 시간
- `redis_operation_duration_seconds`: Redis 작업 실행 시간
- `health_check_duration_seconds`: 헬스체크 실행 시간

#### 게이지 메트릭
- `active_users_count`: 현재 활성 사용자 수
- `database_connections_active`: 활성 데이터베이스 연결 수
- `redis_memory_usage_bytes`: Redis 메모리 사용량
- `jvm_memory_used_bytes`: JVM 메모리 사용량

### 헬스체크 엔드포인트

#### `/api/health`
- 전체 시스템 상태 확인
- 데이터베이스 및 Redis 연결 상태 포함
- 메트릭 수집 포함

#### `/api/health/ready`
- 애플리케이션 준비 상태 확인
- Kubernetes readiness probe 용도

#### `/api/health/live`
- 애플리케이션 생존 상태 확인
- Kubernetes liveness probe 용도

## Grafana 대시보드

### TofuMaker Application Overview
- API 요청률 및 에러율
- 데이터베이스 쿼리 성능
- 메모리 사용량
- 활성 사용자 수
- 시스템 리소스 상태

### 대시보드 접속
1. Grafana (http://localhost:3000) 접속
2. admin/admin123으로 로그인
3. "TofuMaker Application Overview" 대시보드 선택

## 로그 분석 (Kibana)

### 로그 인덱스
- `tofumaker-logs-*`: 애플리케이션 로그
- 일별 인덱스 생성 (예: tofumaker-logs-2024.01.15)

### 로그 필드
- `@timestamp`: 로그 시간
- `level`: 로그 레벨 (ERROR, WARN, INFO, DEBUG)
- `logger`: 로거 이름
- `thread`: 스레드 이름
- `service`: 서비스 이름
- `environment`: 환경 (production, staging, development)
- `log_message`: 로그 메시지

### Kibana 사용법
1. Kibana (http://localhost:5601) 접속
2. Index Patterns에서 `tofumaker-logs-*` 패턴 생성
3. Discover 탭에서 로그 검색 및 분석

## 알림 설정

### Alertmanager 설정
- 설정 파일: `monitoring/alertmanager.yml`
- 알림 규칙: `monitoring/alert_rules.yml`

### 주요 알림 규칙
- 높은 에러율 (5분간 5% 이상)
- 높은 응답 시간 (95th percentile > 1초)
- 데이터베이스 연결 실패
- 메모리 사용량 임계치 초과

## 성능 최적화

### Prometheus 설정
- 메트릭 보존 기간: 200시간
- 스크래핑 간격: 15초
- 평가 간격: 15초

### Elasticsearch 설정
- 샤드 수: 1 (단일 노드)
- 복제본: 0 (개발/테스트 환경)
- 압축: best_compression

## 트러블슈팅

### 일반적인 문제

#### 1. 메트릭이 수집되지 않는 경우
```bash
# Prometheus 타겟 상태 확인
curl http://localhost:9090/api/v1/targets

# 애플리케이션 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus
```

#### 2. 로그가 Elasticsearch에 저장되지 않는 경우
```bash
# Logstash 로그 확인
docker logs tofumaker-logstash

# Filebeat 상태 확인
docker logs tofumaker-filebeat

# Elasticsearch 인덱스 확인
curl http://localhost:9200/_cat/indices
```

#### 3. Grafana 대시보드가 로드되지 않는 경우
```bash
# Grafana 로그 확인
docker logs tofumaker-grafana

# Prometheus 연결 확인
curl http://localhost:3000/api/datasources
```

### 로그 레벨 조정

개발 환경에서 더 자세한 로그가 필요한 경우:

```yaml
# application-development.yml
logging:
  level:
    com.tofumaker: DEBUG
    org.springframework.web: DEBUG
```

## 보안 고려사항

### 프로덕션 환경 설정
1. Grafana 기본 비밀번호 변경
2. Elasticsearch 보안 활성화
3. 네트워크 접근 제한
4. SSL/TLS 인증서 적용

### 환경 변수
```bash
# .env 파일에 추가
GRAFANA_PASSWORD=your_secure_password
ELASTICSEARCH_PASSWORD=your_secure_password
```

## 확장 가능성

### 추가 메트릭 수집
- OpenStack API 메트릭
- 비즈니스 메트릭 (리소스 생성/삭제 등)
- 사용자 행동 메트릭

### 추가 알림 채널
- Slack 통합
- 이메일 알림
- PagerDuty 연동

### 고가용성 구성
- Prometheus HA 설정
- Elasticsearch 클러스터
- Grafana 로드 밸런싱

## 참고 자료

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Elastic Stack Documentation](https://www.elastic.co/guide/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs/) 