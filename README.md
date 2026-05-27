# Barter Platform

Barter Platform is a barter marketplace for listing items, discovering matches, and managing trade offers without money changing hands.

## Current Status

**Phase: active full-stack development**

- The backend is a Gradle multi-module Spring Boot application running on **Spring Boot 4.0.6** and **Java 21**.
- A **React + TypeScript** frontend already exists in `frontend/` and is being expanded alongside the backend.
- Local development is available today, and a separate DEV deployment stack exists under `deployment/`.
- The project is usable for ongoing development and DEV testing, but it should **not** be described as production-ready yet.

## What Exists Today

- **Authentication and account APIs** — register, login, refresh, logout, current-user lookup
- **RBAC and user administration** — roles, permissions, user status management, admin/moderator protections
- **Catalog and listing APIs** — categories, tags, item search, item CRUD, favorites, image upload/serving
- **Trade workflows** — trade offers, offer messaging, offer status transitions, reviews
- **Community and moderation features** — public profiles, notifications, reports, admin listing/report/review management
- **OpenAPI-first backend** — generated API interfaces and Swagger/OpenAPI docs
- **Frontend foundations** — active React/Vite SPA work for landing, auth, dashboard, admin, and marketplace flows
- **Operational basics** — Flyway migrations, PostgreSQL, Testcontainers-based integration tests, Docker-based local/dev setup

## Repository Overview

```text
backend/      Spring Boot 4 multi-module backend
frontend/     React 18 + TypeScript + Vite frontend
deployment/   DEV deployment assets, Compose stack, Caddy, env examples, ops scripts
docs/         Product, architecture, and delivery notes
uploads/      Local-profile file storage
```

### Backend module structure

```text
backend/
├── barter-api            OpenAPI specs, generated DTOs and API interfaces
├── barter-domain         Domain entities, enums, repository interfaces
├── barter-application    Use cases and business services
├── barter-infrastructure Persistence implementations and adapters
├── barter-web            REST layer, security, Spring Boot app
└── barter-common         Shared utilities and cross-cutting concerns
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 4.0.6 |
| Backend build | Gradle wrapper, multi-module project |
| Frontend | React 18, TypeScript, Vite |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| API contract | OpenAPI 3 |
| Authentication | JWT access + refresh tokens |
| Testing | JUnit 5, Testcontainers |
| Local containers | Docker Compose |
| DEV edge/runtime | Docker Compose + Caddy HTTPS |
| Image storage | Local filesystem in `local`; Azure Blob Storage in DEV-style deployments |

## Local Development Setup

### Prerequisites

- Java 21
- Docker Desktop / Docker Engine with `docker compose`
- Node.js 18+ and Yarn
- Gradle wrapper (already included in `backend/`)

### 1. Start local services

Run this from the repository root:

```powershell
docker compose up -d
```

This starts:

- PostgreSQL 16 on `localhost:5432`
- Mailpit on `localhost:8025` for local email capture

Default database credentials:

- database: `barter_db`
- username: `barter_user`
- password: `barter_password`

### 2. Build the backend

```powershell
Set-Location backend
.\gradlew.bat clean build
```

### 3. Run the backend with the local profile

```powershell
Set-Location backend
.\gradlew.bat :barter-web:bootRun --args="--spring.profiles.active=local"
```

The backend starts on port `8080`.

### 4. Run the frontend

Open a second terminal from the repository root:

```powershell
Set-Location frontend
yarn install
Copy-Item .env.example .env -ErrorAction SilentlyContinue
yarn dev
```

The Vite dev server runs on port `5173` by default and targets `http://localhost:8080/api/v1`.

### Local storage note

With the `local` Spring profile, uploaded files are stored on the local filesystem under `uploads/`.
The Azure Blob Storage path is used in the DEV-style deployment configuration, not for default local startup.

## Local URLs

| Resource | URL |
|---|---|
| Frontend dev server | http://localhost:5173 |
| API Base | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/api/v1/v3/api-docs |
| Mailpit | http://localhost:8025 |

## DEV Deployment

A separate DEV deployment setup already exists under `deployment/`.

At a high level, that stack uses:

- Docker Compose for multi-container runtime orchestration
- **Caddy** for public HTTP/HTTPS and reverse proxying
- **PostgreSQL** for application data
- separate backend and frontend containers
- **Azure Blob Storage** for item-image binaries in DEV-style deployed environments

See `deployment/docs/DEV_DEPLOYMENT.md` for deployment details and operational notes.

## Quick Backend Smoke Test

Once the backend is running locally, you can verify the basics with PowerShell:

```powershell
Invoke-WebRequest http://localhost:8080/api/v1/ping | Select-Object -ExpandProperty Content
```

Or open Swagger UI directly:

- http://localhost:8080/api/v1/swagger-ui/index.html

## Documentation

Detailed design and implementation notes live in `docs/`:

| Document | Topic |
|---|---|
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
| [20 — Frontend Architecture](docs/20-frontend-architecture.md) | Frontend architecture notes |
| [28 — Production Readiness Roadmap](docs/28-production-readiness-roadmap.md) | Hardening and launch planning |

## Known Limitations

- The product is still in active development; some frontend flows and broader UX polish are not finished yet.
- The current documented hosted environment is a DEV/public-beta style Compose deployment, not a fully hardened production platform.
- Local and deployed environments intentionally differ in a few areas, such as file storage (`uploads/` locally vs Azure Blob Storage in DEV-style deployments).
- OAuth/social login, real-time messaging, and broader launch hardening remain future work.

## Roadmap

- Continue expanding the React frontend across catalog, offers, messaging, and admin workflows
- Keep refining backend marketplace and moderation capabilities
- Harden deployment, operations, and release practices beyond the current DEV setup
- Improve onboarding, email/account flows, and password-recovery experience
- Add OAuth/social login options
- Add richer messaging and notification experiences

