# ---------- 1단계: 소스코드를 jar 파일로 빌드 ----------
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

# 의존성 설정 파일을 먼저 복사 → 소스만 바뀌면 의존성 다운로드를 다시 하지 않음(캐시)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon > /dev/null 2>&1 || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---------- 2단계: 빌드된 jar만 가볍게 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar app.jar

ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
