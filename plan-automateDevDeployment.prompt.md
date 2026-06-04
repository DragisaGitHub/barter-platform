I reviewed the current deployment assets and will keep this planning-only, with no file edits and no implementation code.

## Checklist

- ✅ Confirmed `deploy-dev.sh` already handles state capture, image pull, Compose update, and health checks.
- ✅ Confirmed `rollback-dev.sh` already supports image-based rollback using captured state.
- ✅ Confirmed `backup-db.sh` already supports PostgreSQL backup upload to Azure Blob Storage.
- ✅ Confirmed DEV Compose defaults to `dragisahub1984/barter-backend:latest` and `dragisahub1984/barter-frontend:latest`.
- ✅ Confirmed no `.github/workflows/*` files exist in the checked-out workspace, although `deployment/docs/DEV_DEPLOYMENT.md` references `.github/workflows/docker-publish.yml`.

---

# 1. Recommended workflow architecture

Use a **manual GitHub Actions DEV deployment workflow** with a clear two-stage pipeline:

```text
workflow_dispatch
        |
        v
Build and publish Docker images
        |
        v
SSH to DEV server
        |
        v
Optionally refresh deployment files from main
        |
        v
Optionally run existing backup-db.sh
        |
        v
Run existing deploy-dev.sh
        |
        v
Report deployment result
```

## Key design principles

### DEV uses mutable `latest`

For DEV, keep the current simple behavior:

```text
dragisahub1984/barter-backend:latest
dragisahub1984/barter-frontend:latest
```

This matches `deployment/compose/docker-compose.dev.yml`:

```yaml
backend:
  image: ${BACKEND_IMAGE:-dragisahub1984/barter-backend:latest}

frontend:
  image: ${FRONTEND_IMAGE:-dragisahub1984/barter-frontend:latest}
```

The workflow should publish new `latest` images first, then let the DEV server pull those latest images through `deploy-dev.sh`.

### Production later uses immutable tags only

Production should not use `latest`.

Future production deployments should use tags like:

```text
dragisahub1984/barter-backend:1.0.0
dragisahub1984/barter-frontend:1.0.0
```

Optionally, production can go further and pin image digests:

```text
dragisahub1984/barter-backend@sha256:<digest>
dragisahub1984/barter-frontend@sha256:<digest>
```

### Keep deployment logic on the server

Do **not** duplicate this in GitHub Actions:

- `docker compose pull`
- `docker compose up`
- state capture
- health polling
- rollback mechanics

Those already belong to:

```text
deployment/scripts/deploy-dev.sh
deployment/scripts/rollback-dev.sh
deployment/scripts/capture-deployment-state.sh
```

GitHub Actions should only orchestrate:

1. image publishing;
2. remote command execution over SSH.

---

# 2. DEV workflow steps

Recommended workflow name:

```text
DEV Deploy
```

Recommended trigger:

```text
workflow_dispatch only
```

This keeps deployment manual for now, as requested.

## Step-by-step DEV flow

### Step 1 — Operator starts workflow manually

From GitHub Actions:

```text
Actions → DEV Deploy → Run workflow → branch: main
```

Recommended restriction:

- allow only `main`;
- do not allow arbitrary feature branches for DEV deployment initially.

Optional manual inputs:

| Input | Default | Purpose |
|---|---:|---|
| `run_backup` | `true` or `false` depending rollout phase | Whether to run `backup-db.sh --force` before deployment |
| `skip_server_git_update` | `false` | Whether to skip refreshing deployment scripts on the DEV server |
| `health_timeout_seconds` | `180` | Optional override passed as env var to `deploy-dev.sh` |
| `dry_run` | `false` | Later useful for testing SSH and script execution without changing containers |

For the first version, I would keep inputs minimal:

```text
run_backup: false
```

Then turn it on by default after the first successful workflow runs.

---

### Step 2 — Checkout repository

The workflow checks out the repository at `main`.

This gives GitHub Actions access to:

```text
deployment/docker/backend/Dockerfile
deployment/docker/frontend/Dockerfile
backend/
frontend/
```

---

### Step 3 — Login to Docker Hub

Use repository secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

The workflow should not use a personal Docker Hub password.

Use a Docker Hub access token with minimal scope required for image publishing.

---

### Step 4 — Build and push backend image

Build context should be repository root.

Backend Dockerfile:

```text
deployment/docker/backend/Dockerfile
```

Publish at least:

```text
dragisahub1984/barter-backend:latest
dragisahub1984/barter-backend:main-<git-sha>
```

Why keep `main-<git-sha>`?

Because even though DEV deploys `latest`, the SHA tag gives you traceability:

```text
latest      = current DEV pointer
main-sha    = immutable-ish build identifier for debugging
```

The currently documented strategy in `deployment/docs/DEV_DEPLOYMENT.md` already expects this shape.

---

### Step 5 — Build and push frontend image

Build context should also be repository root.

Frontend Dockerfile:

```text
deployment/docker/frontend/Dockerfile
```

Build argument should remain:

```text
VITE_API_BASE_URL=/api/v1
```

Publish at least:

```text
dragisahub1984/barter-frontend:latest
dragisahub1984/barter-frontend:main-<git-sha>
```

---

### Step 6 — SSH to DEV server

After both images are pushed successfully, the workflow SSHes to the DEV server.

The remote command should run from the server-side repository/deployment directory, for example:

```text
/opt/barter-platform
```

or whatever path is actually used on the DEV VM.

Recommended remote sequence:

```text
cd <DEV_DEPLOY_PATH>
```

Then optionally update server-side deployment files.

There are two acceptable models.

---

## Option A — Server has a checked-out repo

Recommended if the DEV server already has Git installed.

Remote sequence:

```text
git fetch origin main
git reset --hard origin/main
chmod +x deployment/scripts/*.sh
```

Then run the deployment script:

```text
./deployment/scripts/deploy-dev.sh
```

Benefits:

- server always uses the latest `deploy-dev.sh`, Compose file, Caddy config, and env examples;
- `deployment/env/dev.env` remains safe because it is ignored by Git;
- `deployment/state/` and `deployment/backups/` remain safe because they are ignored by Git.

Risk:

- `git reset --hard` will discard uncommitted tracked changes on the server.

Mitigation:

- server should not have manual edits to tracked files;
- all persistent secrets stay in ignored files like `deployment/env/dev.env`.

---

## Option B — GitHub Actions syncs only deployment assets

Recommended if you do not want the full repo checked out on the server.

The workflow would copy:

```text
deployment/compose/
deployment/docker/caddy/
deployment/scripts/
```

But it should **not** copy or overwrite:

```text
deployment/env/dev.env
deployment/state/
deployment/backups/
deployment/logs/
```

Benefits:

- less server dependency on Git;
- smaller server footprint.

Risk:

- more workflow complexity;
- more chance of accidentally overwriting runtime state if sync exclusions are wrong.

For this project, **Option A is cleaner** if the server can safely keep a Git checkout.

---

### Step 7 — Optional pre-deploy backup

Because `backup-db.sh` already exists, the workflow can optionally run:

```text
./deployment/scripts/backup-db.sh --force
```

This should be a workflow input, at least during rollout.

Recommended progression:

| Phase | Backup behavior |
|---|---|
| Initial testing | Manual input, default `false` |
| Stable DEV automation | Manual input, default `true` |
| Production later | Mandatory backup/preflight, not optional |

The backup script already uploads to Azure Blob Storage, so GitHub Actions should not handle database dump logic directly.

---

### Step 8 — Run existing DEV deployment script

The actual deployment should be exactly:

```text
./deployment/scripts/deploy-dev.sh
```

Optionally with env override:

```text
HEALTH_TIMEOUT_SECONDS=180 ./deployment/scripts/deploy-dev.sh
```

Do not inline these operations in the workflow:

```text
docker compose pull
docker compose up -d
docker compose ps
```

`deploy-dev.sh` already does those correctly and also captures the previous image state first.

---

### Step 9 — Deployment result

If `deploy-dev.sh` exits `0`, the workflow is successful.

If it exits non-zero, the workflow fails.

The failed workflow should direct the operator to:

```text
deployment/scripts/rollback-dev.sh
```

Rollback should not be automatic in the first version.

---

# 3. Required secrets

## GitHub repository secrets

### Docker Hub publishing

Required:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Purpose:

- authenticate to Docker Hub;
- push backend/frontend images.

Recommended:

- use a Docker Hub access token, not the account password;
- scope the token to the repositories if Docker Hub plan/features allow it;
- rotate periodically.

---

### DEV SSH access

Required:

```text
DEV_SSH_HOST
DEV_SSH_USER
DEV_SSH_PRIVATE_KEY
```

Recommended:

```text
DEV_SSH_PORT
DEV_SSH_KNOWN_HOSTS
DEV_DEPLOY_PATH
```

Suggested definitions:

| Secret / Variable | Type | Example | Purpose |
|---|---|---|---|
| `DEV_SSH_HOST` | secret or variable | `203.0.113.10` | DEV server hostname/IP |
| `DEV_SSH_USER` | secret or variable | `barter-deploy` | Linux deploy user |
| `DEV_SSH_PORT` | variable | `22` | SSH port |
| `DEV_SSH_PRIVATE_KEY` | secret | private key text | SSH identity used by GitHub Actions |
| `DEV_SSH_KNOWN_HOSTS` | secret | output of `ssh-keyscan` verified out-of-band | Prevent MITM attacks |
| `DEV_DEPLOY_PATH` | variable | `/opt/barter-platform` | Server repo/deployment path |

I would store these as:

## Secrets

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
DEV_SSH_PRIVATE_KEY
DEV_SSH_KNOWN_HOSTS
```

## Repository or environment variables

```text
DEV_SSH_HOST
DEV_SSH_PORT
DEV_SSH_USER
DEV_DEPLOY_PATH
```

If you prefer to hide all infrastructure details, store them all as secrets.

---

## Secrets that should **not** move to GitHub Actions

Keep these only on the DEV server in:

```text
deployment/env/dev.env
```

Do not put these in GitHub Actions unless there is a strong reason:

```text
POSTGRES_PASSWORD
DB_PASSWORD
JWT_SECRET
AZURE_STORAGE_CONNECTION_STRING_DEV
AZURE_STORAGE_CONTAINER_DEV
BACKUP_AZURE_CONNECTION_STRING
SMTP_PASSWORD
```

Reason:

GitHub Actions does not need runtime application secrets if deployment is delegated to the existing server scripts.

---

# 4. Changes needed in existing workflows/scripts

No files should be edited yet, but these are the exact files that would need to be created or modified later.

## Files to create

### 1. `.github/workflows/dev-deploy.yml`

Purpose:

- manual `workflow_dispatch`;
- build and push backend/frontend Docker images;
- SSH to DEV server;
- optionally update deployment files;
- optionally run backup;
- run `deployment/scripts/deploy-dev.sh`.

This is the main new workflow.

---

## Files to create or modify, depending current repo state

### 2. `.github/workflows/docker-publish.yml`

The workspace does **not** currently contain this file, but `deployment/docs/DEV_DEPLOYMENT.md` references it.

There are two paths.

---

### Path A — If the Docker publish workflow exists in another branch/upstream

Modify:

```text
.github/workflows/docker-publish.yml
```

Recommended changes:

- keep existing manual image publish behavior;
- add `workflow_call` support so `dev-deploy.yml` can reuse it;
- ensure it can publish both:
  - `latest`;
  - `main-<sha>`;
- optionally expose published image metadata as workflow outputs.

This avoids duplicating Docker build/push logic.

---

### Path B — If the Docker publish workflow truly does not exist

Create:

```text
.github/workflows/docker-publish.yml
```

Purpose:

- standalone image publishing workflow;
- triggered by:
  - `workflow_dispatch`;
  - optionally `push` to `main`;
  - optionally Git tags later;
- reusable by `dev-deploy.yml`.

Then create:

```text
.github/workflows/dev-deploy.yml
```

which calls the Docker publish workflow first, then deploys.

---

## Files likely unchanged

### `deployment/scripts/deploy-dev.sh`

No required change for the requested flow.

It already does the right things:

- validates env/Compose file;
- captures current deployment state;
- pulls configured images;
- runs Compose;
- waits for backend/frontend health;
- records public deployment metadata.

Potential future improvement only:

- add a `--print-current-images` or `--summary-json` option if you later want richer GitHub Actions summaries.

Not needed now.

---

### `deployment/scripts/rollback-dev.sh`

No required change.

It already supports:

- default rollback from `deployment/state/dev/latest.env`;
- explicit backend/frontend image overrides;
- health checks after rollback.

Potential future improvement only:

- add a workflow-dispatch rollback workflow later.

Do not add it in the first phase.

---

### `deployment/scripts/backup-db.sh`

No required change.

The DEV workflow can call it as-is:

```text
./deployment/scripts/backup-db.sh --force
```

Potential future improvement only:

- add structured output with backup blob name for GitHub summary.

Not required now.

---

### `deployment/compose/docker-compose.dev.yml`

No required change for DEV.

It already defaults to `latest`:

```text
dragisahub1984/barter-backend:latest
dragisahub1984/barter-frontend:latest
```

For future production, do **not** reuse this file blindly unless production configuration is separated and pinned to immutable image tags.

---

## Files to update for documentation later

### `deployment/docs/DEV_DEPLOYMENT.md`

Update after implementation to reflect:

- the new manual DEV deployment workflow;
- required GitHub secrets;
- SSH security model;
- rollback process;
- backup behavior.

### `README.md`

Optional short update:

- mention that DEV deployment is now manually triggered from GitHub Actions.

---

# 5. Production tagging strategy for later

Do not implement production deployment now.

But the image publishing strategy should be compatible with production from the start.

## Recommended tag policy

### DEV tags

For every successful `main` build:

```text
dragisahub1984/barter-backend:latest
dragisahub1984/barter-backend:main-<sha>

dragisahub1984/barter-frontend:latest
dragisahub1984/barter-frontend:main-<sha>
```

DEV deploys:

```text
latest
```

The SHA tag is retained for traceability and emergency rollback references.

---

### Production release tags

When you are ready for production, create release tags from `main`.

Recommended Git tag format:

```text
v1.0.0
```

Recommended Docker image tag format matching your examples:

```text
dragisahub1984/barter-backend:1.0.0
dragisahub1984/barter-frontend:1.0.0
```

Optionally also publish:

```text
dragisahub1984/barter-backend:v1.0.0
dragisahub1984/barter-frontend:v1.0.0
```

But I recommend choosing one canonical production tag format.

Given your examples, use this as canonical:

```text
1.0.0
```

---

## Production deployment rule

Production should pin exact versions in its environment file:

```text
BACKEND_IMAGE=dragisahub1984/barter-backend:1.0.0
FRONTEND_IMAGE=dragisahub1984/barter-frontend:1.0.0
```

Do not use:

```text
latest
latest-release
main-<sha>
```

for production runtime.

---

## Even safer production option

For high confidence, resolve version tags to immutable digests during release:

```text
BACKEND_IMAGE=dragisahub1984/barter-backend@sha256:<digest>
FRONTEND_IMAGE=dragisahub1984/barter-frontend@sha256:<digest>
```

This prevents accidental tag mutation from changing production behavior.

---

# 6. Rollback strategy

## DEV rollback should remain operator-triggered at first

Because DEV uses `latest`, rollback is important.

The current rollback design is already good:

```text
deployment/scripts/deploy-dev.sh
```

captures current backend/frontend image state before pulling new images.

Then:

```text
deployment/scripts/rollback-dev.sh
```

can restore the previously running backend/frontend images.

---

## Normal failed deployment response

If the GitHub Actions deployment fails during `deploy-dev.sh`:

1. Open the failed workflow log.
2. SSH to the DEV server.
3. Inspect service state:

```bash
cd <DEV_DEPLOY_PATH>
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
```

4. Inspect logs:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 backend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 frontend
```

5. Roll back:

```bash
./deployment/scripts/rollback-dev.sh
```

---

## Why not automatic rollback immediately?

I would not add automatic rollback in phase 1.

Reason:

- automatic rollback may hide failures;
- database migrations may have run;
- an image rollback may be unsafe if schema/data changed incompatibly;
- DEV should teach the team what common failures look like before automating recovery.

Add automatic rollback later only if:

- migrations are backward-compatible;
- rollback conditions are well understood;
- logs and state capture are reliable;
- the workflow clearly reports both original failure and rollback outcome.

---

## Database rollback

Do not automate database restore in GitHub Actions.

Current guidance should remain:

- image rollback is normal;
- database restore is exceptional;
- database restore must be manual and deliberate.

`restore-db.sh` should only be used for an explicit recovery event.

---

# 7. Risks

## 1. `latest` is mutable

DEV using `latest` is acceptable, but it means:

- the deployed image cannot be understood from the tag alone;
- two deploys of `latest` may represent different code;
- rollback depends on captured image IDs/digests/state.

Mitigation:

- also publish `main-<sha>` tags;
- have `deploy-dev.sh` capture state before pulling;
- include the Git SHA in the GitHub Actions summary;
- later consider having DEV deploy `main-<sha>` instead of `latest` while still keeping manual workflow dispatch.

---

## 2. Image publish succeeds but server deploy fails

Possible causes:

- server cannot pull from Docker Hub;
- Docker Hub rate limit/auth issue;
- disk full;
- container health failure;
- bad runtime env;
- DB migration issue;
- Caddy/HTTPS issue;
- Azure Blob config issue.

Mitigation:

- keep deploy script health checks;
- keep rollback script;
- optionally run `backup-db.sh --force`;
- show clear remote logs in workflow output;
- keep deployment logic centralized in server scripts.

---

## 3. SSH key compromise

If the GitHub Actions deploy key is compromised, an attacker may access the DEV server.

Mitigation:

- create a dedicated Linux user, for example `barter-deploy`;
- do not use `root`;
- disable password login for that user;
- restrict the SSH key to the minimum required server;
- pin host key with `DEV_SSH_KNOWN_HOSTS`;
- rotate the key periodically;
- use GitHub Environments with required reviewers for DEV if desired;
- keep server runtime secrets out of GitHub Actions.

Advanced hardening later:

- restrict `authorized_keys` with source IPs if GitHub-hosted runner IP handling is acceptable;
- use a self-hosted runner inside the trusted network;
- use a forced command wrapper, though that can complicate Git updates and rollback access.

---

## 4. Host key verification skipped

Using SSH with `StrictHostKeyChecking=no` is convenient but unsafe.

Mitigation:

- store the verified server host key in `DEV_SSH_KNOWN_HOSTS`;
- configure SSH to require host key validation;
- refresh the secret only after intentionally rotating the server host key.

---

## 5. Server deployment files drift from repo

If the server’s `deploy-dev.sh` or Compose file is stale, GitHub Actions may publish correct images but run old deployment logic.

Mitigation:

- have the remote deploy step run:

```bash
git fetch origin main
git reset --hard origin/main
```

before deployment, if the server stores a Git checkout.

Or explicitly sync deployment files.

---

## 6. `git reset --hard` could erase server edits

If someone manually edits tracked files on the server, `git reset --hard` will remove those edits.

Mitigation:

- do not manually edit tracked deployment files on the server;
- keep server-specific config only in ignored files:
  - `deployment/env/dev.env`;
  - `deployment/state/`;
  - `deployment/backups/`;
  - `deployment/logs/`.

---

## 7. Backup automation can block deployment

If the workflow runs `backup-db.sh --force` and Azure Blob upload fails, deployment will fail before updating images.

This is usually desirable for safer deployments, but it can be noisy during early rollout.

Mitigation:

- phase it in;
- start with optional backup input;
- once reliable, make backup default `true`.

---

## 8. Docker Hub rate limits or outages

The DEV server depends on Docker Hub availability during deployment.

Mitigation:

- authenticate server-side Docker pulls if images are private or rate limits appear;
- keep previous images locally for rollback;
- later consider GitHub Container Registry or Azure Container Registry.

---

## 9. Schema migration risk

If the backend image includes destructive or non-backward-compatible Flyway migrations, image rollback may not restore compatibility.

Mitigation:

- require backward-compatible migrations;
- backup before risky deployments;
- avoid destructive migrations in DEV/public-beta without a restore plan;
- document migration risk in pull requests.

---

# 8. Phased implementation plan

## Phase 0 — Confirm server assumptions

Before creating workflows, confirm:

- DEV server has a stable deployment path, for example:

```text
/opt/barter-platform
```

- that path contains the repository or deployment folder;
- `deployment/env/dev.env` exists on the server;
- `deploy-dev.sh` works manually from that path;
- `rollback-dev.sh` works manually or at least dry-runs;
- Docker can pull `dragisahub1984/barter-backend:latest`;
- Docker can pull `dragisahub1984/barter-frontend:latest`.

Deliverable:

```text
Document actual DEV_DEPLOY_PATH and SSH user.
```

---

## Phase 1 — Create image publish workflow foundation

Create or restore:

```text
.github/workflows/docker-publish.yml
```

Responsibilities:

- build backend image;
- build frontend image;
- push `latest`;
- push `main-<sha>`;
- support manual execution.

If the existing workflow exists elsewhere, modify it rather than replacing it.

Deliverable:

```text
Manual image publishing from main works.
```

Validation:

- Docker Hub shows new backend/frontend `latest`;
- Docker Hub shows new backend/frontend `main-<sha>` tags.

---

## Phase 2 — Create manual DEV deploy workflow

Create:

```text
.github/workflows/dev-deploy.yml
```

Responsibilities:

1. run manually with `workflow_dispatch`;
2. publish images first;
3. SSH to DEV server;
4. refresh deployment files from `main`;
5. run `deploy-dev.sh`.

Initial recommendation:

- make backup optional and default it to `false` for the first few test runs;
- do not implement automatic rollback yet.

Deliverable:

```text
One manual GitHub Actions workflow deploys DEV end-to-end.
```

Validation:

- workflow publishes images;
- workflow SSHes successfully;
- DEV server pulls new `latest` images;
- backend and frontend become healthy;
- public DEV URL works.

---

## Phase 3 — Add safer backup behavior

Once the SSH deploy is stable, enable existing backup integration.

Change workflow behavior:

```text
run_backup default: true
```

Remote flow becomes:

```text
./deployment/scripts/backup-db.sh --force
./deployment/scripts/deploy-dev.sh
```

Deliverable:

```text
Each normal DEV deployment creates an Azure Blob PostgreSQL backup first.
```

Validation:

- backup appears under configured Azure Blob prefix, currently expected around:

```text
postgres-backups/dev/postgres/
```

- deployment still succeeds after backup.

---

## Phase 4 — Improve workflow reporting

Add GitHub Actions summary output later.

Useful summary items:

- Git SHA deployed;
- backend image tags pushed;
- frontend image tags pushed;
- server path;
- whether backup ran;
- deploy script result;
- DEV URL;
- rollback command.

No deployment logic should move into Actions.

Deliverable:

```text
Operators can understand deployment result from the workflow summary.
```

---

## Phase 5 — Add manual rollback workflow, optional

Only after the main DEV deploy flow is stable, optionally create:

```text
.github/workflows/dev-rollback.yml
```

Trigger:

```text
workflow_dispatch
```

It would SSH to the server and run:

```text
./deployment/scripts/rollback-dev.sh
```

Optional inputs:

- backend image override;
- frontend image override;
- state file override;
- dry run.

Do not add automatic rollback before this manual rollback path is proven.

Deliverable:

```text
Manual rollback can be triggered from GitHub Actions.
```

---

## Phase 6 — Prepare production release tagging, but do not deploy production

Update image publishing to support release tags.

When Git tag is:

```text
v1.0.0
```

Publish:

```text
dragisahub1984/barter-backend:1.0.0
dragisahub1984/barter-frontend:1.0.0
```

Optionally also:

```text
dragisahub1984/barter-backend:v1.0.0
dragisahub1984/barter-frontend:v1.0.0
```

Deliverable:

```text
Immutable production-ready images exist, but no production deployment workflow exists yet.
```

---

# Exact files that would need to be created or modified later

## Create

```text
.github/workflows/dev-deploy.yml
```

## Create or modify

```text
.github/workflows/docker-publish.yml
```

Reason:

- referenced by docs but not present in this workspace;
- should either be restored/created or made reusable by `dev-deploy.yml`.

## Optionally create later

```text
.github/workflows/dev-rollback.yml
```

Not for phase 1.

## Modify docs after implementation

```text
deployment/docs/DEV_DEPLOYMENT.md
README.md
```

## Likely no changes required

```text
deployment/scripts/deploy-dev.sh
deployment/scripts/rollback-dev.sh
deployment/scripts/backup-db.sh
deployment/scripts/capture-deployment-state.sh
deployment/compose/docker-compose.dev.yml
```

---

# Final recommendation

Implement this as a **manual DEV deploy workflow that composes existing responsibilities instead of replacing them**:

```text
GitHub Actions:
  build images
  push images
  SSH to server
  run existing scripts

DEV server scripts:
  backup
  state capture
  image pull
  compose up
  health checks
  rollback
```

This gives you a clean bridge from the current manual process to CI/CD while preserving the most important operational boundary: deployment behavior remains in the existing, tested server scripts.

