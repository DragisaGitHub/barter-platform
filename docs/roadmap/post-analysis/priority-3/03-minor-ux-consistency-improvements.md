# Priority 3 — Minor UX Consistency Improvements

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current frontend baseline

### Shared UI primitives already available

- `frontend/src/components/ui/Badge.tsx`
- `frontend/src/components/ui/Button.tsx`
- `frontend/src/components/ui/EmptyState.tsx`
- `frontend/src/components/ui/Spinner.tsx`
- `frontend/src/components/ui/StatusBadge.tsx`
- `frontend/src/features/catalog/ItemBadges.tsx`
- `frontend/src/features/trade/TradeOfferStatusBadge.tsx`

### Repeated patterns still implemented manually

- Duplicate `formatDateTime(...)` helpers in:
  - `frontend/src/features/admin/AdminReportsPage.tsx`
  - `frontend/src/features/admin/AdminReviewsPage.tsx`
  - `frontend/src/features/admin/AdminListingsPage.tsx`
  - `frontend/src/features/admin/AdminListingDetailPage.tsx`
  - `frontend/src/features/admin/AdminListingModerationTimeline.tsx`
  - `frontend/src/features/admin/AdminCategoriesPage.tsx`
  - `frontend/src/features/admin/AdminTagsPage.tsx`
  - `frontend/src/features/admin/AdminBetaFeedbackPage.tsx`
  - `frontend/src/features/catalog/OwnerModerationPanel.tsx`
  - `frontend/src/features/trade/TradeOfferCompletionActions.tsx`
- Manual catalog badge markup duplicated in:
  - `frontend/src/features/catalog/MarketplacePage.tsx`
  - `frontend/src/features/catalog/FavoritesPage.tsx`
  - `frontend/src/features/catalog/ItemDetailPage.tsx`

## Already implemented

- The frontend is not missing a component foundation.
- `EmptyState`, `Badge`, item badges, and status-badge patterns already exist.
- Most remaining UX inconsistency is now duplication/adoption debt, not missing UI building blocks.

## Confirmed missing

1. **Date/time formatting is still repeated instead of standardized.**
   - Multiple pages implement their own `formatDateTime(...)` helpers and raw `toLocaleString()` calls.

2. **Badge usage is only partially centralized.**
   - Some item/trade/user status displays use shared badge components.
   - Other catalog surfaces still render badge markup manually, even though `ItemBadges.tsx` and `StatusBadge.tsx` exist.

3. **This is a consistency pass only after feature work settles.**
   - The code already works; the issue is uneven reuse of shared patterns.

## Not needed / false positives

- Do **not** start a redesign or design-system migration.
- Do **not** create net-new user flows.
- Do **not** use this item to justify broad refactors in high-risk areas while P0/P1 work is still moving.

## Intentionally deferred

- Larger accessibility or visual-system overhauls remain outside this compact cleanup item.

## Implementation-ready backlog

1. Standardize date/time presentation with a shared formatter before touching additional admin/trade/catalog pages.
2. Replace remaining manual item-status/soft-badge markup where a shared badge component already exists or can be trivially extended.
3. Limit the pass to duplicated patterns already visible in the codebase; do not expand into subjective page-by-page polish.

## Exit criteria

- The final cleanup bundle targets repeated code patterns, not open-ended polish.
- Shared primitives are used more consistently across admin, catalog, and trade surfaces.
- The item remains a low-risk consolidation pass.
