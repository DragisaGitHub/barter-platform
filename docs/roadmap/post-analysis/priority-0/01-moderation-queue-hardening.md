# Priority 0 — Moderation Queue Hardening

> Scope: roadmap and implementation planning only.
>
> Source references: `docs/roadmap/post-analysis/01-launch-feature-gap-analysis.md`, `docs/roadmap/post-analysis/02-launch-feature-gap-verification.md`.

# Purpose

Harden the existing moderation queue by closing the remaining operational gaps in assignment and prioritization. The platform already has report creation, report review, queue counters, and a working admin/moderator queue page. The remaining work is to make ownership explicit and prevent queue actions from silently reassigning work.

# Verified Current Implementation

## Current moderation entities

- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportEntity.java`
  - `reporterUserId`
  - `targetType`
  - `targetUuid`
  - `reasonCode`
  - `details`
  - `status`
  - `assignedModeratorUserId`
  - `resolutionNote`
  - `resolvedAt`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/catalog/moderation/ListingModerationActionEntity.java`
  - item-level moderation audit/action record for remove/restore flows

## Current report statuses

- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportStatus.java`
  - `OPEN`
  - `IN_REVIEW`
  - `RESOLVED`
  - `DISMISSED`

## Current moderation endpoints

- `POST /reports`
  - implemented by `backend/barter-web/src/main/java/com/barterplatform/web/moderation/controller/ReportsController.java`
- `GET /admin/reports/summary`
- `GET /admin/reports`
- `GET /admin/reports/{reportUuid}`
- `PATCH /admin/reports/{reportUuid}/status`
  - implemented by `backend/barter-web/src/main/java/com/barterplatform/web/admin/controller/AdminReportsController.java`

## Current moderation frontend pages

- `frontend/src/features/admin/AdminReportsPage.tsx`
  - queue summary cards
  - filters by status, target type, and reason code
  - report list with pagination
  - detail panel with reporter/target context
  - status update form
  - item listing moderation actions for `ITEM` reports
- `frontend/src/routes/router.tsx`
  - route: `/admin/reports`
- `frontend/src/features/reports/ReportTrigger.tsx`
- `frontend/src/features/reports/ReportDialog.tsx`

## Existing database schema

- `backend/barter-web/src/main/resources/db/migration/V017__reports_foundation.sql`
  - `reports` table already includes `assigned_moderator_user_id`, `resolution_note`, and `resolved_at`
  - indexes already exist for:
	- `(status, created_at DESC)`
	- `(target_type, created_at DESC)`
	- `(target_uuid)`
	- `(reporter_user_id, status)`
	- `(assigned_moderator_user_id, status)`
- `backend/barter-web/src/main/resources/db/migration/V018__reports_queue_reason_index.sql`
  - index already exists for `(reason_code, status, created_at DESC)`
- `backend/barter-web/src/main/resources/db/migration/V012__listing_moderation_and_offer_invalidation.sql`
  - `listing_moderation_actions` table already exists for item moderation actions

# Confirmed Implementation Gaps

The queue is not missing core moderation primitives. The confirmed gaps are narrower:

1. `assignedModeratorUserId` exists on `ReportEntity`, but there is no explicit claim / assign / unassign API.
2. `ReportServiceImpl.updateReport(...)` currently overwrites `assignedModeratorUserId` with the acting moderator on every status change, which means status updates double as silent reassignment.
3. `GET /admin/reports` only filters by `status`, `targetType`, and `reasonCode`; it cannot filter by assignee, unassigned reports, or stale-only reports.
4. `AdminReportsPage.tsx` shows the current assignee but has no UI to claim a report, assign it to another moderator, release it, or filter to “my reports” / “unassigned”.
5. Queue history / timeline is still missing, but that belongs in `02-report-audit-trail.md` rather than this implementation unit.

# Scope

- Add explicit assignment workflow on top of the existing `assignedModeratorUserId` field.
- Decouple assignment from status transitions so report ownership is no longer changed implicitly.
- Add assignment-aware and stale-aware queue filtering to the existing `/admin/reports` list flow.
- Add the corresponding moderator controls to `AdminReportsPage.tsx`.

# Out of Scope

- Replacing the existing report status model.
- Rebuilding the current queue list/detail/status APIs from scratch.
- Audit timeline/history work covered by `02-report-audit-trail.md`.
- Bulk moderation actions.
- Escalation workflows, SLA automation, or case-management features.

# Concrete Technical Tasks

- Add a dedicated report assignment request contract in OpenAPI, e.g. `AdminAssignReportRequest`, with nullable moderator UUID support for unassign.
- Add a dedicated assignment endpoint, e.g. `PATCH /admin/reports/{reportUuid}/assignment`, instead of continuing to infer assignment from `PATCH /admin/reports/{reportUuid}/status`.
- Change `ReportServiceImpl.updateReport(...)` so status updates do not silently reassign the report to the actor.
- Add server-side validation that rejects a status update when the report is already assigned to another moderator and no explicit reassignment has happened.
- Extend `listAdminReports(...)` so the queue can be filtered by assignee, unassigned-only, and stale-only.
- Extend `ReportSpecifications` with assignment and stale-open predicates instead of keeping queue filtering limited to status / target / reason.

# Backend Changes

- Add `assignReport(...)` to `ReportService` and implement it in `ReportServiceImpl`.
- Resolve moderator UUIDs to user IDs when assigning through the new assignment endpoint, using the existing `UserRepository` lookup pattern.
- Preserve the existing status transition rules in `validateTransition(...)`; only remove the implicit assignment side effect from `updateReport(...)`.
- Reject cross-moderator overwrite scenarios where moderator B changes the status of a report already assigned to moderator A without an explicit reassignment step.
- Extend the queue query contract and implementation to support:
  - `assignedModeratorUuid`
  - `unassignedOnly`
  - `staleOnly`

# Frontend Changes

- Add explicit claim / unassign / reassign controls to `frontend/src/features/admin/AdminReportsPage.tsx` instead of showing `assignedModerator` as read-only text.
- Add queue filters for:
  - assigned to me
  - unassigned only
  - stale open reports
- Keep the existing status update form, but stop treating status change as the only way a moderator can take ownership of a report.
- Surface assignment conflicts returned by the backend so moderators understand why a status update was rejected.

# Database Changes

- No schema change is required for the core assignment workflow because `reports.assigned_moderator_user_id` and its supporting index already exist.
- No new queue index is required unless the new stale-only or assignee filters prove slow in practice after implementation.

# API Changes

- Add an assignment endpoint to `backend/barter-api/src/main/resources/openapi/paths/admin-reports.yaml`.
- Extend the `/admin/reports` query contract with assignment-aware filters.
- Keep `/admin/reports/{reportUuid}/status` focused on status and resolution-note changes only.

# Testing Requirements

- Add service tests covering:
  - explicit assignment
  - unassignment
  - blocking status changes when the report is assigned to another moderator
  - assignment-aware queue filtering
- Add MVC tests for the new assignment endpoint and new `/admin/reports` query parameters.
- Add frontend tests for:
  - assigned-to-me filtering
  - unassigned filtering
  - stale-only filtering
  - assignment conflict error handling in `AdminReportsPage.tsx`

# Risks

- Leaving implicit reassignment in place will keep queue ownership ambiguous even though `assignedModeratorUserId` already exists.
- Adding assignment UI without backend conflict checks will let moderators overwrite each other’s work.
- Pulling audit-history work into this item will expand the scope beyond queue hardening.

# Acceptance Criteria

- `assignedModeratorUserId` is managed through an explicit assignment action rather than being overwritten inside `updateReport(...)`.
- Moderators can filter the queue by assignee, unassigned reports, and stale open reports.
- A moderator cannot silently take over another moderator’s report by changing status.
- `AdminReportsPage.tsx` exposes explicit assignment controls that match the backend contract.
- Queue hardening remains limited to assignment and prioritization; report timeline/history work stays in `02-report-audit-trail.md`.

# Suggested Implementation Order

1. Add OpenAPI contract for report assignment and assignment-aware queue filters.
2. Implement backend assignment flow and remove implicit reassignment from `updateReport(...)`.
3. Add backend conflict validation and tests for cross-moderator status updates.
4. Add assignment and stale filters to `/admin/reports`.
5. Update `AdminReportsPage.tsx` to support claim / unassign / reassign and the new filters.
6. Keep report history/timeline work sequenced under `02-report-audit-trail.md`.
