# 빌드 스테이지
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 캐시를 위해 Gradle 관련 파일만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 런타임 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]