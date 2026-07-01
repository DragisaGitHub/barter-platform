# Production Backup — Barter Platform

This document describes the production PostgreSQL backup strategy for Zameni.rs.

---

## Overview

Production uses a **managed/external PostgreSQL** instance (Azure Database for PostgreSQL
Flexible Server). There is **no local PostgreSQL container** in the production Docker Compose
stack. Backups are taken by running `pg_dump` inside a **versioned Docker PostgreSQL client
container** on the production host, connecting to the managed database URL.

Using a Docker client image — rather than whatever `pg_dump` is installed on the host — ensures
that the pg_dump client version always matches or exceeds the managed PostgreSQL server version.
An older host `pg_dump` would be rejected immediately by PostgreSQL with:
`"aborting because of server version mismatch"`.

Item image **binaries** are stored in Azure Blob Storage (`item-images-prod`) and are **not
part of the server-side backup**. Only the PostgreSQL database is backed up by these scripts.
Image metadata (UUIDs, storage keys, content types) lives in PostgreSQL and is therefore
covered by the database backup.

---

## Automatic pre-deploy backup

`deploy-prod.sh` **always runs a database backup before deploying**. This protects data when
a release includes Flyway schema migrations or other changes that are hard to roll back.

Flow:

1. `deploy-prod.sh <tag>` is invoked on the production server.
2. The script reads `BACKUP_ENABLED` from `deployment/env/prod.env`.
3. If `BACKUP_ENABLED=true`, it calls `backup-db.sh` with `BACKUP_DB_MODE=external`.
4. The backup must **succeed** before the deployment continues. If it fails, the deployment
   is aborted immediately.
5. After a successful backup, image tags are updated and containers are recreated.

If `BACKUP_ENABLED=false`, the pre-deploy backup is **skipped** with a warning. This is
strongly discouraged for production.

---

## Backup mode: external

`backup-db.sh` supports two modes, controlled by `BACKUP_DB_MODE` in the env file:

| Mode       | How pg_dump runs                                                         | Use for      |
|------------|--------------------------------------------------------------------------|--------------|
| `compose`  | `docker compose exec` into the local postgres container                  | dev          |
| `external` | `pg_dump` inside a Docker PostgreSQL client image via managed DB URL     | **production** |

Production `prod.env` must contain:

```env
BACKUP_DB_MODE=external
BACKUP_ENABLED=true
```

`backup-db.sh` in external mode:

- reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the env file;
- strips the `jdbc:` prefix from `DB_URL` to build a libpq connection URI before passing it
  into the container (the raw `DB_URL` value is never logged);
- launches `docker run --rm` with `POSTGRES_CLIENT_DOCKER_IMAGE` (default `postgres:18`),
  passing `PGPASSWORD`, `PG_CONN_URI`, and `DB_USERNAME` via environment variables;
- mounts the backup work directory (`BACKUP_WORK_DIR`) into the container at `/backup`;
- pipes pg_dump stdout through gzip on the host and writes a `.dump.gz` file to
  `BACKUP_WORK_DIR` with `umask 077` (owner-read-only) permissions;
- uploads the `.dump.gz` to Azure Blob Storage;
- applies local retention.

**Secrets are never echoed or logged.** `DB_PASSWORD`, `DB_URL`, and connection strings are
passed via environment variables only and do not appear in process argument lists.

**There is no fallback to host `pg_dump`.** If Docker is unavailable, the script fails
immediately with a clear error message rather than silently attempting a version-mismatched
host binary.

---

## Required env variables (prod.env)

```env
# Backup mode
BACKUP_DB_MODE=external
BACKUP_ENABLED=true

# Database credentials — also used by the backend Spring Boot container
DB_URL=jdbc:postgresql://your-server.postgres.database.azure.com:5432/barter_db?sslmode=require
DB_USERNAME=barter_user
DB_PASSWORD=<strong-password>

# PostgreSQL client Docker image — must match or exceed the managed server major version.
# Azure Database for PostgreSQL Flexible Server 18.x → use postgres:18
# Update this value whenever the managed server is upgraded to a new major version.
POSTGRES_CLIENT_DOCKER_IMAGE=postgres:18

# Backup destination
BACKUP_AZURE_CONTAINER=postgres-backups
BACKUP_AZURE_PREFIX=prod/postgres
BACKUP_AZURE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=...

# Local retention (number of .dump.gz files to keep on the server)
BACKUP_LOCAL_RETENTION_COUNT=3

# Schedule (used by setup-backup-cron.sh)
BACKUP_FREQUENCY=daily
BACKUP_SCHEDULE=          # leave empty to derive from BACKUP_FREQUENCY
```

---

## Keeping the Docker client image in sync with the server version

`pg_dump` rejects connections to a server whose major version is **higher** than the client's
with a fatal error:

```
pg_dump: error: server version: 18.x; pg_dump version: 16.x
pg_dump: error: aborting because of server version mismatch
```

The `POSTGRES_CLIENT_DOCKER_IMAGE` variable controls which image is pulled for `docker run`.
Docker pulls the image on first use and caches it — subsequent backups re-use the cached layer.

**Rule:** `POSTGRES_CLIENT_DOCKER_IMAGE` major version ≥ managed server major version.

| Azure PostgreSQL version | Recommended image |
|--------------------------|-------------------|
| 16.x                     | `postgres:16`     |
| 17.x                     | `postgres:17`     |
| 18.x                     | `postgres:18`     |

When Azure upgrades the managed server major version:

1. Update `POSTGRES_CLIENT_DOCKER_IMAGE` in `deployment/env/prod.env`.
2. Run a manual backup to verify the new image works before the next cron run:
   ```bash
   BACKUP_DB_MODE=external ENV_FILE=deployment/env/prod.env \
     bash deployment/scripts/backup-db.sh --force
   ```

---

## Azure Blob Storage destination

Backup files are uploaded to:

```
<BACKUP_AZURE_CONTAINER>/<BACKUP_AZURE_PREFIX>/<filename>
```

With the defaults above, each backup lands at:

```
postgres-backups/prod/postgres/barter-barter_db-20260701T030000Z.dump.gz
```

The blob upload is performed using:
1. Host `az` CLI — if `az` is installed on the server.
2. `mcr.microsoft.com/azure-cli` Docker image — if `az` is absent but Docker is available.

The production server has Docker installed, so `az` does not need to be installed on the host.

---

## Local retention

`BACKUP_LOCAL_RETENTION_COUNT` controls how many `.dump.gz` files are kept on the server
after a successful upload. Older files are automatically removed. Setting `3` keeps the
three most recent local copies as a fast-access fallback.

Backups uploaded to Azure Blob Storage are kept according to Azure lifecycle rules (not
managed by these scripts). Configure a lifecycle policy on the `postgres-backups` container
in the Azure Portal to expire old blobs according to your RPO requirements.

---

## Manual backup (run at any time)

To run an on-demand backup without triggering a full deployment:

```bash
cd /opt/barter-platform
BACKUP_DB_MODE=external ENV_FILE=deployment/env/prod.env \
  bash deployment/scripts/backup-db.sh --force
```

`--force` bypasses `BACKUP_ENABLED=false` if the backup is temporarily disabled.

---

## Scheduled backup (cron)

`setup-backup-cron.sh` installs a crontab entry for periodic backups. To install or refresh
the production cron schedule:

```bash
cd /opt/barter-platform
BACKUP_DB_MODE=external ENV_FILE=deployment/env/prod.env \
  bash deployment/scripts/setup-backup-cron.sh
crontab -l | grep barter-platform-postgres-backup
```

Cron output is appended to:

```
deployment/logs/backup-db.log
```

---

## Restore procedure (production — external PostgreSQL)

> ⚠️ **Never restore automatically.** Restores are a manual, deliberate operator action.
> Always restore into a test database first to validate the backup before touching the live DB.

Production uses a managed PostgreSQL instance. The `restore-db.sh` script is designed for
the compose/dev stack and **cannot be used directly** against the managed production database.
Use `pg_restore` directly from the host.

### Step 1 — List available backups in Azure Blob Storage

```bash
# Use host az or run via Docker
BACKUP_CONN='<BACKUP_AZURE_CONNECTION_STRING from prod.env>'

az storage blob list \
  --connection-string "$BACKUP_CONN" \
  --container-name postgres-backups \
  --prefix prod/postgres/ \
  --output table
```

Or via Docker if `az` is not installed on the host:

```bash
docker run --rm \
  -e AZURE_STORAGE_CONNECTION_STRING="$BACKUP_CONN" \
  mcr.microsoft.com/azure-cli \
  az storage blob list \
    --connection-string "$AZURE_STORAGE_CONNECTION_STRING" \
    --container-name postgres-backups \
    --prefix prod/postgres/ \
    --output table
```

### Step 2 — Download the backup file

```bash
BACKUP_FILE=barter-barter_db-20260701T030000Z.dump.gz
BACKUP_DIR=/var/backups/barter  # or deployment/backups/postgres

mkdir -p "$BACKUP_DIR"

az storage blob download \
  --connection-string "$BACKUP_CONN" \
  --container-name postgres-backups \
  --name "prod/postgres/${BACKUP_FILE}" \
  --file "${BACKUP_DIR}/${BACKUP_FILE}" \
  --no-progress
```

### Step 3 — Restore into a test database (validation first)

Connect to the managed PostgreSQL as an admin user and create a temporary target database:

```bash
# Replace with your actual connection values
PGPASSWORD='<DB_PASSWORD>' psql \
  --host=your-server.postgres.database.azure.com \
  --username=barter_user \
  --dbname=postgres \
  -c "CREATE DATABASE barter_restore_test;"
```

Restore the dump into the test database:

```bash
PGPASSWORD='<DB_PASSWORD>' \
  gzip -dc "${BACKUP_DIR}/${BACKUP_FILE}" | pg_restore \
    --host=your-server.postgres.database.azure.com \
    --port=5432 \
    --username=barter_user \
    --dbname=barter_restore_test \
    --clean --if-exists \
    --no-owner \
    --no-privileges \
    --single-transaction
```

Verify the restore:

```bash
PGPASSWORD='<DB_PASSWORD>' psql \
  --host=your-server.postgres.database.azure.com \
  --username=barter_user \
  --dbname=barter_restore_test \
  -c "SELECT COUNT(*) FROM users;"
```

### Step 4 — (Production recovery only) Restore into the live database

Only proceed when you have validated the backup in step 3 and have made a deliberate decision
to restore production data.

**Stop the backend first** to prevent writes during restore:

```bash
cd /opt/barter-platform
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  stop backend
```

Restore into the live database:

```bash
PGPASSWORD='<DB_PASSWORD>' \
  gzip -dc "${BACKUP_DIR}/${BACKUP_FILE}" | pg_restore \
    --host=your-server.postgres.database.azure.com \
    --port=5432 \
    --username=barter_user \
    --dbname=barter_db \
    --clean --if-exists \
    --no-owner \
    --no-privileges \
    --single-transaction
```

**Restart the backend:**

```bash
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  start backend
```

Check health:

```bash
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | jq .status
```

### Step 5 — Drop the test database (cleanup)

```bash
PGPASSWORD='<DB_PASSWORD>' psql \
  --host=your-server.postgres.database.azure.com \
  --username=barter_user \
  --dbname=postgres \
  -c "DROP DATABASE barter_restore_test;"
```

---

## Item images

Item image **binaries** are stored in Azure Blob Storage (`item-images-prod`) and are **not**
copied to the server filesystem and **not** included in PostgreSQL backups.

- The PostgreSQL backup covers image **metadata** (`item_images` table rows).
- The actual blobs remain in Azure Blob Storage independently of any deployment or restore.
- Azure Blob Storage has its own redundancy and optional soft-delete / versioning policies.

After a database restore, image metadata and blobs will be consistent as long as:
- the backup was taken after the images were uploaded, and
- no blobs were deleted from Azure Blob Storage since the backup.

---

## See also

- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — full pre/post deployment checklist
- [`production-runbook.md`](production-runbook.md) — full operations runbook
- [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md) — recovery procedures for infrastructure failures
- [`DEV_DEPLOYMENT.md`](DEV_DEPLOYMENT.md) — dev backup flow (compose mode)

