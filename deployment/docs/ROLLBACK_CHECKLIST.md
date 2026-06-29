# Production Rollback Checklist — Barter Platform

This checklist walks through the complete rollback procedure: assessing whether rollback is the
right action, executing it, verifying it succeeded, and documenting the incident.

> **Quick rollback command** (when you already know the previous tag):
> ```bash
> cd /opt/barter-platform
> bash deployment/scripts/rollback-prod.sh <PREVIOUS_TAG>
> ```

---

## Part 1 — Assess Whether to Roll Back

Do not rollback reflexively.  First confirm that rollback is the correct response.

### Gather information

- [ ] **What is broken?** Describe the symptom in one sentence:
  ```
  Symptom: _______________________________________________
  ```
- [ ] **When did it break?** (UTC time and tag that introduced the issue)
  ```
  Broke at: _______________  Tag: _______________
  ```
- [ ] **Is the issue application code or infrastructure?**
  - Application code → rollback is likely the right action
  - Infrastructure (DNS, Azure, network, TLS, Docker engine) → rollback will NOT fix it

### Check the logs first

```bash
# Last 200 lines of backend
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  logs --tail=200 backend

# Last 50 lines of Caddy
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  logs --tail=50 caddy
```

### Rollback is appropriate when

- [ ] The issue appeared immediately after a new deployment
- [ ] The root cause is in application code or configuration (not infrastructure)
- [ ] The previous version is known-good
- [ ] No database migration rollback is needed (or you have a pre-migration backup)

### Rollback is NOT sufficient when

- Infrastructure (server, Docker engine, Azure, DNS, TLS) is the root cause
- The database has irreversible migrations in the broken version (see §2 below)
- A security incident requires more than a code rollback (secrets rotation, etc.)

→ In those cases use [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md) instead.

---

## Part 2 — Database Migration Risk Assessment

> ⚠️ This section is critical.  Skipping it may cause data loss or corruption.

### Determine the rollback target

- [ ] **What is the previous known-good tag?** (printed by `deploy-prod.sh`)
  ```
  Previous tag: _______________
  ```
- [ ] **What is the current broken tag?**
  ```
  Broken tag: _______________
  ```

### Check for irreversible migrations in the broken version

Review the Flyway migration files added between the previous tag and the broken tag:

```bash
# On local machine — list Flyway migrations in the broken version that are NOT in the previous version
git log v<PREVIOUS_TAG>..v<BROKEN_TAG> -- \
  backend/barter-infrastructure/src/main/resources/db/migration/
```

For each migration found:

| Migration file | Type | Safe to roll back? |
|---------------|------|-------------------|
| (none) | — | ✓ Safe — no migrations changed |
| V*.sql — adds nullable column | Additive | ✓ Safe — old code ignores it |
| V*.sql — drops column | Destructive | ✗ **Requires DB restore** |
| V*.sql — renames table/column | Destructive | ✗ **Requires DB restore** |
| V*.sql — removes data | Destructive | ✗ **Requires DB restore** |

- [ ] **Are any destructive migrations present?**
  - NO → proceed to Part 3 directly
  - YES → **stop here**; confirm an Azure backup from BEFORE the forward deploy exists,
    then follow the database restore procedure in [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md)
    before rolling back the application

---

## Part 3 — Pre-Rollback Snapshot

Before changing anything, capture the current state.

- [ ] **Record the current deployed tag** (before rollback):
  ```bash
  grep "^BACKEND_IMAGE=" /opt/barter-platform/deployment/env/prod.env
  ```
  ```
  Current tag: _______________
  ```

- [ ] **Record container status** before rollback:
  ```bash
  docker compose \
    -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
    --env-file /opt/barter-platform/deployment/env/prod.env \
    ps
  ```

- [ ] **Save recent logs** for post-incident analysis:
  ```bash
  docker compose \
    -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
    --env-file /opt/barter-platform/deployment/env/prod.env \
    logs --tail=500 > /tmp/pre-rollback-logs-$(date +%Y%m%dT%H%M%SZ).txt
  ```

---

## Part 4 — Execute Rollback

```bash
cd /opt/barter-platform
bash deployment/scripts/rollback-prod.sh <PREVIOUS_TAG>
```

The rollback script will:

1. Validate the tag is a valid semver
2. Confirm the env file and compose file exist
3. Print the currently deployed tag (for your records)
4. Warn about database migration risks
5. Update `prod.env` with the rollback image tags
6. Pull the rollback images
7. Recreate all containers with `--force-recreate`
8. Run external health checks

- [ ] Script printed `=== Production ROLLBACK completed successfully ===`
- [ ] No errors during the pull or recreate phase

---

## Part 5 — Post-Rollback Verification

Repeat all health and smoke checks from [`PRODUCTION_CHECKLIST.md §6–7`](PRODUCTION_CHECKLIST.md).

### Container health

```bash
docker compose \
  -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
  --env-file /opt/barter-platform/deployment/env/prod.env \
  ps
```

- [ ] All four containers: `Up ... (healthy)`

### External endpoints

```bash
curl -sf https://zameni.rs/health                                       && echo "landing OK"
curl -sf https://app.zameni.rs/health                                   && echo "frontend OK"
curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | jq . && echo "backend OK"
```

- [ ] Landing: `OK`
- [ ] Frontend: `OK`
- [ ] Backend readiness: `{"status":"UP"}`

### Verify the correct image is running

```bash
grep "^BACKEND_IMAGE=" /opt/barter-platform/deployment/env/prod.env
# Expected: dragisahub1984/barter-backend:<PREVIOUS_TAG>
```

- [ ] `prod.env` shows the rollback tag, not the broken tag

### Smoke tests

- [ ] `https://zameni.rs` loads
- [ ] `https://app.zameni.rs` loads (React SPA)
- [ ] Login flow works
- [ ] No `5xx` errors in backend logs for the last 2 minutes:
  ```bash
  docker compose \
    -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
    --env-file /opt/barter-platform/deployment/env/prod.env \
    logs --tail=100 backend | grep -c " 5[0-9][0-9] "
  # Expected: 0
  ```

---

## Part 6 — Rollback Failed

If the rollback script fails or health checks do not pass after rollback:

1. **Do not panic** — services may still be partially degraded, not completely down
2. Inspect logs:
   ```bash
   docker compose \
     -f /opt/barter-platform/deployment/compose/docker-compose.prod.yml \
     --env-file /opt/barter-platform/deployment/env/prod.env \
     logs --tail=200 backend
   ```
3. Check if the issue is database-related (schema incompatibility with old code)
4. If containers will not start at all → follow [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md)
5. If the issue is a missing database table/column → a DB restore is required

---

## Part 7 — Incident Documentation

Complete this section within 1 hour of resolving the incident.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ROLLBACK INCIDENT RECORD
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Date (UTC):             _______________
Duration of outage:     _______________
Tag rolled back from:   _______________
Tag rolled back to:     _______________
Root cause:             _______________
Detection method:       _______________
Time to detect:         _______________
Time to rollback:       _______________
Database restore needed: YES / NO
Service restored:       YES / NO
Handled by:             _______________

Actions taken:
1. _______________
2. _______________
3. _______________

Follow-up items:
- [ ] Root cause fixed in code before next release
- [ ] Broken tag removed from Docker Hub (if applicable)
- [ ] Post-mortem shared with team
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## See Also

- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — Forward deployment checklist
- [`DISASTER_RECOVERY.md`](DISASTER_RECOVERY.md) — Infrastructure failure recovery
- [`production-runbook.md`](production-runbook.md) — Full operations guide

