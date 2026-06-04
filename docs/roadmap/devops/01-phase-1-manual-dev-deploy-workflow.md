# DevOps Roadmap 01 — Phase 1 Manual DEV Deploy Workflow

> Outcome target: a manually triggered GitHub Actions workflow deploys the latest DEV images to the DEV server by invoking the existing server-side deploy script.

# Goal

- Replace the current manual SSH-and-run deploy habit with a manually triggered GitHub Actions workflow for DEV.
- Keep DEV aligned with the latest published images from `main`.
- Preserve `deployment/scripts/deploy-dev.sh` as the only place where deployment mechanics live.

# Scope

## In scope for this phase

- The implemented `workflow_dispatch` GitHub Actions workflow at `.github/workflows/dev-deploy.yml` for DEV deployment.
- Remote execution on the DEV server over SSH.
- Running `git pull` or equivalent repo refresh on the DEV host before deploy.
- Calling `deployment/scripts/deploy-dev.sh` on the server.
- Logging workflow start, operator, target branch/ref, and completion result.

## Recommended workflow behavior

1. Manual trigger only.
2. Default target ref is `main`.
3. Verify that the Docker publish workflow for the same ref has already completed successfully.
4. SSH to the DEV server.
5. Refresh the checked-out repository on the server to the desired ref.
6. Execute `./deployment/scripts/deploy-dev.sh` from the repo root.
7. Surface success/failure in GitHub Actions logs.

## Deployment contract for DEV

- DEV uses the newest backend and frontend images.
- `deployment/env/dev.env` should keep:
  - `BACKEND_IMAGE=dragisahub1984/barter-backend:latest`
  - `FRONTEND_IMAGE=dragisahub1984/barter-frontend:latest`
- The workflow must not rewrite deployment logic that already exists inside `deploy-dev.sh`.
- The workflow may pass context to the script through shell environment or logging, but should not replace the script with inline `docker compose` commands.

# Required files and secrets

## Required repository files

- `deployment/scripts/deploy-dev.sh`
- `deployment/compose/docker-compose.dev.yml`
- `deployment/env/dev.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`

## Required GitHub secrets and variables for implementation

- Required:
  - `DEV_SSH_HOST`
  - `DEV_SSH_USER`
  - `DEV_SSH_PRIVATE_KEY`
  - `DEV_DEPLOY_PATH`
- Optional:
  - `DEV_SSH_PORT` (defaults to `22` when not set)
  - `DEV_SSH_KNOWN_HOSTS` (recommended; if omitted, the workflow falls back to `ssh-keyscan` before connecting)

Recommended GitHub classification:

- repository or environment variable: `DEV_SSH_HOST`
- repository or environment variable: `DEV_SSH_USER`
- repository or environment variable: `DEV_DEPLOY_PATH`
- optional repository or environment variable: `DEV_SSH_PORT`
- repository or environment secret: `DEV_SSH_KNOWN_HOSTS`
- environment secret: `DEV_SSH_PRIVATE_KEY`

## Manual usage steps

1. Confirm the `main` branch images have already been published and are available to the DEV server.
2. In GitHub, open **Actions** and then select **DEV Deploy**.
3. Choose the `main` branch workflow definition unless there is a specific reason to run the workflow file from another ref.
4. Click **Run workflow**.
5. The workflow connects to the DEV host over SSH and runs the following on the server from the configured checkout path:

   ```bash
   cd "$DEV_DEPLOY_PATH"
   git fetch origin main
   git reset --hard origin/main
   chmod +x deployment/scripts/*.sh
   ./deployment/scripts/deploy-dev.sh
   ```

6. Review the workflow logs for the triggering actor, target ref (`main`), and final success or failure state.

## Required server-side files and prerequisites

- checked-out repository on the DEV host
- `deployment/env/dev.env`
- Docker Engine and `docker compose`
- access to pull `dragisahub1984/barter-backend:latest` and `dragisahub1984/barter-frontend:latest`

# Explicit non-goals

- Auto-deploying DEV on every merge to `main`.
- Publishing Docker images from the deploy workflow.
- Editing `deployment/scripts/deploy-dev.sh`.
- Adding database backup execution in this phase.
- Implementing rollback execution in this phase.
- Introducing production deployment behavior.

# Validation checklist

- [ ] The future workflow is documented as `workflow_dispatch` only.
- [ ] The workflow is documented to deploy from `main` by default.
- [ ] The workflow uses remote SSH execution rather than duplicating deployment commands in YAML.
- [ ] `deployment/scripts/deploy-dev.sh` remains the source of deploy logic.
- [ ] DEV image references remain on `:latest` for backend and frontend.
- [ ] Operators can identify who triggered the deployment and when.
- [ ] Failures in remote script execution are visible in GitHub Actions logs.

# Risks

- If the deploy workflow runs before images are published, DEV may redeploy stale images.
- If the workflow performs inline Docker commands instead of calling `deploy-dev.sh`, operational drift will start immediately.
- If the server checkout is dirty or diverged, `git pull` behavior may become unreliable.
- If SSH host verification is skipped, the workflow will be easier to abuse.
- If `:latest` is manually overridden on the server without team visibility, DEV may stop reflecting the newest intended build.

