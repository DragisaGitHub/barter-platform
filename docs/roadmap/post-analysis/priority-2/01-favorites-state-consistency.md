# Priority 2 — Favorites State Consistency

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-domain/src/main/java/com/barterplatform/domain/catalog/entity/FavoriteItemEntity.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/FavoriteItemService.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/impl/FavoriteItemServiceImpl.java`
- `backend/barter-web/src/main/java/com/barterplatform/web/catalog/controller/CatalogController.java`

### Frontend

- `frontend/src/features/catalog/MarketplacePage.tsx`
- `frontend/src/features/catalog/FavoritesPage.tsx`
- `frontend/src/features/catalog/useCatalog.ts`

### OpenAPI / schema / database

- `backend/barter-web/src/main/resources/db/migration/V011__favorite_items.sql`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemSummaryResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemDetailResponse.yaml`

## Already implemented

- Favorite/unfavorite persistence already exists.
- `FavoriteItemEntity` enforces uniqueness by `(user_id, item_id)`.
- `FavoriteItemServiceImpl.favoriteItem(...)` is intentionally idempotent and tolerates concurrent duplicate inserts.
- `MarketplacePage.tsx` already supports favorite toggles using React Query invalidation plus local `favoriteOverrides`.
- `FavoritesPage.tsx` already lists saved items and allows unfavorite actions.

## Confirmed missing

1. **There is still no contract-level `isFavorited` field on item responses.**
   - `ItemSummaryResponse.yaml` does not expose favorite state.
   - `ItemDetailResponse.yaml` does not expose favorite state.
   - The frontend therefore reconstructs favorite status by separately loading the favorites list.

2. **Item detail has no favorite affordance at all right now.**
   - `ItemDetailPage.tsx` does not use `useFavoriteItem` / `useUnfavoriteItem` and contains no favorite toggle.
   - That makes “cross-surface consistency” currently limited to marketplace cards plus the favorites page.

3. **Marketplace favorite state depends on local reconciliation logic.**
   - `MarketplacePage.tsx` merges server favorites with `favoriteOverrides` and `pendingFavoriteUuid`.
   - This works, but it is still compensating for the lack of a single item-level favorite field.

## Not needed / false positives

- Do **not** treat favorites as a missing feature; create/list/delete behavior already exists.
- Do **not** expand this into recommendations, wishlists, or saved-search features.
- Do **not** add database redesign work unless a genuine performance problem appears.

## Intentionally deferred

- Shared or social favorites are not present and do not need to be smuggled into this consistency item.

## Implementation-ready backlog

### API / backend

1. Decide whether favorite state should be included directly in `ItemSummaryResponse` / `ItemDetailResponse` for authenticated users.
2. If yes, stop forcing the frontend to derive favorite state from a second query.
3. Keep existing toggle idempotency behavior.

### Frontend

4. Add favorite parity to `ItemDetailPage.tsx` or explicitly narrow the supported favorites surfaces to marketplace + favorites page.
5. Reduce marketplace-only override logic once item responses can carry favorite state directly.
6. Preserve current handling where removed items do not appear in the favorites list; `FavoriteItemServiceImpl.listFavoriteItems(...)` already filters out `REMOVED` items.

## Dependencies and follow-on impact

- Pairs well with targeted frontend coverage after the API contract is clarified.
- Does not depend on search/recommendation expansion beyond existing catalog APIs.

## Exit criteria

- Favorite state comes from one clear source of truth instead of a joined client-side reconstruction.
- Supported surfaces for favorite actions are explicit and consistent.
- The roadmap no longer implies favorites are missing when the real issue is response-shape and cross-surface consistency.
