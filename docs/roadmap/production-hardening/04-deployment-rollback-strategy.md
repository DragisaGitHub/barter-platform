# Production Hardening 04 — Deployment and Rollback Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 release safety  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Make production deployments repeatable, observable, and reversible without adopting Kubernetes-first complexity.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

## Implementation status

- Status: implemented for the current Docker Compose DEV deployment path.
- Scope kept intentionally small-operator friendly: image-based rollback only, no Kubernetes, no blue/green, no paid tooling.
- Primary runbook: `deployment/docs/DEV_DEPLOYMENT.md`

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

# Implemented Strategy

## What was added

- `deployment/scripts/capture-deployment-state.sh`
  - captures the current running backend/frontend image refs before a deploy;
  - stores timestamped state files under `deployment/state/dev/`;
  - refreshes `deployment/state/dev/latest.env` for the default rollback path;
  - prefers repo digests for rollback when available, otherwise falls back to the container config image ref and then local image ID.
- `deployment/scripts/deploy-dev.sh`
  - still supports the simple `latest`-based DEV workflow;
  - now captures deployment state before pulling new images;
  - now supports `--help`, `--dry-run`, `--skip-pull`, and `--skip-state-capture`;
  - waits for backend/frontend health checks before declaring success.
- `deployment/scripts/rollback-dev.sh`
  - rolls back backend/frontend images only;
  - supports `--help`, `--dry-run`, `--skip-pull`, and explicit `--backend-image` / `--frontend-image` overrides;
  - recreates `backend` and `frontend` only, preserving PostgreSQL, Caddy, Docker volumes, and Azure Blob data;
  - does not restore PostgreSQL and does not prune Docker resources.
- `deployment/docs/DEV_DEPLOYMENT.md`
  - now includes a pre-deploy checklist, normal deploy flow, rollback flow, safe/unsafe rollback boundaries, database rollback warning, and Azure Blob behavior during rollback.
- `deployment/env/dev.env.example` and `deployment/env/prod.env.example`
  - now document image pinning expectations more clearly.

## Rollback model

- Normal DEV deploy may continue using `BACKEND_IMAGE=...:latest` and `FRONTEND_IMAGE=...:latest`.
- Before deployment, capture the currently running backend/frontend image state.
- Roll back by temporarily pinning backend/frontend to the previously running image refs captured in the state file.
- Prefer immutable refs for rollback in this order:
  1. repo digest (`image@sha256:...`)
  2. explicit tag (`vX.Y.Z`, `main-<sha>`, etc.)
  3. local image ID only when the image still exists on the host
- Rollback is intentionally **image-based, not database-based**.
- PostgreSQL restore remains manual and high-risk.
- Operators are expected to take a fresh DB backup before risky deploys.

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
- Container health checks now gate both deploy and rollback completion.
- The rollback path avoids destructive host cleanup actions.

# Security Impact

- Image scanning and immutable tags improve supply-chain confidence.
- Deploy scripts must not print secrets.
- Rollback must consider vulnerable images and data migrations.
- Production guidance remains to pin immutable release tags or digests instead of mutable `latest`.

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

- Deploy script updated to capture pre-deploy state and wait for backend/frontend health.
- Dedicated rollback script added for backend/frontend image rollback.
- Docs updated to define when image rollback is safe versus when manual DB recovery is required.
- Docker publish workflow is still relevant because it already produces immutable release tags (`v*`) and main-branch SHA tags (`main-<full-git-sha>`), which are the preferred rollback inputs.
- Optional image scanning remains a future improvement, not part of this implementation unit.

# Testing Strategy

- Validate Bash syntax for all deployment scripts with `bash -n`.
- Validate `--help` output for new scripts.
- Validate `--dry-run` for the new capture/rollback flow without requiring a live Docker Hub or Azure call.
- Keep application builds out of scope because no business-logic code changed.

# Rollout Plan

- Done: document the manual checklist and recovery boundaries.
- Done: add capture + rollback commands for the current Compose deployment shape.
- Done: keep the normal DEV `latest` workflow intact while enabling safer rollback to captured refs.
- Future: promote the same discipline to production env files using immutable tags/digests by default.

# Future Improvements

- Add a lightweight public smoke-test script (`/`, `/actuator/health`, `/actuator/health/readiness`) if the operator workflow needs stronger post-deploy verification than container health alone.
- Blue/green or canary strategy if traffic grows.
- Managed database with PITR.
- Automated environment promotion.
- Infrastructure as Code if deployment target stabilizes.

# Explicitly Deferred

- Kubernetes rollout strategy.
- Service mesh/canary platform.
- Fully automated production deploys before manual safety is proven.
- Multi-region deployment.
- Automatic PostgreSQL restore during rollback.
- Docker volume deletion or automatic `docker system prune` during deploy/rollback.
