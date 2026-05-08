# Backend Architecture

## Architectural Style

The backend follows a modular monolith architecture.

The application is deployed as a single backend service, but internally separated into clear modules and responsibilities.

The architecture is designed to support future extraction into microservices if needed.

## Main Goals

- clean separation of concerns
- maintainable codebase
- scalable domain structure
- API-first development
- strong domain isolation
- future scalability

---

# Backend Modules

## barter-api

Contains:
- OpenAPI YAML definitions
- generated DTO classes
- generated API interfaces

This module represents the external API contract.

---

## barter-domain

Contains:
- JPA entities
- domain enums
- aggregate roots
- business state models
- domain rules

This module must not depend on infrastructure details.

---

## barter-application

Contains:
- application services
- use cases
- transaction orchestration
- MapStruct mappers
- validation logic
- business workflows

Coordinates domain operations.

---

## barter-infrastructure

Contains:
- Spring Data repositories
- persistence configuration
- external integrations
- storage implementations
- security integrations
- messaging integrations

Responsible for technical implementation details.

---

## barter-web

Contains:
- REST controllers
- exception handling
- security configuration
- request filters
- API implementation layer

Controllers implement generated API interfaces.

---

# Dependency Direction

Allowed dependencies:

    barter-web -> barter-application
    barter-application -> barter-domain
    barter-infrastructure -> barter-domain
    barter-web -> barter-api

Forbidden dependencies:

- domain must not depend on web
- domain must not depend on infrastructure
- API DTOs must not be used as entities
- infrastructure logic must not leak into controllers

---

# Package Structure

Recommended package structure:

    com.barterplatform

        api
        domain
        application
        infrastructure
        web
        common

---

# Common Module Responsibility

Shared reusable components:

- base entities
- UUID utilities
- audit support
- exception hierarchy
- constants
- common enums

---

# Entity Principles

- Entities represent business state
- Entities are persistence models
- Entities are not exposed directly through REST
- Use UUID as public identifiers
- Use BIGINT as internal database identifiers

---

# Transaction Strategy

- Transactions belong in application services
- Controllers must remain thin
- Repositories must not contain business orchestration

---

# Mapping Strategy

MapStruct is used for:

- DTO -> entity mapping
- entity -> DTO mapping
- partial updates
- nested object mapping

Generated DTOs are mapped explicitly.

---

# Security Strategy

Spring Security is responsible for:

- JWT authentication
- role authorization
- MFA integration
- OAuth2 integration
- endpoint protection

---

# Persistence Strategy

Database:
- PostgreSQL

Migration tool:
- Flyway

Caching:
- Redis (future phase)

Search:
- PostgreSQL full-text search initially
- Elasticsearch/OpenSearch later if needed

---

# File Storage Strategy

Initial:
- local development storage

Future:
- S3 compatible storage
- Azure Blob Storage

---

# Deployment Strategy

Initial deployment:
- Docker Compose

Future deployment:
- Kubernetes
- Helm
- horizontal scaling

---

# Scalability Principles

- stateless backend
- externalized file storage
- cache-ready architecture
- modular separation
- async-ready design