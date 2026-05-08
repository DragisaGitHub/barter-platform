# Barter Platform

A modern barter marketplace platform for item exchange and community-driven trading. Users can list items, propose trades, and manage exchanges — all without money changing hands.

## Current Status

**Phase: Backend Complete · Frontend In Progress**

The backend is fully functional with authentication, user management, RBAC, and an OpenAPI-first API. The frontend (React + TypeScript) is planned and will be generated next.

## Features Implemented

- **JWT Authentication** — register, login, token refresh, logout
- **Role-Based Access Control** — USER, MODERATOR, ADMIN roles with granular permissions
- **User Management** — paginated user listing, status management (ACTIVE, SUSPENDED, BANNED, etc.)
- **OpenAPI-First API** — contract-first design with generated DTOs and API interfaces
- **Global Error Handling** — structured `ErrorResponse` with field-level validation errors
- **Pagination & Sorting** — backend-driven pagination matching Spring Data conventions
- **Database Migrations** — versioned schema management via Flyway
- **Integration Tests** — Testcontainers-based tests running against real PostgreSQL
- **API Documentation** — Swagger UI with full endpoint documentation

## Architecture Overview

The backend follows a **clean architecture** pattern with a Gradle multi-module structure:

```
backend/
├── barter-api            # OpenAPI specs, generated DTOs and API interfaces
├── barter-domain         # Domain entities, enums, repository interfaces
├── barter-application    # Use cases, services, business logic
├── barter-infrastructure # JPA repositories, persistence implementations
├── barter-web            # REST controllers, security config, Spring Boot app
└── barter-common         # Shared utilities and cross-cutting concerns
```

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 3.5                     |
| Build          | Gradle (multi-module)               |
| Database       | PostgreSQL 16                       |
| Migrations     | Flyway                              |
| API Design     | OpenAPI 3.0 (contract-first)        |
| Auth           | JWT (access + refresh tokens)       |
| Testing        | JUnit 5, Testcontainers             |
| Containers     | Docker Compose                      |
| Frontend       | React 18+, TypeScript, Tailwind CSS |

## Local Development Setup

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle (wrapper included)

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL 16 on port `5432` with default credentials (`barter_user` / `barter_password` / `barter_db`).

### 2. Build the Backend

```bash
cd backend
.\gradlew.bat clean build
```

### 3. Run the Backend

```bash
cd backend
.\gradlew.bat :barter-web:bootRun --args='--spring.profiles.active=local'
```

The application starts on port `8080`.

## Application URLs

| Resource       | URL                                                        |
|----------------|------------------------------------------------------------|
| API Base       | http://localhost:8080/api/v1                                |
| Swagger UI     | http://localhost:8080/api/v1/swagger-ui/index.html          |
| OpenAPI JSON   | http://localhost:8080/api/v1/api-docs                       |

## Authentication Smoke Test

Verify the backend is running with a quick auth flow:

```bash
# 1. Health check
curl http://localhost:8080/api/v1/ping

# 2. Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Test1234!"}'

# 3. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"testuser","password":"Test1234!"}'

# 4. Use the returned accessToken for authenticated requests
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

## Documentation

Detailed design documents are available in the [`docs/`](docs/) directory:

| Document | Topic |
|----------|-------|
| [01 — Product Vision](docs/01-product-vision.md) | Product goals and scope |
| [02 — Domain Model](docs/02-domain-model.md) | Core domain concepts |
| [03 — Data Model](docs/03-data-model.md) | Database schema design |
| [04 — Identity & Access](docs/04-identity-access-model.md) | Auth and RBAC model |
| [05 — API-First Strategy](docs/05-api-first-strategy.md) | OpenAPI contract approach |
| [06 — Backend Architecture](docs/06-backend-architecture.md) | Module structure and patterns |
| [07 — Local Development](docs/07-local-development.md) | Dev environment setup |
| [16 — OpenAPI Generation](docs/16-openapi-generation-strategy.md) | Code generation strategy |
| [17 — Pagination & Search](docs/17-pagination-search-strategy.md) | Pagination conventions |
| [18 — Auth & JWT](docs/18-auth-jwt-strategy.md) | JWT token architecture |
| [20 — Frontend Architecture](docs/20-frontend-architecture.md) | Frontend design plan |

## Known Limitations

- **No frontend yet** — the React frontend is designed but not yet implemented
- **No email verification** — registration creates users in `PENDING_VERIFICATION` status but no email is sent
- **No OAuth/social login** — only username/password authentication is available
- **No item/listing endpoints** — marketplace functionality is not yet built
- **Admin stats are placeholder** — no aggregation endpoints exist yet

## Roadmap

- [ ] Generate React + TypeScript frontend with Figma Make
- [ ] Implement item listing and marketplace endpoints
- [ ] Add trade/offer proposal flow
- [ ] Email verification and password reset
- [ ] OAuth2 social login integration
- [ ] Real-time messaging between users
- [ ] Admin analytics and reporting
- [ ] Production deployment configuration

