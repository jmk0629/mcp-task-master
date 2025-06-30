#!/bin/bash

# 프로덕션 환경 배포 스크립트
set -e

echo "🚀 Starting production deployment..."

# 환경 변수 설정
export ENVIRONMENT="production"
export COMPOSE_FILE="docker-compose.production.yml"
export IMAGE_TAG=${GITHUB_SHA:-latest}

# 환경 변수 파일 확인
if [ ! -f ".env.production" ]; then
    echo "❌ .env.production file not found!"
    exit 1
fi

# 환경 변수 로드
source .env.production

# 백업 생성
echo "💾 Creating database backup..."
BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
docker exec tofumaker-postgres-prod pg_dump -U tofumaker_user tofumaker_production > "backups/$BACKUP_FILE" || echo "⚠️ Backup failed, continuing..."

echo "📦 Pulling latest images..."
docker-compose -f $COMPOSE_FILE pull

echo "🔄 Rolling update deployment..."
# 백엔드 롤링 업데이트
echo "🔄 Updating backend services..."
docker-compose -f $COMPOSE_FILE up -d --no-deps backend

echo "⏳ Waiting for new backend to be healthy..."
for i in {1..60}; do
    if curl -f http://localhost:8083/actuator/health > /dev/null 2>&1; then
        echo "✅ New backend is healthy"
        break
    fi
    echo "Waiting for new backend to be ready... ($i/60)"
    sleep 10
done

# 프론트엔드 롤링 업데이트
echo "🔄 Updating frontend services..."
docker-compose -f $COMPOSE_FILE up -d --no-deps frontend

echo "⏳ Waiting for new frontend to be healthy..."
for i in {1..30}; do
    if curl -f http://localhost:80 > /dev/null 2>&1; then
        echo "✅ New frontend is healthy"
        break
    fi
    echo "Waiting for new frontend to be ready... ($i/30)"
    sleep 10
done

echo "🧹 Cleaning up old images..."
docker image prune -f

echo "🔍 Checking service health..."
docker-compose -f $COMPOSE_FILE ps

# 종합 헬스 체크
echo "🏥 Running comprehensive health checks..."
HEALTH_CHECK_FAILED=false

# 백엔드 헬스 체크
if ! curl -f http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "❌ Backend health check failed"
    HEALTH_CHECK_FAILED=true
fi

# 프론트엔드 헬스 체크
if ! curl -f http://localhost:80 > /dev/null 2>&1; then
    echo "❌ Frontend health check failed"
    HEALTH_CHECK_FAILED=true
fi

# 데이터베이스 연결 체크
if ! docker exec tofumaker-postgres-prod pg_isready -U tofumaker_user > /dev/null 2>&1; then
    echo "❌ Database health check failed"
    HEALTH_CHECK_FAILED=true
fi

if [ "$HEALTH_CHECK_FAILED" = true ]; then
    echo "❌ Health checks failed! Rolling back..."
    
    # 롤백 로직
    echo "🔄 Rolling back to previous version..."
    docker-compose -f $COMPOSE_FILE down
    
    # 이전 이미지로 복원 (태그 기반)
    PREVIOUS_TAG=$(docker images --format "table {{.Repository}}:{{.Tag}}" | grep tofumaker | head -2 | tail -1 | cut -d: -f2)
    if [ ! -z "$PREVIOUS_TAG" ]; then
        export IMAGE_TAG=$PREVIOUS_TAG
        docker-compose -f $COMPOSE_FILE up -d
        echo "🔄 Rolled back to version: $PREVIOUS_TAG"
    fi
    
    # 슬랙 알림
    if [ ! -z "$SLACK_WEBHOOK_URL" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"❌ TofuMaker production deployment failed and rolled back! Image: $IMAGE_TAG\"}" \
            $SLACK_WEBHOOK_URL
    fi
    
    exit 1
fi

echo "📊 Deployment summary:"
echo "Environment: $ENVIRONMENT"
echo "Image Tag: $IMAGE_TAG"
echo "Backend URL: http://localhost:8083"
echo "Frontend URL: http://localhost:80"
echo "Backup File: $BACKUP_FILE"

echo "✅ Production deployment completed successfully!"

# 성공 알림
if [ ! -z "$SLACK_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"✅ TofuMaker production deployment completed successfully! Image: $IMAGE_TAG\"}" \
        $SLACK_WEBHOOK_URL
fi

# 모니터링 알림
if [ ! -z "$MONITORING_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"event\":\"deployment\",\"environment\":\"production\",\"version\":\"$IMAGE_TAG\",\"status\":\"success\"}" \
        $MONITORING_WEBHOOK_URL
fi 