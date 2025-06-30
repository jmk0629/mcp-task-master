# 멀티 스테이지 빌드를 위한 백엔드 Dockerfile
FROM maven:3.8.6-openjdk-11-slim as build

# 작업 디렉토리 설정
WORKDIR /app

# pom.xml 복사
COPY pom.xml .

# 의존성 다운로드 (캐시 최적화)
RUN mvn dependency:go-offline -B

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN mvn clean package -DskipTests

# 실행 스테이지
FROM openjdk:11-jre-slim

# 작업 디렉토리 설정
WORKDIR /app

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 빌드된 JAR 파일 복사
COPY --from=build /app/target/*.jar app.jar

# 포트 노출
EXPOSE 8081

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"] 