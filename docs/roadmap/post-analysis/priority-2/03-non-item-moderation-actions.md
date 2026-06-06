# Priority 2 — Non-Item Moderation Actions

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-domain/src/main/java/com/barterplatform/domain/moderation/report/ReportTargetType.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/moderation/service/impl/ReportTargetResolverImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/moderation/service/impl/ReportServiceImpl.java`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/catalog/moderation/ListingModerationActionEntity.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/impl/ListingModerationServiceImpl.java`

### Frontend

- `frontend/src/features/reports/ReportTrigger.tsx`
- `frontend/src/features/trade/TradeOfferMessagesPanel.tsx`
- `frontend/src/features/reviews/ReviewsPage.tsx`
- `frontend/src/features/profile/PublicProfilePage.tsx`
- `frontend/src/features/admin/AdminReportsPage.tsx`

### OpenAPI / schema / database

- `backend/barter-api/src/main/resources/openapi/paths/admin-reports.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/report/ReportDetailResponse.yaml`
- `backend/barter-web/src/main/resources/db/migration/V012__listing_moderation_and_offer_invalidation.sql`
- `backend/barter-web/src/main/resources/db/migration/V017__reports_foundation.sql`

## Already implemented

- Non-item **reporting** already exists.
  - `ReportTargetType` includes `USER`, `MESSAGE`, `TRADE_OFFER`, and `REVIEW`.
  - `ReportTargetResolverImpl` validates report creation and builds summaries for all of those target types.
  - Frontend report triggers already exist on user profiles, trade messages, and reviews.
- The admin report queue can already list/filter/detail those reports because report detail includes `targetType`, `targetUuid`, and `targetSummary`.
- Item-specific moderation actions already exist separately through listing moderation.

## Confirmed missing

1. **Moderation actions are still effectively item-only.**
   - `ListingModerationActionEntity` and `ListingModerationServiceImpl` only cover listing actions.
   - `ReportServiceImpl.updateReport(...)` can only change report status and resolution note; it does not execute any non-item moderation outcome.

2. **Admin report detail is only action-rich for item reports.**
   - `AdminReportsPage.tsx` loads listing detail and listing remove/restore actions when `targetType === "ITEM"`.
   - The same page does not expose equivalent action panels for `USER`, `MESSAGE`, `TRADE_OFFER`, or `REVIEW` reports.

3. **Target-specific follow-on effects are undefined in code.**
   - There is no backend command service for hiding a review, suppressing a message, suspending a user, or invalidating a trade as a moderation outcome from the report queue.

## Not needed / false positives

- Do **not** treat non-item reporting as missing; it is already implemented.
- Do **not** build a generalized abuse case-management platform here.
- Do **not** duplicate listing moderation rules under a new abstraction if only non-item targets are missing.

## Intentionally deferred

- Automated trust/fraud enforcement remains outside the scope of this item.

## Implementation-ready backlog

### Backend

1. Define which moderator actions are actually supported for each non-item target type:
   - user
   - message
   - trade offer
   - review
2. Add explicit command handling for those actions instead of overloading report status updates.
3. Reuse the report queue as the entry point, but keep target-type side effects in dedicated services.

### API / frontend

4. Extend report detail responses with target-type-specific action metadata once backend actions exist.
5. Update `AdminReportsPage.tsx` so non-item reports are not second-class queue entries that can only be marked resolved/dismissed.

## Dependencies and follow-on impact

- Depends on `priority-0/02-report-audit-trail.md` so non-item actions are auditable once added.
- Touches `priority-2/04-admin-review-governance-improvements.md` for review-target actions, but should not merge the two scopes.

## Exit criteria

- The report queue can do more than close non-item reports administratively.
- Each non-item target type has an explicit action matrix or is explicitly declared read-only.
- Item moderation remains separate and intact while non-item coverage catches up.
