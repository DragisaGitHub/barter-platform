# Backend Current Status

## Overview

The backend is a modular monolith built with Spring Boot 3.5.0 and Java 21.

It currently covers the Identity & Access domain, including user registration, JWT authentication, role-based authorization, and user management.

---

## Completed Modules

### barter-api

- OpenAPI 3.0.3 specification with code generation (openapi-generator `spring`)
- Generated DTOs, API interfaces, and validation annotations
- Defined endpoints: Auth, Users, Roles, Permissions, MFA (spec only), OAuth (spec only), System

### barter-domain

- JPA entities: `UserEntity`, `RoleEntity`, `PermissionEntity`, `RolePermissionEntity`, `UserRoleEntity`, `RefreshTokenEntity`, `OAuthAccountEntity`, `UserMfaSettingsEntity`, `UserMfaRecoveryCodeEntity`
- Composite key classes: `RolePermissionId`, `UserRoleId`
- Domain enums: `UserStatus`, `RoleCode`, `PermissionCode`, `OAuthProvider`

### barter-application

- Auth service: registration, login (username or email), token refresh, logout, current user lookup
- JWT service: access token generation and validation (JJWT 0.12.6)
- Refresh token service: persisted hashed refresh tokens with revocation
- User management service: get by UUID, update user status
- User query service: paginated user listing with sorting
- Role service and Permission service: lookup operations
- MapStruct mappers: `UserMapper`, `RoleMapper`, `PermissionMapper`
- Central mapper config: `CentralMapperConfig`
- Pagination utilities in `common/pagination`

### barter-infrastructure

- Spring Data JPA repositories: `UserRepository`, `RoleRepository`, `PermissionRepository`, `RolePermissionRepository`, `UserRoleRepository`, `RefreshTokenRepository`, `OAuthAccountRepository`, `UserMfaSettingsRepository`, `UserMfaRecoveryCodeRepository`

### barter-web

- REST controllers: `AuthController`, `UsersController`, `RolesController`, `PermissionsController`
- JWT security: `JwtAuthenticationFilter`, `JwtAuthenticationService`, `AuthenticatedUser`
- Security configuration: `SecurityConfig`
- Global exception handler: `GlobalExceptionHandler`

### barter-common

- Shared base classes, UUID utilities, audit support, exception hierarchy, constants

### Database

- PostgreSQL with Flyway migrations
- `V001__initial_identity_access_schema.sql` — identity & access tables
- `V002__identity_access_seed.sql` — seed data (roles, permissions)

---

## Local Startup

### Prerequisites

- Java 21
- Docker Desktop (for PostgreSQL)

### Steps

Start the database:

    docker compose up -d

Build the project:

    cd backend
    .\gradlew.bat clean build

Run the application:

    cd backend
    .\gradlew.bat :barter-web:bootRun --args='--spring.profiles.active=local'

The application starts on port `8080` with the context path `/api/v1`.


---

## Swagger / API Docs

| Resource       | URL                                              |
|----------------|--------------------------------------------------|
| Swagger UI     | http://localhost:8080/api/v1/swagger-ui/index.html |
| OpenAPI JSON   | http://localhost:8080/api/v1/v3/api-docs           |

---

## Postman Smoke Test Sequence

Run these requests in order against `http://localhost:8080/api/v1`.

### 1. Register

    POST /auth/register
    Content-Type: application/json

    {
      "username": "smoketest",
      "email": "smoketest@example.com",
      "password": "Test1234!"
    }

Expected: `201 Created` with `CurrentUserResponse` body.

### 2. Login

    POST /auth/login
    Content-Type: application/json

    {
      "identifier": "smoketest@example.com",
      "password": "Test1234!"
    }

Expected: `200 OK` with `TokenResponse` body containing `accessToken` and `refreshToken`.

Save `accessToken` and `refreshToken` from the response for subsequent requests.

### 3. Get Current User (auth/me)

    GET /auth/me
    Authorization: Bearer {{accessToken}}

Expected: `200 OK` with `CurrentUserResponse` body showing the authenticated user.

### 4. Refresh Token

    POST /auth/refresh
    Content-Type: application/json

    {
      "refreshToken": "{{refreshToken}}"
    }

Expected: `200 OK` with a new `TokenResponse` containing a fresh `accessToken` and `refreshToken`.

Update saved tokens with the new values.

### 5. Logout

    POST /auth/logout
    Content-Type: application/json

    {
      "refreshToken": "{{refreshToken}}"
    }

Expected: `204 No Content`. The refresh token is revoked.

### 6. List Users (requires ADMIN or MODERATOR role)

    GET /users?page=0&size=10&sort=username,asc
    Authorization: Bearer {{accessToken}}

> **Note:** This endpoint requires an authenticated user with the ADMIN or MODERATOR role.
> A newly registered user will not have these roles by default.
> To test this endpoint, either:
> - Use a seeded admin account (if one exists in `V002__identity_access_seed.sql`), or
> - Manually assign the ADMIN role to the test user in the database.

Expected: `200 OK` with `UserPagedResponse` containing paginated user data.

---

## API Endpoint Summary

| Method  | Path                        | Auth Required | Description                        |
|---------|-----------------------------|---------------|------------------------------------|
| POST    | `/auth/register`            | No            | Register a new user                |
| POST    | `/auth/login`               | No            | Authenticate and receive tokens    |
| GET     | `/auth/me`                  | Yes (Bearer)  | Get current authenticated user     |
| POST    | `/auth/refresh`             | No            | Refresh access token               |
| POST    | `/auth/logout`              | No            | Revoke refresh token               |
| GET     | `/users`                    | Yes (Bearer)  | List users (paginated)             |
| GET     | `/users/{userUuid}`         | Yes (Bearer)  | Get user by UUID                   |
| PATCH   | `/users/{userUuid}/status`  | Yes (Bearer)  | Update user status                 |
| GET     | `/roles`                    | Yes (Bearer)  | List all roles                     |
| GET     | `/roles/{code}`             | Yes (Bearer)  | Get role by code                   |
| GET     | `/permissions`              | Yes (Bearer)  | List all permissions               |
| GET     | `/ping`                     | No            | System health check                |

All paths are relative to the base URL `http://localhost:8080/api/v1`.

---

## Known Limitations

- **Email verification** — not implemented; users are created with `PENDING_VERIFICATION` status and `emailVerified=false`, but login is allowed because the verification flow is not implemented yet
- **MFA endpoints** — OpenAPI spec defined but controllers and services not implemented
- **OAuth endpoints** — OpenAPI spec defined but controllers and services not implemented
- **Catalog / Items** — not implemented yet; the bartering domain (items, trades, offers) has no code
- **Frontend** — not implemented yet; no UI exists
- **Redis caching** — not integrated; planned for a future phase
- **File storage** — not integrated; planned for a future phase
- **Search** — no full-text search implemented yet

