# Backend Architecture

## Architectural Style

The backend follows a modular monolith architecture: deployed as a single service, internally separated into clear modules with well-defined responsibilities.

---

# Backend Modules

## barter-api

Contains OpenAPI YAML definitions, generated DTO classes, and generated API interfaces. Represents the external API contract.

## barter-domain

Contains JPA entities, domain enums, aggregate roots, business state models, and domain rules. Must not depend on infrastructure details.

## barter-application

Contains application services, use cases, transaction orchestration, MapStruct mappers, validation logic, and business workflows. Coordinates domain operations.

## barter-infrastructure

Contains Spring Data repositories, persistence configuration, external integrations, storage implementations, and security integrations.

## barter-web

Contains REST controllers, exception handling, security configuration, and request filters. Controllers implement generated API interfaces.

---

# Dependency Direction

Allowed:

    barter-web -> barter-application
    barter-application -> barter-domain
    barter-infrastructure -> barter-domain
    barter-web -> barter-api

Forbidden:
- domain must not depend on web or infrastructure
- API DTOs must not be used as entities
- infrastructure logic must not leak into controllers

---

# Transaction Strategy

- Transactions belong in application services
- Controllers must remain thin
- Repositories must not contain business orchestration

---

# Mapping Strategy

MapStruct handles DTO ↔ entity mapping, partial updates, and nested object mapping. Generated DTOs are mapped explicitly and never used as JPA entities.

---

# Persistence Strategy

- Database: PostgreSQL
- Migrations: Flyway
- File storage: local filesystem (dev), with a storage abstraction layer for future backends
