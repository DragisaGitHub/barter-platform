# Production Restore — Barter Platform

This document describes how to use `restore-db.sh` to download, verify, and restore
PostgreSQL backups for Zameni.rs.

---

## Overview

Production backups are stored as **custom-format `pg_dump` files** (`.dump.gz`) in Azure
Blob Storage under `postgres-backups/prod/postgres/`. Restores are performed by
`restore-db.sh`, which:

1. Downloads the `.dump.gz` from Azure Blob Storage using the host `az` CLI (or a Docker
   Azure CLI container as a fallback).
2. Decompresses it with `gzip`.
3. Runs `pg_restore` inside the versioned `POSTGRES_CLIENT_DOCKER_IMAGE` Docker container
   to ensure the client version matches the managed PostgreSQL server.
4. Runs validation queries (`SELECT COUNT(*)` on key tables) and prints a summary.

**The script never prints secrets.** `DB_PASSWORD` and `BACKUP_AZURE_CONNECTION_STRING`
are read from the env file and passed only via environment variables into Docker containers.

---

## Prerequisites

| Tool   | Required for              | Note                                        |
|--------|---------------------------|---------------------------------------------|
| Docker | All restore operations    | Runs `POSTGRES_CLIENT_DOCKER_IMAGE` for pg_restore |
| `az`   | Azure download (preferred)| Falls back to Docker Azure CLI if absent    |
| `gzip` | Decompressing backups     | Standard on Linux; not needed for `--verify-only` |

The production server has Docker installed and does **not** require the `az` CLI on the host.

---

## Environment file

All commands below use `deployment/env/prod.env`. The script reads:

| Variable                        | Purpose                                                        |
|---------------------------------|----------------------------------------------------------------|
| `BACKUP_AZURE_CONNECTION_STRING`| Azure Storage connection string                                |
| `BACKUP_AZURE_CONTAINER`        | Blob container name (default: `postgres-backups`)              |
| `BACKUP_AZURE_PREFIX`           | Blob path prefix (default: `prod/postgres`)                    |
| `DB_URL`                        | Production JDBC URL — used to derive the default test DB host  |
| `DB_USERNAME`                   | Database user for pg_restore and validation queries            |
| `DB_PASSWORD`                   | Database password (never printed)                              |
| `POSTGRES_CLIENT_DOCKER_IMAGE`  | Docker image for pg_restore (default: `postgres:18`)           |

The env file path can be overridden with `--env-file <path>` or the `ENV_FILE` variable.

---

## How to list available backups

```bash
cd /opt/barter-platform
bash deployment/scripts/restore-db.sh --list
```

Output (table format):

```
Name                                                        LastModified              Bytes
----------------------------------------------------------  ------------------------  ---------
prod/postgres/barter-barter_db-20260701T030000Z.dump.gz     2026-07-01T03:00:12+00:00  48234567
prod/postgres/barter-barter_db-20260702T030000Z.dump.gz     2026-07-02T03:00:08+00:00  48891234
```

Use the **Name** column value (full blob path) with `--download`.

---

## How to restore the latest backup to a test database

This is the **recommended first step** for any recovery scenario. It proves the backup
is restorable and gives you real row counts before touching production.

```bash
cd /opt/barter-platform
bash deployment/scripts/restore-db.sh --latest
```

The script:

1. Finds the most recently modified blob in Azure.
2. Downloads it to `/tmp/barter-restore/`.
3. Decompresses it and runs `pg_restore` into **`barter_restore_test`** on the same
   PostgreSQL host as production (never into `barter_db`).
4. Runs validation queries and prints a summary:

```
══════════════════════════════════════════════════════════════════════
  RESTORE SUMMARY
══════════════════════════════════════════════════════════════════════
  Backup blob:       prod/postgres/barter-barter_db-20260702T030000Z.dump.gz
  Local file:        /tmp/barter-restore/barter-barter_db-20260702T030000Z.dump.gz
  Target host:       your-server.postgres.database.azure.com:5432
  Target database:   barter_restore_test
  Restore duration:  47s
  ────────────────────────────────────────────────────────────────────
  Validation counts:
    users:                    1842
    categories:               12
    tags:                     87
    flyway_schema_history:    34
══════════════════════════════════════════════════════════════════════
```

### Restore a specific backup

```bash
bash deployment/scripts/restore-db.sh \
  --download prod/postgres/barter-barter_db-20260701T030000Z.dump.gz
```

### Restore from a local file (already downloaded)

```bash
bash deployment/scripts/restore-db.sh \
  --backup-file /var/backups/barter/barter-barter_db-20260701T030000Z.dump.gz
```

### Clean up the test database when finished

```bash
PGPASSWORD='<DB_PASSWORD>' psql \
  --host=your-server.postgres.database.azure.com \
  --username=barter_user \
  --dbname=postgres \
  -c 'DROP DATABASE IF EXISTS barter_restore_test;'
```

---

## How to verify a backup

`--verify-only` downloads and decompresses the backup, then runs `pg_restore --list` to
inspect the object catalog. **No database is modified.** This is a fast structural check
that does not require a target database to exist.

```bash
bash deployment/scripts/restore-db.sh --latest --verify-only
```

Sample output:

```
── Verifying backup structure (pg_restore --list) ──────────────────────────────────

; Archive created at 2026-07-02 03:00:05 UTC
;     dbname: barter_db
;     TOC Entries: 218
;     Compression: gzip
;     Dump Version: 1.14-0
;     Format: CUSTOM
...
2084; 1259 24601 TABLE public users barter_user
2085; 1259 24612 TABLE public categories barter_user
2086; 1259 24623 TABLE public tags barter_user
...

══════════════════════════════════════════════════════════════════════
  VERIFY SUMMARY
══════════════════════════════════════════════════════════════════════
  Backup blob:  prod/postgres/barter-barter_db-20260702T030000Z.dump.gz
  Local file:   /tmp/barter-restore/barter-barter_db-20260702T030000Z.dump.gz
  Verification: pg_restore --list passed — backup is structurally valid
══════════════════════════════════════════════════════════════════════
```

For a **full data verification** (restore + row counts), omit `--verify-only`:

```bash
bash deployment/scripts/restore-db.sh --latest
```

---

## How to do an emergency production restore

> ⚠️ **Only proceed after verifying the backup in a test database first.**
> A production restore overwrites all live data. It cannot be undone without another backup.

### Step 1 — Verify the backup (test DB)

```bash
bash deployment/scripts/restore-db.sh --latest
```

Confirm the row counts look correct before continuing.

### Step 2 — Stop the backend

Prevent new writes during the restore:

```bash
cd /opt/barter-platform
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  stop backend
```

### Step 3 — Dry-run (optional but recommended)

Preview what will happen without executing:

```bash
bash deployment/scripts/restore-db.sh \
  --latest \
  --restore-to postgresql://your-server.postgres.database.azure.com:5432/barter_db \
  --allow-production-restore \
  --dry-run
```

### Step 4 — Restore to production

```bash
bash deployment/scripts/restore-db.sh \
  --latest \
  --restore-to postgresql://your-server.postgres.database.azure.com:5432/barter_db \
  --allow-production-restore
```

The script will print a warning and prompt:

```
  ┌───────────────────────────────────────────────────────────────┐
  │  ⚠️   WARNING: PRODUCTION DATABASE RESTORE                     │
  ├───────────────────────────────────────────────────────────────┤
  │  Host:     your-server.postgres.database.azure.com            │
  │  Database: barter_db                                          │
  │                                                               │
  │  This will OVERWRITE all production data with the backup.     │
  │  ...                                                          │
  │  Type exactly (case-sensitive) and press Enter to confirm:    │
  │                                                               │
  │    RESTORE PRODUCTION DATABASE                                │
  └───────────────────────────────────────────────────────────────┘

Confirmation: _
```

Type **exactly** `RESTORE PRODUCTION DATABASE` (case-sensitive) and press Enter. Any
other input, including extra spaces, aborts with no data changed.

### Step 5 — Restart the backend

```bash
docker compose \
  -f deployment/compose/docker-compose.prod.yml \
  --env-file deployment/env/prod.env \
  start backend
```

### Step 6 — Verify application health

```bash
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | jq .status
```

Expected: `"UP"`

---

## Why production restore is protected

A restore replaces **all data** in the target database with the contents of the backup.
Any writes that occurred after the backup point-in-time are **permanently lost**. This is
irreversible without a newer backup.

The script enforces a two-layer gate:

| Layer | Mechanism | Purpose |
|-------|-----------|---------|
| 1 | `--allow-production-restore` flag must be explicitly passed | Forces a deliberate, non-automated invocation |
| 2 | Interactive phrase `RESTORE PRODUCTION DATABASE` must be typed | Prevents accidental confirmation — cannot be scripted via stdin redirect |

The production database is detected by comparing the `--restore-to` host and database
name against `DB_URL` in the env file. If they match, both gates are enforced.

**Why not just `--yes`?**

A simple `--yes` flag can be added to a script and triggered accidentally.
The typed confirmation phrase forces the operator to read the warning, understand the
target, and make a deliberate decision. It cannot be passed via a non-interactive pipe.

---

## Restore flags quick reference

| Flag | Required | Description |
|------|----------|-------------|
| `--list` | — | List blobs in Azure and exit |
| `--latest` | (one source required) | Auto-select most recent blob |
| `--download <blob>` | (one source required) | Download a specific blob by full path |
| `--backup-file <path>` | (one source required) | Use an already-downloaded local file |
| `--restore-to <url>` | Optional | Target DB URL. Default: `barter_restore_test` |
| `--verify-only` | Optional | Check backup structure, no DB restore |
| `--allow-production-restore` | Required for prod | Unlocks production restore (+ prompt) |
| `--dry-run` | Optional | Print plan without executing |
| `--env-file <path>` | Optional | Override env file path |

---

## Restore flow diagram

```
restore-db.sh
│
├─ --list          → az blob list → print table → exit
│
├─ --latest        → find newest blob name
├─ --download      ─┐
│                   ├─ az blob download → /tmp/barter-restore/*.dump.gz
└─ --backup-file   ─┘
                    │
                    ├─ --verify-only → gzip -dc → pg_restore --list → exit
                    │
                    └─ (restore path)
                        │
                        ├─ production guard check
                        │   ├─ missing --allow-production-restore → BLOCK
                        │   └─ interactive: type "RESTORE PRODUCTION DATABASE"
                        │
                        ├─ gzip -dc → *.dump
                        ├─ docker run pg_restore → target DB
                        ├─ docker run psql: SELECT COUNT(*) × 4 tables
                        └─ print RESTORE SUMMARY
```

---

## Restore from a specific point in time

If you need a backup from before a specific incident:

```bash
# 1. List all available backups
bash deployment/scripts/restore-db.sh --list

# 2. Identify the blob with a LastModified timestamp before the incident
#    e.g. prod/postgres/barter-barter_db-20260701T030000Z.dump.gz

# 3. Restore to test database first
bash deployment/scripts/restore-db.sh \
  --download prod/postgres/barter-barter_db-20260701T030000Z.dump.gz

# 4. Verify counts, inspect data as needed

# 5. If confirmed, restore to production (follow the steps above)
```

---

## Troubleshooting

### `No backup blobs found`

```
ERROR: No backup blobs found in postgres-backups/prod/postgres/. Run --list to check.
```

- Verify `BACKUP_AZURE_CONNECTION_STRING` is set correctly in `prod.env`.
- Run `--list` to confirm the container and prefix are correct.
- Check that `backup-db.sh` has run at least once (cron or manual).

### `pg_restore: error: server version mismatch`

```
pg_restore: error: server version: 18.x; pg_restore version: 16.x
pg_restore: error: aborting because of server version mismatch
```

Update `POSTGRES_CLIENT_DOCKER_IMAGE` in `prod.env` to match the managed server version:

```env
POSTGRES_CLIENT_DOCKER_IMAGE=postgres:18
```

### `Confirmation phrase did not match`

The phrase must be typed **exactly** as shown, case-sensitive, with no extra spaces:

```
RESTORE PRODUCTION DATABASE
```

### Restore exits non-zero with pg_restore warnings

`pg_restore --clean --if-exists` may emit warnings for objects that do not exist in an
empty target database (e.g., `ERROR: table "foo" does not exist`). These are non-fatal
and expected on the first restore into a clean database. Check the validation counts in
the summary to confirm the restore succeeded.

---

## See also

- [`PRODUCTION_BACKUP.md`](PRODUCTION_BACKUP.md) — backup strategy, scheduling, Azure Storage layout
- [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md) — recovery procedures for infrastructure failures
- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — pre/post deployment checklist
- [`production-runbook.md`](production-runbook.md) — full operations runbook

