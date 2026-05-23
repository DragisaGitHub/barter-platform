# Launch Blocker 04 — Backup and Restore Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 data protection  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Protect the current DEV/public-beta deployment against PostgreSQL data loss using a simple, operator-friendly backup and restore process.
- Keep the implementation realistic for a small VM: compressed dumps, off-server Azure Blob upload, minimal local retention, and no permanent backup accumulation on the VM.
- Make backup cadence configurable now so future production can switch to daily backups without changing script logic.

# Scope Decision

This implementation intentionally covers **PostgreSQL only**.

- PostgreSQL contains application records and metadata: users, listings, offers, messages, reviews, and image references/keys.
- Item image binaries already live in Azure Blob Storage and must **not** be duplicated into local VM backups.
- This launch-blocker is therefore satisfied by a reliable PostgreSQL backup/upload/restore process plus clear operator documentation.

# Current Deployment Reality

- Runtime deployment: `deployment/compose/docker-compose.dev.yml`
- PostgreSQL service: `postgres`
- Primary env file: `deployment/env/dev.env`
- Azure Blob container for DB backups: `postgres-backups`
- DEV blob prefix: `dev/postgres/`
- Existing image storage remains separate via `AZURE_STORAGE_CONTAINER_DEV`

# Implemented Artifacts

- `deployment/scripts/backup-db.sh`
  - runs `pg_dump`
  - compresses the dump to `*.dump.gz`
  - uploads to Azure Blob Storage
  - trims local backup files to the configured retention count after successful upload
- `deployment/scripts/restore-db.sh`
  - restores a compressed PostgreSQL dump into a chosen database
  - supports recreating a dedicated restore-test database
  - refuses to recreate the primary DB unless explicitly allowed
- `deployment/scripts/setup-backup-cron.sh`
  - installs or refreshes the cron schedule using `BACKUP_FREQUENCY` or `BACKUP_SCHEDULE`
- `deployment/docs/DEV_DEPLOYMENT.md`
  - documents manual backup, monthly DEV schedule, restore testing, failure checklist, and pre-deploy backup guidance

# Configurable Parameters

These parameters are now the contract for backup behavior:

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
POSTGRES_DB=barter_db
POSTGRES_USER=barter_user
```

Notes:

- `BACKUP_SCHEDULE` overrides `BACKUP_FREQUENCY` when set.
- DEV defaults to `BACKUP_FREQUENCY=monthly`.
- Future production can later move to `BACKUP_FREQUENCY=daily` or an explicit cron expression with no backup script rewrite.
- `BACKUP_AZURE_CONNECTION_STRING` may reuse the same Azure Storage account as DEV image storage or point to a separate backup account.
- If `BACKUP_AZURE_CONNECTION_STRING` is empty, the backup script falls back to `AZURE_STORAGE_CONNECTION_STRING_DEV`.

# Backup Flow

1. Read configuration from `deployment/env/dev.env` plus optional exported env overrides.
2. Resolve the Azure CLI runner: use host `az` when present, otherwise use Docker with `mcr.microsoft.com/azure-cli`.
3. Execute `pg_dump` against the running `postgres` container.
4. Produce a compressed file named like:

   ```text
   barter-barter_db-20260522T040000Z.dump.gz
   ```

5. Upload the file to:

   ```text
   postgres-backups/dev/postgres/<backup-file>.dump.gz
   ```

   The storage connection string is still taken from `BACKUP_AZURE_CONNECTION_STRING` with fallback to `AZURE_STORAGE_CONNECTION_STRING_DEV`, and the Docker fallback passes it through an environment variable rather than requiring Azure CLI on the VM.

6. After successful upload, prune local files down to `BACKUP_LOCAL_RETENTION_COUNT`.
7. Keep backup logs in `deployment/logs/backup-db.log` when run from cron.

# Retention Policy

## DEV now

- **Backup cadence:** monthly by default.
- **Local retention:** keep only the newest `1-2` compressed PostgreSQL backup files; default is `2`.
- **Azure retention:** keep monthly DEV backups off-server in `postgres-backups/dev/postgres/` until a stricter lifecycle rule is introduced.

This is intentionally conservative for a small public-beta deployment:

- the VM does not accumulate large backup history;
- the authoritative off-server copy lives in Azure Blob Storage;
- monthly DEV cadence keeps blob growth modest.

## Future PROD direction

- No production scheduling is implemented yet.
- Production is expected to move to at least daily PostgreSQL backups by changing configuration only:

  ```env
  BACKUP_FREQUENCY=daily
  ```

  or:

  ```env
  BACKUP_SCHEDULE=0 2 * * *
  ```

# Restore Strategy

## Non-destructive restore verification

Before public launch, and after major migration risk, perform a manual restore test into a separate database such as `barter_restore_test`.

Recommended flow:

1. List available blobs under `dev/postgres/`.
2. Download one backup file locally.
3. Restore into `barter_restore_test` using `restore-db.sh --recreate-target-db --yes`.
4. Validate a few representative tables and image metadata references.
5. Record the date and result.

## Destructive live restore

Only use for an intentional incident recovery:

1. Stop the backend container.
2. Restore the selected backup into the primary database.
3. Start the backend container again.
4. Verify health and key flows.

The restore script deliberately requires explicit confirmation before recreating the primary DB.

# RPO / RTO Expectations

## DEV / public-beta

- **Default RPO:** up to one month with the current monthly schedule.
- **Target RTO:** manual operator recovery within roughly 1-2 hours, including backup selection, restore, and smoke verification.

This is acceptable for current DEV/public-beta preparation but is **not** the intended long-term production posture.

## Future PROD target

- Move to daily backups at minimum.
- Keep the same script path and blob layout pattern, with a production prefix such as `prod/postgres/`.

# Operational Rules

- Run a manual PostgreSQL backup before deployments that may affect data, infrastructure, container images, or migrations.
- Do not store backup files permanently on the VM.
- Do not back up item images locally.
- Do not change application business code or schema for this launch-blocker.
- Keep secrets out of logs; use env/secret configuration for Azure storage access.

# Failure Checklist

If backup or restore fails, verify:

1. `postgres` is healthy in the current Compose stack.
2. Either host `az` is available, or Docker is available to run `mcr.microsoft.com/azure-cli` for blob access.
3. `BACKUP_AZURE_CONTAINER=postgres-backups` is correct.
4. `BACKUP_AZURE_PREFIX=dev/postgres` is correct.
5. The VM has enough free space for one compressed dump plus one in-progress file.
6. `BACKUP_WORK_DIR` is writable.
7. The latest cron/manual output in `deployment/logs/backup-db.log` or terminal output explains the failure cause.

# Security Considerations

- Backups contain user and operational data; treat them as sensitive.
- Azure Blob upload uses encrypted transport and Azure storage encryption at rest.
- Backup credentials must remain in server env/secrets, not in Git.
- Prefer a dedicated backup credential in the future if operational separation becomes necessary.

# Testing / Verification Requirement Before Public Launch

The launch blocker is only considered operationally closed when all of the following are true:

- A successful manual backup has been created and uploaded to `postgres-backups/dev/postgres/`.
- The monthly DEV cron schedule has been installed and verified.
- A restore test into a fresh non-primary database has completed successfully.
- Operators know the pre-deploy manual backup step and failure checklist.

# Explicitly Deferred

- Production backup cadence rollout
- Azure Blob lifecycle automation for old backup blobs
- Managed PostgreSQL PITR
- Enterprise backup tools
- Kubernetes backup tooling
- Multi-region disaster recovery
