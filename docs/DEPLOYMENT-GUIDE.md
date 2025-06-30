# TofuMaker 배포 가이드

## 개요

이 문서는 TofuMaker 애플리케이션을 다양한 환경에 배포하는 방법을 설명합니다.

## 시스템 요구사항

### 최소 요구사항

- **CPU**: 2 코어
- **메모리**: 4GB RAM
- **디스크**: 20GB 여유 공간
- **OS**: Ubuntu 20.04 LTS 이상, CentOS 8 이상

### 권장 요구사항

- **CPU**: 4 코어
- **메모리**: 8GB RAM
- **디스크**: 50GB 여유 공간 (SSD 권장)
- **네트워크**: 1Gbps

### 필수 소프트웨어

- Docker 20.10 이상
- Docker Compose 2.0 이상
- Git
- curl

## 사전 준비

### 1. Docker 설치

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 방화벽 설정

```bash
# Ubuntu UFW
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3000/tcp  # Grafana (선택사항)
sudo ufw allow 9090/tcp  # Prometheus (선택사항)
sudo ufw enable

# CentOS/RHEL firewalld
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

## 배포 방법

### 방법 1: 자동 배포 (권장)

GitHub Actions를 통한 자동 배포를 사용하는 경우:

1. **GitHub Secrets 설정**
   - 리포지토리 Settings > Secrets and variables > Actions
   - 필요한 환경 변수들을 설정 (CI-CD-GUIDE.md 참조)

2. **배포 서버 설정**
   ```bash
   # 배포 서버에서 실행
   git clone https://github.com/your-username/tofumaker.git
   cd tofumaker
   
   # 환경 파일 설정
   cp scripts/deploy/env-template-production.txt .env.production
   # .env.production 파일을 실제 값으로 수정
   ```

3. **자동 배포 실행**
   - main 브랜치에 푸시하면 자동으로 프로덕션 배포
   - develop 브랜치에 푸시하면 자동으로 스테이징 배포

### 방법 2: 수동 배포

#### 스테이징 환경 배포

```bash
# 1. 리포지토리 클론
git clone https://github.com/your-username/tofumaker.git
cd tofumaker

# 2. 환경 파일 설정
cp scripts/deploy/env-template-staging.txt .env.staging
nano .env.staging  # 실제 값으로 수정

# 3. 네트워크 생성
docker network create tofumaker-network

# 4. 스테이징 배포 실행
chmod +x scripts/deploy/deploy-staging.sh
./scripts/deploy/deploy-staging.sh
```

#### 프로덕션 환경 배포

```bash
# 1. 환경 파일 설정
cp scripts/deploy/env-template-production.txt .env.production
nano .env.production  # 실제 값으로 수정

# 2. 백업 디렉토리 생성
mkdir -p backups

# 3. SSL 인증서 설정 (Let's Encrypt)
sudo apt install certbot
sudo certbot certonly --standalone -d tofumaker.com -d www.tofumaker.com
sudo mkdir -p nginx/ssl
sudo cp /etc/letsencrypt/live/tofumaker.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/tofumaker.com/privkey.pem nginx/ssl/

# 4. 프로덕션 배포 실행
chmod +x scripts/deploy/deploy-production.sh
./scripts/deploy/deploy-production.sh
```

### 방법 3: 개발 환경 배포

```bash
# 1. 리포지토리 클론
git clone https://github.com/your-username/tofumaker.git
cd tofumaker

# 2. 개발 환경 시작
docker network create tofumaker-network
docker-compose up -d

# 3. 접속 확인
curl http://localhost:80
curl http://localhost:8081/actuator/health
```

## 환경별 설정

### 스테이징 환경

- **목적**: 개발 완료된 기능의 통합 테스트
- **데이터**: 테스트 데이터 사용
- **모니터링**: 기본 모니터링
- **백업**: 일일 백업

**접속 정보**:
- Frontend: http://staging.tofumaker.com
- Backend API: http://staging.tofumaker.com/api
- Grafana: http://staging.tofumaker.com:3000

### 프로덕션 환경

- **목적**: 실제 서비스 운영
- **데이터**: 실제 운영 데이터
- **모니터링**: 완전한 모니터링 및 알림
- **백업**: 실시간 백업 + 일일 백업

**접속 정보**:
- Frontend: https://tofumaker.com
- Backend API: https://tofumaker.com/api
- Grafana: https://tofumaker.com:3000

## 모니터링 설정

### 모니터링 스택 배포

```bash
# 모니터링 스택 시작
docker-compose -f docker-compose.monitoring.yml up -d

# 서비스 확인
docker-compose -f docker-compose.monitoring.yml ps
```

### Grafana 대시보드 설정

1. **Grafana 접속**: http://localhost:3000
2. **로그인**: admin / admin123
3. **데이터 소스 추가**:
   - Prometheus: http://prometheus:9090
4. **대시보드 임포트**:
   - Spring Boot 대시보드: ID 6756
   - Node Exporter 대시보드: ID 1860

## 백업 및 복구

### 데이터베이스 백업

```bash
# 수동 백업
docker exec tofumaker-postgres-prod pg_dump -U tofumaker_user tofumaker_production > backup_$(date +%Y%m%d_%H%M%S).sql

# 자동 백업 스크립트 설정 (crontab)
0 2 * * * /path/to/tofumaker/scripts/backup/daily-backup.sh
```

### 데이터베이스 복구

```bash
# 복구 실행
docker exec -i tofumaker-postgres-prod psql -U tofumaker_user -d tofumaker_production < backup_file.sql
```

### 전체 시스템 백업

```bash
# Docker 볼륨 백업
docker run --rm -v tofumaker_postgres_data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/postgres_data_$(date +%Y%m%d).tar.gz -C /data .
docker run --rm -v tofumaker_redis_data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/redis_data_$(date +%Y%m%d).tar.gz -C /data .
```

## 성능 튜닝

### 데이터베이스 튜닝

```sql
-- PostgreSQL 설정 최적화
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
SELECT pg_reload_conf();
```

### JVM 튜닝

```bash
# backend/Dockerfile에 JVM 옵션 추가
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"
```

### Nginx 튜닝

```nginx
# nginx/nginx.conf 최적화
worker_processes auto;
worker_connections 2048;
keepalive_timeout 30;
client_max_body_size 10m;
```

## 보안 설정

### SSL/TLS 설정

```bash
# Let's Encrypt 인증서 자동 갱신
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -
```

### 방화벽 강화

```bash
# 불필요한 포트 차단
sudo ufw deny 5432  # PostgreSQL (내부 접근만 허용)
sudo ufw deny 6379  # Redis (내부 접근만 허용)
sudo ufw deny 9090  # Prometheus (VPN 접근만 허용)
```

### 보안 헤더 설정

Nginx 설정에 이미 포함된 보안 헤더들:
- X-Frame-Options
- X-Content-Type-Options
- X-XSS-Protection
- Strict-Transport-Security
- Content-Security-Policy

## 트러블슈팅

### 일반적인 문제 해결

1. **컨테이너 시작 실패**
   ```bash
   # 로그 확인
   docker logs <container_name>
   
   # 컨테이너 재시작
   docker restart <container_name>
   ```

2. **데이터베이스 연결 실패**
   ```bash
   # 네트워크 확인
   docker network ls
   docker network inspect tofumaker-network
   
   # 포트 확인
   netstat -tlnp | grep :5432
   ```

3. **메모리 부족**
   ```bash
   # 메모리 사용량 확인
   docker stats
   free -h
   
   # 불필요한 컨테이너 정리
   docker system prune -f
   ```

### 로그 수집 및 분석

```bash
# 모든 컨테이너 로그 수집
mkdir -p logs/$(date +%Y%m%d)
docker logs tofumaker-backend-prod > logs/$(date +%Y%m%d)/backend.log 2>&1
docker logs tofumaker-frontend-prod > logs/$(date +%Y%m%d)/frontend.log 2>&1
docker logs tofumaker-postgres-prod > logs/$(date +%Y%m%d)/postgres.log 2>&1
```

## 업데이트 및 롤백

### 애플리케이션 업데이트

```bash
# 1. 새 버전 배포
git pull origin main
./scripts/deploy/deploy-production.sh

# 2. 헬스 체크
curl -f https://tofumaker.com/actuator/health
```

### 롤백 절차

```bash
# 1. 이전 버전으로 롤백
docker tag tofumaker-backend:previous tofumaker-backend:latest
docker tag tofumaker-frontend:previous tofumaker-frontend:latest

# 2. 컨테이너 재시작
docker-compose -f docker-compose.production.yml up -d

# 3. 확인
curl -f https://tofumaker.com/actuator/health
```

## 체크리스트

### 배포 전 체크리스트

- [ ] 환경 변수 설정 완료
- [ ] SSL 인증서 설정 완료
- [ ] 방화벽 규칙 설정 완료
- [ ] 백업 디렉토리 생성 완료
- [ ] 모니터링 설정 완료

### 배포 후 체크리스트

- [ ] 애플리케이션 정상 시작 확인
- [ ] 데이터베이스 연결 확인
- [ ] API 엔드포인트 응답 확인
- [ ] 프론트엔드 페이지 로딩 확인
- [ ] 모니터링 메트릭 수집 확인
- [ ] 로그 수집 확인
- [ ] 백업 스케줄 확인

## 지원 및 문의

- **기술 지원**: tech-support@tofumaker.com
- **긴급 상황**: +82-10-xxxx-xxxx
- **문서 업데이트**: docs@tofumaker.com

## 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [PostgreSQL 문서](https://www.postgresql.org/docs/)
- [Nginx 문서](https://nginx.org/en/docs/) 