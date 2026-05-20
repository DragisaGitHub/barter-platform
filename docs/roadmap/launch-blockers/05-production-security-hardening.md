# Launch Blocker 05 — Production Security Hardening

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 security / configuration safety  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Create a production-safe configuration baseline that prevents DEV settings, weak secrets, broad CORS, exposed docs, and disabled verification from reaching public environments.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap identifies production configuration hardening as a launch blocker.
- A strong codebase can still fail publicly if launched with DEV profile assumptions.

# Current State

- JWT, refresh tokens, RBAC, and security annotations exist.
- DEV profile disables email verification.
- CORS is described as too permissive for production.
- Security headers, Swagger exposure, secret validation, and prod profile rules need tightening.

# Risks

- Weak/default JWT secret in public environment.
- Unverified accounts if DEV profile is accidentally used.
- Browser attack surface from broad CORS and missing CSP/HSTS/referrer/permissions headers.
- Sensitive API docs or Actuator details exposed.

# Proposed Solution

- Add explicit `prod` profile expectations and fail-fast validation for JWT secret, frontend origin, SMTP/email verification, storage mode, and public URL settings.
- Restrict CORS to configured production origins and prefer same-origin proxying.
- Add security headers at reverse proxy/static frontend layer and backend where appropriate.
- Disable or restrict Swagger/OpenAPI UI and unsafe diagnostic endpoints in production.
- Review logging for secret/token/code redaction.

# Simpler Alternatives

- If launch remains invite-only, email verification can be staged, but production must still fail on weak secrets and broad origins.
- Use proxy-level headers first, then backend headers for API responses.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- Security hardening remains configuration and web-layer focused.
- No identity-provider migration or auth service extraction is needed.

# Operational Impact

- Requires environment-specific configuration discipline.
- Adds startup failures for unsafe production misconfiguration, which is desirable.
- Creates checklist items for deploy and rollback.

# Security Impact

- Reduces XSS impact, token theft risk, misconfiguration risk, and unwanted cross-origin access.
- Does not fully solve localStorage refresh-token risk; that is handled in `production-hardening/02-jwt-token-hardening.md`.

# Developer Velocity Impact

- Fail-fast checks prevent hidden production bugs.
- May require clearer local/DEV profiles so developers are not blocked by production requirements.

# Backend Changes

- Add typed security/config properties and validation.
- Tighten Spring CORS configuration.
- Restrict Swagger/Actuator by profile.
- Add tests for prod startup failure on missing unsafe settings.

# Frontend Changes

- Remove or revise copy that overpromises verified users/smart algorithms if included in this milestone.
- Verify app works with same-origin `/api/v1` proxy and strict CSP.

# Database Changes

- None.

# Deployment Changes

- Add production environment example with required variables but no secrets.
- Update Caddy/nginx headers for CSP, HSTS, Referrer-Policy, Permissions-Policy, X-Frame-Options/frame-ancestors, and nosniff.
- Document never using DEV profile publicly.

# Testing Strategy

- Config validation unit tests.
- Integration test allowed/disallowed CORS origins.
- Manual browser smoke test under CSP.
- Security smoke test for Swagger/Actuator exposure in prod-like config.

# Rollout Plan

- Add prod profile checks in staging first.
- Fix missing env vars until startup is clean.
- Enable strict headers with report-only CSP if needed, then enforce.
- Make production profile validation part of release checklist.

# Future Improvements

- Move refresh token to httpOnly cookie.
- Add admin MFA.
- Add secret manager integration when deployment platform justifies it.
- Add automated dependency/container scans if not handled elsewhere.

# Explicitly Deferred

- Full OAuth/social login.
- Enterprise SSO.
- Managed identity/secret-manager migration unless platform changes.
- Complete auth architecture rewrite.
