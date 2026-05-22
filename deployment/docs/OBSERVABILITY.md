# Backend Observability and Monitoring

This project intentionally keeps launch-stage observability lightweight.
There is no Prometheus, Grafana, distributed tracing, or Kubernetes-specific stack yet.

## What is available now

### Public-safe actuator endpoints

The backend exposes only Spring Actuator health endpoints over HTTP.
These infrastructure endpoints stay outside the versioned public REST API:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Safety rules:

- only the `health` actuator endpoint family is exposed;
- health details/components stay hidden from public callers;
- non-health actuator endpoints remain protected;
- the existing `GET /api/v1/ping` endpoint remains available as a simple legacy smoke check.

### Readiness meaning

`/actuator/health/readiness` is the most useful operational endpoint for launch.
It rolls up these contributors:

- Spring readiness state
- database health (`db`)
- disk space
- simple ping health

If readiness is `DOWN` or returns HTTP `503`, treat the instance as unavailable for user traffic.

## Correlation IDs

Every request gets a correlation ID.

### Accepted inbound headers

The backend will reuse one of these headers when the value is safe:

- `X-Correlation-Id`
- `X-Request-Id`

If neither header is present, the backend generates a UUID.

### Response header

Every backend response includes:

- `X-Correlation-Id: <value>`

Use this value when investigating failures from browser/API logs against backend logs.

## Request logging

The backend now logs a single completion line per request with:

- HTTP method
- request path
- HTTP status
- duration in milliseconds
- correlation ID in the log pattern MDC

Health endpoints stay quiet at normal log levels to avoid noisy uptime-check spam.

Example log shape:

```text
2026-05-22T12:00:00.123+00:00 level=INFO  app=barter-platform correlationId=5f0a3c0d-6a8f-4f16-b35d-2ed1458d6781 thread=http-nio-8080-exec-4 logger=c.b.web.observability.RequestLoggingFilter - HTTP request completed: method=POST, path=/api/v1/auth/login, status=401, durationMs=23
```

## What to check first when the app is down

Run these checks in order.

### 1. Is the backend process/container alive?

- local JVM: confirm the Spring Boot app is still running;
- DEV server: `docker compose ... ps` and confirm `backend` is `healthy`.

### 2. Does liveness respond?

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/health/readiness
```

From the public DEV hostname:

```bash
curl -i https://barter-platform-dev.duckdns.org/actuator/health
curl -i https://barter-platform-dev.duckdns.org/actuator/health/readiness
```

### 3. If readiness is down, is the database reachable?

Check:

- PostgreSQL container/process health
- datasource credentials/env vars
- recent backend startup logs for Flyway/JPA/DataSource failures

### 4. Are requests failing with a correlation ID?

If a user reports an API failure:

1. capture the `X-Correlation-Id` response header;
2. search backend logs for that correlation ID;
3. inspect the matching request completion line and surrounding stack trace.

### 5. Is the host unhealthy?

Check:

- disk pressure on the VM/host
- container restarts / OOM kills
- `docker stats` for memory pressure
- reverse proxy availability (`caddy`)

## Safe DEV/public-beta monitoring

For the current deployment model, use lightweight checks:

- public uptime check: `GET /actuator/health`
- internal/container healthcheck: `GET /actuator/health/readiness`
- log monitoring: watch backend logs for `status >= 500`, startup failures, and repeated `429` or auth abuse patterns

Recommended initial checks:

- frontend root reachable
- backend health reachable
- backend readiness reachable
- postgres container healthy

## Local verification checklist

- start PostgreSQL locally;
- run `barter-web` with the `local` profile;
- call health and readiness endpoints;
- call any API endpoint and confirm `X-Correlation-Id` is returned;
- confirm request logs contain the same correlation ID.

See also:

- `docs/07-local-development.md`
- `deployment/docs/DEV_DEPLOYMENT.md`

