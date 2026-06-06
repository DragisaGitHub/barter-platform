# Priority 3 — Documentation Synchronization

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified documentation sets in the repository

### Product / architecture docs already present

- `README.md`
- `frontend/README.md`
- `docs/19-backend-current-status.md`
- `docs/20-frontend-architecture.md`
- `docs/25-frontend-audit.md`
- `docs/26-catalog-items-feature-plan.md`
- `docs/27-trade-offers-feature-plan.md`
- `docs/28-production-readiness-roadmap.md`

### Deployment / operations docs already present

- `deployment/docs/DEV_DEPLOYMENT.md`
- `deployment/docs/OBSERVABILITY.md`

### API / schema sources already present

- `backend/barter-api/src/main/resources/openapi/openapi.yaml`
- `backend/barter-api/src/main/resources/openapi/paths/**/*.yaml`
- `frontend/src/api/generated/schema.ts`

## Already implemented

- The repository already has meaningful product, architecture, deployment, and API documentation.
- The issue is not missing docs categories; it is keeping them aligned with a codebase that has outgrown older roadmap assumptions.

## Confirmed missing

1. **There is no documented synchronization checklist linking code changes to doc updates.**
2. **There is no explicit source-of-truth rule when roadmap docs, status docs, README files, and OpenAPI all describe the same feature.**
3. **Generated API artifacts are present, but documentation does not clearly identify when OpenAPI regeneration is required after contract changes.**

## Not needed / false positives

- Do **not** rewrite the entire docs tree in one pass.
- Do **not** treat this as a replacement for feature implementation work.
- Do **not** mix marketing copy or product re-positioning into this item.

## Intentionally deferred

- This work should follow the code-facing post-analysis backlog; synchronizing too early would just force repeat rewrites.

## Implementation-ready backlog

1. Define source-of-truth ownership by category:
   - API contract → OpenAPI files
   - current implementation state → status / audit docs
   - deployment behavior → `deployment/docs/*`
   - backlog status → roadmap docs
2. Add a lightweight completion checklist for every post-analysis item that changes contracts or user-visible behavior.
3. Explicitly include `frontend/src/api/generated/schema.ts` regeneration whenever OpenAPI changes.
4. Revisit the current-state docs (`docs/19-backend-current-status.md`, `docs/25-frontend-audit.md`, `docs/28-production-readiness-roadmap.md`) after each higher-priority implementation wave.

## Exit criteria

- Contributors know which doc to update for which kind of change.
- API changes no longer leave generated frontend schema and prose docs drifting apart.
- Synchronization remains targeted and downstream of the higher-value backlog items.
