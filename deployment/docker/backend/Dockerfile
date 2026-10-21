# syntax=docker/dockerfile:1.7

FROM eclipse-temurin@sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438 AS build
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

FROM eclipse-temurin@sha256:61d6c7b34d36aee3f45d043101259f97f3c6d428dc2a6f75513789983c5e254f AS runtime
WORKDIR /app

RUN set -eu; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        ca-certificates=20260816ubuntu0.22.04.1 \
        curl; \
    rm -rf /var/lib/apt/lists/*; \
    dpkg-query -W -f='${Version}' ca-certificates \
        | grep -Fx '20260816ubuntu0.22.04.1'; \
    keytool -delete -cacerts -storepass changeit \
        -alias 'cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]'; \
    keytool -delete -cacerts -storepass changeit \
        -alias 'cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]'; \
    keytool -delete -cacerts -storepass changeit \
        -alias 'cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]'; \
    if keytool -list -cacerts -storepass changeit 2>/dev/null \
        | grep -E 'cn_emsign_(root_ca__c1|ecc_root_ca__c3|ecc_root_ca__g3),'; then \
        echo 'An eMudhra root removed by the official CA bundle remains in the JVM cacerts store.' >&2; \
        exit 1; \
    fi; \
    keytool -list -cacerts -storepass changeit \
        -alias 'cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' >/dev/null; \
    test -f "$JAVA_HOME/lib/security/cacerts"

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

