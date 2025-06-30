Write-Host "🚀 TofuMaker 모니터링 시스템 시작 중..." -ForegroundColor Green

# 네트워크 생성 (이미 존재하면 무시)
Write-Host "📡 Docker 네트워크 생성 중..." -ForegroundColor Yellow
try {
    docker network create tofumaker-network 2>$null
    Write-Host "네트워크 생성 완료" -ForegroundColor Green
} catch {
    Write-Host "네트워크가 이미 존재합니다." -ForegroundColor Yellow
}

# 로그 디렉터리 생성 (Windows의 경우 Docker 볼륨 사용)
Write-Host "📁 로그 디렉터리 확인 중..." -ForegroundColor Yellow

# Prometheus + Grafana 시작
Write-Host "📊 Prometheus 및 Grafana 시작 중..." -ForegroundColor Yellow
docker-compose -f docker-compose.monitoring.yml up -d

# ELK Stack 시작
Write-Host "🔍 ELK Stack 시작 중..." -ForegroundColor Yellow
docker-compose -f docker-compose.elk.yml up -d

# 서비스 상태 확인
Write-Host "⏳ 서비스 시작 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "🔍 서비스 상태 확인 중..." -ForegroundColor Yellow

# 헬스 체크
Write-Host "🏥 헬스 체크 수행 중..." -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Prometheus 정상" -ForegroundColor Green
} catch {
    Write-Host "❌ Prometheus 오류" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Grafana 정상" -ForegroundColor Green
} catch {
    Write-Host "❌ Grafana 오류" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:9200/_cluster/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Elasticsearch 정상" -ForegroundColor Green
} catch {
    Write-Host "❌ Elasticsearch 오류" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:5601/api/status" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Kibana 정상" -ForegroundColor Green
} catch {
    Write-Host "❌ Kibana 오류" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎉 모니터링 시스템 시작 완료!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 접속 정보:" -ForegroundColor Cyan
Write-Host "  - Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "  - Grafana: http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host "  - Kibana: http://localhost:5601" -ForegroundColor White
Write-Host "  - Alertmanager: http://localhost:9093" -ForegroundColor White
Write-Host ""
Write-Host "📊 주요 메트릭:" -ForegroundColor Cyan
Write-Host "  - Node Exporter: http://localhost:9100/metrics" -ForegroundColor White
Write-Host "  - cAdvisor: http://localhost:8080" -ForegroundColor White
Write-Host "  - PostgreSQL Exporter: http://localhost:9187/metrics" -ForegroundColor White
Write-Host "  - Redis Exporter: http://localhost:9121/metrics" -ForegroundColor White 