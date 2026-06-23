# API First Strategy

## Principles

OpenAPI is the source of truth for REST endpoints, request/response models, and generated DTO classes and API interfaces.

Controllers must not define request or response models manually. Generated DTOs are used only at the API boundary. Domain entities are kept separate from generated DTOs. Mapping between DTOs and domain models is handled by MapStruct.

## Module Responsibilities

**barter-api** — OpenAPI YAML files, generated DTOs, generated API interfaces.

**barter-domain** — domain entities, enums, business state models, domain rules.

**barter-application** — application services, use cases, MapStruct mappers, transaction boundaries.

**barter-infrastructure** — repositories, persistence configuration, external integrations, file storage.

**barter-web** — REST controllers implementing generated API interfaces, Spring Security, exception handling.

## Mapping Rule

    Controller -> Generated DTO -> Mapper -> Domain Model -> Entity
    Entity -> Mapper -> Generated DTO -> Controller Response

Controllers implement generated interfaces:

    UserController implements UsersApi
    ItemController implements ItemsApi

## Versioning

Current API version: `/api/v1`

New versions are introduced explicitly as `/api/v2`, etc.
