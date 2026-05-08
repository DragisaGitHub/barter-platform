# OpenAPI Generation Strategy

## Goal

Define API-first generation for DTO classes and controller interfaces.

OpenAPI is the source of truth for:
- REST endpoints
- request DTOs
- response DTOs
- validation rules
- generated API interfaces
- Swagger documentation

---

# Main Rules

- Generated classes must not be edited manually.
- Generated DTOs are used only at the API boundary.
- JPA entities are never exposed directly.
- Internal database IDs are never exposed.
- Sensitive fields are never exposed.
- Controllers implement generated API interfaces.
- MapStruct maps between generated DTOs and domain entities.

---

# Module

OpenAPI lives in:

    backend/barter-api

Generated output belongs to:

    backend/barter-api/build/generated

---

# Package Strategy

Generated model package:

    com.barterplatform.api.model

Generated API interface package:

    com.barterplatform.api.controller

---

# Initial API Scope

Identity & Access API scope includes:

## Auth API

- POST /api/v1/auth/register
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/auth/logout
- GET /api/v1/auth/me

## Users API

- GET /api/v1/users
- GET /api/v1/users/{userUuid}
- PATCH /api/v1/users/{userUuid}/status

## Roles API

- GET /api/v1/roles
- GET /api/v1/roles/{code}

## Permissions API

- GET /api/v1/permissions

## MFA API

- GET /api/v1/mfa/settings
- POST /api/v1/mfa/setup
- POST /api/v1/mfa/verify
- POST /api/v1/mfa/disable
- GET /api/v1/mfa/recovery-codes

## OAuth API

- GET /api/v1/oauth/providers
- GET /api/v1/oauth/accounts

---

# DTO Exposure Rules

DTOs may expose:

- uuid
- username
- email
- status
- emailVerified
- mfaEnabled
- roles
- permissions
- provider
- providerEmail
- linkedAt
- createdAt
- updatedAt where useful

DTOs must never expose:

- internal numeric id
- passwordHash
- tokenHash
- secretEncrypted
- recoveryCodeHash
- deletedAt
- raw refresh token hashes
- MFA secret

---

# Initial DTOs

## Auth DTOs

- RegisterUserRequest
- LoginRequest
- RefreshTokenRequest
- TokenResponse
- CurrentUserResponse

## User DTOs

- UserResponse
- UserSummaryResponse
- UpdateUserStatusRequest

## Role DTOs

- RoleResponse

## Permission DTOs

- PermissionResponse

## OAuth DTOs

- OAuthProviderResponse
- OAuthAccountResponse

## MFA DTOs

- MfaSettingsResponse
- MfaSetupResponse
- MfaVerifyRequest
- MfaRecoveryCodeResponse

---

# Enum Exposure

OpenAPI must define explicit enum values for:

- UserStatus
- RoleCode
- PermissionCode
- OAuthProvider

---

# Controller Rule

Controllers in barter-web implement generated API interfaces.

Examples:

    AuthController implements AuthApi
    UsersController implements UsersApi
    RolesController implements RolesApi
    PermissionsController implements PermissionsApi

---

# Mapping Rule

Allowed flow:

    Controller -> generated request DTO -> service -> entity
    entity -> mapper -> generated response DTO -> controller

---

# Implementation Order

1. Configure OpenAPI generator
2. Generate DTOs and API interfaces
3. Add controllers with placeholder responses only if needed
4. Add MapStruct mappers
5. Add services/use cases
6. Implement real auth logic