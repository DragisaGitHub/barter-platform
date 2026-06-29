# Production Runbook — zameni.rs

This document covers first-time and ongoing production deployment of the Barter Platform (zameni.rs).

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

```bash
# Update image tag(s) in prod.env, then:
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env pull
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d
```

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

```bash
# 1. Edit prod.env — set the previous working image tag:
#    BACKEND_IMAGE=dragisahub1984/barter-backend:0.9.0

# 2. Pull old image (if not cached locally)
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env pull backend

# 3. Recreate the service
docker compose -f compose/docker-compose.prod.yml --env-file env/prod.env up -d backend

# 4. Verify health
curl -s https://app.zameni.rs/api/v1/actuator/health/readiness
```

If the database has irreversible migrations in the new version, you must also restore the database from backup before rolling back.

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

- [ ] CI/CD pipeline to automate image tag bump + deploy via SSH
- [ ] Uptime monitoring (e.g., UptimeRobot, Azure Monitor)
- [ ] Log aggregation (e.g., Grafana Loki, Azure Log Analytics)
- [ ] Database backup automation script for managed PostgreSQL
- [ ] Rate limiting at Caddy layer
- [ ] WAF / DDoS protection (Cloudflare or Azure Front Door)
