#!/bin/bash

# TofuMaker 성능 테스트 실행 스크립트

set -e

echo "=== TofuMaker Performance Test Runner ==="

# 변수 설정
JMETER_HOME=${JMETER_HOME:-"/opt/apache-jmeter"}
TEST_PLAN="performance-tests/load-test.jmx"
RESULTS_DIR="performance-tests/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_DIR="$RESULTS_DIR/html-report-$TIMESTAMP"

# 결과 디렉터리 생성
mkdir -p "$RESULTS_DIR"

# JMeter 설치 확인
if ! command -v jmeter &> /dev/null; then
    echo "JMeter가 설치되지 않았습니다. 설치를 진행합니다..."
    
    # JMeter 다운로드 및 설치 (Linux/macOS)
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Linux에서 JMeter 설치 중..."
        wget -O /tmp/apache-jmeter.tgz https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.4.1.tgz
        sudo tar -xzf /tmp/apache-jmeter.tgz -C /opt/
        sudo ln -sf /opt/apache-jmeter-5.4.1 /opt/apache-jmeter
        sudo ln -sf /opt/apache-jmeter/bin/jmeter /usr/local/bin/jmeter
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macOS에서 JMeter 설치 중..."
        if command -v brew &> /dev/null; then
            brew install jmeter
        else
            echo "Homebrew가 필요합니다. 수동으로 JMeter를 설치해주세요."
            exit 1
        fi
    fi
fi

# 애플리케이션 상태 확인
echo "애플리케이션 상태 확인 중..."
if ! curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "경고: 애플리케이션이 실행되지 않았거나 응답하지 않습니다."
    echo "http://localhost:8080에서 애플리케이션을 시작한 후 다시 시도하세요."
    exit 1
fi

echo "애플리케이션이 정상적으로 실행 중입니다."

# 성능 테스트 실행
echo "성능 테스트 실행 중..."
echo "테스트 계획: $TEST_PLAN"
echo "결과 디렉터리: $RESULTS_DIR"

# JMeter 명령행 실행
jmeter -n -t "$TEST_PLAN" \
    -l "$RESULTS_DIR/results-$TIMESTAMP.jtl" \
    -e -o "$REPORT_DIR" \
    -Jjmeter.reportgenerator.overall_granularity=60000 \
    -Jjmeter.reportgenerator.graph.responseTimeDistribution.property.set_granularity=100

echo "성능 테스트 완료!"
echo "결과 파일: $RESULTS_DIR/results-$TIMESTAMP.jtl"
echo "HTML 리포트: $REPORT_DIR/index.html"

# 간단한 결과 요약 출력
echo ""
echo "=== 테스트 결과 요약 ==="
if [ -f "$RESULTS_DIR/results-$TIMESTAMP.jtl" ]; then
    # 평균 응답 시간 계산
    avg_response_time=$(awk -F',' 'NR>1 {sum+=$2; count++} END {if(count>0) print sum/count; else print 0}' "$RESULTS_DIR/results-$TIMESTAMP.jtl")
    
    # 성공률 계산
    success_rate=$(awk -F',' 'NR>1 {total++; if($8=="true") success++} END {if(total>0) print (success/total)*100; else print 0}' "$RESULTS_DIR/results-$TIMESTAMP.jtl")
    
    echo "평균 응답 시간: ${avg_response_time}ms"
    echo "성공률: ${success_rate}%"
    
    # 임계값 확인
    if (( $(echo "$avg_response_time > 1000" | bc -l) )); then
        echo "⚠️  경고: 평균 응답 시간이 1초를 초과했습니다."
    fi
    
    if (( $(echo "$success_rate < 95" | bc -l) )); then
        echo "⚠️  경고: 성공률이 95% 미만입니다."
    fi
fi

echo ""
echo "상세한 결과는 HTML 리포트를 확인하세요: $REPORT_DIR/index.html"

# 브라우저에서 리포트 열기 (선택사항)
read -p "브라우저에서 리포트를 여시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v xdg-open &> /dev/null; then
        xdg-open "$REPORT_DIR/index.html"
    elif command -v open &> /dev/null; then
        open "$REPORT_DIR/index.html"
    else
        echo "브라우저를 자동으로 열 수 없습니다. 수동으로 $REPORT_DIR/index.html을 열어주세요."
    fi
fi 