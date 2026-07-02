# Admin Operations Center

## Purpose

The Admin Operations Center (`/admin/operations`) is the single place where platform administrators can observe the current operational state of Zameni.rs. It is designed to be:

- **Safe** — never exposes secrets, raw connection strings, environment variables, or user PII.
- **Extensible** — each section is a self-contained tab so new modules can be wired in without touching existing ones.
- **Honest about placeholders** — when real integrations are not yet available, the UI clearly shows a placeholder state rather than empty data.

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
| Backups | ✅ Live (Azure Blob) | `GET /api/v1/admin/operations/backups` |
| Deployments | ✅ Metadata | `GET /api/v1/admin/operations/deployments` |
| Costs | ✅ Live (Azure Cost Management) | `GET /api/v1/admin/operations/costs` |
| Monitoring | ✅ Live (Actuator + JVM) | `GET /api/v1/admin/operations/monitoring` |
| Security | ✅ Live (config posture) | `GET /api/v1/admin/operations/security` |

### Backend structure

```
barter-web/
  admin/
    controller/
      AdminOperationsController.java          ← implements AdminOperationsApi
    service/
      AdminOperationsOverviewService.java     ← live data from DB + runtime
      AdminOperationsBackupsService.java      ← Azure Blob Storage backup listing
      AdminOperationsDeploymentsService.java  ← safe env metadata + placeholder
      AdminOperationsCostsService.java        ← Azure Cost Management Query API
      AdminOperationsMonitoringService.java   ← Actuator, JVM MXBeans, infra probes
      AdminOperationsSecurityService.java     ← security configuration posture
```

### Frontend structure

```
features/admin/
  AdminOperationsPage.tsx      ← tab layout + all tab content
  useAdminOperations.ts        ← React Query hooks (overview, backups, deployments, costs, monitoring, security)
api/
  adminOperationsApi.ts        ← axios wrappers for all six endpoints
i18n/locales/en/admin.json     ← operationsPage section
i18n/locales/sr/admin.json     ← operationsPage section (Serbian)
```

---

## Security tab

### Purpose

The Security tab provides a **read-only configuration posture snapshot** for administrators. It evaluates existing application configuration and environment state to surface security risks without requiring any external API calls.

Each section returns a status value: **OK**, **WARNING**, **CRITICAL**, or **UNKNOWN**.

The backend endpoint is `GET /api/v1/admin/operations/security` and is served by `AdminOperationsSecurityService`.

### What is checked

#### 1. Authentication
- `jwtConfigured` — whether `JWT_SECRET` is set and non-blank
- `accessTokenMinutes` — access token expiration in minutes (sourced from config, not secret)
- `refreshTokenDays` — refresh token expiration in days
- `emailVerificationEnabled` — whether new accounts must verify email
- `bootstrapAdminEnabled` — whether `BARTER_BOOTSTRAP_ADMIN_ENABLED=true` (dangerous in prod)
- `swaggerEnabled` — whether the Swagger UI is accessible

Status rules:
- **CRITICAL** — JWT secret missing, bootstrap admin enabled, email verification disabled, or Swagger enabled in production
- **WARNING** — Swagger enabled in non-production, or access token > 60 minutes
- **OK** — all production-safe settings present

#### 2. CORS
- `allowedOriginsConfigured` — whether at least one origin is configured
- `allowedOriginsCount` — number of configured origins (safe count, no values)
- `allowCredentials` — whether credentials are allowed in CORS requests
- `allowedMethods` — list of allowed methods (not sensitive)
- `exposedHeaders` — headers exposed to browser (not sensitive)

Status rules:
- **CRITICAL** — `allowCredentials=true` with no allowed origins
- **WARNING** — no origins configured
- **OK** — at least one origin configured

#### 3. Storage
- `azureBlobConfigured` — whether `AZURE_STORAGE_CONNECTION_STRING` is set (boolean only, no value)
- `imageContainerConfigured` — whether `AZURE_STORAGE_CONTAINER` is set
- `backupContainerConfigured` — whether `BACKUP_AZURE_CONTAINER` is set

Status rules:
- **WARNING** — Azure Blob not configured (uses local disk)
- **OK** — Azure Blob and image container both configured

#### 4. Backups
- `backupEnabled` — whether `BACKUP_ENABLED=true`
- `backupMode` — `azure-blob` or `unconfigured`
- `backupContainerConfigured` / `backupPrefixConfigured` — configuration completeness

Status rules:
- **CRITICAL** — `BACKUP_ENABLED=false` (no active backups in production)
- **WARNING** — backups enabled but container not configured
- **OK** — backups enabled and container configured

#### 5. Email
- `smtpConfigured` — whether `SPRING_MAIL_HOST` is set (boolean only, no hostname)
- `mailFromConfigured` — whether the mail-from address differs from the default
- `emailVerificationEnabled` — same value as in authentication section

Status rules:
- **CRITICAL** — email verification disabled
- **WARNING** — SMTP not configured (uses console logging fallback)
- **OK** — SMTP configured and verification enabled

#### 6. Observability
- `backendSentryConfigured` — whether `SENTRY_DSN_BACKEND` is set (boolean only, no DSN value)
- `frontendSentryRuntimeKnown` — always `null` (frontend state is not observable from the backend)
- `operationsMonitoringAvailable` — always `true` (Monitoring tab is live)

Status rules:
- **WARNING** — backend Sentry not configured
- **OK** — backend Sentry configured

#### 7. Deployment Safety
- `releaseVersionConfigured` — whether `BARTER_RELEASE_VERSION` is set
- `deployedAtConfigured` — whether `BARTER_DEPLOYED_AT` is set
- `deploySourceConfigured` — whether `BARTER_DEPLOY_SOURCE` is set
- `immutableImageTagsDetected` — whether the release version is not a mutable tag (latest/main/master/develop). `null` when release version not set

Status rules:
- **WARNING** — release version and deploy timestamp not set; mutable image tags detected; or deploy source not set
- **OK** — all deployment tracking fields configured with an immutable tag

#### 8. Edge / Security Headers
- `httpsAssumedEnabled` — `true` if production profile is active, `null` otherwise
- `caddyConfigured` — `true` if production profile is active (Caddy assumed), `null` otherwise
- `hstsKnown` — always `true` (HSTS is hardcoded in `SecurityConfig`)

Status rules:
- **OK** — production profile active (Caddy + HTTPS assumed)
- **UNKNOWN** — non-production environment; edge state cannot be determined from the backend

#### 9. Overall
- `overallStatus` — worst status across all sections (CRITICAL > WARNING > UNKNOWN > OK)
- `notes` — summary list of sections requiring attention

### What is intentionally not exposed

| Item | Reason |
|------|--------|
| `JWT_SECRET` value | Cryptographic secret — exposure would allow token forgery |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP credentials |
| `AZURE_STORAGE_CONNECTION_STRING` | Storage account key |
| `BACKUP_AZURE_CONNECTION_STRING` | Backup storage account key |
| `AZURE_CLIENT_SECRET` | Service principal credential |
| `SENTRY_DSN_BACKEND` | Full DSN contains project key |
| `BARTER_BOOTSTRAP_ADMIN_PASSWORD` | Bootstrap admin credential |
| CORS allowed origins list | Treated as safe (allowed origins/methods are returned) |

### How the service works

`AdminOperationsSecurityService` is constructed with `@Value`-injected properties and a `SecurityProperties` bean. It reads all values at request time (no caching) from:

- Spring `Environment` (for deployment env vars)
- `SecurityProperties` ConfigurationProperties bean (for CORS/security config)
- Individual `@Value` bindings for JWT, email, storage, backup, Sentry

Each section builder method produces its own status independently. The overall status is the worst-case across all sections.

No external network calls are made.

### React Query

The security hook uses `staleTime: 60_000` (60 seconds) — consistent with the Monitoring tab. The data is considered fresh for 60 seconds after each fetch, preventing unnecessary re-requests on tab switch.

---

## Azure Cost Management integration

### How it works

`AdminOperationsCostsService` authenticates with the Microsoft identity platform using the **client credentials flow** (service principal), then calls the [Azure Cost Management Query API](https://learn.microsoft.com/en-us/rest/api/cost-management/query/usage) to return spend data for the admin UI.

The service makes two API calls on the first request (after which results are cached for 15 minutes):

1. **Current month** — `timeframe: MonthToDate`, `granularity: Daily`, grouped by `ServiceName`. Provides daily trend data and per-service breakdown for the current billing month.
2. **Previous month** — `timeframe: TheLastMonth`, `granularity: None`, grouped by `ServiceName`. Provides the previous month total.

A simple linear projection is computed from the current month data:
```
projectedMonthCost = (currentMonthCost / dayOfMonth) * daysInMonth
```

### API endpoint used

```
POST https://management.azure.com/{scope}/providers/Microsoft.CostManagement/query?api-version=2025-03-01
```

> ⚠️ The deprecated Azure Consumption Usage Details API (`/providers/Microsoft.Consumption/usageDetails`) is **not used**.

### Required environment variables

| Variable | Description |
|----------|-------------|
| `AZURE_TENANT_ID` | Azure Active Directory tenant ID. **Never logged or returned.** |
| `AZURE_CLIENT_ID` | Service principal (app registration) client ID. **Never logged or returned.** |
| `AZURE_CLIENT_SECRET` | Service principal client secret. **Never logged or returned.** |
| `AZURE_SUBSCRIPTION_ID` | Azure subscription ID used to derive the default cost scope. |
| `AZURE_COST_SCOPE` | *(Optional)* Override the default scope. Defaults to `/subscriptions/{AZURE_SUBSCRIPTION_ID}`. Example: `/subscriptions/{id}/resourceGroups/{rg}`. |

### Required Azure RBAC permission

The service principal must have the **Cost Management Reader** role (`Microsoft.CostManagement/*/read`) assigned at the appropriate scope (subscription or resource group). The built-in role ID is `72fafb9e-0641-4937-9268-a91bfd8191a3`.

Minimum required actions:
- `Microsoft.CostManagement/query/action`

### Caching behavior

Results are cached in-memory for **15 minutes** per application instance. This avoids repeated Azure API calls on each admin page load.

- The cache is stored in a `volatile` field — simple visibility guarantee for single-instance deployments.
- The React Query client adds a client-side `staleTime` of 10 minutes to further reduce re-fetches.
- Access tokens are also cached until 5 minutes before their `expires_in` expiry (typically ~55 minutes for Azure tokens).

To force a fresh fetch, click the **Refresh** button in the UI or restart the application.

### Graceful degradation

| Condition | Response |
|-----------|----------|
| Any of `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, or scope is absent/blank | `availability: "placeholder"` — safe static response, never throws |
| Azure token endpoint returns an error or times out | `availability: "unavailable"` — generic note, no Azure error body exposed |
| Cost Management API returns an error or unexpected format | `availability: "unavailable"` — generic note, no Azure error body exposed |
| Azure configured and reachable, data returned successfully | `availability: "configured"` — full cost metadata returned |

### Security notes

- `AZURE_CLIENT_SECRET` and access tokens are **never** included in any API response or log message.
- Error messages are sanitized before logging — `client_secret=`, `access_token=`, and Bearer tokens are redacted via regex.
- The endpoint is guarded by `@PreAuthorize("hasRole('ADMIN')")` — unauthenticated requests receive 401, non-admin tokens receive 403.
- Azure error response bodies are **not** forwarded to the client to prevent leaking subscription or tenant information.

### Known limitations

- **Single scope**: Only one cost scope is supported per application instance. Use `AZURE_COST_SCOPE` to target a resource group if needed.
- **No pagination**: The Cost Management API may paginate large result sets. The current implementation does not follow `nextLink` URLs. For large environments with many daily rows this may result in partial data.
- **Linear projection only**: `projectedMonthCost` is a simple linear extrapolation — not a forecast from the Azure Forecast API.
- **In-memory cache only**: Cache does not survive application restarts. For multi-instance deployments, each instance maintains its own 15-minute cache independently.
- **No resource group breakdown**: Costs are grouped by `ServiceName`. Resource group or tag-based breakdown is not supported in the current implementation.

---

## Future improvements for the Security tab

The following enhancements are planned for future releases:

- **Failed login metrics** — track and display recent failed authentication attempts (brute force signals). Requires adding a persistent failure counter to the auth service.
- **Admin audit log** — surface the last N admin actions (role changes, report decisions, listing removals) for accountability. Requires a dedicated `admin_audit_log` table.
- **Rate limit stats** — display current in-memory rate limit hit counts per endpoint. Requires exposing the `RateLimitService` metrics to the security service.
- **IP ban / fail2ban visibility** — if a fail2ban or IP block list is added at the infrastructure level, surface blocked IP counts and last ban events.
- **Security header live probe** — make an outbound HTTP HEAD request to the production frontend URL and verify that `Strict-Transport-Security`, `Content-Security-Policy`, and `X-Frame-Options` are present in the response. Requires an opt-in probe URL config.
- **Dependency vulnerability status** — integrate with a CVE/NVD feed or OWASP Dependency Check report artifact to surface known vulnerable dependencies. Requires a build-time artifact or sidecar service.

---

## Backup integration

### How it works

`AdminOperationsBackupsService` connects to Azure Blob Storage on every `GET /admin/operations/backups` request when configured. It:

1. Lists all blobs under `BACKUP_AZURE_CONTAINER / BACKUP_AZURE_PREFIX /`.
2. Filters blobs ending in `.dump.gz`.
3. Finds the newest blob by parsing the `yyyyMMdd'T'HHmmssZ` timestamp embedded in the filename (e.g. `barter-barter_db-20260701T194213Z.dump.gz` → `2026-07-01T19:42:13Z`). Falls back to Azure's blob `lastModified` property if the filename timestamp cannot be parsed.
4. Returns safe metadata: blob name (path), parsed backup timestamp, blob last-modified, size in bytes, container, prefix, provider (`azure-blob`), and `scheduledBackupEnabled` flag.

The connection to Azure uses a single retry with a 15-second timeout — it is a read-only admin operation and must not cause long hangs.

### Required environment variables

| Variable | Description |
|----------|-------------|
| `BACKUP_AZURE_CONNECTION_STRING` | Azure Storage account connection string. **Never logged or returned.** |
| `BACKUP_AZURE_CONTAINER` | Container name where backup blobs are stored (e.g. `postgres-backups`). |
| `BACKUP_AZURE_PREFIX` | Path prefix for backup blobs (e.g. `prod/postgres`). |
| `BACKUP_ENABLED` | Set to `true` to indicate that scheduled backups are active. Controls the `scheduledBackupEnabled` field. |

### Graceful degradation

| Condition | Response |
|-----------|----------|
| `BACKUP_AZURE_CONNECTION_STRING` or `BACKUP_AZURE_CONTAINER` is absent/blank | `availability: "placeholder"` — safe static response, never throws |
| Azure connection fails or listing throws | `availability: "unavailable"` — error is logged with connection string redacted |
| Azure configured and reachable but no blobs found | `availability: "configured"`, blob fields null, `note` explains no blobs found |
| Azure configured, blobs found | `availability: "configured"`, full metadata returned |

### Security notes

- The Azure connection string (`BACKUP_AZURE_CONNECTION_STRING`) is **never** included in any API response.
- Log messages sanitise `AccountKey=`, `SharedAccessSignature=`, and `sig=` values via regex replacement with `[REDACTED]`.
- The endpoint is guarded by `@PreAuthorize("hasRole('ADMIN')")` — unauthenticated requests receive 401, non-admin tokens receive 403.
- Container name and blob prefix are safe operational metadata and are returned to the admin UI.

### Limitations

- **No restore verification**: The service only reads blob metadata. It does not download or verify backup integrity.
- **Single Azure region**: Connects to a single Storage account. Multi-region visibility is not supported.
- **No pagination**: All blobs under the prefix are listed in a single call. Consider adding `maxResults` for very large archives.
- **No scheduled next-backup time**: `nextScheduledBackupTimestamp` is not populated — backup scheduling happens in external scripts.

### Future restore verification plan

1. After each successful `pg_restore` run in CI/staging, write a `restore-verification.json` blob alongside the backup.
2. Update `AdminOperationsBackupsService` to read this sidecar blob and populate a `lastRestoreVerifiedAt` field.
3. Surface the restore verification timestamp in the Backups tab UI alongside a pass/fail badge.

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

### Deployments tab
Returns safe metadata available to the application:
- Active Spring profiles (as environment label)
- Application version from `BuildProperties` (if available)
- Last deployment timestamp from `BARTER_DEPLOYED_AT` env var or `barter.deployment.deployed-at` property (if set)
- `availability: "placeholder"` when no timestamp is found, `"configured"` otherwise

---

## Security

All endpoints require `ROLE_ADMIN` via `@PreAuthorize("hasRole('ADMIN')")` on the controller class.

Integration tests verify:
- Admin token → 200 OK
- Regular user token → 403 Forbidden
- No token → 401 Unauthorized
- Missing backup config → placeholder response (no crash)
- Missing Azure cost config → placeholder response (no crash)
- Security endpoint → CRITICAL overall when backups disabled (default test config)
- Security endpoint → no secrets in response body

---

## Future integrations

The following integrations are deferred and will be wired into existing tabs once the corresponding infrastructure is in place.

### Deployments tab
- **GitHub Actions** — use the GitHub REST API (`/repos/{owner}/{repo}/actions/runs`) to show the last successful workflow run, commit SHA, actor, and duration.
- **Docker image tag** — read from a deployment state file or environment variable set at build time.

### Monitoring tab
- **Sentry** — recent error rate, unresolved issues count, P95 response time.
- **Uptime** — external uptime check integration.

### Security tab
See [Future improvements for the Security tab](#future-improvements-for-the-security-tab) above.

---

## i18n

All visible text uses the `admin:operationsPage` namespace with keys for both English (`en`) and Serbian (`sr`). New integrations should add i18n keys to both files before shipping visible text.

Key prefix structure:
```
operationsPage.tabs.*                    ← tab labels
operationsPage.backupsTab.*              ← Backups tab content
operationsPage.backupsTab.metrics.*      ← Backups tab metric labels
operationsPage.backupsTab.availability.* ← Backups availability badge labels
operationsPage.deploymentsTab.*          ← Deployments tab content
operationsPage.costsTab.*                ← Costs tab content
operationsPage.costsTab.metrics.*        ← Costs tab metric labels
operationsPage.costsTab.availability.*   ← Costs availability badge labels
operationsPage.monitoringTab.*           ← Monitoring tab content
operationsPage.monitoringTab.metrics.*   ← Monitoring tab metric labels
operationsPage.monitoringTab.statusLabels.* ← Monitoring status badge labels
operationsPage.securityTab.*             ← Security tab content
operationsPage.securityTab.sections.*    ← Security tab section titles/descriptions
operationsPage.securityTab.metrics.*     ← Security tab metric labels
operationsPage.securityTab.statusLabels.* ← Security status badge labels
operationsPage.cards.*                   ← Overview tab card titles/descriptions
operationsPage.metrics.*                 ← Overview tab metric labels
operationsPage.statusLabels.*            ← Shared status badge labels
```
