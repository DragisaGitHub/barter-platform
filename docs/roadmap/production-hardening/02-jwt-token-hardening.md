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
- CSP/security headers are planned as immediate mitigation.

# Risks

- XSS can steal long-lived refresh tokens.
- Cookie migration can break CORS, same-site, proxy, and local development flows if rushed.
- Session invalidation and multi-device behavior may become confusing without clear semantics.

# Proposed Solution

- Short term: enforce CSP/security headers, remove unsafe rendering patterns, and keep token lifetimes conservative.
- Medium term: move refresh token to secure httpOnly same-site cookie while keeping access token short-lived and preferably in memory.
- Make refresh endpoint CSRF-aware if cookie-based refresh is used.
- Preserve refresh-token rotation and revocation semantics.
- Document token lifetime and logout behavior.

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

- Add cookie-based refresh option and secure cookie attributes when selected.
- Update CORS credentials rules only for allowed origins.
- Add logout/revoke behavior for cookie sessions.
- Add tests for refresh rotation, expired tokens, revoked tokens, and cookie attributes.

# Frontend Changes

- Refactor `token.service.ts` and Axios refresh handling for cookie refresh flow.
- Avoid reading refresh tokens from JavaScript after migration.
- Handle unauthenticated refresh failures consistently.

# Database Changes

- Likely none; existing refresh-token persistence can remain.
- Optional device/session metadata can be added later.

# Deployment Changes

- Require HTTPS and correct public domain settings.
- Configure same-origin proxy or strict CORS credentials only for production origins.
- Document local development behavior separately from production.

# Testing Strategy

- Auth integration tests for login, refresh, logout, expiry, and revocation.
- Browser smoke tests for login persistence, route refresh, and logout.
- Security tests for CORS credentials and cookie attributes.

# Rollout Plan

- Complete CSP/security-header milestone first.
- Implement cookie refresh behind feature/config switch if practical.
- Test in staging with production-like domain/HTTPS.
- Cut over during low traffic and monitor auth errors.

# Future Improvements

- Admin MFA.
- Session/device management UI.
- Refresh-token anomaly detection.
- OAuth/social login only after core auth is stable.

# Explicitly Deferred

- Complete identity-provider migration.
- Enterprise SSO.
- Opaque token introspection service.
- Microservice auth boundary.
