# First Production Deployment Guide — zameni.rs / v1.0.0

This document covers everything required to bring the Barter Platform online for the
**first time ever** on an empty production server.  Follow all steps in order.

> ⚠️ This guide is for the **first deployment only**.  For subsequent deployments, use
> `deployment/scripts/deploy-prod.sh <TAG>` and the `PRODUCTION_CHECKLIST.md`.

---

## 0 — Prerequisites

### 0.1 Server

| Item | Required |
|------|----------|
| OS | Ubuntu 22.04 LTS (recommended) |
| Ports open | 22 (SSH), 80 (HTTP/ACME), 443 (HTTPS + HTTP/3) |
| RAM | ≥ 2 GB (backend JVM alone uses up to 1 GB) |
| Disk | ≥ 20 GB free |
| Docker Engine | ≥ 24.x (`curl -fsSL https://get.docker.com | sudo sh`) |
| Docker Compose plugin | ≥ 2.x (`docker compose version`) |

### 0.2 DNS

All three domains must have A records pointing to the server public IP **before** deployment,
or Caddy cannot obtain Let's Encrypt certificates.

```
zameni.rs      A  <PROD_SERVER_IP>
www.zameni.rs  A  <PROD_SERVER_IP>
app.zameni.rs  A  <PROD_SERVER_IP>
```

Verify with:
```bash
dig +short A zameni.rs www.zameni.rs app.zameni.rs
# All three must return the same server IP.
```

### 0.3 Azure services

| Service | Required state |
|---------|---------------|
| Azure Database for PostgreSQL Flexible Server | Running, firewall rule allows server public IP |
| Azure Blob Storage account | Exists, container `item-images-prod` created (Private access level) |
| SMTP provider | Valid credentials for `noreply@zameni.rs` |

---

## 1 — Server Setup

```bash
# Create deployment user
sudo useradd -m -s /bin/bash barter
sudo usermod -aG docker barter

# Clone the repository (or copy from CI artefacts)
sudo mkdir -p /opt/barter-platform
sudo chown barter:barter /opt/barter-platform
sudo -u barter git clone https://github.com/<org>/<repo>.git /opt/barter-platform

# Make all scripts executable
chmod +x /opt/barter-platform/deployment/scripts/*.sh
```

---

## 2 — Create `prod.env`

```bash
cd /opt/barter-platform
cp deployment/env/prod.env.example deployment/env/prod.env
chmod 600 deployment/env/prod.env
nano deployment/env/prod.env   # or your preferred editor
```

### Mandatory values to replace (no placeholders allowed)

| Variable | How to generate | Notes |
|----------|-----------------|-------|
| `DB_URL` | Azure Portal → PostgreSQL → Connection strings | Must contain `sslmode=require` |
| `DB_USERNAME` | Your DB admin username | |
| `DB_PASSWORD` | `openssl rand -base64 32` | |
| `JWT_SECRET` | `openssl rand -base64 48` | Must be ≥ 32 characters |
| `SMTP_HOST` | SMTP provider docs | e.g. `smtp.resend.com` |
| `SMTP_USERNAME` | SMTP provider | |
| `SMTP_PASSWORD` | SMTP provider | |
| `AZURE_STORAGE_CONNECTION_STRING_PROD` | Azure Portal → Storage → Access Keys | |

### First-deployment bootstrap admin (temporary)

Uncomment and fill in **all four** bootstrap admin variables:

```env
BARTER_BOOTSTRAP_ADMIN_ENABLED=true
BARTER_BOOTSTRAP_ADMIN_USERNAME=admin
BARTER_BOOTSTRAP_ADMIN_EMAIL=admin@zameni.rs
BARTER_BOOTSTRAP_ADMIN_PASSWORD=<openssl rand -base64 24>
```

> ⚠️ These four variables are read together.  `ENABLED=true` is the gate — without it
> the other three are ignored and NO admin user is created.

### Verify no placeholder values remain

```bash
grep -E "replace-with|your-server|change-me|REPLACE|placeholder" \
  /opt/barter-platform/deployment/env/prod.env \
  && echo "PLACEHOLDERS FOUND — fix before deploying" || echo "OK"
```

### Set image tags to the release you are deploying

`deploy-prod.sh` will update these automatically, but set them now as a sanity check:

```env
BACKEND_IMAGE=dragisahub1984/barter-backend:1.0.0
FRONTEND_IMAGE=dragisahub1984/barter-frontend:1.0.0
LANDING_IMAGE=dragisahub1984/barter-landing:1.0.0
```

---

## 3 — Verify Docker Images Exist on Docker Hub

```bash
TAG=1.0.0
for repo in dragisahub1984/barter-backend dragisahub1984/barter-frontend dragisahub1984/barter-landing; do
  echo -n "Checking ${repo}:${TAG} ... "
  curl -sf "https://hub.docker.com/v2/repositories/${repo}/tags/${TAG}/" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK — pushed:', d['tag_last_pushed'])" \
    || echo "NOT FOUND — build and push first!"
done
```

All three must return `OK`.  If any are missing, push the `v1.0.0` git tag to trigger the
Docker Publish workflow:

```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# Wait for the Docker Publish GitHub Actions workflow to complete.
```

---

## 4 — Verify Azure PostgreSQL Connectivity

From the production server:

```bash
# Install psql client if not present
sudo apt-get install -y postgresql-client

# Test connection (replace with real values from prod.env)
psql "host=your-server.postgres.database.azure.com \
      user=barter_user \
      dbname=barter_db \
      sslmode=require" \
  -c "SELECT version();"
# Expected: PostgreSQL version string with no error.
```

If the connection is refused, check:
1. Azure Portal → PostgreSQL → Networking → Firewall rules: add the server IP.
2. The server can reach the Azure endpoint: `nc -zv your-server.postgres.database.azure.com 5432`.

---

## 5 — Verify Azure Blob Storage Write Access

```bash
# Install Azure CLI if not present
sudo apt-get install -y azure-cli

# Test blob write (replace with real connection string from prod.env)
az storage blob upload \
  --connection-string "<AZURE_STORAGE_CONNECTION_STRING_PROD>" \
  --container-name item-images-prod \
  --name healthcheck-test.txt \
  --data "ok" \
  --overwrite

# Clean up test blob
az storage blob delete \
  --connection-string "<AZURE_STORAGE_CONNECTION_STRING_PROD>" \
  --container-name item-images-prod \
  --name healthcheck-test.txt
```

Expected: both commands succeed with no error.

---

## 6 — Run the First Deployment

```bash
cd /opt/barter-platform
bash deployment/scripts/deploy-prod.sh 1.0.0
```

What the script does:
1. Validates the semver tag (`1.0.0`).
2. Confirms `prod.env` and `docker-compose.prod.yml` exist.
3. Updates `BACKEND_IMAGE`, `FRONTEND_IMAGE`, `LANDING_IMAGE` in `prod.env`.
4. Runs `docker compose pull` to fetch images from Docker Hub.
5. Runs `docker compose up -d --force-recreate --remove-orphans`.
6. Polls `https://zameni.rs/health`, `https://app.zameni.rs/health`, and
   `https://app.zameni.rs/api/v1/actuator/health/readiness` until all return `200 OK`
   (up to 300 s timeout).

### Expected startup sequence

The `depends_on: condition: service_healthy` chain enforces this order:

```
backend starts
  └─ Spring Boot loads
  └─ Flyway runs V001 … V026 migrations on empty database
  └─ Roles, permissions, role_permissions seeded (V002)
  └─ Bootstrap admin created (BARTER_BOOTSTRAP_ADMIN_ENABLED=true)
  └─ Readiness endpoint → UP
       └─ frontend starts (depends_on backend healthy)
       └─ landing starts (independent, no depends_on)
            └─ caddy starts (depends_on backend + frontend + landing healthy)
```

The backend has `start_period: 180s` in the compose healthcheck — Spring Boot cold-start
on a fresh JVM takes 60–120 s.  The `deploy-prod.sh` health poll runs for up to 300 s.

### What to watch during startup

In a separate terminal:
```bash
cd /opt/barter-platform
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  logs -f backend
```

Look for:
- `Flyway ... Successfully applied X migrations` — database schema is ready
- `ADMIN role not found` → ERROR: V002 seed migration did not run; check DB connection
- `barter.bootstrap.admin.username must be set` → ERROR: USERNAME var is missing
- `Initial admin user 'admin' created successfully` → admin bootstrap succeeded
- `Started BarterApplication` → backend is up

---

## 7 — Verify Bootstrap Admin

Immediately after deployment completes, verify the admin user was created:

```bash
# Via the readiness endpoint
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | python3 -m json.tool
# Expected: {"status": "UP", ...}

# Log in with bootstrap credentials
curl -sf -X POST https://app.zameni.rs/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<BARTER_BOOTSTRAP_ADMIN_PASSWORD>"}' \
  | python3 -m json.tool
# Expected: {"accessToken": "...", "refreshToken": "..."}
```

If login fails:
1. Check backend logs for `barter.bootstrap.admin.*` errors.
2. Confirm all four `BARTER_BOOTSTRAP_ADMIN_*` vars were set and non-empty in `prod.env`.
3. Confirm `BARTER_BOOTSTRAP_ADMIN_ENABLED` was passed through in the compose file.

---

## 8 — Remove Bootstrap Admin Secrets

After successfully logging in:

```bash
nano /opt/barter-platform/deployment/env/prod.env
```

Comment out or delete all four bootstrap admin lines:
```env
# BARTER_BOOTSTRAP_ADMIN_ENABLED=true
# BARTER_BOOTSTRAP_ADMIN_USERNAME=admin
# BARTER_BOOTSTRAP_ADMIN_EMAIL=admin@zameni.rs
# BARTER_BOOTSTRAP_ADMIN_PASSWORD=...
```

Recreate the backend container to clear the secrets from its environment:
```bash
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  up -d --no-deps --force-recreate backend
```

Wait for the backend to return healthy again (check `docker compose ... ps`), then
verify login still works — the admin account persists in the database.

---

## 9 — Smoke Tests

Run the full smoke test checklist in `PRODUCTION_CHECKLIST.md` Part 7, then run the
additional first-deployment checks below:

### Database bootstrap verification

```bash
# Confirm Flyway applied all 26 migrations
docker run --rm \
  -e "PGPASSWORD=<DB_PASSWORD>" \
  postgres:16-alpine \
  psql "postgresql://barter_user@your-server.postgres.database.azure.com/barter_db?sslmode=require" \
  -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
# Expected: 26 rows (V001 through V026)

# Confirm roles are seeded
docker run --rm \
  -e "PGPASSWORD=<DB_PASSWORD>" \
  postgres:16-alpine \
  psql "postgresql://barter_user@your-server.postgres.database.azure.com/barter_db?sslmode=require" \
  -c "SELECT code FROM roles ORDER BY code;"
# Expected: ADMIN, MODERATOR, USER

# Confirm admin user exists
docker run --rm \
  -e "PGPASSWORD=<DB_PASSWORD>" \
  postgres:16-alpine \
  psql "postgresql://barter_user@your-server.postgres.database.azure.com/barter_db?sslmode=require" \
  -c "SELECT email, status, email_verified FROM users WHERE email = 'admin@zameni.rs';"
# Expected: 1 row with status=ACTIVE, email_verified=true

# Confirm no demo content
docker run --rm \
  -e "PGPASSWORD=<DB_PASSWORD>" \
  postgres:16-alpine \
  psql "postgresql://barter_user@your-server.postgres.database.azure.com/barter_db?sslmode=require" \
  -c "SELECT COUNT(*) AS categories FROM categories; SELECT COUNT(*) AS tags FROM tags; SELECT COUNT(*) AS items FROM items;"
# Expected: all three counts are 0
```

### Azure Blob Storage via backend

```bash
# Obtain admin JWT
TOKEN=$(curl -sf -X POST https://app.zameni.rs/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<BARTER_BOOTSTRAP_ADMIN_PASSWORD>"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# This test requires a valid item — skip if no items exist yet.
# Once an item is created and an image uploaded, verify serving:
# curl -I -H "Authorization: Bearer $TOKEN" \
#   "https://app.zameni.rs/api/v1/files/<storage_key>"
# Expected: HTTP 200 or 302 (no 500 or 403)
echo "Blob smoke test: perform via UI once first item is created."
```

### Security headers

```bash
curl -sI https://zameni.rs | grep -E "Strict-Transport|X-Frame|X-Content|Referrer"
# Expected: HSTS, X-Frame-Options: DENY, X-Content-Type-Options: nosniff, Referrer-Policy
```

### Swagger disabled

```bash
curl -o /dev/null -w "%{http_code}\n" https://app.zameni.rs/api/v1/swagger-ui.html
# Expected: 404
curl -o /dev/null -w "%{http_code}\n" https://app.zameni.rs/api/v1/actuator/env
# Expected: 404 (blocked by Caddy)
```

---

## 10 — Set Up Azure PostgreSQL Automated Backups

In Azure Portal → PostgreSQL → Backup and restore:
- Retention period: ≥ 7 days
- Geo-redundant backup: enabled (recommended)

Note the first backup timestamp:
```
First backup completed: _______________________________
```

---

## 11 — Sign Off

```
First deployment completed at (UTC):  ______________________
Tag deployed:                         1.0.0
Admin email:                          admin@zameni.rs
Bootstrap secrets removed:            YES / NO
Health checks:                        PASS / FAIL
Smoke tests:                          PASS / FAIL
Signed off by:                        ______________________

Rollback tag (previous = none):       N/A — first deployment
Database restore point:               Azure automated backup (see step 10)
```

---

## Troubleshooting — First Deployment

### Backend never becomes healthy

```bash
docker compose -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env logs --tail=100 backend
```

| Log message | Cause | Fix |
|-------------|-------|-----|
| `Connection refused` / `connect to … failed` | PostgreSQL unreachable | Check firewall rule, DB_URL format |
| `FATAL: password authentication failed` | Wrong DB_PASSWORD | Correct DB_PASSWORD in prod.env |
| `Flyway … Unable to obtain connection` | DB not reachable during migration | Same as above |
| `Azure storage … Invalid connection string` | Malformed AZURE_STORAGE_CONNECTION_STRING_PROD | Copy exact string from Azure Portal |
| `barter.bootstrap.admin.username must be set` | USERNAME var not passed | Add BARTER_BOOTSTRAP_ADMIN_USERNAME to prod.env and docker-compose.prod.yml |
| `ADMIN role not found` | V002 seed migration not applied | Wipe the DB and let Flyway re-run from scratch |

### Caddy cannot obtain TLS certificate

- Confirm DNS A records resolve to the correct IP (`dig +short A zameni.rs`).
- Confirm ports 80 and 443 are open and not blocked by a host firewall.
- Check Caddy logs: `docker compose ... logs caddy`.
- Wait up to 5 minutes for Let's Encrypt propagation.

### Frontend shows API errors

- Confirm `VITE_API_BASE_URL=/api/v1` was set at image build time (cannot be changed at runtime).
- Confirm Caddy proxies `/api/v1/*` to `backend:8080`.

---

## See Also

- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — Pre/post-deployment operator checklist
- [`production-runbook.md`](production-runbook.md) — Ongoing operations guide
- [`ROLLBACK_CHECKLIST.md`](ROLLBACK_CHECKLIST.md) — How to roll back
- [`GITHUB_SECRETS.md`](GITHUB_SECRETS.md) — CI/CD secret setup

