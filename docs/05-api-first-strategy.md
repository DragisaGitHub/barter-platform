# API First Strategy

## Goal

The backend will follow an API-first development approach using OpenAPI.

The OpenAPI specification is the source of truth for REST endpoints, request models, response models, validation rules, generated DTO classes, generated API interfaces and Swagger documentation.

## Main Principle

Controllers must not define request or response models manually.

API contracts are defined in OpenAPI YAML files first.

Generated DTOs are used only at the web/API boundary.

Domain entities are kept separate from generated DTOs.

Mapping between generated DTOs and domain/entity models is handled by MapStruct.

## Benefits

- Consistent API contract
- Reduced manual DTO errors
- Easier frontend integration
- Automatic Swagger documentation
- Generated controller interfaces
- Clear separation between API and domain model
- Safer refactoring

## Backend Module Responsibility

### barter-api

Responsible for OpenAPI YAML files, generated DTO classes and generated API interfaces.

### barter-domain

Responsible for domain entities, domain enums, business state models and core domain rules.

### barter-application

Responsible for application services, use cases, MapStruct mappers and transaction boundaries.

### barter-infrastructure

Responsible for database repositories, persistence configuration, external integrations and file storage integrations.

### barter-web

Responsible for REST controllers, implementation of generated API interfaces, Spring Security configuration and exception handling.

## OpenAPI Structure

Recommended structure:

    barter-api/
      src/main/resources/openapi/
        openapi.yaml
        paths/
          auth.yaml
          users.yaml
          items.yaml
          wishlists.yaml
          trades.yaml
          messages.yaml
        components/
          schemas/
          requests/
          responses/
          parameters/
          security/

## Mapping Rule

Generated DTOs must never be used as JPA entities.

JPA entities must never be exposed directly through REST endpoints.

Allowed flow:

    Controller -> Generated DTO -> Mapper -> Domain/Application Model -> Entity
    Entity -> Mapper -> Generated DTO -> Controller Response

## Controller Rule

Controllers implement generated API interfaces.

Example:

    UserController implements UsersApi
    ItemController implements ItemsApi

## Validation Rule

Validation should be declared as much as possible in OpenAPI schemas.

Backend services still perform business validation.

## Versioning

The initial API version is:

    /api/v1

Future versions should be introduced explicitly:

    /api/v2