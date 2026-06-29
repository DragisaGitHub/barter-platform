# Production Deployment Checklist — Barter Platform

Use this checklist before and after every production deployment.  Tick each item only when you
have personally verified it.  Do not skip items under time pressure — every skipped item is a
known risk you are accepting.

> For the first-ever deployment, complete the server setup and environment file sections of
> [`production-runbook.md`](production-runbook.md) first, then return to this checklist.

---

## Part 1 — Infrastructure Readiness

Complete this section at least 30 minutes before every deployment.

### Azure PostgreSQL

- [ ] **Managed instance is running** — Azure Portal → PostgreSQL Flexible Server → Status: Ready
- [ ] **Firewall rule exists** for the production server IP
  ```
  Azure Portal → PostgreSQL → Networking → Firewall rules
  Rule: barter-prod-server  <server-public-ip>/32
  ```
- [ ] **Connection test from the production server**:
  ```bash
  # Replace with your actual values
  psql "host=your-server.postgres.database.azure.com \
        user=barter_user \
        dbname=barter_db \
        sslmode=require" \
    -c "SELECT version();"
  ```
  Expected: PostgreSQL version string (no error)
- [ ] **Azure automated backup is enabled** — Azure Portal → PostgreSQL → Backup and restore
  - Backup retention: at minimum 7 days
  - Geo-redundant backup: enabled for production (recommended)
- [ ] **Note the most recent automated backup timestamp** for rollback reference:
  ```
  Last backup: _______________________________________________
  ```

### Azure Blob Storage

- [ ] **Storage account exists** and is accessible — Azure Portal → Storage Accounts
- [ ] **Container `item-images-prod` exists** and is set to **Private** access level
  ```
  Azure Portal → Storage Account → Containers → item-images-prod → Access level: Private
  ```
- [ ] **Connection string is valid** — verify `AZURE_STORAGE_CONNECTION_STRING_PROD` in
  `deployment/env/prod.env` resolves to the correct account
- [ ] **Write access test** (optional but recommended before first deployment):
  ```bash
  az storage blob upload \
    --connection-string "<AZURE_STORAGE_CONNECTION_STRING_PROD>" \
    --container-name item-images-prod \
    --name health-check-$(date +%s).txt \
    --data "ok" \
    --overwrite
  ```
  Expected: upload succeeds; then delete the test blob

### Database Backup Confirmation

> ⚠️ Verify a backup exists before EVERY deployment that includes schema changes.
> Even for application-only releases, a backup is recommended.

- [ ] **Azure automated backup is current** (within the last 24 hours) — confirm in Azure Portal
- [ ] **Backup timestamp noted above** is recent enough for rollback to succeed
- [ ] If the release includes Flyway migrations: **manual backup recommendation**
  - Azure Database for PostgreSQL does not currently have a point-in-click manual backup trigger.
  - Use a manual `pg_dump` from the production server if the risk warrants it:
    ```bash
    pg_dump "postgresql://barter_user:<password>@your-server.postgres.database.azure.com/barter_db?sslmode=require" \
      --format=custom --no-owner --no-privileges \
      | gzip > /tmp/pre-deploy-$(date +%Y%m%dT%H%M%SZ).dump.gz
    ```

---

## Part 2 — Image and Tag Verification

### Docker Hub

- [ ] **Tag exists on Docker Hub** — confirm all three images are published for the target tag:
  ```bash
  # Replace 1.1.0 with the target tag
  TAG=1.1.0
  for repo in dragisahub1984/barter-backend dragisahub1984/barter-frontend dragisahub1984/barter-landing; do
    echo -n "Checking ${repo}:${TAG} ... "
    curl -sf "https://hub.docker.com/v2/repositories/${repo}/tags/${TAG}/" \
      | jq -r '"OK — pushed: " + .tag_last_pushed' \
      || echo "NOT FOUND"
  done
  ```
  Expected: all three return "OK" with a recent push timestamp

- [ ] **The Docker Publish workflow completed successfully** for the `v<TAG>` git tag:
  - GitHub → Actions → Docker Publish — confirm the run for `v${TAG}` is green

- [ ] **Tag is a valid semver** — not `latest`, `main`, `dev`, or any branch tag

### Git tag

- [ ] **The annotated git tag exists**:
  ```bash
  git ls-remote --tags origin | grep "refs/tags/v1.1.0"
  ```
- [ ] **The commit on the tag has passed CI** (tests, lint, build all green)

---

## Part 3 — Environment File

- [ ] **`prod.env` exists on the server**:
  ```bash
  ls -la /opt/barter-platform/deployment/env/prod.env
  # Expected: -rw------- 1 barter barter
  ```
- [ ] **No placeholder values remain** — search for known placeholder strings:
  ```bash
  grep -E "replace-with|your-server|change-me|REPLACE|placeholder" \
    /opt/barter-platform/deployment/env/prod.env && echo "PLACEHOLDERS FOUND" || echo "OK"
  ```
  Expected: no output (no placeholders)
- [ ] **`DB_URL` points to the managed PostgreSQL** (not `localhost` or `postgres:5432`):
  ```bash
  grep "^DB_URL" /opt/barter-platform/deployment/env/prod.env
  # Expected: jdbc:postgresql://...azure.com:5432/barter_db?sslmode=require
  ```
- [ ] **`SPRING_PROFILES_ACTIVE=prod`** is set (overridden in compose, but verify env file too)
- [ ] **`BARTER_SWAGGER_ENABLED=false`** (locked in compose, but document the state)
- [ ] **`BARTER_EMAIL_VERIFICATION_ENABLED=true`**
- [ ] **`BARTER_SEED_DEMO_CONTENT=false`** (locked in compose)
- [ ] **JWT_SECRET is a strong random value** (not the example placeholder):
  ```bash
  grep "^JWT_SECRET" /opt/barter-platform/deployment/env/prod.env | wc -c
  # Expected: > 50 characters
  ```
- [ ] **Bootstrap admin variables are commented out** (only needed on first deployment):
  ```bash
  grep "^BARTER_BOOTSTRAP_ADMIN" /opt/barter-platform/deployment/env/prod.env \
    && echo "WARNING: bootstrap vars are active" || echo "OK — bootstrap vars not set"
  ```

---

## Part 4 — Server Readiness

- [ ] **Server SSH is reachable**:
  ```bash
  ssh barter@<PROD_SSH_HOST> "echo connected"
  ```
- [ ] **Docker is running**:
  ```bash
  ssh barter@<PROD_SSH_HOST> "docker info --format 'Engine: {{.ServerVersion}}'"
  ```
- [ ] **Disk space is adequate** (at least 20% free):
  ```bash
  ssh barter@<PROD_SSH_HOST> "df -h / | tail -1"
  # Used% column should be below 80%
  ```
- [ ] **No existing health issues** — current containers (if any) are healthy:
  ```bash
  ssh barter@<PROD_SSH_HOST> \
    "docker compose -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
     --env-file /opt/barter-platform/deployment/env/prod.env ps"
  ```
- [ ] **DNS records are correct** — all three domains resolve to the server IP:
  ```bash
  for domain in zameni.rs www.zameni.rs app.zameni.rs; do
    echo -n "${domain}: "; dig +short A "${domain}"
  done
  ```

---

## Part 5 — Deployment Execution

### Record the deployment

```
Deploying tag:           _______________
Deployed by:             _______________
Start time (UTC):        _______________
Previous tag (rollback): _______________
```

### Run the deployment script

```bash
cd /opt/barter-platform
bash deployment/scripts/deploy-prod.sh <TAG>
```

- [ ] Script completed with `=== Production deployment completed successfully ===`
- [ ] Previous tag was printed — **record it above** before proceeding
- [ ] No errors in the output

---

## Part 6 — Post-Deployment Health Checks

Run these within 5 minutes of the deployment script completing.

### Container health

```bash
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  ps
```

- [ ] `barter-prod-caddy` — `Up ... (healthy)`
- [ ] `barter-prod-backend` — `Up ... (healthy)`
- [ ] `barter-prod-frontend` — `Up ... (healthy)`
- [ ] `barter-prod-landing` — `Up ... (healthy)`

### External health endpoints

```bash
curl -sf https://zameni.rs/health           && echo "landing OK"   || echo "LANDING FAIL"
curl -sf https://app.zameni.rs/health       && echo "frontend OK"  || echo "FRONTEND FAIL"
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness \
  | jq .status                              && echo "backend OK"   || echo "BACKEND FAIL"
```

- [ ] `landing OK`
- [ ] `frontend OK`
- [ ] `backend OK` — `"UP"` from the readiness endpoint

### TLS certificate

```bash
echo | openssl s_client -connect zameni.rs:443 -servername zameni.rs 2>/dev/null \
  | openssl x509 -noout -subject -dates
```

- [ ] Certificate is valid (not expired)
- [ ] Certificate subject matches `zameni.rs`

---

## Part 7 — Smoke Tests

Run a manual end-to-end check for the most critical user flows.

### Landing page

- [ ] Browse to `https://zameni.rs` — page loads with expected content
- [ ] Browse to `https://www.zameni.rs` — redirects to or loads correctly

### Frontend SPA

- [ ] Browse to `https://app.zameni.rs` — React SPA loads, no console errors
- [ ] Login flow works — POST `/api/v1/auth/login` returns a JWT

### Backend API

```bash
# Health
curl -sf https://app.zameni.rs/api/v1/actuator/health | jq .

# Ping (legacy smoke)
curl -sf https://app.zameni.rs/api/v1/ping
```

- [ ] `/actuator/health` returns `{"status":"UP"}`
- [ ] No `5xx` errors in the backend logs for the last 2 minutes:
  ```bash
  docker compose \
    -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
    --env-file /opt/barter-platform/deployment/env/prod.env \
    logs --tail=100 backend | grep -c " 5[0-9][0-9] "
  # Expected: 0
  ```

### Blocked endpoints

- [ ] `/api/v1/actuator/env` is blocked (returns 404 from Caddy):
  ```bash
  curl -o /dev/null -w "%{http_code}" https://app.zameni.rs/api/v1/actuator/env
  # Expected: 404
  ```
- [ ] Swagger is disabled:
  ```bash
  curl -o /dev/null -w "%{http_code}" https://app.zameni.rs/swagger-ui.html
  # Expected: 404
  ```

---

## Part 8 — Sign-Off

```
Deployment complete:      _______________  (UTC)
Tag deployed:             _______________
Health checks:            PASS / FAIL
Smoke tests:              PASS / FAIL
Signed off by:            _______________

Rollback tag if needed:   _______________
```

---

## Rollback Trigger Criteria

Initiate a rollback immediately if any of these are true:

| Symptom | Action |
|---------|--------|
| Backend readiness returns `DOWN` or `503` after 5 minutes | Rollback |
| Frontend SPA does not load | Rollback |
| Login flow broken | Rollback |
| Error rate > 5% of requests in logs | Rollback |
| Container repeatedly restarting | Rollback |
| TLS certificate error in browser | Investigate first — may not be rollback-related |

See [`ROLLBACK_CHECKLIST.md`](ROLLBACK_CHECKLIST.md) for the full rollback procedure.

---

## See Also

- [`production-runbook.md`](production-runbook.md) — Full deployment and operations guide
- [`ROLLBACK_CHECKLIST.md`](ROLLBACK_CHECKLIST.md) — Step-by-step rollback procedure
- [`GITHUB_SECRETS.md`](GITHUB_SECRETS.md) — CI/CD secret configuration
- [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md) — Recovery procedures for infrastructure failures

