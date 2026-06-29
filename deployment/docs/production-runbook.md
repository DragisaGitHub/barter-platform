# Production Runbook — zameni.rs

This document covers first-time and ongoing production deployment of the Barter Platform (zameni.rs).

## See Also

| Document | Purpose |
|----------|---------|
| [SERVER_HARDENING.md](SERVER_HARDENING.md) | OS-level hardening: SSH, UFW, Fail2Ban, Docker, log rotation |
| [GITHUB_SECRETS.md](GITHUB_SECRETS.md) | Every GitHub Actions secret — what it is and how to generate it |
| [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) | Step-by-step pre/post deployment operator checklist |
| [ROLLBACK_CHECKLIST.md](ROLLBACK_CHECKLIST.md) | When and how to rollback, including migration risk assessment |
| [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) | Recovery from VM failure, DB restore, DNS/TLS issues |
| [README.md](README.md) | Index of all deployment documentation |

---


## Production Deployment Policy

### Why Production Deploy Is Manual-Only

Production deployments are **never automatic**. There is no CI/CD trigger that pushes to production on merge to `main` or on any branch push. This is by design:

- **Safety**: Every production release is a deliberate, reviewed human decision.
- **Auditability**: The GitHub Actions run log records who triggered the deploy and which exact tag was deployed.
- **Immutability**: Production only runs images tagged with a specific semver version (e.g. `1.0.0`). The `:latest` tag is never used.
- **Rollback clarity**: If something breaks, you know exactly which version was deployed and which version to roll back to.

### What Must NEVER Be Used in Production

| Forbidden Tag / Pattern | Why |
|-------------------------|-----|
| `latest` | Mutable, unpredictable — could change between pull and restart |
| `main` / `master` | Branch HEAD, not a release — untested for production |
| `dev` / `develop` | Development-only images |
| `main-<sha>` | Dev commit SHA tags — not a release |
| Auto-deploy from `main` push | Bypasses manual review gate |

### Tag-Based Release Flow

```
1.  Merge feature PRs into main
2.  CI runs on main (tests, lint, build)
3.  When ready for a release:
      git tag v1.0.0
      git push origin v1.0.0
4.  Docker Publish workflow triggers automatically on the tag push
    → builds and pushes images tagged: v1.0.0, 1.0.0
5.  Manually trigger PROD Deploy workflow with image_tag = "v1.0.0" (or "1.0.0")
6.  Workflow SSHs into production, runs deploy-prod.sh with the normalized tag
7.  Health checks verify the deployment
```

### How to Create and Push a Release Tag

```bash
# Ensure main is up to date
git checkout main
git pull origin main

# Create an annotated tag
git tag -a v1.0.0 -m "Release 1.0.0: <brief description>"

# Push the tag — triggers Docker image build
git push origin v1.0.0
```

Wait for the **Docker Publish** workflow to complete before deploying.

### How to Run the Production Deploy Workflow

1. Go to **Actions** → **PROD Deploy** in the GitHub repository
2. Click **Run workflow**
3. Enter the `image_tag` (e.g. `v1.0.0` or `1.0.0`)
4. Click **Run workflow**
5. The workflow validates the tag, SSHs into production, and runs `deploy-prod.sh`
6. Monitor the workflow run for health check results

### How to Rollback

**Option A: Via rollback script on the server**

```bash
cd /opt/barter-platform
bash deployment/scripts/rollback-prod.sh 0.9.0
```

**Option B: Via deploy workflow with the old tag**

1. Go to **Actions** → **PROD Deploy**
2. Enter the previous known-good tag (e.g. `0.9.0`)
3. Run the workflow

> ⚠️ **Database warning**: If the release you are rolling back FROM introduced
> irreversible database migrations (dropped columns, renamed tables, deleted
> data), you must also restore the database from a backup taken before the
> forward deploy. Database restore is a separate manual step.

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `PROD_SSH_HOST` | Production server hostname or IP |
| `PROD_SSH_PORT` | SSH port (default: 22) |
| `PROD_SSH_USER` | SSH username for deployment |
| `PROD_SSH_PRIVATE_KEY` | SSH private key (ed25519 recommended) |
| `PROD_DEPLOY_PATH` | Absolute path to the barter-platform checkout on the server (e.g. `/opt/barter-platform`) |
| `PROD_SSH_KNOWN_HOSTS` | (Optional) SSH known_hosts content for strict host verification |
| `DOCKERHUB_USERNAME` | DockerHub username (used by Docker Publish workflow) |
| `DOCKERHUB_TOKEN` | DockerHub access token (used by Docker Publish workflow) |
| `VITE_SENTRY_DSN_PROD` | (Optional) Frontend Sentry DSN for production images |

The `production` GitHub environment should have protection rules (e.g. required reviewers) configured.

---

## Prerequisites

### DNS

Create the following A records pointing to the production server's public IP:

| Record | Type | Value |
|--------|------|-------|
| `zameni.rs` | A | `<server-ip>` |
| `www.zameni.rs` | A / CNAME | `<server-ip>` or `zameni.rs` |
| `app.zameni.rs` | A | `<server-ip>` |

Caddy will automatically obtain Let's Encrypt certificates for all three once DNS propagates and ports 80/443 are reachable.

### Managed PostgreSQL

Production does **not** run a local PostgreSQL container.
Provision a managed instance (e.g., Azure Database for PostgreSQL Flexible Server) and note:
- Hostname (e.g., `barter-prod.postgres.database.azure.com`)
- Database name (e.g., `barter_db`)
- Username and password
- Ensure `sslmode=require` in the JDBC URL
- Whitelist the production server IP in the database firewall rules

### Server requirements

- Linux host with Docker Engine 24+ and Docker Compose v2
- Ports 80 and 443 open to the internet (Caddy ACME + HTTPS)
- Minimum: 4 vCPU / 8 GB RAM
- Recommended: 4–8 vCPU / 16 GB RAM
- SSH access for deployment

---

## Environment File Setup

```bash
# On the production server
cd /opt/barter-platform/deployment
cp env/prod.env.example env/prod.env
chmod 600 env/prod.env
```

Edit `env/prod.env` and fill every value marked with ⚠️. Generate secrets:

```bash
# JWT secret
openssl rand -base64 48

# DB password
openssl rand -base64 32

# Bootstrap admin password (one-time)
openssl rand -base64 24
```

**Never commit `prod.env` to the repository.**

---

## Image Tag Selection

Production uses pinned image tags — never `:latest`.

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:1.0.0
FRONTEND_IMAGE=dragisahub1984/barter-frontend:1.0.0
LANDING_IMAGE=dragisahub1984/barter-landing:1.0.0
```

To deploy a new release:
1. Update the tag(s) in `prod.env`
2. Pull and recreate (see below)

---

## First-Time Deployment

```bash
cd /opt/barter-platform/deployment

# 1. Create env file (see above)

# 2. Uncomment and set bootstrap admin in prod.env:
#    BARTER_BOOTSTRAP_ADMIN_EMAIL=admin@zameni.rs
#    BARTER_BOOTSTRAP_ADMIN_PASSWORD=<generated-password>

# 3. Pull images
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env pull

# 4. Start the stack
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d

# 5. Wait for backend health (may take up to 3 minutes on first startup)
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env ps

# 6. Verify health
curl -s https://app.zameni.rs/api/v1/actuator/health/readiness
curl -s https://zameni.rs/health

# 7. Login with the bootstrap admin credentials, change password

# 8. REMOVE bootstrap variables from prod.env:
#    Comment out or delete BARTER_BOOTSTRAP_ADMIN_EMAIL and BARTER_BOOTSTRAP_ADMIN_PASSWORD

# 9. Recreate backend to drop bootstrap env:
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d --force-recreate backend
```

### Post-Launch: Creating Catalog Content

A fresh production database starts with **no categories, tags, or listings**.
The admin must create all marketplace taxonomy manually via the Admin API:

1. **Create categories** — `POST /api/v1/admin/categories`
2. **Create tags** — `POST /api/v1/admin/tags`
3. Once categories exist, users can create item listings.

This is by design. Demo/business content is never seeded in production
(`BARTER_SEED_DEMO_CONTENT=false` is the default and must remain so).

---

## Ongoing Operations

### Start / Stop / Restart

```bash
cd /opt/barter-platform/deployment

# Start
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d

# Stop (preserves volumes)
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env down

# Restart a single service
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env restart backend
```

### Deploy a New Release

Use `deploy-prod.sh` — it validates the tag, updates `prod.env`, pulls images, recreates the
stack, and runs health checks. It also prints the previous tag so you have a rollback reference.

```bash
cd /opt/barter-platform
bash deployment/scripts/deploy-prod.sh 1.1.0
```

> ⚠️ **Before deploying**: confirm a recent managed-PostgreSQL backup exists (Azure automated
> backup or a manual `pg_dump`) — especially for releases that include schema migrations.

<details>
<summary>Manual equivalent (advanced / emergency use only)</summary>

```bash
cd /opt/barter-platform/deployment

# Update image tag(s) in prod.env, then:
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env pull
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d --force-recreate
```

</details>

### Check Logs

```bash
# All services
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env logs --tail=100

# Specific service
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env logs --tail=200 backend

# Follow live
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env logs -f backend
```

### Health Checks

```bash
# Landing
curl -s https://zameni.rs/health

# Frontend
curl -s https://app.zameni.rs/health

# Backend readiness
curl -s https://app.zameni.rs/api/v1/actuator/health/readiness

# Container status
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env ps
```

---

## Rollback

Rollback = revert to a known-good image tag.

Use `rollback-prod.sh` — it validates the tag, prints the currently deployed tag, warns about
database migration risks, and runs the same health checks as a forward deploy.

```bash
cd /opt/barter-platform
bash deployment/scripts/rollback-prod.sh 1.0.0
```

If the database has irreversible migrations in the new version, you must also restore the database from backup before rolling back.

<details>
<summary>Manual equivalent (advanced / emergency use only)</summary>

```bash
cd /opt/barter-platform/deployment

# 1. Edit prod.env — set the previous working image tag:
#    BACKEND_IMAGE=dragisahub1984/barter-backend:0.9.0

# 2. Pull old image (if not cached locally)
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env pull backend

# 3. Recreate the service
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d backend

# 4. Verify health
curl -s https://app.zameni.rs/api/v1/actuator/health/readiness
```

</details>

---

## What Must NOT Be Committed

| Path | Reason |
|------|--------|
| `deployment/env/prod.env` | Contains real secrets |
| `deployment/env/dev.env` | Contains real secrets |
| Any `*.pem`, `*.key` files | Private keys |
| Caddy `/data` volume contents | ACME account + certs |

The `.gitignore` should already exclude `deployment/env/prod.env` and `deployment/env/dev.env`.

---

## Remaining TODOs (Future Phases)

- [x] CI/CD pipeline to automate image tag bump + deploy via SSH (PROD Deploy workflow)
- [ ] **Managed-PostgreSQL backup strategy** — `backup-db.sh` uses `docker compose exec` and
      targets a local postgres container; it cannot run against Azure Database for PostgreSQL as-is.
      Options: rely on Azure's built-in automated backups, add a scheduled `pg_dump` job that
      connects directly to the managed endpoint, or use `pgbackup` / Azure Data Factory.
      Until this is in place, verify Azure automated backup retention in the Azure Portal.
- [ ] Uptime monitoring (e.g., UptimeRobot, Azure Monitor)
- [ ] Log aggregation (e.g., Grafana Loki, Azure Log Analytics)
- [ ] Rate limiting at Caddy layer
- [ ] WAF / DDoS protection (Cloudflare or Azure Front Door)
