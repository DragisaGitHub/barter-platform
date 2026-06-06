# Priority 1 — Frontend Test Coverage Expansion

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Why this is still Priority 1

The frontend has substantial product surface area, but there is currently no frontend test runner, no test setup, and no existing `.test` / `.spec` files. This is not about broadening an existing suite; it is about creating the first targeted safety net around already-implemented launch flows.

## Verified current implementation

### Frontend application surfaces already worth protecting

- `frontend/src/features/trade/TradeOfferMessagesPanel.tsx`
- `frontend/src/features/trade/useTradeOfferMessages.ts`
- `frontend/src/features/catalog/MarketplacePage.tsx`
- `frontend/src/features/catalog/FavoritesPage.tsx`
- `frontend/src/features/notifications/NotificationBell.tsx`
- `frontend/src/features/notifications/NotificationsPage.tsx`
- `frontend/src/features/admin/AdminReportsPage.tsx`
- `frontend/src/features/admin/AdminReviewsPage.tsx`
- `frontend/src/features/profile/TrustSummary.tsx`

### Existing backend-side coverage that already exercises adjacent contracts

- `backend/barter-application/src/test/java/com/barterplatform/application/trade/service/impl/TradeOfferMessageServiceImplTest.java`
- `backend/barter-application/src/test/java/com/barterplatform/application/catalog/service/impl/FavoriteItemServiceImplTest.java`
- `backend/barter-application/src/test/java/com/barterplatform/application/catalog/service/impl/CatalogQueryServiceImplTest.java`
- `backend/barter-application/src/test/java/com/barterplatform/application/moderation/service/impl/ReportServiceImplTest.java`
- `backend/barter-application/src/test/java/com/barterplatform/application/reputation/service/impl/AdminTradeReviewServiceImplTest.java`
- `backend/barter-web/src/test/java/com/barterplatform/web/catalog/controller/CatalogControllerMvcTest.java`
- `backend/barter-web/src/test/java/com/barterplatform/web/admin/controller/AdminReportsControllerMvcTest.java`
- `backend/barter-web/src/test/java/com/barterplatform/web/notification/controller/NotificationsControllerMvcTest.java`

### Confirmed frontend test-infra absence

- `frontend/package.json` has no `test` script and no Vitest/Jest/Testing Library dependencies.
- `frontend/vite.config.ts` has no test configuration.
- No frontend `.test.*`, `.spec.*`, `vitest.config.*`, `jest.config.*`, or `setupTests.*` files exist.

## Already implemented

- The frontend already centralizes most data access through React Query hooks, which is a good base for testing.
- Shared UI primitives such as `Button`, `Badge`, `EmptyState`, `Card`, and `Spinner` already exist.
- High-value flows are implemented; the problem is lack of automated protection, not lack of UI to test.

## Confirmed missing

1. **There is no frontend test harness at all.**
2. **There are no route-level or feature-level regression tests for launch flows.**
3. **Complex async surfaces are currently unprotected:**
   - trade-message polling + optimistic send
   - notification unread cache updates
   - marketplace URL/search-param state
   - favorites cross-page invalidation
   - admin reports filter/detail/update behavior
   - admin reviews debounce/filtering behavior

## Not needed / false positives

- Do **not** set blanket percentage targets first.
- Do **not** expand this into a backend testing initiative; backend coverage already exists separately.
- Do **not** redesign CI in the same work item.

## Intentionally deferred

- Large browser E2E infrastructure can wait until targeted component/integration coverage exists.

## Implementation-ready backlog

### Test infrastructure first

1. Add a frontend test runner and setup (`package.json`, Vite-compatible config, shared test bootstrap).
2. Create shared render helpers for:
   - React Query
   - router state
   - auth context
   - i18n wrappers where needed

### First coverage wave

3. Add tests for `TradeOfferMessagesPanel.tsx` and `useTradeOfferMessages.ts` after the P0 read-state contract is settled.
4. Add tests for `MarketplacePage.tsx` URL persistence and favorites interactions.
5. Add tests for `NotificationBell.tsx` / `NotificationsPage.tsx` unread-update behavior.
6. Add tests for `AdminReportsPage.tsx` before further moderation UI changes land.

### Second coverage wave

7. Add tests for `AdminReviewsPage.tsx`, `FavoritesPage.tsx`, and `TrustSummary.tsx` once their related P2 scope stabilizes.

## Dependencies and follow-on impact

- Should follow the P0 contract fixes for trade-message read state and catalog status semantics.
- Protects nearly every remaining P1 and P2 UI roadmap item.

## Exit criteria

- The frontend has a real automated test entry point.
- The first tests cover the currently riskiest async and stateful flows, not low-value static components.
- Manual verification remains supplemental instead of being the only safety net.
