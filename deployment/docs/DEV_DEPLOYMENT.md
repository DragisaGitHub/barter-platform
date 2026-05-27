# Barter Platform DEV Deployment

This deployment foundation runs Barter Platform on a small DEV server, such as an OCI Always Free VM, using separate Docker images for the Spring Boot backend and Vite frontend.

It intentionally does **not** add Kubernetes or server-side deployment automation yet. Docker image publishing is handled by `.github/workflows/docker-publish.yml`; DEV server deployment still uses `deployment/compose/docker-compose.dev.yml` and is started manually on the server.

## Runtime shape

- `caddy`: public reverse proxy on ports `80` and `443` with automatic Let's Encrypt HTTPS.
- `postgres`: PostgreSQL 16 with a persistent Docker volume.
- `backend`: Spring Boot `barter-web` app running with `SPRING_PROFILES_ACTIVE=dev` and constrained JVM memory.
- `frontend`: production Vite static build served by nginx.
- Browser traffic goes to Caddy on `https://barter-platform-dev.duckdns.org`.
- Caddy redirects HTTP to HTTPS, terminates TLS, and proxies `/api/*` plus `/actuator/health*` to the internal backend container.
- Caddy proxies all non-API traffic to the internal frontend nginx container, which serves the SPA.
- The frontend, backend, and PostgreSQL containers are not published directly to the internet by the provided Compose file.

## Security hardening guardrails

- The backend Docker image no longer defaults to `SPRING_PROFILES_ACTIVE=dev`.
- Public deployments must set `SPRING_PROFILES_ACTIVE=prod` explicitly.
- `deployment/env/prod.env.example` now documents the minimum production-safe variables.
- Swagger/OpenAPI must stay disabled in prod.
- The preferred browser deployment shape remains same-origin `/api/v1` proxying, with CORS allowlists only when a separate frontend origin is truly required.

### Security-header ownership

- **Backend API (`barter-web`)**: API responses set CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, and HSTS for secure requests.
- **Frontend nginx**: SPA/static responses set the frontend CSP plus `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, and `Permissions-Policy`.
- **Caddy**: public HTTPS edge owns HSTS and mirrors the other core headers as a safety net.

Keep these values aligned when changing browser-security policy.

### Why Caddy is used for DEV HTTPS

Mobile apps and browsers, including WhatsApp in-app browsers, increasingly require or strongly prefer HTTPS for opened links. DEV now uses a Dockerized Caddy reverse proxy to provide production-style HTTPS without a manual Certbot workflow.

Caddy automatically:

- requests Let's Encrypt certificates for `barter-platform-dev.duckdns.org` when DNS points to the VM and ports `80`/`443` are reachable;
- answers ACME HTTP-01 challenges on port `80`;
- redirects normal HTTP requests to HTTPS;
- renews certificates before expiry.

Certificate and ACME account state is persisted in Docker volumes:

- `caddy_data` mounted at `/data` stores certificates and ACME account material;
- `caddy_config` mounted at `/config` stores Caddy runtime configuration/state.

Do not delete these volumes unless you intentionally want Caddy to request fresh certificates and risk Let's Encrypt rate limits.

## Docker Hub images

- Backend: `dragisahub1984/barter-backend`
- Frontend: `dragisahub1984/barter-frontend`

Default Compose tags use `:latest`, but you can override them in `deployment/env/dev.env`:

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:latest
FRONTEND_IMAGE=dragisahub1984/barter-frontend:latest
CADDY_DOMAIN=barter-platform-dev.duckdns.org
```

## Docker image publishing CI/CD

GitHub Actions publishes Docker images to Docker Hub using `.github/workflows/docker-publish.yml`.

Required GitHub repository secrets:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

The workflow runs for:

- pushes to `main`
- Git tag pushes matching `v*`, for example `v1.0.0`, `v1.0.1`, `v1.1.0`
- manual `workflow_dispatch` runs from supported refs (`main` or `v*` tags)

It has separate jobs for backend and frontend image publishing and uses Docker Buildx, Docker Hub login, Docker metadata, and GitHub Actions layer cache.

### Main branch / DEV tags

On every push to `main`, the workflow pushes:

```text
dragisahub1984/barter-backend:latest
dragisahub1984/barter-backend:main-<full-git-sha>
dragisahub1984/barter-frontend:latest
dragisahub1984/barter-frontend:main-<full-git-sha>
```

`latest` is intended mainly for DEV deployment convenience. The DEV server Compose env defaults to these `latest` images, so `deployment/scripts/deploy-dev.sh` pulls the newest DEV images before restarting containers.

### Versioned release tags

When a Git tag like `v1.0.0` is pushed, the workflow pushes immutable release tags:

```text
dragisahub1984/barter-backend:v1.0.0
dragisahub1984/barter-backend:latest-release
dragisahub1984/barter-frontend:v1.0.0
dragisahub1984/barter-frontend:latest-release
```

`latest-release` is a convenience pointer for the newest release build. Future production deployments should pin explicit immutable tags such as `v1.0.0`; production should not deploy `latest`.

## Required server packages

On the OCI VM install:

- Docker Engine
- Docker Compose plugin (`docker compose`)
- `gzip`
- `cron`/`crontab`
- Optional: Azure CLI (`az`) on the host for direct blob operations. If it is not installed on the VM, `deployment/scripts/backup-db.sh` can fall back to the Docker image `mcr.microsoft.com/azure-cli` for backup upload.
- Git or another way to copy the `deployment/` folder to the server
- Optional: `ufw` or OCI security-list/network-security-group rules for firewall management

Typical Ubuntu setup outline:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
newgrp docker
docker version
docker compose version
```

## Required env file

Create the real DEV env file on the server:

```bash
cp deployment/env/dev.env.example deployment/env/dev.env
nano deployment/env/dev.env
```

Do not commit `deployment/env/dev.env`. It is ignored by `.gitignore`.

Minimum values to review and replace:

- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION_MINUTES` and `JWT_REFRESH_EXPIRATION_DAYS` if you need to override the documented defaults
- `AZURE_STORAGE_CONNECTION_STRING_DEV`
- `AZURE_STORAGE_CONTAINER_DEV`
- `AZURE_STORAGE_CONNECTION_STRING_PROD` / `AZURE_STORAGE_CONTAINER_PROD` in the future production env file, or the neutral aliases `AZURE_STORAGE_CONNECTION_STRING` / `AZURE_STORAGE_CONTAINER`
- `BACKUP_ENABLED=true`
- `BACKUP_FREQUENCY=monthly`
- `BACKUP_LOCAL_RETENTION_COUNT=2`
- `BACKUP_AZURE_CONTAINER=postgres-backups`
- `BACKUP_AZURE_PREFIX=dev/postgres`
- `BACKUP_AZURE_CONNECTION_STRING` or reuse `AZURE_STORAGE_CONNECTION_STRING_DEV`
- `CADDY_DOMAIN=barter-platform-dev.duckdns.org`
- `BARTER_EMAIL_VERIFICATION_ENABLED=false` for DEV-only registration/login bypass
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` if real email delivery is needed
- `BARTER_SECURITY_ALLOWED_ORIGINS` if DEV browser traffic should come from a separate origin instead of same-origin proxying
- `BARTER_SECURITY_ALLOWED_METHODS`, `BARTER_SECURITY_ALLOWED_HEADERS`, `BARTER_SECURITY_EXPOSED_HEADERS`, and `BARTER_SECURITY_ALLOW_CREDENTIALS` if you intentionally run cross-origin browser traffic
- `FRONTEND_ORIGIN` only if you still need the legacy single-origin alias

The supplied Caddy/nginx same-origin proxy means the browser calls `/api/v1`, so CORS should not be needed for the default DEV deployment path. Leave `BARTER_SECURITY_ALLOWED_ORIGINS` empty unless you intentionally expose the backend to a different browser origin.

For the current JWT bearer-token flow, `BARTER_SECURITY_ALLOW_CREDENTIALS=false` is the safer default because browser cookies are not required for auth.

### JWT runtime defaults by profile

- **DEV** default: `JWT_ACCESS_EXPIRATION_MINUTES=30`, `JWT_REFRESH_EXPIRATION_DAYS=14`
- **PROD** default: `JWT_ACCESS_EXPIRATION_MINUTES=15`, `JWT_REFRESH_EXPIRATION_DAYS=7`

`JWT_SECRET` must now be explicitly strong for deployed DEV/PROD environments. The backend fails fast for blank, too-short, or placeholder JWT secrets.

Example secret generators:

```bash
openssl rand -base64 48
```

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))
```

## Image storage strategy by profile

Uploaded item-image binaries and image metadata are intentionally split:

- **Binary image content** lives in the configured storage provider.
- **Image metadata** (`item_images` rows such as `uuid`, `storage_key`, `content_type`, `file_size`, ordering, and primary-image flags) stays in PostgreSQL.
- **Public serving** stays on the existing backend file endpoint: `GET /api/v1/files/**`.

This means Azure Blob containers can remain private. The app never needs to expose direct blob URLs for normal browser rendering.

### Local profile

- Profile: `local`
- Storage mode: `barter.storage.type=local`
- Binary storage location: local filesystem under `barter.storage.local.base-path` (defaults to `./uploads`)
- Use case: simple local development only

### DEV profile

- Profile: `dev`
- Storage mode: `barter.storage.type=azure`
- Binary storage location: Azure Blob Storage
- Current expected DEV container: `item-images-dev`
- Supported env variables:
  - preferred legacy DEV-specific names: `AZURE_STORAGE_CONNECTION_STRING_DEV`, `AZURE_STORAGE_CONTAINER_DEV`
  - optional neutral aliases: `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`

### PROD profile

- Profile: `prod`
- Storage mode: `barter.storage.type=azure`
- Binary storage location: Azure Blob Storage
- Recommended production container: `item-images-prod`
- Supported env variables:
  - preferred prod-scoped names: `AZURE_STORAGE_CONNECTION_STRING_PROD`, `AZURE_STORAGE_CONTAINER_PROD`
  - optional neutral aliases: `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`

### Required Azure container rules

- Create the container before first production-like use.
- Keep the container **private**; the backend is the supported serving path.
- Use separate containers for DEV and PROD.
- Recommended names:
  - DEV: `item-images-dev`
  - PROD: `item-images-prod`

The backend now fails startup clearly in `dev`/`prod` if the Azure connection string or container name is missing, blank, or malformed.

## DEV image upload verification checklist

Use this checklist after a fresh DEV deployment or when changing Azure storage settings.

1. Confirm the backend is healthy:

   ```bash
   docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
   curl -i https://barter-platform-dev.duckdns.org/actuator/health/readiness
   ```

2. Sign in to the DEV UI and upload a small JPEG/PNG/WebP image to a draft or active item.

3. Confirm the UI shows the image and the upload request returns `201 Created`.

4. Check backend logs for a successful Azure store operation:

   ```bash
   docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs --tail=200 backend | grep "Azure blob operation"
   ```

5. Confirm metadata landed in PostgreSQL:

   ```bash
   docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
     sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT uuid, storage_key, content_type, file_size FROM item_images ORDER BY id DESC LIMIT 5;"'
   ```

6. Copy one `storage_key` value from the query above and verify backend serving still works:

   ```bash
   curl -I "https://barter-platform-dev.duckdns.org/api/v1/files/<storage_key-from-db>"
   ```

7. Optional portal-side verification: in Azure Portal, open the DEV storage account and confirm the blob exists in the private `item-images-dev` container under the expected `items/<itemUuid>/<imageUuid>.<ext>` path.

## DEV observability and monitoring

Backend monitoring stays intentionally lightweight for launch.

- public-safe health endpoint: `GET /actuator/health`
- readiness endpoint: `GET /actuator/health/readiness`
- legacy smoke endpoint: `GET /api/v1/ping`
- every response includes `X-Correlation-Id`

Only health actuator endpoints are exposed publicly. They remain separate from `/api/v1` REST versioning. Non-health actuator endpoints remain protected.

Recommended safe DEV/public-beta checks:

- uptime check against `https://<your-domain>/actuator/health`
- container/runtime healthcheck against `http://backend:8080/actuator/health/readiness`
- backend log review for repeated `5xx`, startup failures, and suspicious auth/rate-limit spikes

For the full runbook, see `deployment/docs/OBSERVABILITY.md`.

## Email verification in DEV and PROD

The backend has a safety-first feature flag:

```env
BARTER_EMAIL_VERIFICATION_ENABLED=true
```

This maps to `barter.email-verification.enabled` and defaults to `true` in `application.yml`.

For DEV deployments, `application-dev.yml` sets this flag to `false`. With the DEV profile active:

- registration does not send a verification email
- new users are created as `ACTIVE` and `emailVerified=true`
- users can login immediately after registration
- SMTP/Resend configuration can remain present for other mail flows or future testing

For PROD/staging/non-DEV environments, keep `BARTER_EMAIL_VERIFICATION_ENABLED=true`. Production email delivery must use a configured SMTP/Resend sender whose domain is verified with the provider, otherwise registration verification messages may fail delivery and users will remain unable to login until their email is verified. The prod profile now fails fast if email verification is disabled or if SMTP host is missing.

## Frontend API base URL strategy

The frontend currently reads `import.meta.env.VITE_API_BASE_URL` in `frontend/src/api/axios.ts`. In Vite, this value is embedded at **build time**, not read dynamically at container runtime.

The frontend Dockerfile defaults to:

```env
VITE_API_BASE_URL=/api/v1
```

That is recommended for DEV because browser traffic stays same-origin through Caddy, `/api/*` is proxied to the internal `backend:8080` service, and infrastructure health checks are separately proxied on `/actuator/*`. If you need a different public API URL, rebuild the frontend image with a different build arg and push the new image.

## Build images locally

Run these from the repository root.

### Backend image

```bash
docker build -f deployment/docker/backend/Dockerfile -t dragisahub1984/barter-backend:latest .
```

The backend Dockerfile:

- uses Java 21, matching the Gradle toolchain
- builds `:barter-web:bootJar` with Gradle
- runs `/app/barter-web.jar`
- supports `JAVA_OPTS`, `SPRING_PROFILES_ACTIVE`, datasource vars, JWT, Azure Storage, Spring mail vars, and explicit security/CORS vars through environment variables
- resolves Azure image storage from either the DEV/prod-scoped variables or the neutral aliases documented above
- defaults JVM memory to `-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxMetaspaceSize=160m`

### Frontend image

```bash
docker build -f deployment/docker/frontend/Dockerfile -t dragisahub1984/barter-frontend:latest --build-arg VITE_API_BASE_URL=/api/v1 .
```

The frontend Dockerfile:

- installs dependencies with Yarn using `frontend/yarn.lock`
- runs `yarn build`
- serves the `dist` output with nginx
- does not run the Vite/Yarn dev server
- supports SPA fallback routing to `index.html`

## Push images manually

GitHub Actions is the preferred way to publish images. Manual pushes are still useful for local validation or emergency publishing.

```bash
docker login
docker push dragisahub1984/barter-backend:latest
docker push dragisahub1984/barter-frontend:latest
```

For safer deployments, prefer immutable tags, for example:

```bash
docker tag dragisahub1984/barter-backend:latest dragisahub1984/barter-backend:v1.0.0
docker tag dragisahub1984/barter-frontend:latest dragisahub1984/barter-frontend:v1.0.0
docker push dragisahub1984/barter-backend:v1.0.0
docker push dragisahub1984/barter-frontend:v1.0.0
```

Then set these tags in `deployment/env/dev.env`.

## Run on the OCI VM

Copy or pull the repo/deployment folder onto the VM, create `deployment/env/dev.env`, then run:

```bash
chmod +x deployment/scripts/deploy-dev.sh deployment/scripts/backup-db.sh
./deployment/scripts/deploy-dev.sh
```

Manual equivalent:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml pull
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml up -d --remove-orphans
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
```

By default, DEV pulls:

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:latest
FRONTEND_IMAGE=dragisahub1984/barter-frontend:latest
```

For future production-style testing, pin explicit immutable versions in the env file instead:

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:v1.0.0
FRONTEND_IMAGE=dragisahub1984/barter-frontend:v1.0.0
```

Open in a browser:

```text
https://barter-platform-dev.duckdns.org/
```

If this is the first run after enabling Caddy, watch the Caddy logs while it obtains the certificate:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f caddy
```

The DNS `A` record for `barter-platform-dev.duckdns.org` must point to the VM public IP before Let's Encrypt can issue a certificate.

## Open ports

At minimum:

- SSH: `22/tcp` from your admin IP range
- HTTP: `80/tcp` from the internet for Caddy redirects and Let's Encrypt HTTP-01 challenges
- HTTPS: `443/tcp` from the internet for the app

Do **not** expose PostgreSQL publicly. The provided Compose file does not publish PostgreSQL or backend ports to the host.

Remember that OCI has both in-VM firewall rules and VCN security lists/network security groups.

## DNS and HTTPS

For the DEV hostname:

1. Point `barter-platform-dev.duckdns.org` to the OCI VM public IP.
2. Ensure OCI ingress and any in-VM firewall allow `80/tcp` and `443/tcp`.
3. Start the DEV Compose stack. Caddy will request and renew certificates automatically.

No manual Certbot command or standalone certificate renewal job is required anymore. Caddy owns the public ports and stores certificate material in the `caddy_data` volume.

## Backups

Backups now focus on **PostgreSQL only**.

- Application data such as users, listings, offers, messages, reviews, and image metadata is stored in PostgreSQL and is covered by the database backup.
- Item image binaries already live in Azure Blob Storage and are **not** copied into local VM backups.
- PostgreSQL backups are created with `pg_dump`, compressed with gzip, uploaded off-server to Azure Blob Storage, and then trimmed locally to a very small retention count.
- DEV defaults use the Azure Blob container `postgres-backups` with blob prefix `dev/postgres/`.

### Manual backup command

Create and upload a compressed PostgreSQL backup immediately:

```bash
./deployment/scripts/backup-db.sh --force
```

Azure upload behavior stays config-compatible and now resolves the Azure CLI runner automatically:

1. Use host `az` when it exists on the VM.
2. Otherwise use Docker to run `mcr.microsoft.com/azure-cli`.

The script still reads `BACKUP_AZURE_CONNECTION_STRING`, falls back to `AZURE_STORAGE_CONNECTION_STRING_DEV` when needed, mounts the generated backup file into the Azure CLI container, and passes the storage connection string through an environment variable instead of requiring Azure CLI on the host.

What it does:

- creates a custom-format `pg_dump`
- compresses it to `*.dump.gz`
- uploads it to Azure Blob Storage
- keeps only the newest `BACKUP_LOCAL_RETENTION_COUNT` local backup files

Default local working directory:

```text
deployment/backups/postgres/
```

This directory is ignored by Git and should not be treated as permanent storage.

### Monthly DEV backup schedule

Set these values in `deployment/env/dev.env` for the current DEV/public-beta phase:

```env
BACKUP_ENABLED=true
BACKUP_FREQUENCY=monthly
BACKUP_SCHEDULE=
BACKUP_LOCAL_RETENTION_COUNT=2
BACKUP_AZURE_CONTAINER=postgres-backups
BACKUP_AZURE_PREFIX=dev/postgres
BACKUP_WORK_DIR=
BACKUP_AZURE_CONNECTION_STRING=
POSTGRES_SERVICE=postgres
```

Notes:

- Leave `BACKUP_SCHEDULE` empty to use the built-in monthly default (`0 4 1 * *`).
- Leave `BACKUP_WORK_DIR` empty to use the script default under `deployment/backups/postgres/`, or set an absolute path on the VM.
- Leave `BACKUP_AZURE_CONNECTION_STRING` empty only if you intentionally want to reuse `AZURE_STORAGE_CONNECTION_STRING_DEV`.
- No VM-level Azure CLI install is required for scheduled backups as long as Docker is available.
- Future production can switch to `BACKUP_FREQUENCY=daily` or set an explicit cron expression in `BACKUP_SCHEDULE` without changing backup script logic.

Install or refresh the monthly cron entry:

```bash
chmod +x deployment/scripts/backup-db.sh deployment/scripts/setup-backup-cron.sh deployment/scripts/restore-db.sh
./deployment/scripts/setup-backup-cron.sh
crontab -l | grep barter-platform-postgres-backup
```

Cron output is appended to:

```text
deployment/logs/backup-db.log
```

### Pre-deploy manual backup recommendation

Before any deployment that changes backend images, infrastructure, env values, or database migrations, run a manual backup first:

```bash
./deployment/scripts/backup-db.sh --force
```

Do this before restarting containers.

### Manual restore test procedure

Do **not** restore automatically into the live application database during normal verification. Test restores should go to a temporary database first.

The blob listing/download examples below use `az` directly for readability. You can run them either on a machine with host Azure CLI installed or by using the Docker image `mcr.microsoft.com/azure-cli` with the same storage connection string.

1. Pick the blob to test:

   ```bash
   BACKUP_CONNECTION_STRING='<use BACKUP_AZURE_CONNECTION_STRING or AZURE_STORAGE_CONNECTION_STRING_DEV>'
   az storage blob list \
     --connection-string "$BACKUP_CONNECTION_STRING" \
     --container-name postgres-backups \
     --prefix dev/postgres/ \
     --output table
   ```

2. Download the selected backup locally:

   ```bash
   mkdir -p deployment/backups/postgres
   az storage blob download \
     --connection-string "$BACKUP_CONNECTION_STRING" \
     --container-name postgres-backups \
     --name dev/postgres/<backup-file>.dump.gz \
     --file deployment/backups/postgres/<backup-file>.dump.gz \
     --no-progress
   ```

3. Restore into a fresh test database, not the live one:

   ```bash
   ./deployment/scripts/restore-db.sh \
     --file deployment/backups/postgres/<backup-file>.dump.gz \
     --target-db barter_restore_test \
     --recreate-target-db \
     --yes
   ```

4. Verify the restored database manually inside PostgreSQL:

   ```bash
   docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
     sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -d barter_restore_test -c "SELECT COUNT(*) FROM users;"'
   ```

5. Sanity-check that image references exist in the restored database, remembering the actual image files stay in Azure Blob Storage:

   ```bash
   docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
     sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -d barter_restore_test -c "SELECT id, storage_key FROM item_images ORDER BY id DESC LIMIT 10;"'
   ```

6. Record the restore test date and result before public launch.

### Live restore example

Only for an intentional recovery event, stop the backend first, then restore:

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

### Retention policy

- **Local VM retention:** keep only the newest `BACKUP_LOCAL_RETENTION_COUNT` compressed PostgreSQL backup files. DEV default is `2`.
- **Azure Blob retention:** keep monthly DEV PostgreSQL backups off-server in `postgres-backups/dev/postgres/`. Because this is monthly DEV backup cadence, keeping a longer Azure history is acceptable until a production lifecycle policy is defined.
- **Images:** no local image backup retention is required because item images already live in Azure Blob Storage and should not be duplicated onto the VM.

### Backup failure checklist

If a backup or restore fails, check the following in order:

1. `docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps` shows `postgres` healthy.
2. Either host `az` is available on the server, or Docker is available so the script can run `mcr.microsoft.com/azure-cli`.
3. `BACKUP_AZURE_CONNECTION_STRING` or `AZURE_STORAGE_CONNECTION_STRING_DEV` is valid.
4. The `postgres-backups` container exists and the prefix is correct: `dev/postgres/`.
5. `BACKUP_WORK_DIR` is writable and the VM has enough free disk for one compressed dump plus one in-progress file.
6. `deployment/logs/backup-db.log` or the interactive script output does not show authentication, permission, or network errors.
7. Manual blob listing works:

   ```bash
   BACKUP_CONNECTION_STRING='<use BACKUP_AZURE_CONNECTION_STRING or AZURE_STORAGE_CONNECTION_STRING_DEV>'
   az storage blob list \
     --connection-string "$BACKUP_CONNECTION_STRING" \
     --container-name postgres-backups \
     --prefix dev/postgres/ \
     --output table
   ```

For older manual restore syntax, replacing the dump path:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges' \
  < <(gzip -dc deployment/backups/postgres/barter-barter_db-YYYYMMDDTHHMMSSZ.dump.gz)
```

## Troubleshooting

Show service status:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
```

Follow logs:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f caddy
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f backend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f frontend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f postgres
```

Check backend health from the public route:

```bash
curl -i https://barter-platform-dev.duckdns.org/actuator/health
curl -i https://barter-platform-dev.duckdns.org/actuator/health/readiness
```

Common issues:

- **Frontend still calls localhost:8080**: rebuild the frontend image with `--build-arg VITE_API_BASE_URL=/api/v1`, then push and redeploy.
- **Caddy cannot obtain a certificate**: verify DuckDNS points to the VM public IP, OCI ingress allows `80/tcp` and `443/tcp`, no other host process is bound to those ports, and `docker compose ... logs caddy` does not show ACME rate-limit or DNS errors.
- **Backend cannot connect to DB**: ensure `DB_URL=jdbc:postgresql://postgres:5432/<POSTGRES_DB>` and `DB_PASSWORD` matches `POSTGRES_PASSWORD` for the initialized volume. If readiness returns `503`, inspect backend startup logs and confirm the `postgres` service is healthy. If you change Postgres init credentials after the first run, recreate the volume intentionally.
- **Azure image upload fails**: verify the active profile is `dev`, the backend started with `barter.storage.type=azure`, the relevant Azure variables are present (`AZURE_STORAGE_CONNECTION_STRING_DEV` / `AZURE_STORAGE_CONTAINER_DEV` or the neutral aliases), and the private `item-images-dev` container exists.
- **Backend fails immediately after startup with image-storage configuration errors**: check the backend logs for `barter.storage.azure.*` validation messages. Missing/blank/malformed Azure connection strings or container names now fail fast instead of silently falling back to local disk.
- **Image metadata exists but the file does not render**: fetch the `storage_key` from PostgreSQL and test `curl -I https://<your-domain>/api/v1/files/<storage_key>`; if that fails, inspect backend logs for `Azure blob operation failed` and confirm the blob exists in Azure.
- **Need to verify uploads are not staying on the VM**: remember DEV/prod do not mount a persistent image upload volume; image binaries are expected in Azure Blob Storage while only metadata stays in PostgreSQL.
- **SMTP does not send**: set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD`. If `SMTP_HOST` is empty, the backend may use its logging/fallback mail sender according to current app configuration.
- **Out of memory on 1GB VM**: lower `JAVA_OPTS` `-Xmx`, avoid running extra services, add swap if acceptable, and monitor `docker stats`.
- **Need to trace a failing request**: capture the `X-Correlation-Id` response header from the failing API call, then search `docker compose ... logs backend` for that exact value.

## Memory notes for 1GB OCI VM

The Compose file sets conservative memory limits:

- Postgres: `256m`
- Backend: `512m`
- Frontend nginx: `128m`

The backend defaults to:

```env
JAVA_OPTS=-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxMetaspaceSize=160m
```

If the VM is under pressure, first reduce backend `-Xmx` to `320m` or `256m`, then redeploy.

## Deployment automation boundary

Current CI/CD scope is image publishing only:

- build backend and frontend Docker images
- tag images for DEV (`latest`, `main-<full-git-sha>`)
- tag images for releases (`vX.Y.Z`, `latest-release`)
- push images to Docker Hub

The workflow does **not** SSH to the server, run `docker compose up`, or deploy to production. Keep runtime environment secrets on the server, not in GitHub Actions image builds and not in the repository.

Recommended next step, when ready, is a separate deployment workflow or release process that updates server env image tags and runs the existing manual pull/up commands. Production should pin explicit version tags such as `v1.0.0`, never `latest`, and should use `deployment/env/prod.env.example` as the starting point for its runtime configuration.

