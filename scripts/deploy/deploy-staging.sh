#!/bin/bash

# 스테이징 환경 배포 스크립트
set -e

echo "🚀 Starting staging deployment..."

# 환경 변수 설정
export ENVIRONMENT="staging"
export COMPOSE_FILE="docker-compose.staging.yml"
export IMAGE_TAG=${GITHUB_SHA:-latest}

# 환경 변수 파일 확인
if [ ! -f ".env.staging" ]; then
    echo "❌ .env.staging file not found!"
    exit 1
fi

# 환경 변수 로드
source .env.staging

echo "📦 Pulling latest images..."
docker-compose -f $COMPOSE_FILE pull

echo "🛑 Stopping existing containers..."
docker-compose -f $COMPOSE_FILE down --remove-orphans

echo "🧹 Cleaning up unused images..."
docker image prune -f

echo "🔧 Starting services..."
docker-compose -f $COMPOSE_FILE up -d

echo "⏳ Waiting for services to be healthy..."
timeout 300 bash -c '
    while ! docker-compose -f '$COMPOSE_FILE' ps | grep -q "healthy"; do
        echo "Waiting for services to start..."
        sleep 10
    done
'

echo "🔍 Checking service health..."
docker-compose -f $COMPOSE_FILE ps

# 헬스 체크
echo "🏥 Running health checks..."
for i in {1..30}; do
    if curl -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
        echo "✅ Backend health check passed"
        break
    fi
    echo "Waiting for backend to be ready... ($i/30)"
    sleep 10
done

for i in {1..30}; do
    if curl -f http://localhost:81 > /dev/null 2>&1; then
        echo "✅ Frontend health check passed"
        break
    fi
    echo "Waiting for frontend to be ready... ($i/30)"
    sleep 10
done

echo "📊 Deployment summary:"
echo "Environment: $ENVIRONMENT"
echo "Image Tag: $IMAGE_TAG"
echo "Backend URL: http://localhost:8082"
echo "Frontend URL: http://localhost:81"

echo "✅ Staging deployment completed successfully!"

# 슬랙 알림 (선택사항)
if [ ! -z "$SLACK_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"🚀 TofuMaker staging deployment completed successfully! Image: $IMAGE_TAG\"}" \
        $SLACK_WEBHOOK_URL
fi 