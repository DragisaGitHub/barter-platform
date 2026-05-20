# Production Hardening 01 — Image Storage Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 operational cost/security/performance  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Define a production-safe image storage, serving, validation, and lifecycle strategy for user-uploaded item images.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- Images are one of the most likely cost, performance, security, and backup pressure points for a barter marketplace.
- The roadmap notes existing storage abstraction is good, but production object storage policy, resizing, CDN, lifecycle, and scanning are incomplete.

# Current State

- Item image upload/list/delete/set-primary APIs exist.
- Local storage and Azure Blob storage implementations exist behind `FileStorageService`.
- Magic-byte validation, max size/count controls, filename sanitization, compensation delete, and `nosniff` serving are already present.

# Risks

- Large images increase bandwidth, storage, backup, and page-load costs.
- Unsafe or malformed files can create security/support issues.
- Local disk storage can become a production bottleneck and complicate restore.
- CDN/scanning can be overengineered too early if added before launch needs are proven.

# Proposed Solution

- For production, prefer object storage over local disk and document the supported storage mode.
- Add server-side dimension validation and resizing/compression before CDN-level optimization.
- Define lifecycle rules for orphaned/deleted images and backup expectations.
- Keep serving through the existing abstraction; use signed or proxied URLs later if public blob exposure becomes a concern.
- Document current threat model: JPEG/PNG/WebP only, size/count limits, no arbitrary file serving.

# Simpler Alternatives

- Keep current validation and object storage for controlled beta; defer resizing until bandwidth or UX shows pressure.
- Use proxy-served images initially instead of adding CDN and signed URLs immediately.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- Keep `FileStorageService` as the boundary; do not extract an image microservice.
- A background image worker is deferred unless resizing becomes too slow for request/response processing.

# Operational Impact

- Requires storage retention/lifecycle decisions.
- Operators need cleanup guidance for orphaned files and failed uploads.
- Backup/restore strategy must include image metadata and blobs together.

# Security Impact

- Keeps upload validation strict and limits executable/content-sniffing risk.
- Future signed/private serving can reduce public enumeration risk.
- Malware scanning is a later risk-based decision, not a default launch dependency.

# Developer Velocity Impact

- Preserves existing abstraction and avoids a storage rewrite.
- Adds clear image rules for future upload features.
- Resizing libraries may add test and platform dependencies.

# Backend Changes

- Add dimension validation and optional resizing/compression pipeline if selected for this milestone.
- Clarify storage configuration validation for production.
- Add orphan cleanup command/service only if safe and testable.

# Frontend Changes

- Update upload guidance for allowed formats, max size, image quality, and recommended dimensions.
- Handle image-processing errors with clear messages.

# Database Changes

- No schema change required unless adding image variants.
- If variants are added, extend image metadata minimally rather than creating a separate asset service.

# Deployment Changes

- Document production storage provider variables and lifecycle policy.
- Ensure image storage is included in backup/restore docs.
- Add object storage credentials handling to production config validation.

# Testing Strategy

- Test upload validation for format, size, dimension, count, and malicious extension cases.
- Integration test storage compensation behavior.
- Manual/browser test thumbnail/detail rendering after resize if implemented.

# Rollout Plan

- Document production storage mode first.
- Enable object storage in staging.
- Add resizing if needed and backfill only if valuable.
- Monitor storage growth and image load performance.

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
