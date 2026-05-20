# Production Hardening 04 — Deployment and Rollback Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 release safety  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Make production deployments repeatable, observable, and reversible without adopting Kubernetes-first complexity.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap endorses Compose + Caddy as right-sized but notes missing rollback discipline, smoke tests, immutable tags, and release gates.
- A small team needs boring, reliable release mechanics.

# Current State

- Backend/frontend Dockerfiles exist.
- DEV Compose stack with Caddy, Postgres, backend, and frontend exists.
- Deployment and backup scripts exist.
- CI builds/tests and Docker image publishing exist.

# Risks

- Mutable tags can make rollback ambiguous.
- Deployments can succeed while app health is broken.
- Database migrations may be hard to reverse under pressure.
- Overbuilding CI/CD can distract from launch blockers.

# Proposed Solution

- Use immutable image tags for production deployments.
- Maintain a release checklist: CI green, image scan, backup confirmed, migration reviewed, smoke tests pass, rollback tag known.
- Add a rollback script/procedure to previous known-good image tags.
- Add container smoke test: backend health/ping, frontend health/static load, basic API path.
- Document migration safety rules and restore fallback.

# Simpler Alternatives

- Keep manual deploys but require checklist and immutable tags before public beta.
- Add automated deployment only after manual path is safe and repeatable.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- Keep Compose/Caddy until operational pain justifies managed orchestration.
- Do not introduce Kubernetes to solve rollback before basic image-tag rollback is mature.

# Operational Impact

- Operators get a predictable release and rollback flow.
- Releases require pre-deploy backup verification.
- Smoke tests reduce broken deployments reaching users.

# Security Impact

- Image scanning and immutable tags improve supply-chain confidence.
- Deploy scripts must not print secrets.
- Rollback must consider vulnerable images and data migrations.

# Developer Velocity Impact

- Slightly slower releases, much lower incident risk.
- A clear checklist helps future contributors ship safely.

# Backend Changes

- Expose or confirm stable health/readiness endpoint for smoke tests.
- Ensure migrations are reviewed for backward/rollback safety.

# Frontend Changes

- Expose or confirm lightweight frontend health/static endpoint.
- Ensure SPA fallback remains functional after deploy.

# Database Changes

- No schema solely for deployment.
- Add migration review checklist and pre-migration backup requirement.

# Deployment Changes

- Update deploy script/docs for immutable tag input and previous-tag rollback.
- Add smoke-test script.
- Add optional Trivy/container scan in publish workflow if not already present.

# Testing Strategy

- Test deploy script against staging/dev VM if available.
- Test rollback from tag N to tag N-1.
- Test smoke checks fail fast when backend/DB/frontend is unavailable.

# Rollout Plan

- Document manual checklist first.
- Add immutable tag deploy support.
- Add rollback command/procedure.
- Add smoke tests and later wire them into CI/CD.

# Future Improvements

- Blue/green or canary strategy if traffic grows.
- Managed database with PITR.
- Automated environment promotion.
- Infrastructure as Code if deployment target stabilizes.

# Explicitly Deferred

- Kubernetes rollout strategy.
- Service mesh/canary platform.
- Fully automated production deploys before manual safety is proven.
- Multi-region deployment.
