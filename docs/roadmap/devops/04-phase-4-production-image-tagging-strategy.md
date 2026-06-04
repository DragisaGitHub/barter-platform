# DevOps Roadmap 04 — Phase 4 Production Image Tagging Strategy

> Outcome target: production deployments use immutable image tags such as `dragisahub1984/barter-backend:1.0.0` and `dragisahub1984/barter-frontend:1.0.0` instead of `:latest`.

# Goal

- Define the production image-tagging model before production deployment automation is introduced.
- Ensure production releases are reproducible, auditable, and rollback-friendly.
- Keep DEV convenience on `:latest` while production moves to immutable version tags.

# Scope

## In scope for this phase

- Define how production-ready images should be tagged.
- Separate Git ref naming from runtime image tag naming where helpful.
- Document expected production env-file usage.
- Define the minimum operator expectations for release promotion.

## Recommended production tagging model

- DEV continues to consume:
  - `dragisahub1984/barter-backend:latest`
  - `dragisahub1984/barter-frontend:latest`
- Production should consume explicit version tags such as:
  - `dragisahub1984/barter-backend:1.0.0`
  - `dragisahub1984/barter-frontend:1.0.0`
- If Git release tags continue to use `v1.0.0`, publish a normalized container tag without the `v` prefix as well so runtime env files can stay clean and stable.
- Later hardening may prefer image digests in production, but this phase stops at immutable version tags.

## Promotion expectations

1. Merge code to `main`.
2. Build and publish DEV images for normal `main` usage.
3. When a release is approved, publish immutable production tags.
4. Update `deployment/env/prod.env` to the approved version tags.
5. Deploy production using those pinned tags only.

## Env-file contract

- `deployment/env/dev.env` may continue to point to `:latest`.
- `deployment/env/prod.env` should never point to `:latest`.
- Example future production values:
  - `BACKEND_IMAGE=dragisahub1984/barter-backend:1.0.0`
  - `FRONTEND_IMAGE=dragisahub1984/barter-frontend:1.0.0`

# Required files and secrets

## Required repository files

- `deployment/env/prod.env.example`
- `deployment/env/dev.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`

## Expected CI/CD-side secrets for later implementation

- Docker registry credentials for publishing immutable release tags
- later production deploy credentials, kept separate from DEV credentials

## Required operational data

- agreed release version number
- mapping from Git commit or Git tag to published backend/frontend image tags
- release notes identifying the exact promoted build

# Explicit non-goals

- Building the production deployment workflow.
- Defining a full semantic-versioning governance process.
- Switching DEV away from `:latest`.
- Requiring digests immediately for production.
- Changing runtime infrastructure or deployment scripts.

# Validation checklist

- [ ] Production is documented to use immutable version tags instead of `:latest`.
- [ ] DEV is documented to stay on `:latest` until a later decision changes that.
- [ ] The examples use explicit production tags such as `1.0.0`.
- [ ] The document distinguishes image publishing from image deployment.
- [ ] `deployment/env/prod.env` is identified as the place where the chosen release version is pinned.
- [ ] Rollback friendliness is improved because old image refs remain addressable.

# Risks

- If production tagging is inconsistent between backend and frontend, releases may drift.
- If only mutable tags exist, rollback and audit become unreliable.
- If Git tag naming and image tag naming are not aligned deliberately, operators may deploy the wrong image.
- If production env files are edited without release tracking, the chosen version may become ambiguous.
- If version tags are published before validation is complete, bad images may look release-ready.

