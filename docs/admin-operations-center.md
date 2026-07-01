# Admin Operations Center

## Purpose

The Admin Operations Center (`/admin/operations`) is the single place where platform administrators can observe the current operational state of Zameni.rs. It is designed to be:

- **Safe** — never exposes secrets, raw connection strings, environment variables, or user PII.
- **Extensible** — each section is a self-contained tab so new modules can be wired in without touching existing ones.
- **Honest about placeholders** — when real integrations are not yet available, the UI clearly shows "Coming soon" or a placeholder state rather than empty data.

---

## Architecture

### Routing

```
/admin/operations  →  AdminOperationsPage  (ADMIN role required)
```

The page lives under the existing `AdminRoute` guard and is registered in `routePaths.admin.operations`.

### Tab structure

| Tab | Status | Backend endpoint |
|-----|--------|-----------------|
| Overview | ✅ Live | `GET /api/v1/admin/operations/overview` |
| Backups | ✅ Placeholder | `GET /api/v1/admin/operations/backups` |
| Deployments | ✅ Placeholder | `GET /api/v1/admin/operations/deployments` |
| Costs | 🔜 Coming soon | — |
| Monitoring | 🔜 Coming soon | — |
| Security | 🔜 Coming soon | — |

### Backend structure

```
barter-web/
  admin/
    controller/
      AdminOperationsController.java        ← implements AdminOperationsApi
    service/
      AdminOperationsOverviewService.java   ← live data from DB + runtime
      AdminOperationsBackupsService.java    ← placeholder (future: Azure/cron)
      AdminOperationsDeploymentsService.java← safe env metadata + placeholder
```

### Frontend structure

```
features/admin/
  AdminOperationsPage.tsx      ← tab layout + all tab content
  useAdminOperations.ts        ← React Query hooks (overview, backups, deployments)
api/
  adminOperationsApi.ts        ← axios wrappers for the three endpoints
i18n/locales/en/admin.json     ← operationsPage section
i18n/locales/sr/admin.json     ← operationsPage section (Serbian)
```

---

## Current placeholder implementation

### Overview tab
Fully live. Reads from the database via lightweight aggregate queries:
- System info (app name, version, active profiles, server time, uptime)
- Health (database connectivity, storage provider type)
- User counters (total, active, suspended, banned, pending verification)
- Marketplace counters (items, listings, trade offers)
- Moderation queue (open/in-review reports, negative reviews)
- Storage (image record counts, provider type)
- Deployment (environment label, last deployment timestamp if configured)

### Backups tab
Returns placeholder/static data. The response correctly represents the current state:
- `availability: "placeholder"` — no automated backup pipeline is configured yet
- `scheduledBackupEnabled: false`
- `note` — human-readable message explaining the current state

### Deployments tab
Returns safe metadata available to the application:
- Active Spring profiles (as environment label)
- Application version from `BuildProperties` (if available)
- Last deployment timestamp from `BARTER_DEPLOYED_AT` env var or `barter.deployment.deployed-at` property (if set)
- `availability: "placeholder"` when no timestamp is found, `"configured"` otherwise

---

## Security

All three endpoints require `ROLE_ADMIN` via `@PreAuthorize("hasRole('ADMIN')")` on the controller class.

Integration tests verify:
- Admin token → 200 OK
- Regular user token → 403 Forbidden
- No token → 401 Unauthorized

---

## Future integrations

The following integrations are deferred and will be wired into existing tabs once the corresponding infrastructure is in place. Each can be added by updating the relevant service and extending the OpenAPI schema — no changes to routing or tab structure are needed.

### Backups tab
- **Azure PostgreSQL automated backups** — query Azure Management API for last backup time, retention period, backup type (full/incremental).
- **pg_dump cron jobs** — read a deployment state file or environment variable written by the backup script to report last backup and size.
- **Backup restore verification** — flag from a scheduled restore-test job.

### Deployments tab
- **GitHub Actions** — use the GitHub REST API (`/repos/{owner}/{repo}/actions/runs`) to show the last successful workflow run, commit SHA, actor, and duration.
- **Docker image tag** — read from a deployment state file or environment variable set at build time.
- **Rollback capability** — surface the last N deployment records to enable one-click rollback from the UI.

### Costs tab (new)
- **Azure Cost Management API** — query current month spend by resource group, surface daily burn rate and forecast.
- **Azure Blob Storage** — storage account size and egress costs.

### Monitoring tab (new)
- **Sentry** — recent error rate, unresolved issues count, P95 response time.
- **Server health** — CPU, memory, disk usage via a lightweight agent or `/actuator/health` endpoint.
- **Uptime** — external uptime check integration (e.g. UptimeRobot or Better Uptime).

### Security tab (new)
- **Login anomalies** — failed login attempts above threshold, geo-anomalies.
- **Admin audit log** — last N admin actions (moderation decisions, user status changes).
- **Security headers** — confirm HSTS, CSP, and other headers are active.

---

## i18n

All visible text uses the `admin:operationsPage` namespace with keys for both English (`en`) and Serbian (`sr`). New integrations should add i18n keys to both files before shipping visible text.

Key prefix structure:
```
operationsPage.tabs.*             ← tab labels
operationsPage.backupsTab.*       ← Backups tab content
operationsPage.deploymentsTab.*   ← Deployments tab content
operationsPage.comingSoon.*       ← Coming soon states (costs, monitoring, security)
operationsPage.cards.*            ← Overview tab card titles/descriptions
operationsPage.metrics.*          ← Overview tab metric labels
operationsPage.statusLabels.*     ← Shared status badge labels
```

