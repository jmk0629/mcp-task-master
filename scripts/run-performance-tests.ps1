# TofuMaker 성능 테스트 실행 스크립트 (Windows PowerShell)

param(
    [string]$JMeterPath = "C:\apache-jmeter\bin\jmeter.bat",
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "=== TofuMaker Performance Test Runner ===" -ForegroundColor Green

# 변수 설정
$TestPlan = "performance-tests\load-test.jmx"
$ResultsDir = "performance-tests\results"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ReportDir = "$ResultsDir\html-report-$Timestamp"

# 결과 디렉터리 생성
if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
}

# JMeter 설치 확인
if (!(Test-Path $JMeterPath)) {
    Write-Host "JMeter를 찾을 수 없습니다: $JMeterPath" -ForegroundColor Red
    Write-Host "JMeter 설치 방법:" -ForegroundColor Yellow
    Write-Host "1. https://jmeter.apache.org/download_jmeter.cgi 에서 JMeter 다운로드" -ForegroundColor Yellow
    Write-Host "2. C:\apache-jmeter 에 압축 해제" -ForegroundColor Yellow
    Write-Host "3. 또는 -JMeterPath 매개변수로 올바른 경로 지정" -ForegroundColor Yellow
    
    # Chocolatey를 통한 자동 설치 시도
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        $install = Read-Host "Chocolatey를 통해 JMeter를 설치하시겠습니까? (y/n)"
        if ($install -eq 'y' -or $install -eq 'Y') {
            Write-Host "JMeter 설치 중..." -ForegroundColor Yellow
            choco install jmeter -y
            $JMeterPath = "jmeter.bat"  # PATH에 추가된 경우
        } else {
            exit 1
        }
    } else {
        exit 1
    }
}

# 애플리케이션 상태 확인
Write-Host "애플리케이션 상태 확인 중..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/health" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "애플리케이션이 정상적으로 실행 중입니다." -ForegroundColor Green
    } else {
        throw "응답 코드: $($response.StatusCode)"
    }
} catch {
    Write-Host "경고: 애플리케이션이 실행되지 않았거나 응답하지 않습니다." -ForegroundColor Red
    Write-Host "$BaseUrl 에서 애플리케이션을 시작한 후 다시 시도하세요." -ForegroundColor Red
    Write-Host "오류: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 성능 테스트 실행
Write-Host "성능 테스트 실행 중..." -ForegroundColor Yellow
Write-Host "테스트 계획: $TestPlan" -ForegroundColor Cyan
Write-Host "결과 디렉터리: $ResultsDir" -ForegroundColor Cyan

$ResultFile = "$ResultsDir\results-$Timestamp.jtl"

# JMeter 명령행 실행
$JMeterArgs = @(
    "-n",
    "-t", $TestPlan,
    "-l", $ResultFile,
    "-e",
    "-o", $ReportDir,
    "-Jjmeter.reportgenerator.overall_granularity=60000",
    "-Jjmeter.reportgenerator.graph.responseTimeDistribution.property.set_granularity=100"
)

Write-Host "JMeter 실행 중..." -ForegroundColor Yellow
try {
    & $JMeterPath $JMeterArgs
    if ($LASTEXITCODE -eq 0) {
        Write-Host "성능 테스트 완료!" -ForegroundColor Green
    } else {
        throw "JMeter 실행 실패 (종료 코드: $LASTEXITCODE)"
    }
} catch {
    Write-Host "JMeter 실행 중 오류 발생: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "결과 파일: $ResultFile" -ForegroundColor Cyan
Write-Host "HTML 리포트: $ReportDir\index.html" -ForegroundColor Cyan

# 간단한 결과 요약 출력
Write-Host ""
Write-Host "=== 테스트 결과 요약 ===" -ForegroundColor Green

if (Test-Path $ResultFile) {
    try {
        $results = Import-Csv $ResultFile
        
        if ($results.Count -gt 0) {
            # 평균 응답 시간 계산 (elapsed 컬럼)
            $avgResponseTime = ($results | Where-Object { $_.elapsed -match '^\d+$' } | 
                               Measure-Object -Property elapsed -Average).Average
            
            # 성공률 계산 (success 컬럼)
            $totalRequests = $results.Count
            $successfulRequests = ($results | Where-Object { $_.success -eq 'true' }).Count
            $successRate = if ($totalRequests -gt 0) { ($successfulRequests / $totalRequests) * 100 } else { 0 }
            
            Write-Host "총 요청 수: $totalRequests" -ForegroundColor White
            Write-Host "성공한 요청 수: $successfulRequests" -ForegroundColor White
            Write-Host "평균 응답 시간: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor White
            Write-Host "성공률: $([math]::Round($successRate, 2))%" -ForegroundColor White
            
            # 임계값 확인
            if ($avgResponseTime -gt 1000) {
                Write-Host "⚠️  경고: 평균 응답 시간이 1초를 초과했습니다." -ForegroundColor Yellow
            }
            
            if ($successRate -lt 95) {
                Write-Host "⚠️  경고: 성공률이 95% 미만입니다." -ForegroundColor Yellow
            }
            
            if ($avgResponseTime -le 1000 -and $successRate -ge 95) {
                Write-Host "✅ 성능 테스트 통과!" -ForegroundColor Green
            }
        } else {
            Write-Host "결과 파일이 비어있습니다." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "결과 파일 분석 중 오류 발생: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "결과 파일을 찾을 수 없습니다: $ResultFile" -ForegroundColor Red
}

Write-Host ""
Write-Host "상세한 결과는 HTML 리포트를 확인하세요: $ReportDir\index.html" -ForegroundColor Cyan

# 브라우저에서 리포트 열기 (선택사항)
$openReport = Read-Host "브라우저에서 리포트를 여시겠습니까? (y/n)"
if ($openReport -eq 'y' -or $openReport -eq 'Y') {
    $reportPath = Join-Path $ReportDir "index.html"
    if (Test-Path $reportPath) {
        Start-Process $reportPath
    } else {
        Write-Host "리포트 파일을 찾을 수 없습니다: $reportPath" -ForegroundColor Red
    }
}

Write-Host "성능 테스트 완료!" -ForegroundColor Green 