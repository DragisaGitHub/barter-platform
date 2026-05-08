# Frontend Technical Specification

## Goal

Define the frontend engineering architecture for the Barter Platform.

The frontend must:
- integrate cleanly with the backend OpenAPI contract
- support RBAC
- support scalable feature growth
- remain modular and maintainable

---

# Stack

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- OpenAPI generated TypeScript client

---

# OpenAPI Client Generation

The frontend must generate API client code from the backend OpenAPI specification.

The generated client is the source of truth for:
- DTO models
- request models
- response models
- API endpoints

Frontend developers must not manually recreate DTO types.

---

# API Layer

## Structure

src/api/
generated/
client/
interceptors/

## Responsibilities

Generated layer:
- generated DTOs
- generated API clients

Custom API layer:
- auth headers
- token refresh handling
- request interceptors
- response interceptors
- centralized error handling

---

# Authentication Architecture

## Token Strategy

Store:
- access token
- refresh token

Preferred storage:
- localStorage initially
- future migration possible

---

## Session Bootstrap

On app startup:
1. load tokens
2. call /auth/me
3. populate authenticated user state
4. render role-aware navigation

---

## Token Refresh

When access token expires:
1. call /auth/refresh
2. update tokens
3. retry failed request

If refresh fails:
1. clear auth state
2. redirect to login

---

# Auth State

Centralized auth state should contain:
- authenticated user
- roles
- permissions
- access token
- loading/auth bootstrap state

---

# Routing

## Public Routes

- /
- /login
- /register

## Protected Routes

- /dashboard
- /profile
- /offers
- /messages

## Admin Routes

- /admin
- /admin/users
- /admin/roles
- /admin/system

---

# Route Protection

The frontend must support:
- authentication guards
- role guards
- redirect handling
- unauthorized fallback pages

---

# Layout System

## Main Layout

Contains:
- top bar
- sidebar
- content area
- notification area

---

## Admin Layout

Admin pages should reuse the main layout.

Additional admin navigation should appear dynamically based on role.

Do not create a completely separate application shell for admins.

---

# Feature Organization

Preferred organization:

src/features/
auth/
users/
roles/
permissions/
admin/
marketplace/
offers/
messages/

Each feature should contain:
- pages
- hooks
- components
- services
- validation
- route definitions

---

# Forms

Preferred:
- React Hook Form
- Zod validation

Validation should:
- support backend validation errors
- map fieldErrors cleanly

---

# Data Fetching

Use TanStack Query.

Requirements:
- request caching
- invalidation
- optimistic updates where useful
- loading states
- retry strategy

---

# Tables & Pagination

Admin tables should support:
- backend pagination
- sorting
- filtering
- search
- loading states
- empty states

---

# Error Handling

Use backend ErrorResponse contract.

Global handling:
- toast/snackbar notifications
- auth failure handling
- API failure handling

Field validation:
- inline form errors

---

# UI Requirements

Frontend must feel:
- modern
- responsive
- premium
- enterprise-grade

Must support:
- desktop
- tablet
- mobile

---

# Future Architecture Support

The frontend architecture should support future additions:
- websocket notifications
- realtime chat
- MFA
- OAuth login
- file uploads
- dark mode
- i18n
- microfrontend separation if needed