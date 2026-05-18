# Barter Platform DEV Deployment

This deployment foundation runs Barter Platform on a small DEV server, such as an OCI Always Free VM, using separate Docker images for the Spring Boot backend and Vite frontend.

It intentionally does **not** add Kubernetes or server-side deployment automation yet. Docker image publishing is handled by `.github/workflows/docker-publish.yml`; DEV server deployment still uses `deployment/compose/docker-compose.dev.yml` and is started manually on the server.

## Runtime shape

- `postgres`: PostgreSQL 16 with a persistent Docker volume.
- `backend`: Spring Boot `barter-web` app running with `SPRING_PROFILES_ACTIVE=dev` and constrained JVM memory.
- `frontend`: production Vite static build served by nginx.
- Browser traffic goes to nginx on port `80`.
- nginx serves the SPA and proxies `/api/v1/*` to the internal backend container.
- The backend is not published directly to the internet by the provided Compose file.

## Docker Hub images

- Backend: `dragisahub1984/barter-backend`
- Frontend: `dragisahub1984/barter-frontend`

Default Compose tags use `:latest`, but you can override them in `deployment/env/dev.env`:

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:latest
FRONTEND_IMAGE=dragisahub1984/barter-frontend:latest
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
- `AZURE_STORAGE_CONNECTION_STRING_DEV`
- `AZURE_STORAGE_CONTAINER_DEV`
- `BARTER_EMAIL_VERIFICATION_ENABLED=false` for DEV-only registration/login bypass
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` if real email delivery is needed
- `FRONTEND_ORIGIN` for documentation/future explicit CORS support

The supplied nginx same-origin proxy means the browser calls `/api/v1`, so CORS should not be needed for the default DEV deployment path.

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

For PROD/staging/non-DEV environments, keep `BARTER_EMAIL_VERIFICATION_ENABLED=true`. Production email delivery must use a configured SMTP/Resend sender whose domain is verified with the provider, otherwise registration verification messages may fail delivery and users will remain unable to login until their email is verified.

## Frontend API base URL strategy

The frontend currently reads `import.meta.env.VITE_API_BASE_URL` in `frontend/src/api/axios.ts`. In Vite, this value is embedded at **build time**, not read dynamically at container runtime.

The frontend Dockerfile defaults to:

```env
VITE_API_BASE_URL=/api/v1
```

That is recommended for DEV because nginx proxies `/api/v1/*` to the internal `backend:8080` service. If you need a different public API URL, rebuild the frontend image with a different build arg and push the new image.

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
- supports `JAVA_OPTS`, `SPRING_PROFILES_ACTIVE`, datasource vars, JWT, Azure Storage, and Spring mail vars through environment variables
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
http://<oci-vm-public-ip>/
```

## Open ports

At minimum:

- SSH: `22/tcp` from your admin IP range
- HTTP: `80/tcp` from the internet or your test IP range

Optional later:

- HTTPS: `443/tcp` after adding a TLS reverse proxy or certificate automation

Do **not** expose PostgreSQL publicly. The provided Compose file does not publish PostgreSQL or backend ports to the host.

Remember that OCI has both in-VM firewall rules and VCN security lists/network security groups.

## DNS and HTTPS note

For a real DEV hostname:

1. Point an `A` record to the OCI VM public IP.
2. Add HTTPS using a host-level reverse proxy such as Caddy, Traefik, or nginx with Certbot.
3. Keep this app Compose file internal behind that reverse proxy, or update `FRONTEND_HTTP_PORT` if the reverse proxy binds host port `80`.

TLS is intentionally not included yet to keep this foundation simple.

## Backups

Create a timestamped PostgreSQL custom-format backup:

```bash
./deployment/scripts/backup-db.sh
```

Backups are written to:

```text
deployment/backups/
```

This directory is ignored by Git.

Restore example, replacing the dump path:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml exec -T postgres \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges' \
  < deployment/backups/barter-barter_db-YYYYMMDD-HHMMSS.dump
```

For destructive restores, consider stopping the backend first:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml stop backend
# run restore
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml start backend
```

## Troubleshooting

Show service status:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml ps
```

Follow logs:

```bash
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f backend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f frontend
docker compose --env-file deployment/env/dev.env -f deployment/compose/docker-compose.dev.yml logs -f postgres
```

Check backend health from inside the frontend container path:

```bash
curl -i http://localhost/api/v1/ping
```

Common issues:

- **Frontend still calls localhost:8080**: rebuild the frontend image with `--build-arg VITE_API_BASE_URL=/api/v1`, then push and redeploy.
- **Backend cannot connect to DB**: ensure `DB_URL=jdbc:postgresql://postgres:5432/<POSTGRES_DB>` and `DB_PASSWORD` matches `POSTGRES_PASSWORD` for the initialized volume. If you change Postgres init credentials after the first run, recreate the volume intentionally.
- **Azure image upload fails**: verify `AZURE_STORAGE_CONNECTION_STRING_DEV` and `AZURE_STORAGE_CONTAINER_DEV` in `deployment/env/dev.env` and that the container exists or the app can create/use it.
- **SMTP does not send**: set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD`. If `SMTP_HOST` is empty, the backend may use its logging/fallback mail sender according to current app configuration.
- **Out of memory on 1GB VM**: lower `JAVA_OPTS` `-Xmx`, avoid running extra services, add swap if acceptable, and monitor `docker stats`.

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

Recommended next step, when ready, is a separate deployment workflow or release process that updates server env image tags and runs the existing manual pull/up commands. Production should pin explicit version tags such as `v1.0.0`, never `latest`.

