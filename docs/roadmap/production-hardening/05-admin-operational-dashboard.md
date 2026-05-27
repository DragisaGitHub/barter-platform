# Production Hardening 05 — Admin Operational Dashboard

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P1 operations / moderation velocity  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Turn the admin dashboard from navigation/status cards into a small operational control panel for launch and beta operations.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap notes admin dashboard is not operational yet.
- Small teams need quick visibility into reports, users, listings, offers, reviews, and system status without querying the database.

# Current State

- Admin dashboard and admin pages exist.
- Implemented: an ADMIN-only operational dashboard is available at `/admin/operations`.
- Implemented: the backend exposes `GET /api/v1/admin/operations/overview` for production-safe operational visibility.
- Implemented: the endpoint uses simple aggregate count queries and lightweight runtime checks only.

# Risks

- Moderation and operations become reactive and manual.
- Growth or abuse signals can be missed.
- Dashboard scope can balloon into analytics before core operations are solved.

# Proposed Solution

- Add compact dashboard metrics: pending/open reports, stale reports, new users 24h/7d, active listings, removed/restored listings, pending offers, negative reviews, and backend health summary if available.
- Use simple aggregate queries and cache only if needed.
- Link each card to the relevant admin queue or filtered list.
- Keep this operational, not a vanity analytics product.

# Simpler Alternatives

- Start with pending reports, new users, active listings, and negative reviews only.
- Use daily manual SQL snippets temporarily, but replace before broader public beta.

# Architecture Impact

- Keep the modular monolith and simple container deployment as the default. Add infrastructure only when it directly reduces production risk.
- Add admin summary endpoints inside existing backend.
- Avoid analytics warehouse or event pipeline.

# Operational Impact

- Improves daily triage and launch monitoring.
- Provides quick checks after deploys and campaigns.
- Creates foundation for simple runbook-driven operations.

# Security Impact

- Admin metrics can expose sensitive operational data and must remain admin/moderator restricted.
- Avoid displaying report details in broad dashboard cards.

# Developer Velocity Impact

- Reduces manual support/debugging time.
- Aggregate endpoints are straightforward if kept focused.

# Backend Changes

- Added OpenAPI contract and generated DTO alignment for `GET /api/v1/admin/operations/overview`.
- Added `AdminOperationsController` protected with `hasRole('ADMIN')`.
- Added `AdminOperationsOverviewService` to aggregate lightweight, production-safe sections:
  - system: application name, optional version, active profiles, server time, uptime;
  - health: overall status, database status, storage provider type, storage readiness note;
  - users / identity: total, active, suspended, banned, pending verification users;
  - marketplace: total items, active listings, removed listings, open offers, completed trades;
  - moderation: open, in-review, resolved, dismissed reports, negative reviews;
  - storage: total image records, primary image count, provider type;
  - deployment: active environment/profile, deployment-state availability, optional last deployment timestamp.
- Added repository count methods only where needed; no large dataset loading or new schema was introduced.
- Storage status intentionally reports configured provider readiness without probing Azure Blob Storage, avoiding expensive or brittle remote calls from the dashboard endpoint.

# Frontend Changes

- Added `/admin/operations` route inside the existing ADMIN-only admin shell.
- Added sidebar and admin landing-page navigation to the operations dashboard.
- Added operational cards for:
  - System Health;
  - Users & Security;
  - Marketplace Activity;
  - Moderation Queue;
  - Storage;
  - Deployment.
- Added loading state, error state, and manual refresh action using existing TanStack Query/API patterns.
- The UI uses simple cards and badges only; no charting, WebSockets, streaming, or new monitoring infrastructure was added.

# Database Changes

- No schema required if aggregates use existing tables.
- Add indexes only if aggregate queries become slow and are measured.

# Deployment Changes

- No new infrastructure.
- Include dashboard endpoint in admin smoke tests.
- Optional deployment timestamp support is intentionally passive: if `barter.deployment.deployed-at` or `BARTER_DEPLOYED_AT` is supplied as an ISO-8601 timestamp, the dashboard displays it; otherwise it shows deployment state as unavailable.

# Testing Strategy

- Added backend service tests for aggregate mapping and degraded database status handling.
- Added backend integration/security coverage:
  - admin can access;
  - regular user receives `403`;
  - unauthenticated user receives `401`;
  - response includes expected summary sections.
- Frontend build validation covers the new page and typed API wiring. Dedicated frontend tests were not added because the current frontend does not have stable admin page test conventions.

# Security and Privacy Boundaries

- The operations endpoint is ADMIN-only, not MODERATOR-accessible.
- The response intentionally does **not** expose:
  - JWT secrets;
  - Azure Storage connection strings;
  - SMTP credentials;
  - raw environment dumps;
  - raw filesystem paths;
  - user email lists or PII-heavy records;
  - provider-native blob URLs or internal infrastructure details.
- The dashboard is for operational awareness, not investigation detail. Admins should use existing focused admin pages for user, report, listing, and review records.

# Metrics Platform Non-Goal

- This dashboard is not a Prometheus/Grafana replacement.
- It does not implement alerting, time-series retention, live streaming, WebSockets, Kubernetes observability, distributed tracing, CDN logs, or paid monitoring services.
- It is a compact launch/beta operations view for the current Docker Compose DEV deployment model.

# Rollout Plan

- Ship minimal metrics first.
- Use during beta operations.
- Add/remove cards based on real admin usage.
- Only add trends/charts after daily operational questions stabilize.

# Future Improvements

- Trend dashboard for growth and moderation load.
- Exportable operational reports.
- Alert thresholds linked to metrics.
- Admin analytics after core dashboard is stable.
- Optional Prometheus/Grafana, CDN/access-log summaries, and object-storage diagnostics after production usage proves the need.

# Explicitly Deferred

- Full BI/analytics warehouse.
- Event streaming analytics.
- Complex charting library dependency unless justified.
- Predictive moderation scoring.
