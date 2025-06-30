#!/bin/bash

# TofuMaker 부하 테스트 스크립트
set -e

echo "🚀 Starting TofuMaker Load Testing..."

# 설정
BASE_URL=${1:-"http://localhost:8081"}
CONCURRENT_USERS=${2:-10}
TOTAL_REQUESTS=${3:-1000}
TEST_DURATION=${4:-60}

echo "📊 Test Configuration:"
echo "  Base URL: $BASE_URL"
echo "  Concurrent Users: $CONCURRENT_USERS"
echo "  Total Requests: $TOTAL_REQUESTS"
echo "  Test Duration: ${TEST_DURATION}s"
echo ""

# 결과 디렉토리 생성
RESULTS_DIR="results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

# 헬스 체크
echo "🏥 Health Check..."
if ! curl -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "❌ Application is not healthy. Aborting tests."
    exit 1
fi
echo "✅ Application is healthy"
echo ""

# 1. 홈페이지 부하 테스트
echo "🏠 Testing Homepage..."
ab -n $TOTAL_REQUESTS -c $CONCURRENT_USERS -g "$RESULTS_DIR/homepage.tsv" "$BASE_URL/" > "$RESULTS_DIR/homepage.txt" 2>&1
echo "✅ Homepage test completed"

# 2. API 엔드포인트 부하 테스트
echo "📡 Testing API Endpoints..."

# 게시글 목록 조회
echo "  - Testing GET /api/boards..."
ab -n $TOTAL_REQUESTS -c $CONCURRENT_USERS -g "$RESULTS_DIR/boards_list.tsv" "$BASE_URL/api/boards" > "$RESULTS_DIR/boards_list.txt" 2>&1

# 헬스 체크 엔드포인트
echo "  - Testing GET /actuator/health..."
ab -n $TOTAL_REQUESTS -c $CONCURRENT_USERS -g "$RESULTS_DIR/health.tsv" "$BASE_URL/actuator/health" > "$RESULTS_DIR/health.txt" 2>&1

# 메트릭 엔드포인트
echo "  - Testing GET /actuator/metrics..."
ab -n 100 -c 5 -g "$RESULTS_DIR/metrics.tsv" "$BASE_URL/actuator/metrics" > "$RESULTS_DIR/metrics.txt" 2>&1

echo "✅ API endpoint tests completed"

# 3. 지속적인 부하 테스트 (시간 기반)
echo "⏱️ Running sustained load test for ${TEST_DURATION}s..."
timeout $TEST_DURATION ab -t $TEST_DURATION -c $CONCURRENT_USERS -g "$RESULTS_DIR/sustained.tsv" "$BASE_URL/api/boards" > "$RESULTS_DIR/sustained.txt" 2>&1 || true
echo "✅ Sustained load test completed"

# 4. 스파이크 테스트 (높은 동시 사용자)
SPIKE_USERS=$((CONCURRENT_USERS * 5))
echo "⚡ Running spike test with $SPIKE_USERS concurrent users..."
ab -n 500 -c $SPIKE_USERS -g "$RESULTS_DIR/spike.tsv" "$BASE_URL/api/boards" > "$RESULTS_DIR/spike.txt" 2>&1
echo "✅ Spike test completed"

# 결과 분석
echo ""
echo "📊 Test Results Summary:"
echo "========================"

# 각 테스트 결과에서 주요 메트릭 추출
for test in homepage boards_list health sustained spike; do
    if [ -f "$RESULTS_DIR/${test}.txt" ]; then
        echo ""
        echo "🔍 $test Test Results:"
        echo "  Requests per second: $(grep "Requests per second" "$RESULTS_DIR/${test}.txt" | awk '{print $4}')"
        echo "  Time per request: $(grep "Time per request" "$RESULTS_DIR/${test}.txt" | head -1 | awk '{print $4}')"
        echo "  Failed requests: $(grep "Failed requests" "$RESULTS_DIR/${test}.txt" | awk '{print $3}')"
        echo "  95% response time: $(grep "95%" "$RESULTS_DIR/${test}.txt" | awk '{print $2}')"
    fi
done

# 시스템 리소스 사용률 체크
echo ""
echo "💻 System Resource Usage:"
echo "  CPU Usage: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
echo "  Memory Usage: $(free | grep Mem | awk '{printf "%.1f%%", $3/$2 * 100.0}')"
echo "  Disk Usage: $(df -h / | awk 'NR==2{printf "%s", $5}')"

# 결과 파일 위치 안내
echo ""
echo "📁 Detailed results saved to: $RESULTS_DIR"
echo "📈 TSV files can be imported into Excel/Google Sheets for visualization"

# 성능 기준 체크
echo ""
echo "🎯 Performance Criteria Check:"
echo "==============================="

# 홈페이지 응답 시간 체크 (2초 이하)
if [ -f "$RESULTS_DIR/homepage.txt" ]; then
    HOMEPAGE_AVG=$(grep "Time per request" "$RESULTS_DIR/homepage.txt" | head -1 | awk '{print $4}')
    if (( $(echo "$HOMEPAGE_AVG < 2000" | bc -l) )); then
        echo "✅ Homepage response time: ${HOMEPAGE_AVG}ms (< 2000ms)"
    else
        echo "❌ Homepage response time: ${HOMEPAGE_AVG}ms (>= 2000ms)"
    fi
fi

# API 응답 시간 체크 (1초 이하)
if [ -f "$RESULTS_DIR/boards_list.txt" ]; then
    API_AVG=$(grep "Time per request" "$RESULTS_DIR/boards_list.txt" | head -1 | awk '{print $4}')
    if (( $(echo "$API_AVG < 1000" | bc -l) )); then
        echo "✅ API response time: ${API_AVG}ms (< 1000ms)"
    else
        echo "❌ API response time: ${API_AVG}ms (>= 1000ms)"
    fi
fi

# 실패율 체크 (1% 이하)
if [ -f "$RESULTS_DIR/boards_list.txt" ]; then
    FAILED_REQUESTS=$(grep "Failed requests" "$RESULTS_DIR/boards_list.txt" | awk '{print $3}')
    TOTAL_REQUESTS_ACTUAL=$(grep "Complete requests" "$RESULTS_DIR/boards_list.txt" | awk '{print $3}')
    FAILURE_RATE=$(echo "scale=2; $FAILED_REQUESTS * 100 / $TOTAL_REQUESTS_ACTUAL" | bc)
    if (( $(echo "$FAILURE_RATE < 1" | bc -l) )); then
        echo "✅ Failure rate: ${FAILURE_RATE}% (< 1%)"
    else
        echo "❌ Failure rate: ${FAILURE_RATE}% (>= 1%)"
    fi
fi

echo ""
echo "🎉 Load testing completed!"
echo "📊 Check Grafana dashboard for real-time metrics: http://localhost:3000" 