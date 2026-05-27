# DEV Deployment Readiness Plan

Generated from the current repository files on 2026-05-27.

Source files reviewed:

- `deployment/compose/docker-compose.dev.yml`
- `deployment/env/dev.env.example`
- `deployment/env/prod.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`
- `deployment/scripts/deploy-dev.sh`
- `deployment/scripts/capture-deployment-state.sh`
- `deployment/scripts/rollback-dev.sh`
- `deployment/scripts/backup-db.sh`
- `deployment/scripts/setup-backup-cron.sh`
- `backend/barter-web/src/main/resources/application.yml`
- `backend/barter-web/src/main/resources/application-dev.yml`
- `deployment/docker/backend/Dockerfile`
- `deployment/docker/frontend/Dockerfile`
- `deployment/docker/frontend/nginx.conf`
- `deployment/docker/caddy/Caddyfile`
- `frontend/.env.example`
- `frontend/src/api/axios.ts`
- `frontend/src/api/adminOperationsApi.ts`

Repository status note: `git diff --name-only` and `git diff --stat` returned no uncommitted changes at the time this plan was generated, so there were no changed OpenAPI/generated files to compare in the working tree.

## 1. DEV deployment readiness summary

The DEV deployment is mostly ready for a manual Docker Compose deployment on a small public VM, assuming the server has Docker, Compose, DNS, firewall, Azure Storage containers, and a complete `deployment/env/dev.env` file configured.

Current runtime shape from `deployment/compose/docker-compose.dev.yml` and `deployment/docs/DEV_DEPLOYMENT.md`:

- `caddy` is the only public ingress container and publishes ports `80` and `443`.
- `caddy` terminates HTTPS for `CADDY_DOMAIN`, redirects HTTP to HTTPS, proxies `/api/*` and `/actuator/health*` to `backend:8080`, and proxies all other traffic to `frontend:80`.
- `postgres` uses `postgres:16-alpine` with persistent Docker volume `barter_postgres_data`.
- `backend` runs the Spring Boot `barter-web` jar on port `8080`, with the `dev` profile by default.
- `frontend` serves the Vite SPA from nginx and exposes only internal port `80` to the Compose network.
- Browser API traffic should be same-origin through `/api/v1`; `VITE_API_BASE_URL` is embedded at frontend image build time, not runtime.
- DEV image upload storage is Azure Blob Storage through the backend file endpoint; blob containers should remain private.
- PostgreSQL backups are script-driven with `pg_dump`, gzip compression, Azure Blob upload, and local retention trimming.
- Rollback is image-based for `backend` and `frontend` only; it does not restore PostgreSQL, delete volumes, or touch Azure Blob item images.

Readiness status:

| Area | Status | Notes |
|---|---:|---|
| Compose topology | Ready | Services, internal network, health checks, and public ingress are defined. |
| HTTPS/DNS | Ready if server DNS/firewall are correct | `CADDY_DOMAIN` must point to the VM public IP; ports `80` and `443` must be reachable. |
| Backend profile | Ready for DEV | Compose defaults `SPRING_PROFILES_ACTIVE=dev`. Do not use this env for production. |
| Frontend API routing | Ready if image was built with `/api/v1` | Dockerfile defaults `ARG VITE_API_BASE_URL=/api/v1`; runtime env changes will not alter an already-built frontend image. |
| Required secrets | Operator action required | Replace all placeholder passwords/secrets/storage connection strings in `deployment/env/dev.env`. |
| Azure item-image storage | Operator action required | Create private `item-images-dev` container and provide valid `AZURE_STORAGE_CONNECTION_STRING_DEV` / `AZURE_STORAGE_CONTAINER_DEV` or neutral aliases. |
| Backups | Operator action required | Create backup container, set backup connection string or reuse DEV storage connection string, then install cron if desired. |
| Rollback | Ready after first state capture | `deploy-dev.sh` captures state automatically before pull; manual capture is recommended before risky deploys. |
| Repo changes before deploy | None required from current diff | No uncommitted changes were present when checked. |

## 2. New/changed DEV environment variable table

Use `deployment/env/dev.env.example` as the authoritative starting point for the DEV server file `deployment/env/dev.env`. Do not commit the real file.

### Required or must-review runtime variables

| Variable | Required? | Current DEV example/default | Used by | Action |
|---|---:|---|---|---|
| `BACKEND_IMAGE` | Recommended | `dragisahub1984/barter-backend:latest` | Compose backend image | Keep `latest` for simple DEV, or pin immutable tag/digest for safer deploy/rollback testing. |
| `FRONTEND_IMAGE` | Recommended | `dragisahub1984/barter-frontend:latest` | Compose frontend image | Keep `latest` for simple DEV, or pin immutable tag/digest for safer deploy/rollback testing. |
| `CADDY_DOMAIN` | Yes for public HTTPS | `barter-platform-dev.duckdns.org` | Compose + Caddy | Must resolve to the DEV VM public IP before Caddy can obtain a certificate. |
| `POSTGRES_DB` | Yes | `barter_db` | Postgres, backup scripts | Keep consistent with `DB_URL` and backup commands. |
| `POSTGRES_USER` | Yes | `barter_user` | Postgres, backup scripts | Keep consistent with `DB_USERNAME`. |
| `POSTGRES_PASSWORD` | Yes | placeholder | Postgres | Replace with strong secret. If changed after Postgres volume initialization, existing volume credentials will not automatically change. |
| `DB_URL` | Yes | `jdbc:postgresql://postgres:5432/barter_db` | Backend datasource | Keep host as `postgres` inside Compose. |
| `DB_USERNAME` | Yes | `barter_user` | Backend datasource | Usually match `POSTGRES_USER`. |
| `DB_PASSWORD` | Yes | placeholder | Backend datasource | Usually match `POSTGRES_PASSWORD`. Compose fails if missing. |
| `SPRING_PROFILES_ACTIVE` | Yes | `dev` | Backend | Must be `dev` for this DEV deployment. Never use `dev` for prod. |
| `JAVA_OPTS` | Recommended | `-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxMetaspaceSize=160m` | Backend JVM | Suitable for small VM; lower `-Xmx` if memory pressure occurs. |
| `JWT_SECRET` | Yes | placeholder | Backend auth | Replace with long random value, at least 32 characters/bytes. Compose fails if missing; backend fails fast for blank/weak placeholder values. |
| `JWT_ACCESS_EXPIRATION_MINUTES` | Review | `30` in DEV | Backend JWT | DEV default is 30 minutes; production example uses 15. |
| `JWT_REFRESH_EXPIRATION_DAYS` | Review | `14` in DEV | Backend JWT | DEV default is 14 days; production example uses 7. |
| `AZURE_STORAGE_CONNECTION_STRING_DEV` | Yes for DEV image uploads | placeholder | Backend DEV storage, backup fallback | Provide valid Azure Storage connection string. Backend DEV resolves this before neutral alias. |
| `AZURE_STORAGE_CONTAINER_DEV` | Yes for DEV image uploads | `item-images-dev` | Backend DEV storage | Ensure this private blob container exists before image upload testing. |
| `AZURE_STORAGE_CONNECTION_STRING` | Optional alias | commented | Backend storage | Optional neutral alias if not using DEV-specific name. |
| `AZURE_STORAGE_CONTAINER` | Optional alias | commented | Backend storage | Optional neutral alias if not using DEV-specific name. |
| `BARTER_EMAIL_VERIFICATION_ENABLED` | Review | `false` | Backend registration/login | DEV disables email verification. Keep true outside DEV. |
| `SMTP_HOST` | Optional in DEV | empty | Compose maps to `SPRING_MAIL_HOST` | Leave empty for logging/fallback sender if supported; set for real email delivery. |
| `SMTP_PORT` | Optional | `587` | Compose maps to `SPRING_MAIL_PORT` | Set with SMTP provider details if used. |
| `SMTP_USERNAME` | Optional | empty | Compose maps to `SPRING_MAIL_USERNAME` | Set with SMTP provider details if used. |
| `SMTP_PASSWORD` | Optional | empty | Compose maps to `SPRING_MAIL_PASSWORD` | Set with SMTP provider details if used. |
| `MAIL_FROM` | Recommended | `noreply@barter-platform.dev` | Backend mail | Use a sender domain appropriate for the environment/provider. |
| `BARTER_SECURITY_ALLOWED_ORIGINS` | Usually empty | empty | Backend CORS | Leave empty for same-origin Caddy/nginx deployment. Set explicit allowlist only for separate browser origin. |
| `BARTER_SECURITY_ALLOWED_METHODS` | Review | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Backend CORS | Keep unless API method policy changes. |
| `BARTER_SECURITY_ALLOWED_HEADERS` | Review | `Accept,Authorization,Content-Type,Origin,X-Correlation-Id,X-Request-Id,X-Requested-With` | Backend CORS | Keep unless client headers change. |
| `BARTER_SECURITY_EXPOSED_HEADERS` | Review | `X-Correlation-Id` | Backend CORS | Keep to expose request tracing header to browser clients. |
| `BARTER_SECURITY_ALLOW_CREDENTIALS` | Review | `false` | Backend CORS | Correct for bearer-token auth without cookie credentials. |
| `BARTER_SECURITY_CORS_MAX_AGE` | Review | `1h` | Backend CORS | Keep unless CORS preflight caching policy changes. |
| `FRONTEND_ORIGIN` | Legacy/backward-compatible | `https://barter-platform-dev.duckdns.org` | Backend CORS alias | Same-origin deployment should not need CORS; keep only as backward-compatible single-origin alias. |
| `VITE_API_BASE_URL` | Build-time only | `/api/v1` in deployment env example; frontend local example uses localhost | Frontend build | Runtime changes in `dev.env` do not change a built image. Rebuild frontend image if this must change. |

### Backup variables

| Variable | Required? | DEV example/default | Used by | Action |
|---|---:|---|---|---|
| `BACKUP_ENABLED` | Recommended | `true` | `backup-db.sh`, `setup-backup-cron.sh` | Keep true for public DEV unless intentionally disabling backups. |
| `BACKUP_FREQUENCY` | Recommended | `monthly` | `setup-backup-cron.sh` | Defaults: monthly `0 4 1 * *`, weekly `0 4 * * 0`, daily `0 3 * * *`. |
| `BACKUP_SCHEDULE` | Optional | empty | `setup-backup-cron.sh` | Set explicit cron expression only if not using frequency defaults. |
| `BACKUP_LOCAL_RETENTION_COUNT` | Recommended | `2` | `backup-db.sh` | Must be a non-negative integer. |
| `BACKUP_AZURE_CONTAINER` | Yes if backups enabled | `postgres-backups` | `backup-db.sh` | Ensure container exists. |
| `BACKUP_AZURE_PREFIX` | Recommended | `dev/postgres` | `backup-db.sh` | Stored blob path prefix; leading/trailing slashes are normalized. |
| `BACKUP_WORK_DIR` | Optional | empty | `backup-db.sh` | Empty uses `deployment/backups/postgres`; relative paths resolve from repo root. |
| `BACKUP_AZURE_CONNECTION_STRING` | Required unless reusing DEV storage string | empty | `backup-db.sh` | If empty, script falls back to `AZURE_STORAGE_CONNECTION_STRING_DEV`. |
| `POSTGRES_SERVICE` | Recommended | `postgres` | backup/restore scripts | Keep as Compose service name. |

### Script override variables

These are not normally stored in `deployment/env/dev.env`, but can be exported for operator control:

| Variable | Default | Used by | Purpose |
|---|---|---|---|
| `ENV_FILE` | `deployment/env/dev.env` | deploy/capture/rollback/backup/restore/cron scripts | Use a non-default env file path. |
| `COMPOSE_FILE` | `deployment/compose/docker-compose.dev.yml` for most scripts except `deploy-dev.sh` has fixed default | capture/rollback/backup/restore scripts | Override Compose file path. |
| `HEALTH_TIMEOUT_SECONDS` | `180` | deploy/rollback scripts | Increase if backend/frontend health checks need more time. |
| `STATE_DIR` | `deployment/state/dev` | capture script | Override state capture directory. |
| `STATE_FILE` | `deployment/state/dev/latest.env` for rollback | capture/rollback scripts | Use explicit capture or rollback state file. |

### Backend config variables present in `application.yml` but not all listed in `dev.env.example`

The following have safe defaults in `application.yml` and do not require DEV values unless intentionally tuning behavior:

- `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`
- `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`
- `BARTER_SWAGGER_ENABLED`
- `BARTER_VERIFICATION_CODE_EXPIRATION_MINUTES`
- `BARTER_RATE_LIMITS_ENABLED`
- `BARTER_RATE_LIMITS_CLIENT_IP_HEADER`
- `BARTER_RATE_LIMITS_LOGIN_LIMIT`
- `BARTER_RATE_LIMITS_LOGIN_WINDOW`
- `BARTER_RATE_LIMITS_REGISTER_LIMIT`
- `BARTER_RATE_LIMITS_REGISTER_WINDOW`
- `BARTER_RATE_LIMITS_REFRESH_TOKEN_LIMIT`
- `BARTER_RATE_LIMITS_REFRESH_TOKEN_WINDOW`
- `BARTER_RATE_LIMITS_FORGOT_PASSWORD_LIMIT`
- `BARTER_RATE_LIMITS_FORGOT_PASSWORD_WINDOW`
- `BARTER_RATE_LIMITS_RESET_PASSWORD_LIMIT`
- `BARTER_RATE_LIMITS_RESET_PASSWORD_WINDOW`
- `BARTER_RATE_LIMITS_RESEND_VERIFICATION_CODE_LIMIT`
- `BARTER_RATE_LIMITS_RESEND_VERIFICATION_CODE_WINDOW`
- `BARTER_RATE_LIMITS_IMAGE_UPLOAD_LIMIT`
- `BARTER_RATE_LIMITS_IMAGE_UPLOAD_WINDOW`
- `BARTER_RATE_LIMITS_TRADE_OFFER_CREATE_LIMIT`
- `BARTER_RATE_LIMITS_TRADE_OFFER_CREATE_WINDOW`
- `BARTER_RATE_LIMITS_TRADE_MESSAGE_SEND_LIMIT`
- `BARTER_RATE_LIMITS_TRADE_MESSAGE_SEND_WINDOW`
- `BARTER_RATE_LIMITS_FAVORITE_MUTATION_LIMIT`
- `BARTER_RATE_LIMITS_FAVORITE_MUTATION_WINDOW`
- `BARTER_BOOTSTRAP_ADMIN_ENABLED`
- `BARTER_BOOTSTRAP_ADMIN_USERNAME`
- `BARTER_BOOTSTRAP_ADMIN_EMAIL`
- `BARTER_BOOTSTRAP_ADMIN_PASSWORD`
- `BARTER_STORAGE_TYPE`
- `BARTER_STORAGE_BASE_PATH`
- `BARTER_STORAGE_MAX_IMAGES_PER_ITEM`
- `BARTER_STORAGE_MAX_IMAGE_SIZE_BYTES`

## 3. Pre-deploy server checklist

Run these checks on the DEV VM before deployment.

### Host prerequisites

- Docker Engine installed and running.
- Docker Compose plugin available as `docker compose`.
- `gzip` installed for backups.
- `cron` / `crontab` installed if scheduled backups are desired.
- Host Azure CLI `az` is optional; if absent, `backup-db.sh` can use Docker image `mcr.microsoft.com/azure-cli`.
- Git or another reliable way to place the repository/deployment folder on the server.
- Enough disk space for Docker images, Postgres volume, Caddy cert volume, and at least one compressed PostgreSQL dump plus temporary partial file.

```bash
docker version
docker compose version
gzip --version
crontab -l >/dev/null || true
```

### Network/DNS prerequisites

- `CADDY_DOMAIN` DNS `A` record points to the DEV VM public IP.
- Cloud security rules allow:
  - `22/tcp` from admin IP range only.
  - `80/tcp` from the internet for HTTP redirect and Let's Encrypt HTTP-01.
  - `443/tcp` from the internet for HTTPS application traffic.
- In-VM firewall allows ports `80` and `443`.
- No other host process is bound to ports `80` or `443`.
- PostgreSQL and backend ports are not exposed publicly.

```bash
sudo ss -ltnp | grep -E ':80|:443' || true
```

### Azure Storage prerequisites

- Create a private blob container for item images, expected DEV name: `item-images-dev`.
- Create a blob container for PostgreSQL backups, expected name: `postgres-backups`.
- Confirm the storage connection string in `AZURE_STORAGE_CONNECTION_STRING_DEV` can access `item-images-dev` and, if reused for backups, `postgres-backups`.
- If using a dedicated backup account/string, set `BACKUP_AZURE_CONNECTION_STRING`.

### Env file prerequisites

```bash
cp deployment/env/dev.env.example deployment/env/dev.env
nano deployment/env/dev.env
chmod 600 deployment/env/dev.env
```

Minimum replacements:

- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `JWT_SECRET`
- `AZURE_STORAGE_CONNECTION_STRING_DEV`
- `AZURE_STORAGE_CONTAINER_DEV`
- `BACKUP_AZURE_CONNECTION_STRING` if not reusing `AZURE_STORAGE_CONNECTION_STRING_DEV`
- `CADDY_DOMAIN` if the DEV hostname differs
- `SMTP_*` only if real email delivery is required in DEV

Secret generation example:

```bash
openssl rand -base64 48
```

### Preflight commands

```bash
chmod +x deployment/scripts/deploy-dev.sh \
  deployment/scripts/capture-deployment-state.sh \
  deployment/scripts/rollback-dev.sh \
  deployment/scripts/backup-db.sh \
  deployment/scripts/restore-db.sh \
  deployment/scripts/setup-backup-cron.sh

./deployment/scripts/deploy-dev.sh --dry-run

docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml config >/tmp/barter-compose.rendered.yml
```

If a stack is already running and healthy, capture state before changing anything:

```bash
./deployment/scripts/capture-deployment-state.sh
```

Before any deploy that may include database migrations or risky config/image changes, create a fresh PostgreSQL backup:

```bash
./deployment/scripts/backup-db.sh --force
```

## 4. Exact command sequence

### First-time or normal DEV deployment

Run from the repository root on the DEV server:

```bash
chmod +x deployment/scripts/deploy-dev.sh \
  deployment/scripts/capture-deployment-state.sh \
  deployment/scripts/rollback-dev.sh \
  deployment/scripts/backup-db.sh \
  deployment/scripts/restore-db.sh \
  deployment/scripts/setup-backup-cron.sh

cp -n deployment/env/dev.env.example deployment/env/dev.env
nano deployment/env/dev.env
chmod 600 deployment/env/dev.env

docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml config >/tmp/barter-compose.rendered.yml

./deployment/scripts/deploy-dev.sh
```

What `deploy-dev.sh` does:

1. Validates that the env file, Compose file, and Docker are available.
2. Captures currently running backend/frontend image state unless `--skip-state-capture` is passed.
3. Pulls configured images unless `--skip-pull` is passed.
4. Runs `docker compose up -d --remove-orphans`.
5. Waits for backend and frontend health checks.
6. Prints service status and useful log commands.

### Optional monthly backup cron setup

```bash
./deployment/scripts/setup-backup-cron.sh
crontab -l | grep barter-platform-postgres-backup
```

Cron output is written to:

```text
deployment/logs/backup-db.log
```

### Useful manual equivalent

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml pull
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml up -d --remove-orphans
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
```

## 5. Smoke test checklist

Run after deployment completes.

### Container and health checks

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
curl -i https://barter-platform-dev.duckdns.org/actuator/health
curl -i https://barter-platform-dev.duckdns.org/actuator/health/readiness
curl -i https://barter-platform-dev.duckdns.org/
```

Expected:

- `backend` and `frontend` containers report healthy.
- Public health/readiness endpoints return successful HTTP responses.
- Root page returns the SPA over HTTPS.
- Caddy obtains or reuses a valid Let's Encrypt certificate.

### Logs

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 caddy
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 backend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 frontend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 postgres
```

Check for:

- Caddy ACME/DNS/certificate errors.
- Backend startup failures.
- Flyway migration failures.
- Database connectivity errors.
- Azure storage validation errors.
- Repeated `5xx`, auth, rate-limit, or correlation-id errors.

### Browser/API checks

- Open `https://barter-platform-dev.duckdns.org/`.
- Register a DEV account.
- Confirm DEV registration/login works with `BARTER_EMAIL_VERIFICATION_ENABLED=false`.
- Login and load authenticated pages.
- Confirm frontend requests go to same-origin `/api/v1`, not `localhost:8080`.
- Confirm admin operations API route exists if admin UI calls it: `/api/v1/admin/operations/overview` through `frontend/src/api/adminOperationsApi.ts`.

### Image upload / Azure Blob checks

1. Upload a small JPEG/PNG/WebP item image through the UI.
2. Confirm upload returns `201 Created` and the UI renders the image.
3. Check backend Azure operation logs:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 backend | grep "Azure blob operation" || true
```

4. Confirm image metadata exists in PostgreSQL:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT uuid, storage_key, content_type, file_size FROM item_images ORDER BY id DESC LIMIT 5;"'
```

5. Verify file serving through the backend path:

```bash
curl -I "https://barter-platform-dev.duckdns.org/api/v1/files/<storage_key-from-db>"
```

6. Optional Azure Portal check: confirm the blob exists in the private `item-images-dev` container under the expected key path.

### Backup check

```bash
./deployment/scripts/backup-db.sh --force
```

Expected:

- A compressed `*.dump.gz` file is created temporarily/locally according to retention.
- Blob upload completes to `BACKUP_AZURE_CONTAINER` under `BACKUP_AZURE_PREFIX`.
- Local retention leaves at most `BACKUP_LOCAL_RETENTION_COUNT` matching files.

## 6. Rollback checklist

Rollback is image-based for `backend` and `frontend` only. It does not roll back database migrations, business data, Docker volumes, Caddy certificate state, or Azure Blob item images.

### Confirm rollback state exists

```bash
ls -la deployment/state/dev/
cat deployment/state/dev/latest.env
```

If no state exists but a known-good image is known, pass explicit image refs to the rollback script.

### Dry run rollback

```bash
./deployment/scripts/rollback-dev.sh --dry-run
```

### Normal rollback to latest captured images

```bash
./deployment/scripts/rollback-dev.sh
```

The script:

1. Sources `deployment/state/dev/latest.env` by default.
2. Resolves `BACKEND_ROLLBACK_IMAGE` and/or `FRONTEND_ROLLBACK_IMAGE`.
3. Pulls missing rollback images unless `--skip-pull` is passed.
4. Recreates `backend` first with `--no-deps --force-recreate` and waits for health.
5. Recreates `frontend` next with `--no-deps --force-recreate` and waits for health.
6. Leaves `postgres`, `caddy`, Docker volumes, and Azure Blob item images unchanged.

### Explicit image rollback

```bash
./deployment/scripts/rollback-dev.sh \
  --backend-image dragisahub1984/barter-backend@sha256:<digest> \
  --frontend-image dragisahub1984/barter-frontend@sha256:<digest>
```

Or roll back one service only:

```bash
./deployment/scripts/rollback-dev.sh --backend-image dragisahub1984/barter-backend:v1.0.0
```

### Post-rollback validation

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
curl -i https://barter-platform-dev.duckdns.org/actuator/health/readiness
curl -i https://barter-platform-dev.duckdns.org/
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 backend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 frontend
```

### When image rollback is not enough

Do not rely only on image rollback when:

- A destructive or non-backward-compatible database migration ran.
- Bad writes changed business data in a way older code cannot read safely.
- The incident is caused by secrets, DNS, TLS, storage-account permissions, or host/network infrastructure.
- The rollback target image has a known security issue.

Database restore is a separate manual recovery path. Prefer restoring to a temporary database first:

```bash
./deployment/scripts/restore-db.sh \
  --file deployment/backups/postgres/<backup-file>.dump.gz \
  --target-db barter_restore_test \
  --recreate-target-db \
  --yes
```

Only restore into the live DB after an explicit operator decision and after stopping the backend:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml stop backend
./deployment/scripts/restore-db.sh \
  --file deployment/backups/postgres/<backup-file>.dump.gz \
  --target-db barter_db \
  --recreate-target-db \
  --allow-primary-db \
  --yes
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml start backend
```

## 7. Required repo changes before deploy

No required repository changes were found from the current working tree inspection.

Recommended operator actions before deploying:

- Confirm GitHub Actions has published the intended backend/frontend images to Docker Hub.
- For safer public DEV testing, consider setting immutable image tags or digests in `deployment/env/dev.env` instead of `:latest`.
- Confirm the frontend image was built with `VITE_API_BASE_URL=/api/v1`; otherwise the browser may call `localhost:8080` or another stale API URL.
- Keep `BARTER_SECURITY_ALLOWED_ORIGINS` empty for same-origin Caddy/nginx deployment unless a separate frontend origin is intentionally used.
- Keep `BARTER_SECURITY_ALLOW_CREDENTIALS=false` for current bearer-token auth.
- Confirm `deployment/env/dev.env` is not committed and remains protected on the server.
- Run `./deployment/scripts/deploy-dev.sh --dry-run` and `docker compose ... config` before the real deploy.
- Run `./deployment/scripts/backup-db.sh --force` before risky deployments.

If the next refinement pass should convert this manual DEV process into CI/CD, the likely follow-up plan is:

1. Add a deployment workflow that SSHes to the DEV host or uses a safer deployment runner.
2. Keep server runtime secrets out of GitHub Actions image-build jobs.
3. Update server-side image tags/digests explicitly.
4. Run the existing `deploy-dev.sh` flow remotely.
5. Preserve the current state-capture, backup, health-wait, and rollback behaviors.

