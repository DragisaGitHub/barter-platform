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

- Backend CORS now has an origin allowlist, but this task further tightens it with typed method/header settings and stricter defaults.
- Frontend is served through nginx/Caddy in container deployment.
- Stored-file serving already sets `X-Content-Type-Options: nosniff` for one path.

# Risks

- Overly broad origins/methods/headers increase browser attack surface.
- A strict CSP can break Vite-built assets or third-party styles if not tested.
- Header config split between backend, nginx, and Caddy can drift.

# Implemented In This Task

- Backend CORS now uses typed properties for origins, methods, request headers, exposed headers, credentials, and max-age.
- CORS credentials default to `false`, which is safer for the current bearer-token flow.
- Production startup now rejects wildcard CORS methods/headers in addition to unsafe origins.
- Backend API responses emit HSTS on secure requests and continue to send CSP, `nosniff`, frame, referrer, and permissions headers.
- Frontend nginx CSP is tightened around same-origin API usage and explicitly allows only the current Google Fonts dependency.
- Deployment docs now state header ownership across backend API, frontend nginx, and Caddy.

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
- Keep `allowCredentials=false` by default for token-based auth.
- Add HSTS for secure API responses.
- Add tests for allowed and rejected origins, methods, and headers.
- Ensure API responses continue to include expected backend-owned security headers.

# Frontend Changes

- Keep scripts same-origin only.
- Keep current Google Fonts dependency working via an explicit CSP allowlist.
- Keep API calls same-origin via `/api/v1` in container deployment.
- Verify generated assets, fonts, images, and API calls under CSP.

# Database Changes

- None.

# Deployment Changes

- Update nginx/Caddy config for production headers.
- Document header ownership across backend/nginx/Caddy.
- Add environment variables for allowed origins, methods, headers, exposed headers, credentials, and CORS max-age.
- Keep same-origin `/api/v1` proxying as the preferred deployment shape.

# Testing Strategy

- Automated CORS integration tests for allowed/rejected origins, methods, and headers.
- Backend MVC tests for HSTS and existing security headers.
- Browser smoke test login, catalog, item images, trade messages, admin pages under headers.
- Use browser devtools or security scanner to confirm headers.

# Rollout Plan

- Apply strict CORS in staging.
- Enable core headers.
- If frontend assets later introduce new external dependencies, review CSP deliberately instead of broadening it by default.
- Make header verification part of production smoke test.

# Future Improvements

- CSP violation reporting endpoint/service.
- Automated security-header scan in CI.
- Self-host fonts to remove the remaining external font-domain CSP allowances.
- Subresource integrity if external assets are introduced.

# Explicitly Deferred

- Managed WAF as first-line solution.
- Complex nonce build pipeline unless required.
- Full CSP report-only/reporting service rollout.
- Third-party tracking scripts that weaken CSP.
- Browser security policy exceptions for speculative features.
