# Disaster Recovery — Barter Platform

This document describes recovery procedures for major production failures.  Each scenario
lists the symptoms, the blast radius, the recovery steps, and the expected recovery time.

> For routine rollback of a bad release, use [`ROLLBACK_CHECKLIST.md`](ROLLBACK_CHECKLIST.md)
> instead of this document.

---

## Scenario Index

| Scenario | Likely symptoms | Est. recovery time |
|----------|-----------------|--------------------|
| [1 — VM / host failure](#1--vm--host-failure) | Server unreachable, all services down | 1–3 hours |
| [2 — Docker engine failure](#2--docker-engine-failure) | Containers not running, Docker daemon unresponsive | 15–30 min |
| [3 — PostgreSQL data restore](#3--postgresql-data-restore) | Data corruption, accidental delete, migration failure | 30–90 min |
| [4 — Azure Blob Storage inaccessible](#4--azure-blob-storage-inaccessible) | Image uploads fail, images 404 | 15 min – hours (Azure-side) |
| [5 — DNS / TLS failure](#5--dns--tls-failure) | Browser shows "not secure", domains not resolving | 15–60 min |
| [6 — Complete stack rebuild](#6--complete-stack-rebuild) | Total data centre or account loss, start from scratch | 2–4 hours |

---

## 1 — VM / Host Failure

### Symptoms

- SSH connection times out
- All health endpoints (`https://zameni.rs/health`, etc.) are unreachable
- Azure/VPS portal shows the VM as stopped, failed, or terminated

### Immediate actions

1. **Check Azure / VPS status** — confirm the VM is actually down (not just a network issue):
   ```bash
   ping <PROD_SSH_HOST>
   curl -sf --max-time 10 https://zameni.rs/health || echo "unreachable"
   ```

2. **Check your cloud provider's status page** — verify there is no regional outage

3. **Attempt a cold restart** via the cloud provider console (Azure → Virtual Machines → Restart)
   - Wait 3–5 minutes; then retry SSH and health checks

### If the VM must be rebuilt

The application state is fully external to the VM:

- **Database** — Azure Database for PostgreSQL (not on the VM)
- **Item images** — Azure Blob Storage (not on the VM)
- **TLS certificates** — Managed by Caddy; will be re-requested automatically

What IS on the VM and must be re-provisioned:

- The OS and packages (Docker, UFW, Fail2Ban, etc.)
- The repository checkout at `/opt/barter-platform`
- The `deployment/env/prod.env` file (contains secrets, not in git)
- The `barter` deployment user and SSH keys
- Docker volumes for Caddy (`caddy_data`, `caddy_config`)

### VM rebuild procedure

```bash
# ── Step 1: Provision a new Ubuntu 22.04/24.04 LTS VM ──────────────────────
# In your cloud provider console, create a new VM.
# Assign the same static IP (or update DNS if IP changes — see Scenario 5).

# ── Step 2: Run the server hardening script ─────────────────────────────────
# As root on the new VM:
curl -fsSL https://raw.githubusercontent.com/your-org/barter-platform/main/deployment/scripts/harden-server.sh \
  | bash
# Or clone first and run locally:
# git clone https://github.com/your-org/barter-platform.git /tmp/barter
# bash /tmp/barter/deployment/scripts/harden-server.sh

# ── Step 3: Add the deploy SSH key ──────────────────────────────────────────
# Add the PUBLIC key (matching PROD_SSH_PRIVATE_KEY in GitHub) to:
#   /home/barter/.ssh/authorized_keys

# ── Step 4: Clone the repository ────────────────────────────────────────────
su - barter
git clone https://github.com/your-org/barter-platform.git /opt/barter-platform

# ── Step 5: Restore prod.env ────────────────────────────────────────────────
# Option A: Re-create from prod.env.example and fill in values from your secure vault
cp /opt/barter-platform/deployment/env/prod.env.example \
   /opt/barter-platform/deployment/env/prod.env
chmod 600 /opt/barter-platform/deployment/env/prod.env
nano /opt/barter-platform/deployment/env/prod.env   # fill in all real values

# Option B: Retrieve from a secret manager (1Password, Vault, Azure Key Vault, etc.)

# ── Step 6: Deploy the last known-good tag ──────────────────────────────────
cd /opt/barter-platform
bash deployment/scripts/deploy-prod.sh <LAST_KNOWN_GOOD_TAG>
```

- [ ] SSH access restored
- [ ] Health endpoints respond
- [ ] Smoke tests pass (from [`PRODUCTION_CHECKLIST.md §7`](PRODUCTION_CHECKLIST.md))
- [ ] Update `PROD_SSH_HOST` in GitHub secrets if the IP changed
- [ ] Update `PROD_SSH_KNOWN_HOSTS` in GitHub secrets to the new server fingerprint:
  ```bash
  ssh-keyscan -H -p 22 <NEW_PROD_SSH_HOST>
  ```

---

## 2 — Docker Engine Failure

### Symptoms

- SSH still works but `docker ps` hangs or returns errors
- Containers are down but the VM is up
- `systemctl status docker` shows the daemon is failed or inactive

### Recovery steps

```bash
# ── Check Docker daemon status ───────────────────────────────────────────────
systemctl status docker --no-pager
journalctl -xeu docker --no-pager | tail -50

# ── Attempt a daemon restart ─────────────────────────────────────────────────
systemctl restart docker
sleep 5
docker ps

# ── If restart fails: check for filesystem/disk issues ──────────────────────
df -h
# If /var is full, clean up Docker artefacts:
docker system prune -f   # removes stopped containers, unused images, dangling volumes
                          # does NOT remove named volumes (caddy_data is safe)

# ── If the daemon is corrupt: reinstall Docker ──────────────────────────────
apt-get install --reinstall docker-ce docker-ce-cli containerd.io
systemctl daemon-reload
systemctl restart docker

# ── Recreate the stack after Docker is healthy ───────────────────────────────
cd /opt/barter-platform
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  up -d --force-recreate
```

### After Docker is restored

- [ ] `docker info` runs successfully
- [ ] All four containers show `Up ... (healthy)`
- [ ] External health endpoints respond
- [ ] Check `docker volume ls | grep caddy` — Caddy volumes must still be present
  - If Caddy volumes are gone: Caddy will re-request certificates from Let's Encrypt.
    Give it up to 5 minutes.  Do not restart multiple times — aggressive re-requests
    may trigger rate limits.

---

## 3 — PostgreSQL Data Restore

### Symptoms

- Backend readiness returns `DOWN` with `db` component failing
- Data corruption or accidental deletion
- A destructive Flyway migration ran and cannot be reversed

> ⚠️ **Data restore is irreversible and high-risk.**  Restoring will overwrite current
> database state.  All changes made after the backup timestamp will be lost.

### Before restoring

- [ ] **Stop the backend** to prevent writes during restore:
  ```bash
  docker compose \
    -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
    --env-file /opt/barter-platform/deployment/env/prod.env \
    stop backend
  ```
- [ ] **Confirm the backup timestamp** you intend to restore from:
  - Azure Portal → PostgreSQL Flexible Server → Backup and restore → Available restore points
  - Select the latest point-in-time BEFORE the issue was introduced
- [ ] **Inform affected users** if applicable (maintenance window)

### Restore using Azure PITR (Point-in-Time Restore)

Azure Database for PostgreSQL Flexible Server supports point-in-time restore.
This creates a NEW server instance — it does not overwrite the original server.

```
Azure Portal → PostgreSQL Flexible Server → Backup and restore
→ "Restore to point-in-time"
→ Choose restore point (e.g., 5 minutes before the bad deployment)
→ Create a new server: "barter-prod-restore"
→ Wait for restore (5–30 minutes depending on database size)
```

After restore completes:

1. **Verify the restored data** on the new server using psql
2. **Update `DB_URL` in `prod.env`** to point to the restored server
3. **Restart the backend**:
   ```bash
   docker compose \
     -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
     --env-file /opt/barter-platform/deployment/env/prod.env \
     up -d --force-recreate backend
   ```
4. **Verify backend readiness** — `{"status":"UP"}` on readiness endpoint
5. **Delete or decommission the old server** only after the restored server is confirmed stable

### Verify the restore

```bash
# From the production server (update host to restored server)
psql "host=barter-prod-restore.postgres.database.azure.com \
      user=barter_user \
      dbname=barter_db \
      sslmode=require" \
  -c "SELECT COUNT(*) FROM users;"

# Spot-check a recent record
psql "..." -c "SELECT id, email, created_at FROM users ORDER BY created_at DESC LIMIT 5;"
```

- [ ] Row counts are consistent with expectations
- [ ] The most recent record pre-dates the known-good restore point
- [ ] No error messages during startup (Flyway migration logs)

### After data restore

- [ ] Backend is healthy
- [ ] Smoke tests pass
- [ ] Azure automated backup is re-enabled on the restored server
- [ ] Old (corrupt) server is decommissioned (or kept for investigation)
- [ ] Incident documented (see [`ROLLBACK_CHECKLIST.md §7`](ROLLBACK_CHECKLIST.md))

---

## 4 — Azure Blob Storage Inaccessible

### Symptoms

- Item image uploads return 500 errors
- Previously uploaded images return 404 or fail to load
- Backend logs show Azure SDK errors: `BlobStorageException`, `connection refused`, `unauthorized`

### Diagnosis

```bash
# Check backend logs for Azure errors
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  logs --tail=100 backend | grep -i "azure\|blob\|storage"

# Test storage access directly (on the production server)
CONNECTION_STRING=$(grep "^AZURE_STORAGE_CONNECTION_STRING_PROD" \
  /opt/barter-platform/deployment/env/prod.env | cut -d= -f2-)

az storage blob list \
  --connection-string "${CONNECTION_STRING}" \
  --container-name item-images-prod \
  --num-results 5 \
  --output table
```

### Common causes and fixes

| Cause | Fix |
|-------|-----|
| Storage account key rotated | Update `AZURE_STORAGE_CONNECTION_STRING_PROD` in `prod.env`, restart backend |
| Azure Storage regional outage | Monitor https://status.azure.com — wait for resolution |
| Container was deleted or made public | Recreate container as private in Azure Portal |
| IP allowlisting on storage account | Add the production server IP in Azure Portal → Storage → Networking |
| Connection string malformed | Verify the string in `prod.env` matches the Azure Portal → Access Keys value |

### Recovery: update the connection string

```bash
# Edit prod.env with the corrected connection string
nano /opt/barter-platform/deployment/env/prod.env

# Restart backend to pick up new env
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  up -d --force-recreate backend

# Verify
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | jq .
```

### Verify blob data is intact

Azure Blob Storage provides geo-redundant storage (GRS) by default when enabled.
Item image binaries are stored in Azure and are NOT affected by a server rebuild.

```bash
# List blobs in the production container
az storage blob list \
  --connection-string "${CONNECTION_STRING}" \
  --container-name item-images-prod \
  --prefix items/ \
  --output table | head -20
```

- [ ] Blobs are present and accessible
- [ ] Test a specific image endpoint: `curl -I "https://app.zameni.rs/api/v1/files/<storage_key>"`

---

## 5 — DNS / TLS Failure

### Symptoms

- Browser shows "Not Secure" or certificate warning
- `curl` fails with SSL error
- Caddy logs show ACME challenge failures
- `nslookup` or `dig` shows wrong IP

### Diagnosis

```bash
# Check DNS resolution from the server and locally
for domain in zameni.rs www.zameni.rs app.zameni.rs; do
  echo -n "${domain}: "
  dig +short A "${domain}"
done

# Check certificate validity
echo | openssl s_client -connect zameni.rs:443 -servername zameni.rs 2>/dev/null \
  | openssl x509 -noout -dates -subject

# Check Caddy logs for ACME errors
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  logs --tail=100 caddy | grep -i "error\|acme\|certificate\|challenge"
```

### DNS fix

If DNS points to the wrong IP (e.g., after server migration):

```
DNS Registrar / Azure DNS:
  zameni.rs        A  <new-server-ip>
  www.zameni.rs    A  <new-server-ip>
  app.zameni.rs    A  <new-server-ip>
```

After updating DNS, wait for TTL to expire (check your registrar's TTL — typically 300–3600s).
Caddy will automatically re-challenge for a new certificate once DNS resolves correctly.

### Caddy certificate renewal failure

If Caddy cannot obtain or renew a certificate:

```bash
# Check ACME rate limits — if > 5 failures in the last hour, wait before retrying
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  logs --tail=200 caddy | grep -i "rate\|limit\|too many"

# Verify port 80 is accessible from the internet (Caddy ACME HTTP-01 challenge)
curl -sf http://zameni.rs/.well-known/acme-challenge/test 2>&1 | head -5

# If Caddy data volume is corrupt, reset it (LAST RESORT — forces full cert re-request)
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  down
docker volume rm compose_caddy_data compose_caddy_config 2>/dev/null || true
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  up -d
```

> ⚠️ Let's Encrypt rate limit: 5 failed certificate orders per domain per hour.
> If you see rate limit errors in Caddy logs, wait at least 1 hour before retrying.

- [ ] DNS resolves to correct IP
- [ ] Certificate is valid and not expired
- [ ] `https://zameni.rs/health` returns 200

---

## 6 — Complete Stack Rebuild

Use this when both the VM and data recovery are needed — for example, after account
compromise, regional failure, or complete infrastructure loss.

### Estimated recovery time: 2–4 hours

### Prerequisites

Before starting, confirm you have access to:

- [ ] Cloud provider account (Azure / VPS)
- [ ] DNS registrar
- [ ] GitHub repository and GitHub Actions secrets
- [ ] Docker Hub account (`dragisahub1984`)
- [ ] `prod.env` values from a secure vault (1Password, Vault, Azure Key Vault, etc.)
- [ ] Azure Database for PostgreSQL backup (via Azure Portal PITR)
- [ ] Azure Blob Storage (item images — persisted independently)

### Rebuild sequence

```
Step 1 — Provision new VM
    └── Ubuntu 22.04/24.04 LTS
    └── Assign static IP or note the new IP

Step 2 — Update DNS
    └── zameni.rs, www.zameni.rs, app.zameni.rs → new server IP
    └── Wait for propagation (check with: dig +short A zameni.rs)

Step 3 — Run server hardening
    └── bash deployment/scripts/harden-server.sh
    └── Creates barter user, UFW, Fail2Ban, Docker, log rotation

Step 4 — Add deploy SSH key
    └── Add public key to /home/barter/.ssh/authorized_keys

Step 5 — Clone repository
    └── su - barter
    └── git clone https://github.com/your-org/barter-platform.git /opt/barter-platform

Step 6 — Restore prod.env
    └── Copy from secure vault to /opt/barter-platform/deployment/env/prod.env
    └── chmod 600

Step 7 — Restore database (if needed)
    └── Azure Portal → PostgreSQL → Backup and restore → PITR
    └── Wait for restore, update DB_URL in prod.env

Step 8 — Deploy application
    └── cd /opt/barter-platform
    └── bash deployment/scripts/deploy-prod.sh <LAST_KNOWN_GOOD_TAG>

Step 9 — Verify
    └── Run PRODUCTION_CHECKLIST.md §4–7

Step 10 — Update GitHub secrets
    └── PROD_SSH_HOST (if IP changed)
    └── PROD_SSH_KNOWN_HOSTS (always update after server rebuild)
```

### Verify Azure Blob Storage is intact

```bash
# On the new server — verify item images are accessible
grep "^AZURE_STORAGE_CONNECTION_STRING_PROD" /opt/barter-platform/deployment/env/prod.env

az storage blob list \
  --connection-string "<AZURE_STORAGE_CONNECTION_STRING_PROD>" \
  --container-name item-images-prod \
  --prefix items/ \
  --num-results 10 \
  --output table
```

Item images in Azure Blob Storage are independent of the VM and should be fully intact.
Verify a sample image endpoint serves correctly after the application is back up:

```bash
curl -I "https://app.zameni.rs/api/v1/files/<storage_key_from_db>"
# Expected: HTTP/2 200
```

### Post-rebuild checklist

- [ ] All services healthy (PRODUCTION_CHECKLIST.md §6)
- [ ] Smoke tests pass (PRODUCTION_CHECKLIST.md §7)
- [ ] GitHub `PROD_SSH_KNOWN_HOSTS` updated
- [ ] GitHub `PROD_SSH_HOST` updated (if IP changed)
- [ ] DNS propagated fully — `dig +short A zameni.rs` returns correct IP from multiple resolvers
- [ ] Let's Encrypt certificates issued (Caddy auto-handles, give it 5 min)
- [ ] Azure PostgreSQL firewall updated with new server IP
- [ ] Azure automated backup re-enabled
- [ ] Fail2Ban running: `fail2ban-client status sshd`
- [ ] Incident documented

---

## Recovery Time Estimates

| Component | Recovery time | Notes |
|-----------|--------------|-------|
| VM rebuild | 30–60 min | Provisioning + hardening + deploy |
| Docker engine restart | 5–15 min | Usually just a daemon restart |
| Database PITR restore | 15–60 min | Depends on database size; Azure handles it |
| DNS propagation | 5 min – 4 h | Depends on TTL |
| TLS certificate issuance | 1–5 min | Caddy auto-handles once DNS is correct |
| Blob storage recovery | N/A | Azure managed redundancy |

---

## See Also

- [`SERVER_HARDENING.md`](SERVER_HARDENING.md) — Full server setup guide
- [`ROLLBACK_CHECKLIST.md`](ROLLBACK_CHECKLIST.md) — Application rollback (not infrastructure)
- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — Forward deployment verification
- [`production-runbook.md`](production-runbook.md) — Ongoing operations guide

