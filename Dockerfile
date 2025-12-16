# 멀티스테이지 빌드를 사용한 Spring Boot Dockerfile

# Build Stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 먼저 의존성 파일만 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외)
RUN gradle build -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안을 위해 non-root 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 환경 변수 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 포트 노출
EXPOSE 8081

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
