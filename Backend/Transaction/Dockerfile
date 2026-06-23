# Build context: repo root
# docker build -f Transaction/Dockerfile -t transaction-service:latest .

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY Auth/build.gradle Auth/build.gradle
COPY Account/build.gradle Account/build.gradle
COPY Audit/build.gradle Audit/build.gradle
COPY Notification/build.gradle Notification/build.gradle
COPY Report/build.gradle Report/build.gradle
COPY Transaction/build.gradle Transaction/build.gradle
COPY Transaction/src Transaction/src
RUN chmod +x gradlew && ./gradlew :Transaction:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/Transaction/build/libs/*.jar app.jar
RUN chgrp -R 0 /app && chmod -R g=u /app
EXPOSE 8082
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
