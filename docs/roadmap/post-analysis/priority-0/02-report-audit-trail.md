# Priority 0 — Report Audit Trail

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Why this is still Priority 0

The reporting system is live, but moderator decisions are still stored as the current row state only. A moderator can move a report from `OPEN` to `IN_REVIEW` to `RESOLVED`, but there is no append-only history explaining who changed it, when it changed, or what the previous state was.

## Verified current implementation

### Backend

- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportEntity.java`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportStatus.java`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportTargetType.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/moderation/service/ReportService.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/moderation/service/impl/ReportServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/moderation/service/impl/ReportTargetResolverImpl.java`
- `backend/barter-web/src/main/java/com/barterplatform/web/moderation/controller/ReportsController.java`
- `backend/barter-web/src/main/java/com/barterplatform/web/admin/controller/AdminReportsController.java`

### Frontend

- `frontend/src/features/reports/ReportDialog.tsx`
- `frontend/src/features/reports/ReportTrigger.tsx`
- `frontend/src/features/admin/AdminReportsPage.tsx`
- `frontend/src/features/admin/useAdminReports.ts`
- `frontend/src/api/adminReportsApi.ts`

### OpenAPI / schema / database

- `backend/barter-api/src/main/resources/openapi/paths/admin-reports.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/report/ReportDetailResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/report/AdminUpdateReportRequest.yaml`
- `backend/barter-web/src/main/resources/db/migration/V017__reports_foundation.sql`
- `backend/barter-web/src/main/resources/db/migration/V018__reports_queue_reason_index.sql`

## Already implemented

- Report creation already works for all current target types: `ITEM`, `USER`, `MESSAGE`, `TRADE_OFFER`, and `REVIEW`.
- Moderator queue list, queue summary, detail fetch, and status update flows already exist.
- `ReportServiceImpl` already enforces transition rules and requires `resolutionNote` for terminal states.
- `AdminReportsPage.tsx` already provides queue filters, detail loading, status drafting, and resolution-note editing.

## Confirmed missing

1. **No append-only report history model exists.**
   - There is no report-history entity, repository, or migration in `backend/**`.
   - `ReportEntity` stores only the latest `status`, `assignedModeratorUserId`, `resolutionNote`, and `resolvedAt`.

2. **Status changes overwrite state instead of recording events.**
   - `ReportServiceImpl.updateReport(...)` mutates the same `ReportEntity` row in place.
   - That means the system loses prior moderator, prior status, and earlier notes.

3. **The API exposes current state only.**
   - `ReportDetailResponse.yaml` has no `history`, `events`, or timeline field.
   - `admin-reports.yaml` exposes list/detail/update endpoints only; there is no history endpoint.

4. **The admin UI has no timeline surface.**
   - `AdminReportsPage.tsx` shows the selected report and current draft state, but not a chronological audit trail.

## Not needed / false positives

- Do **not** build a platform-wide audit subsystem here.
- Do **not** redesign report submission; report creation already exists and is not the gap.
- Do **not** treat listing moderation history as missing work for this document; listing moderation already has its own action model in `ListingModerationActionEntity` and is tracked separately.

## Intentionally deferred

- No explicit retention-policy implementation is present in code today. Keep retention/privacy policy work documented, but do not block the minimal launch audit trail on broader compliance tooling.

## Implementation-ready backlog

### Backend

1. Add a report-history table and entity, separate from `reports`, with append-only records for:
   - report created
   - status changed
   - moderator assigned/reassigned
   - resolution note recorded/changed
2. Write history entries from the same service path that currently mutates `ReportEntity` (`ReportServiceImpl`).
3. Store actor identity explicitly; current `assignedModeratorUserId` is not enough for historical traceability.

### API

4. Extend `ReportDetailResponse` or add a dedicated history endpoint so the admin UI can read ordered report events.
5. Keep the payload minimal: actor, event type, previous status, new status, note, timestamp.

### Frontend

6. Add a moderator-visible timeline to `AdminReportsPage.tsx` detail view.
7. Distinguish current report state from historical entries so the latest status is not mistaken for the full story.

## Dependencies and follow-on impact

- Depends on the already-reviewed moderation queue baseline in `priority-0/01-moderation-queue-hardening.md`.
- Unblocks later governance work in:
  - `priority-2/03-non-item-moderation-actions.md`
  - `priority-2/04-admin-review-governance-improvements.md`

## Exit criteria

- A moderator can see an ordered event history for every report.
- Every report status change is auditable without reading database row diffs.
- History is sourced from report-specific records, not inferred from `updatedAt` alone.
