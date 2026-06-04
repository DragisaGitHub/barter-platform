# DevOps Roadmap 06 — Operational Validation Checklist

> Practical checks to apply when implementing the roadmap phases.

# Goal

- Provide an implementation-oriented validation checklist for the DEV deploy, backup, rollback, and future production-tagging phases.
- Make sure operational safety is verified before new CI/CD automation is considered complete.

# Scope

## In scope for this document

- readiness checks before Phase 1 implementation
- acceptance checks for Phases 1 through 4
- operator-facing validation steps after deploy or rollback
- minimum audit expectations for manual workflow usage

## Validation areas

### A. Baseline readiness

- The DEV server can pull the latest backend and frontend images.
- `deployment/env/dev.env` exists only on the server and contains valid runtime values.
- Existing deployment scripts run successfully when executed manually on the server.
- Docker publish remains separate from deploy orchestration.

### B. Phase 1 acceptance — manual DEV deploy workflow

- Manual dispatch is available only to intended maintainers.
- The workflow deploys the latest published DEV images from `main`.
- The workflow invokes `deployment/scripts/deploy-dev.sh` rather than reimplementing deploy commands.
- The workflow logs the actor, target ref, start time, and final result.

### C. Phase 2 acceptance — backup before deploy

- The workflow runs `deployment/scripts/backup-db.sh --force` before deploy.
- The deploy step does not run if backup fails.
- Operators can identify the backup attempt in logs.

### D. Phase 3 acceptance — manual rollback workflow

- A separate manual rollback workflow exists.
- Default rollback uses captured deployment state.
- Explicit image-ref override inputs are optional and clearly marked advanced.
- The workflow invokes `deployment/scripts/rollback-dev.sh`.

### E. Phase 4 acceptance — production image tagging

- Production examples and env files use immutable version tags.
- DEV examples remain on `:latest`.
- Backend and frontend release tags follow the same version.

# Required files and secrets

## Files used during validation

- `deployment/scripts/deploy-dev.sh`
- `deployment/scripts/backup-db.sh`
- `deployment/scripts/rollback-dev.sh`
- `deployment/scripts/capture-deployment-state.sh`
- `deployment/docs/DEV_DEPLOYMENT.md`
- `deployment/env/dev.env.example`
- `deployment/env/prod.env.example`

## Secrets and access required later during implementation

- GitHub access to trigger manual workflows
- DEV SSH connection secrets
- Docker registry publish credentials
- server-side runtime env file values

# Explicit non-goals

- Replacing application test strategy.
- Serving as a full incident-response runbook.
- Guaranteeing that a successful deploy also means complete business-level correctness.
- Covering infrastructure automation outside the current server-and-scripts model.

# Validation checklist

## Pre-implementation

- [ ] Manual server-side deploy works today using the existing scripts.
- [ ] Manual server-side rollback works today using the existing scripts.
- [ ] Manual server-side backup works today using the existing scripts.
- [ ] The team agrees that GitHub Actions will orchestrate, not replace, those scripts.

## Phase 1

- [ ] Manual dispatch deploys DEV successfully.
- [ ] DEV still uses the latest backend/frontend images.
- [ ] The workflow fails clearly when SSH or remote script execution fails.
- [ ] No runtime secrets are printed to logs.

## Phase 2

- [ ] Backup runs before deploy.
- [ ] Deploy is blocked if backup fails.
- [ ] Backup result is visible to operators.

## Phase 3

- [ ] Manual rollback completes successfully from captured state.
- [ ] Advanced override inputs are tested at least once in a controlled scenario.
- [ ] Rollback logs identify the image refs used.

## Phase 4

- [ ] Production documentation uses immutable version tags such as `1.0.0`.
- [ ] Production is explicitly documented to avoid `:latest`.
- [ ] Release promotion is traceable from code revision to container tag.

## Post-deploy or post-rollback smoke checks

- [ ] Public homepage responds.
- [ ] `GET /actuator/health` responds successfully.
- [ ] `GET /actuator/health/readiness` responds successfully.
- [ ] Frontend loads the expected build.
- [ ] Basic API flow works against the deployed backend.
- [ ] No obvious error spike appears in container logs.

# Risks

- A workflow can pass while the application still has business-level faults if smoke checks are too shallow.
- SSH-based automation may hide server drift problems unless repo state and path assumptions are kept stable.
- Backup success without restore testing can create false confidence.
- Rollback may appear healthy while data compatibility issues remain.
- Production tag policy can still fail in practice if release naming is not enforced consistently.

