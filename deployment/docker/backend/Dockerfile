# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
COPY backend/barter-api ./barter-api
COPY backend/barter-application ./barter-application
COPY backend/barter-common ./barter-common
COPY backend/barter-domain ./barter-domain
COPY backend/barter-infrastructure ./barter-infrastructure
COPY backend/barter-web ./barter-web

RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon :barter-web:bootJar -x test

FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring

COPY --from=build /workspace/barter-web/build/libs/*.jar /app/barter-web.jar
RUN chown -R spring:spring /app

USER spring
EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxMetaspaceSize=160m" \
    SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://localhost:8080/api/v1/actuator/health/readiness >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/barter-web.jar"]

