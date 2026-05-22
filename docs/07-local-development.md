# Local Development

## Requirements

- Java 21
- Docker Desktop
- IntelliJ IDEA
- PostgreSQL via Docker Compose
- Gradle Wrapper

## Backend

Backend location:

    backend/

Use Gradle Wrapper from the backend folder:

    cd backend
    .\gradlew.bat clean build
    .\gradlew.bat :barter-web:bootRun

Flyway PostgreSQL support for Flyway 11 is provided by the runtime dependency `org.flywaydb:flyway-database-postgresql` in `barter-web`.

## Database

Local database will use PostgreSQL through Docker Compose.

Start the database:

    docker compose up -d

Check status:

    docker compose ps

Stop the database:

    docker compose down

Stop and remove the data volume:

    docker compose down -v

Default local database:

    database: barter_db
    username: barter_user
    password: barter_password
    port: 5432

## Profiles

Local Spring profile:

    local

Application config:

    barter-web/src/main/resources/application.yml
    barter-web/src/main/resources/application-local.yml

## Rule

Do not commit local environment secrets.

Use .env files for local-only configuration when needed.

## Initial Admin Bootstrap

An initial admin user can be created automatically on application startup.
This is disabled by default and intended for local/dev/staging environments only.

### Environment Variables

| Variable                          | Description               | Required when enabled |
|-----------------------------------|---------------------------|-----------------------|
| `BARTER_BOOTSTRAP_ADMIN_ENABLED`  | Enable admin bootstrap    | Yes (`true`)          |
| `BARTER_BOOTSTRAP_ADMIN_USERNAME` | Admin username            | Yes                   |
| `BARTER_BOOTSTRAP_ADMIN_EMAIL`    | Admin email               | Yes                   |
| `BARTER_BOOTSTRAP_ADMIN_PASSWORD` | Admin password            | Yes                   |

### Example .env (do not commit)

```
BARTER_BOOTSTRAP_ADMIN_ENABLED=true
BARTER_BOOTSTRAP_ADMIN_USERNAME=admin
BARTER_BOOTSTRAP_ADMIN_EMAIL=admin@localhost
BARTER_BOOTSTRAP_ADMIN_PASSWORD=change-me-in-production
```

### Behavior

- Disabled by default (`false`).
- When enabled, the admin user is created only if no user with the same email or username exists.
- The password is never logged.
- The ADMIN role must already exist in the database (loaded via seed/migration data).

## Application Rate Limits

The backend applies simple in-memory, fixed-window rate limits for high-risk public and user write actions during the DEV/public-beta stage.
This is intentionally single-node and does not require Redis, Kafka, or other distributed infrastructure.

Configured endpoint groups:

- Auth: login, register, refresh token, forgot password, reset password, resend verification code.
- User-generated actions: image upload, trade offer creation, trade message sending.
- Optional launch-safe action: favorite add/remove.

Defaults are defined in `backend/barter-web/src/main/resources/application.yml` under `barter.rate-limits`.
When a limit is exceeded the API returns HTTP `429` with the standard error body and a `Retry-After` header.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BARTER_RATE_LIMITS_ENABLED` | `true` | Enables/disables application-level rate limiting. |
| `BARTER_RATE_LIMITS_CLIENT_IP_HEADER` | empty | Optional trusted proxy header for client IP resolution. Leave empty locally unless a trusted reverse proxy overwrites this header. |
| `BARTER_RATE_LIMITS_LOGIN_LIMIT` / `BARTER_RATE_LIMITS_LOGIN_WINDOW` | `20` / `1m` | Login attempts per client IP. |
| `BARTER_RATE_LIMITS_REGISTER_LIMIT` / `BARTER_RATE_LIMITS_REGISTER_WINDOW` | `10` / `1m` | Registration attempts per client IP. |
| `BARTER_RATE_LIMITS_REFRESH_TOKEN_LIMIT` / `BARTER_RATE_LIMITS_REFRESH_TOKEN_WINDOW` | `60` / `1m` | Refresh-token requests per client IP. |
| `BARTER_RATE_LIMITS_FORGOT_PASSWORD_LIMIT` / `BARTER_RATE_LIMITS_FORGOT_PASSWORD_WINDOW` | `5` / `15m` | Forgot-password requests per client IP. |
| `BARTER_RATE_LIMITS_RESET_PASSWORD_LIMIT` / `BARTER_RATE_LIMITS_RESET_PASSWORD_WINDOW` | `10` / `15m` | Reset-password requests per client IP. |
| `BARTER_RATE_LIMITS_RESEND_VERIFICATION_CODE_LIMIT` / `BARTER_RATE_LIMITS_RESEND_VERIFICATION_CODE_WINDOW` | `5` / `15m` | Verification-code resend requests per client IP. |
| `BARTER_RATE_LIMITS_IMAGE_UPLOAD_LIMIT` / `BARTER_RATE_LIMITS_IMAGE_UPLOAD_WINDOW` | `30` / `10m` | Image uploads per authenticated user and IP. |
| `BARTER_RATE_LIMITS_TRADE_OFFER_CREATE_LIMIT` / `BARTER_RATE_LIMITS_TRADE_OFFER_CREATE_WINDOW` | `20` / `10m` | Trade offers created per authenticated user and IP. |
| `BARTER_RATE_LIMITS_TRADE_MESSAGE_SEND_LIMIT` / `BARTER_RATE_LIMITS_TRADE_MESSAGE_SEND_WINDOW` | `60` / `10m` | Trade messages sent per authenticated user and IP. |
| `BARTER_RATE_LIMITS_FAVORITE_MUTATION_LIMIT` / `BARTER_RATE_LIMITS_FAVORITE_MUTATION_WINDOW` | `120` / `10m` | Favorite add/remove requests per authenticated user and IP. |

## Observability verification

The backend now exposes launch-safe actuator health endpoints outside the versioned `/api/v1` REST API.

### Endpoints

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- legacy smoke endpoint: `GET /api/v1/ping`

### Local smoke test

Start PostgreSQL and the backend, then run:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health | Select-Object -ExpandProperty Content
Invoke-WebRequest http://localhost:8080/actuator/health/readiness | Select-Object -ExpandProperty Content
```

Expected result: both endpoints return HTTP `200` with a simple `{"status":"UP"}` response when the app and database are healthy.

### Correlation ID verification

Every response includes `X-Correlation-Id`.

```powershell
$response = Invoke-WebRequest http://localhost:8080/api/v1/ping
$response.Headers["X-Correlation-Id"]
```

Use the returned value to search the backend logs. Request completion log lines now include the same correlation ID, HTTP method, path, status, and duration.

### If readiness is down locally

Check these first:

- PostgreSQL container is running and healthy
- datasource settings in `backend/barter-web/src/main/resources/application-local.yml`
- application startup logs for Flyway/JPA/DataSource failures

Operational runbook details live in `deployment/docs/OBSERVABILITY.md`.

