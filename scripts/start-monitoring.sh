#!/bin/bash

echo "🚀 TofuMaker 모니터링 시스템 시작 중..."

# 네트워크 생성 (이미 존재하면 무시)
echo "📡 Docker 네트워크 생성 중..."
docker network create tofumaker-network 2>/dev/null || echo "네트워크가 이미 존재합니다."

# 로그 디렉터리 생성
echo "📁 로그 디렉터리 생성 중..."
sudo mkdir -p /var/log/tofumaker
sudo chmod 755 /var/log/tofumaker

# Prometheus + Grafana 시작
echo "📊 Prometheus 및 Grafana 시작 중..."
docker-compose -f docker-compose.monitoring.yml up -d

# ELK Stack 시작
echo "🔍 ELK Stack 시작 중..."
docker-compose -f docker-compose.elk.yml up -d

# 서비스 상태 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "🔍 서비스 상태 확인 중..."
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin123)"
echo "Kibana: http://localhost:5601"
echo "Elasticsearch: http://localhost:9200"

# 헬스 체크
echo "🏥 헬스 체크 수행 중..."
curl -s http://localhost:9090/-/healthy > /dev/null && echo "✅ Prometheus 정상" || echo "❌ Prometheus 오류"
curl -s http://localhost:3000/api/health > /dev/null && echo "✅ Grafana 정상" || echo "❌ Grafana 오류"
curl -s http://localhost:9200/_cluster/health > /dev/null && echo "✅ Elasticsearch 정상" || echo "❌ Elasticsearch 오류"
curl -s http://localhost:5601/api/status > /dev/null && echo "✅ Kibana 정상" || echo "❌ Kibana 오류"

echo "🎉 모니터링 시스템 시작 완료!"
echo ""
echo "📋 접속 정보:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin123)"
echo "  - Kibana: http://localhost:5601"
echo "  - Alertmanager: http://localhost:9093"
echo ""
echo "📊 주요 메트릭:"
echo "  - Node Exporter: http://localhost:9100/metrics"
echo "  - cAdvisor: http://localhost:8080"
echo "  - PostgreSQL Exporter: http://localhost:9187/metrics"
echo "  - Redis Exporter: http://localhost:9121/metrics" 