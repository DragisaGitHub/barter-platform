# Priority 2 — Admin Review Governance Improvements

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-web/src/main/java/com/barterplatform/web/admin/controller/AdminReviewsController.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/reputation/service/AdminTradeReviewService.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/reputation/service/impl/AdminTradeReviewServiceImpl.java`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/reputation/entity/TradeReviewEntity.java`

### Frontend

- `frontend/src/features/admin/AdminReviewsPage.tsx`
- `frontend/src/features/admin/useAdminReviews.ts`
- `frontend/src/api/adminReviewsApi.ts`

### OpenAPI / schema

- `backend/barter-api/src/main/resources/openapi/paths/admin-reviews.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/reputation/AdminTradeReviewSummaryResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/reputation/AdminTradeReviewPagedResponse.yaml`

## Already implemented

- Admin review queue listing already exists.
- Admins can already filter reviews by rating, negative reason, reviewer query, and reviewed-user query.
- `AdminReviewsPage.tsx` already provides pagination, debounced filters, and a governance-queue-style table.

## Confirmed missing

1. **The admin review queue is read-only.**
   - `AdminTradeReviewService` only defines `listReviews(...)`.
   - `admin-reviews.yaml` exposes only `GET /admin/reviews`.

2. **There is no review-governance action model.**
   - No endpoint exists to hide, restore, annotate, or otherwise govern a review.
   - `TradeReviewEntity` has no governance status, moderation reason, admin note, or visibility field.

3. **There is no admin review detail or audit surface.**
   - `AdminReviewsPage.tsx` lists rows only.
   - There is no review detail endpoint, no decision history, and no required reason capture flow.

4. **Governance cannot currently affect public reputation cleanly.**
   - Because review visibility state does not exist, any future admin action would have no stable contract to flow into public profile and reputation calculations.

## Not needed / false positives

- Do **not** rebuild the admin review queue UI from scratch; the list/filter shell already exists.
- Do **not** merge this item into non-item moderation wholesale; reviews deserve their own governance workflow.
- Do **not** introduce full dispute resolution or appeals.

## Intentionally deferred

- External appeals workflow and fraud-scoring programs remain outside the current scope.

## Implementation-ready backlog

### Backend / data model

1. Add explicit governance state for reviews before adding admin actions.
2. Define a small action set first (for example: hide / restore / annotate), not a broad moderation taxonomy.
3. Require reason capture for any public-visibility-changing action.

### API / frontend

4. Add review detail + action endpoints; the current list-only API is not enough.
5. Extend `AdminReviewsPage.tsx` with action flows only after backend reason/status contracts exist.
6. Surface public-impact cues so admins understand whether a decision changes profile/reputation output.

## Dependencies and follow-on impact

- Depends on `priority-0/02-report-audit-trail.md` if review-governance actions must be historically traceable.
- Shares state-model concerns with `priority-2/02-reviews-and-reputation-hardening.md`, but remains a separate execution unit.

## Exit criteria

- Admin review governance is more than a read-only queue.
- Review actions require explicit reason capture.
- Public review visibility and reputation side effects are defined before admin actions ship.
