# Production Hardening 02 — JWT Token Hardening

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 security  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Reduce token theft and session-abuse risk while preserving a pragmatic SPA developer experience.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap identifies `localStorage` token storage as pragmatic but risky.
- Refresh-token theft via XSS is a serious production concern for a public community platform.

# Current State

- JWT access tokens and persisted refresh-token rotation exist.
- Frontend stores access and refresh tokens in `localStorage` and uses Axios refresh retry logic.
- Refresh tokens are persisted as SHA-256 hashes with `createdAt`, `expiresAt`, and `revokedAt` audit fields.
- Logout already revokes the active refresh token; this task hardens validation and blocked-user refresh behavior.
- CSP/security headers remain an important mitigation, but this task does not force a cookie migration yet.

# Risks

- XSS can steal long-lived refresh tokens.
- Cookie migration can break CORS, same-site, proxy, and local development flows if rushed.
- Session invalidation and multi-device behavior may become confusing without clear semantics.

# Implemented In This Task

- JWT secret handling is now explicit and fail-fast for blank, too-short, or placeholder/default secrets.
- Base token defaults are now production-oriented: access token `15m`, refresh token `7d`.
- DEV keeps more relaxed defaults for manual testing: access token `30m`, refresh token `14d`.
- Refresh/logout now reject missing or blank refresh tokens with a clean `400` response.
- Refresh attempts from suspended/banned accounts are rejected and the presented refresh token is revoked immediately.
- Protected endpoints now return structured JSON `401` responses for missing/invalid/expired access tokens.
- Frontend token handling remains `localStorage`-based for now, but storage access is centralized and the tradeoff is documented.

# Simpler Alternatives

- For controlled beta, keep current localStorage model only after CSP/security-header hardening and dependency cleanup.
- Reduce refresh-token lifetime before full cookie migration if implementation capacity is constrained.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- No auth service extraction.
- Changes stay in auth controller/service, security config, frontend token service, and API client.

# Operational Impact

- Cookie strategy requires production domain, HTTPS, same-site, and proxy consistency.
- Support needs guidance for stale sessions and logout-all behavior.
- Strict token validation reduces incident blast radius.

# Security Impact

- CSP reduces XSS exploitability; httpOnly refresh cookie reduces refresh-token theft impact.
- CSRF must be considered once refresh cookies are introduced.
- Access tokens should remain short-lived and never logged.

# Developer Velocity Impact

- Cookie migration is medium/high complexity and should be isolated to one milestone.
- Clear auth test coverage prevents regressions across protected/admin routes.

# Backend Changes

- Keep JWT claims minimal: subject (`sub`), `username`, `roles`, `iat`, and `exp` only.
- Validate JWT secret + token lifetime configuration at startup.
- Preserve hashed refresh-token persistence and rotation semantics.
- Revoke refresh tokens when blocked users try to refresh.
- Return structured JSON `401` / `403` responses from the security layer.
- Add tests for weak-secret validation, token lifetime validation, refresh rotation/revocation, blocked-user refresh, and clean unauthorized responses.

# Frontend Changes

- Keep the current SPA auth flow working with `localStorage`-backed tokens.
- Centralize token reads/writes in `token.service.ts` instead of scattered direct storage access.
- Document that `localStorage` remains an XSS-sensitive tradeoff until the future httpOnly-cookie milestone.
- Keep refresh-failure handling consistent: clear tokens and redirect to login when appropriate.

# Database Changes

- Likely none; existing refresh-token persistence can remain.
- Optional device/session metadata can be added later.

# Deployment Changes

- Require HTTPS and correct public domain settings.
- Configure same-origin proxy or strict CORS credentials only for production origins.
- Document `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MINUTES`, and `JWT_REFRESH_EXPIRATION_DAYS` explicitly in env examples.
- Document safe secret generation for both shell and PowerShell operators.

# Testing Strategy

- Auth integration tests for login, refresh, logout, blocked-user refresh, and revocation.
- Unit tests for weak-secret rejection and invalid token lifetime settings.
- Protected-endpoint tests for structured `401` responses.
- Browser smoke tests for login persistence, route refresh, and logout remain recommended.

# Rollout Plan

- Complete CSP/security-header milestone first.
- Keep current token storage for now while hardening config, revocation, and documentation.
- Test in staging with production-like domain/HTTPS.
- Revisit cookie-based refresh only as a separate milestone with CSRF/CORS/proxy validation.

# Future Improvements

- Move refresh token to secure `httpOnly` same-site cookie and keep access token short-lived/in-memory.
- Add CSRF-aware refresh/logout flow if cookie transport is introduced.
- Admin MFA.
- Session/device management UI.
- Refresh-token anomaly detection.
- OAuth/social login only after core auth is stable.

# Explicitly Deferred

- Full httpOnly-cookie migration in this milestone.
- OAuth / social login.
- MFA rollout.
- Complete identity-provider migration.
- Enterprise SSO.
- Opaque token introspection service.
- Microservice auth boundary.
