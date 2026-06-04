# DevOps Roadmap 00 — Overview and Principles

> Scope: roadmap only. No GitHub Actions workflows, script changes, or application-code changes are part of this document set.

# Goal

- Define a phased CI/CD roadmap that improves DEV deployment safety first, while preserving the current small-team operating model.
- Keep `deployment/scripts/deploy-dev.sh`, `deployment/scripts/rollback-dev.sh`, `deployment/scripts/backup-db.sh`, and `deployment/scripts/capture-deployment-state.sh` as the source of deployment behavior.
- Separate DEV convenience from future production release discipline.

# Scope

- Document the target operating model for manually triggered GitHub Actions based deployments.
- Define the order of implementation:
  1. manual DEV deploy workflow
  2. backup-before-deploy guardrail
  3. manual DEV rollback workflow
  4. production image tagging strategy
- Define cross-cutting expectations for secrets, validation, and operational safety.

# Required files and secrets

## Existing files that remain authoritative

- `deployment/scripts/deploy-dev.sh`
- `deployment/scripts/rollback-dev.sh`
- `deployment/scripts/backup-db.sh`
- `deployment/scripts/capture-deployment-state.sh`
- `deployment/compose/docker-compose.dev.yml`
- `deployment/env/dev.env.example`
- `deployment/env/prod.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`

## Expected GitHub-side secret categories for later phases

- Docker image publishing credentials already used for image publishing.
- DEV server SSH connection secrets for remote execution.
- Optional environment-protection settings for manual approvals.

## Expected server-side runtime files

- `deployment/env/dev.env`
- later: `deployment/env/prod.env`

# Guiding principles

1. **Scripts own deployment logic**  
   GitHub Actions should orchestrate and audit execution, not duplicate shell logic already encoded in the server scripts.

2. **DEV stays intentionally mutable**  
   DEV should continue using the newest published backend and frontend images through `:latest` unless an operator intentionally pins something else for debugging.

3. **Production must become immutable**  
   Production should move to explicit version tags such as `dragisahub1984/barter-backend:1.0.0` and `dragisahub1984/barter-frontend:1.0.0`, with later option to prefer digests.

4. **Manual approval before automation depth**  
   The next step is manual-trigger CI/CD, not auto-deploy on merge. Human intent remains part of release control.

5. **Back up before risky change**  
   Any deploy path that can change schema, configuration, or runtime behavior should have a documented database backup step before service restart.

6. **Rollback must be first-class**  
   A deploy workflow is incomplete until rollback is documented, testable, and easy to trigger under pressure.

7. **Secrets stay out of the repo**  
   GitHub Actions may hold connection secrets, but runtime application secrets must remain on the target server in env files or a later dedicated secret store.

# Explicit non-goals

- Creating `.github/workflows/*` files in this roadmap item.
- Rewriting existing shell scripts in `deployment/scripts/`.
- Changing Docker image build logic.
- Introducing Kubernetes, Argo CD, Helm, Terraform, or other platform tooling.
- Defining a full production hosting platform.
- Auto-deploying DEV on every merge to `main`.

# Validation checklist

- [ ] The roadmap keeps all existing deployment scripts as the execution source of truth.
- [ ] DEV deployment is defined as manual workflow dispatch, not automatic deploy-on-merge.
- [ ] DEV is explicitly documented to consume the newest published images.
- [ ] Backup and rollback are represented as separate implementation phases.
- [ ] Production image immutability is called out as a later phase, not mixed into DEV behavior.
- [ ] Secrets handling is separated from application source code.

# Risks

- If GitHub Actions reimplements shell behavior instead of calling the existing scripts, deploy logic will drift.
- If DEV adopts immutable release semantics too early, the team may lose the convenience of testing the latest `main` build quickly.
- If rollback is delayed until after deploy automation exists, operational risk will rise.
- If production tagging is deferred too long, mutable-tag usage may leak into production operations.
- If server runtime secrets are copied into GitHub workflows, the blast radius of CI compromise increases.

