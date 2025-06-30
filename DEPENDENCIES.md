# 의존성 관리 가이드

## 개요
이 문서는 Board-Stack 프로젝트의 의존성 관리 정책과 사용된 라이브러리에 대해 설명합니다.

## 주요 의존성

### 1. Spring Boot 관련
- **spring-boot-starter-web**: REST API 개발을 위한 기본 웹 스타터
- **spring-boot-starter-webflux**: 비동기 HTTP 클라이언트 (OpenStack API 호출용)
- **spring-boot-starter-validation**: 입력 데이터 유효성 검증
- **spring-boot-starter-test**: 단위 테스트 및 통합 테스트
- **spring-boot-configuration-processor**: 설정 프로퍼티 자동 완성 지원

### 2. HTTP 클라이언트
- **Apache HttpClient**: RestTemplate 기반 HTTP 통신
- **WebFlux**: 비동기 HTTP 클라이언트 (tofumaker API 호출용)
- **Reactor Test**: WebFlux 테스트 지원

### 3. JSON 처리
- **Jackson Databind**: JSON 직렬화/역직렬화
- **Jackson JSR310**: Java 8 시간 API 지원

### 4. 템플릿 엔진
- **FreeMarker**: Terraform 템플릿 동적 생성용
- **SnakeYAML**: YAML 파일 처리

### 5. 유틸리티
- **Lombok**: 보일러플레이트 코드 자동 생성

## 버전 관리 정책

### Spring Boot BOM 사용
- Spring Boot Parent POM을 상속받아 의존성 버전을 자동 관리
- 버전 충돌 방지 및 호환성 보장

### 명시적 버전 관리
```xml
<properties>
    <httpclient.version>4.5.14</httpclient.version>
    <freemarker.version>2.3.32</freemarker.version>
</properties>
```

## 의존성 추가 가이드

### 1. 새로운 의존성 추가 시
1. `pom.xml`의 `<dependencies>` 섹션에 추가
2. 가능한 경우 Spring Boot BOM에서 관리하는 버전 사용
3. 필요시 `<properties>`에 버전 명시
4. `mvn clean compile`로 컴파일 확인

### 2. 의존성 제거 시
1. `pom.xml`에서 해당 의존성 제거
2. 관련 import 문 정리
3. 컴파일 오류 확인 및 수정

## 설정 관리

### Configuration 클래스
- `WebConfig.java`: HTTP 클라이언트, Jackson 설정
- `OpenStackProperties.java`: OpenStack 연동 설정

### 설정 파일
- `application.yml`: 애플리케이션 기본 설정
- 환경별 설정: `application-{profile}.yml`

## 빌드 플러그인

### Maven 플러그인
- **spring-boot-maven-plugin**: Spring Boot 애플리케이션 빌드
- **maven-compiler-plugin**: Java 컴파일 설정
- **maven-surefire-plugin**: 테스트 실행 설정

## 주의사항

1. **메모리 설정**: 현재 시스템에서 Spring Boot 실행 시 메모리 부족 발생 가능
2. **포트 설정**: 기본 포트 8081 사용 (8080 충돌 방지)
3. **의존성 업데이트**: Spring Boot 버전 업데이트 시 호환성 확인 필요

## 트러블슈팅

### 컴파일 오류
```bash
mvn clean compile
```

### 의존성 확인
```bash
mvn dependency:tree
```

### 의존성 업데이트 확인
```bash
mvn versions:display-dependency-updates
``` 