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
- No reported operational metrics such as pending reports, new users, active listings, failed uploads, negative reviews, or system status.

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

- Add admin operational summary endpoint(s).
- Implement aggregate queries with clear time windows.
- Protect endpoint with admin/moderator roles as appropriate.

# Frontend Changes

- Replace or augment dashboard navigation cards with metric cards, status badges, and links.
- Add loading/error/empty states.
- Avoid chart-heavy UI until metrics prove useful.

# Database Changes

- No schema required if aggregates use existing tables.
- Add indexes only if aggregate queries become slow and are measured.

# Deployment Changes

- No new infrastructure.
- Include dashboard endpoint in admin smoke tests.

# Testing Strategy

- Backend tests for aggregate correctness with seeded data.
- Authorization tests.
- Frontend rendering tests for loading/error/metric states.
- Manual smoke test after reports dashboard exists.

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

# Explicitly Deferred

- Full BI/analytics warehouse.
- Event streaming analytics.
- Complex charting library dependency unless justified.
- Predictive moderation scoring.
