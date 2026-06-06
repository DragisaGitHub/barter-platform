# Priority 2 — Reviews & Reputation Hardening

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-domain/src/main/java/com/barterplatform/domain/reputation/entity/TradeReviewEntity.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/reputation/service/impl/TradeReviewServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/reputation/service/impl/ReputationServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/profile/service/impl/PublicProfileServiceImpl.java`
- `backend/barter-infrastructure/src/main/java/com/barterplatform/infrastructure/reputation/repository/TradeReviewRepository.java`

### Frontend

- `frontend/src/features/reviews/ReviewsPage.tsx`
- `frontend/src/features/reviews/useReviews.ts`
- `frontend/src/features/profile/PublicProfilePage.tsx`
- `frontend/src/features/profile/TrustSummary.tsx`

### OpenAPI / schema / database

- `backend/barter-web/src/main/resources/db/migration/V014__trade_reviews_foundation.sql`
- `backend/barter-api/src/main/resources/openapi/components/schemas/reputation/TradeReviewResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/reputation/ReputationSummaryResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/reputation/AdminTradeReviewSummaryResponse.yaml`

## Already implemented

- Trade reviews already exist end to end for completed trades only.
- Review creation already validates positive/negative rules and notifies the reviewed user.
- Public reputation summary already exists and exposes positive count, negative count, total count, and positive percentage.
- Public profile already renders recent review snippets and trust-summary cards.
- User-facing review history already exists in `ReviewsPage.tsx`.

## Confirmed missing

1. **Reviews have no moderation or visibility state.**
   - `TradeReviewEntity` is immutable and stores rating/reason/comment only.
   - There is no field for hidden, removed, moderated, or disputed review state.

2. **Reputation counts include every stored review with no governance filter.**
   - `ReputationServiceImpl` counts by `reviewedUserId` and `rating` only.
   - If governance later needs to hide a review, current aggregation has nowhere to express that.

3. **Public profile snippets are filtered only by active reviewer accounts, not by review visibility.**
   - `TradeReviewRepository.findLatestCommentedReviewsForReviewedUser(...)` checks reviewer status and comment presence, not review moderation state.

4. **Frontend trust surfaces assume all persisted reviews are public truth.**
   - `TrustSummary.tsx` and `ReviewsPage.tsx` have no way to explain moderated or unavailable reviews because the API exposes no such state.

## Not needed / false positives

- Do **not** introduce trust scoring, badges, or ML weighting here.
- Do **not** rebuild review creation; that workflow already exists.
- Do **not** merge this item with admin review governance; governance actions are a separate missing layer.

## Intentionally deferred

- Identity verification / anti-fraud programs remain outside the current review hardening scope.

## Implementation-ready backlog

### Backend / data model

1. Introduce explicit review visibility/moderation state before changing reputation math.
2. Update `ReputationServiceImpl` and recent-review queries so public trust signals exclude non-public reviews consistently.
3. Keep raw stored review data intact; this is about visibility/governance-aware aggregation, not destructive rewrites.

### API / frontend

4. Add enough metadata for public profile and review-list surfaces to explain when review signals are intentionally limited.
5. Make `TrustSummary.tsx` and `ReviewsPage.tsx` resilient to governed review states rather than assuming every stored review is visible.

## Dependencies and follow-on impact

- Closely related to `priority-2/04-admin-review-governance-improvements.md`.
- Should consume, not redefine, whatever audit/governance model gets chosen for admin review actions.

## Exit criteria

- Reputation summaries and recent-review snippets reflect the same visibility rules.
- Review moderation outcomes cannot leave public trust counts in a contradictory state.
- The roadmap is limited to hardening existing review/reputation features, not creating a new trust program.
