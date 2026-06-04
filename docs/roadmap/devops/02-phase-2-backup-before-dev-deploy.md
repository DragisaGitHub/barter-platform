# DevOps Roadmap 02 — Phase 2 Backup Before DEV Deploy

> Outcome target: the manual DEV deploy workflow takes a fresh PostgreSQL backup before restarting services.

# Goal

- Add a mandatory pre-deploy database backup step to the future manual DEV deploy workflow.
- Reduce the chance that a bad deployment leaves the team without a recent recovery point.
- Keep `deployment/scripts/backup-db.sh` as the authoritative backup implementation.

# Scope

## In scope for this phase

- Extend the future manual DEV deploy workflow to execute `./deployment/scripts/backup-db.sh --force` on the DEV server before `./deployment/scripts/deploy-dev.sh`.
- Fail the deployment workflow if the backup step fails.
- Record backup success in the workflow log before deployment continues.
- Keep the deployment-state capture behavior inside `deploy-dev.sh` unchanged.

## Recommended execution order

1. Manual workflow starts.
2. Workflow verifies target ref and server connectivity.
3. Workflow refreshes the repo checkout on the DEV server.
4. Workflow runs `./deployment/scripts/backup-db.sh --force`.
5. Only after backup success, workflow runs `./deployment/scripts/deploy-dev.sh`.
6. Workflow reports the backup artifact name or timestamp in logs when available.

## Operational notes

- This phase adds a recovery guardrail, not a full disaster-recovery system.
- `backup-db.sh` already represents the supported backup behavior and should not be reimplemented in workflow YAML.
- The deploy script still owns image pull, compose restart, and deployment-state capture.

# Required files and secrets

## Required repository files

- `deployment/scripts/backup-db.sh`
- `deployment/scripts/deploy-dev.sh`
- `deployment/env/dev.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`

## Required GitHub secrets or variables for later implementation

- all DEV SSH secrets from Phase 1
- no new GitHub secret should be required if backup upload uses server-side env values already stored in `deployment/env/dev.env`

## Required server-side configuration

- `deployment/env/dev.env`
- valid backup-related env values, especially:
  - `BACKUP_ENABLED`
  - `BACKUP_AZURE_CONTAINER`
  - `BACKUP_AZURE_PREFIX`
  - `BACKUP_AZURE_CONNECTION_STRING` or fallback-compatible Azure storage connection string
  - `POSTGRES_SERVICE`
- Docker available on the server
- either host Azure CLI or Docker ability to run the Azure CLI container fallback already supported by the script

# Explicit non-goals

- Building a separate backup service.
- Storing database dumps in GitHub Actions artifacts.
- Restoring the database automatically on deploy failure.
- Validating backup contents by full restore during the same workflow run.
- Editing `backup-db.sh` or `deploy-dev.sh`.

# Validation checklist

- [ ] The documented workflow runs `backup-db.sh --force` before `deploy-dev.sh`.
- [ ] Deployment is documented to stop immediately if backup creation fails.
- [ ] Backup logic remains inside `deployment/scripts/backup-db.sh`.
- [ ] Backup credentials remain server-side and are not copied into repository files.
- [ ] Workflow logs give operators enough information to confirm a backup ran.
- [ ] Phase 1 behavior remains intact after this phase is added.

# Risks

- Backup execution increases deploy duration, especially on a small VM.
- If backup configuration in `deployment/env/dev.env` is incomplete, deploys will fail until fixed.
- If operators start bypassing the workflow to avoid backup time, the guardrail loses value.
- A successful backup file upload does not automatically prove restore quality; periodic restore testing is still required.
- If the backup step is made optional too early, the team may skip it during the riskiest deploys.

