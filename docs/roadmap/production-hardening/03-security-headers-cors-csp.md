# Production Hardening 03 — Security Headers, CORS, and CSP

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 browser security  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Enforce browser security boundaries through strict production CORS, CSP, and security headers.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap flags permissive CORS and missing security headers as important production risks.
- Headers are a fast, high-impact mitigation for XSS, clickjacking, referrer leakage, and token-exposure blast radius.

# Current State

- Backend CORS is described as using broad defaults.
- Frontend is served through nginx/Caddy in container deployment.
- Stored-file serving already sets `X-Content-Type-Options: nosniff` for one path.

# Risks

- Overly broad origins/methods/headers increase browser attack surface.
- A strict CSP can break Vite-built assets or third-party styles if not tested.
- Header config split between backend, nginx, and Caddy can drift.

# Proposed Solution

- Define one production frontend origin allowlist and prefer same-origin `/api/v1` proxying.
- Set CORS per environment: permissive only in local DEV, strict in prod.
- Add CSP baseline without unsafe remote script execution; use report-only first if needed.
- Add HSTS, Referrer-Policy, Permissions-Policy, frame-ancestors or X-Frame-Options, and nosniff consistently.
- Document where each header is applied: backend API, frontend nginx, Caddy reverse proxy.

# Simpler Alternatives

- Start with strict CORS plus basic headers and defer fully tuned CSP until after smoke testing.
- Use CSP report-only temporarily to discover asset violations.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- No architecture rewrite.
- Header policy belongs to web/deployment layers and should be documented near deployment config.

# Operational Impact

- Requires careful production domain configuration.
- Header changes become release-checklist items.
- CSP reports can inform frontend cleanup if collection is later added.

# Security Impact

- Reduces XSS impact and clickjacking/referrer risks.
- Protects localStorage-token model until token hardening is complete.
- Must not accidentally allow wildcard origins with credentials.

# Developer Velocity Impact

- May require minor frontend asset/style fixes.
- Clear local-vs-prod config prevents development friction.

# Backend Changes

- Replace broad CORS defaults with typed allowlist properties.
- Add tests for allowed and rejected origins/methods/headers.
- Ensure API error responses include expected security headers where backend-owned.

# Frontend Changes

- Remove inline scripts/unsafe patterns if CSP blocks them.
- Verify generated assets, fonts, images, and API calls under CSP.

# Database Changes

- None.

# Deployment Changes

- Update nginx/Caddy config for production headers.
- Document HSTS only after HTTPS/domain is stable.
- Add environment variables for allowed origins and public frontend URL.

# Testing Strategy

- Automated CORS integration tests.
- Browser smoke test login, catalog, item images, trade messages, admin pages under headers.
- Use browser devtools or security scanner to confirm headers.

# Rollout Plan

- Apply strict CORS in staging.
- Enable core headers.
- Run CSP report-only if needed, fix violations, then enforce.
- Make header verification part of production smoke test.

# Future Improvements

- CSP violation reporting endpoint/service.
- Automated security-header scan in CI.
- Subresource integrity if external assets are introduced.

# Explicitly Deferred

- Managed WAF as first-line solution.
- Complex nonce build pipeline unless required.
- Third-party tracking scripts that weaken CSP.
- Browser security policy exceptions for speculative features.
