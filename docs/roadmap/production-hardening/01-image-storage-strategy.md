# Production Hardening 01 — Image Storage Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 operational cost/security/performance  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Status

- Implemented for the current DEV-ready / pre-PROD phase.
- Scope intentionally stayed within storage hardening and documentation. No CDN, image-resize pipeline, or new infrastructure service was added.

# Goal

- Make local/dev/prod image storage behavior explicit and safe without rebuilding the existing upload feature.
- Keep DEV working on Azure Blob Storage.
- Keep local development simple on local filesystem storage.
- Prepare a clean production path with a separate Azure Blob container and explicit env variables.

# Why It Matters

- Images are one of the most likely cost, performance, security, and backup pressure points for a barter marketplace.
- The roadmap notes existing storage abstraction is good, but production object storage policy, resizing, CDN, lifecycle, and scanning are incomplete.

# Implemented Strategy

## Storage behavior by profile

- `local` profile → local filesystem storage via `LocalFileStorageService`
- `dev` profile → Azure Blob Storage via `AzureBlobStorageService`
- `prod` profile → Azure Blob Storage via `AzureBlobStorageService`

The service selection is still profile-based, so `dev`/`prod` cannot silently fall back to permanent local-disk storage.

## Configuration model

- Canonical property namespace: `barter.storage.*`
- Local storage path: `barter.storage.local.base-path`
- Azure connection string: `barter.storage.azure.connection-string`
- Azure container name: `barter.storage.azure.container-name`

Backward compatibility is preserved for the older Azure property aliases used by the current implementation:

- `azure.storage.connection-string`
- `azure.storage.container-name`

Environment variable support is now documented as:

- DEV preferred: `AZURE_STORAGE_CONNECTION_STRING_DEV`, `AZURE_STORAGE_CONTAINER_DEV`
- DEV optional neutral aliases: `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`
- PROD preferred: `AZURE_STORAGE_CONNECTION_STRING_PROD`, `AZURE_STORAGE_CONTAINER_PROD`
- PROD optional neutral aliases: `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`

## Current storage topology

- DEV container: `item-images-dev`
- PROD recommended container: `item-images-prod`
- Containers should remain **private**.
- The browser-facing URL remains the backend endpoint: `/api/v1/files/**`

## Data ownership boundary

- PostgreSQL keeps image metadata (`item_images` rows, storage keys, ordering, primary flag, content type, file size).
- Azure Blob Storage keeps the binary image payload for DEV and future PROD.
- Local filesystem storage is only for local development.

# Risks

- Large images increase bandwidth, storage, backup, and page-load costs.
- Unsafe or malformed files can create security/support issues.
- Local disk storage can become a production bottleneck and complicate restore.
- CDN/scanning can be overengineered too early if added before launch needs are proven.

# Hardened runtime guarantees

- `dev`/`prod` Azure storage config now fails clearly on startup when the connection string or container name is missing/blank.
- Azure container names are validated before startup continues.
- Local profile continues using the filesystem path under `./uploads` unless overridden.
- Upload serving still goes through the backend controller, so private blob containers remain compatible.
- Existing JPEG/PNG/WebP magic-byte validation, upload size limits, max image count limits, filename sanitization, compensation delete, and `X-Content-Type-Options: nosniff` remain intact.
- Storage connection strings remain excluded from API responses and Azure failure logs continue to redact sensitive values.

# Simpler Alternatives

- Keep current validation and object storage for controlled beta; defer resizing until bandwidth or UX shows pressure.
- Use proxy-served images initially instead of adding CDN and signed URLs immediately.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- Keep `FileStorageService` as the boundary; do not extract an image microservice.
- A background image worker is deferred unless resizing becomes too slow for request/response processing.

# Operational Impact

- Operators now have documented DEV verification steps for image uploads.
- Backup/restore remains split intentionally:
  - PostgreSQL backups cover image metadata.
  - Azure Blob Storage retains binary image data separately.
- The small OCI DEV server no longer needs to be treated as permanent image storage.

# Security Impact

- Keeps upload validation strict and limits executable/content-sniffing risk.
- Future signed/private serving can reduce public enumeration risk.
- Malware scanning is a later risk-based decision, not a default launch dependency.

# Developer Velocity Impact

- Preserves existing abstraction and avoids a storage rewrite.
- Adds clear image rules for future upload features.
- Resizing libraries may add test and platform dependencies.

# Backend Changes Delivered

- Clarified storage configuration under `barter.storage.*`.
- Added fail-fast Azure config validation for `dev`/`prod` startup.
- Preserved backend-proxied image URLs instead of exposing public blob URLs.
- Added profile/configuration tests for local/dev/prod selection and Azure config validation.

# Frontend Changes

- No frontend feature rewrite was required.
- Existing upload flow continues to work against the same backend endpoints.

# Database Changes

- No schema change required unless adding image variants.
- If variants are added, extend image metadata minimally rather than creating a separate asset service.

# Deployment Changes Delivered

- Updated `deployment/env/dev.env.example` and `deployment/env/prod.env.example` with explicit Azure image-storage variables.
- Updated `deployment/docs/DEV_DEPLOYMENT.md` with local vs dev vs prod storage behavior, required containers, verification steps, and troubleshooting.
- DEV compose now relies on env-file-driven Azure settings so the backend can accept either scoped or neutral Azure variable names.

# Testing Strategy

- Keep existing image upload/service tests passing.
- Added targeted configuration tests for:
  - local profile selecting local filesystem storage
  - dev/prod profiles selecting Azure Blob storage
  - fail-fast Azure config validation
  - backward compatibility with legacy Azure property aliases
- Run targeted storage/image tests plus a full backend build.

# DEV Operator Checklist

1. Deploy with `SPRING_PROFILES_ACTIVE=dev`.
2. Confirm `item-images-dev` exists as a private Azure Blob container.
3. Confirm either the DEV-scoped Azure vars or the neutral Azure vars are present in `deployment/env/dev.env`.
4. Upload a JPEG/PNG/WebP image through the normal UI flow.
5. Confirm the backend logs an Azure `operation=store` success.
6. Confirm a new `item_images` row exists in PostgreSQL.
7. Confirm `GET /api/v1/files/<storage_key>` serves the stored image successfully.

# Future Improvements

- CDN-backed delivery.
- Signed URLs/private containers.
- Async image transformation worker.
- Malware scanning if abuse volume or compliance profile requires it.

# Explicitly Deferred

- Dedicated image microservice.
- Kubernetes jobs for image processing.
- Full DAM/media platform.
- Mandatory malware scanning for small controlled beta unless threat model changes.
