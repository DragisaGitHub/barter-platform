# Priority 1 — Search & Filters 2.0 Completion

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/impl/CatalogQueryServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/SavedSearchService.java`
- `backend/barter-web/src/main/java/com/barterplatform/web/catalog/controller/CatalogController.java`
- `backend/barter-web/src/main/resources/db/migration/V019__saved_searches.sql`

### Frontend

- `frontend/src/features/catalog/MarketplacePage.tsx`
- `frontend/src/features/catalog/useCatalog.ts`
- `frontend/src/features/catalog/SavedSearchesPanel.tsx`
- `frontend/src/features/catalog/useSavedSearches.ts`
- `frontend/src/features/catalog/RecommendationsSection.tsx`
- `frontend/src/api/savedSearchesApi.ts`

### OpenAPI / schema

- `backend/barter-api/src/main/resources/openapi/paths/catalog.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/saved-search/SavedSearchCriteria.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemPagedResponse.yaml`

## Already implemented

- Marketplace search already supports:
  - free-text query
  - category filter
  - multi-tag filter
  - condition filter
  - location filter
  - sorting
  - URL persistence through `useSearchParams`
- Saved searches already exist end to end (migration, service, API, list/create/delete UI).
- Recommendations and popular-category browsing already exist and should not be re-added under this roadmap item.

## Confirmed missing

1. **This item is now mostly a cleanup pass, not a feature-build pass.**
   - The broad search/filter feature set already exists.

2. **Invalid/unsupported filter handling is still weak.**
   - `CatalogQueryServiceImpl.buildSearchSpecification(...)` silently drops unknown tag UUIDs when none resolve.
   - The public API returns normal empty results rather than distinguishing “no matches” from “invalid filter input”.

3. **Search completion is blocked on the P0 status mismatch.**
   - `priority-0/04-search-status-contract-consistency.md` must be resolved first because the public contract still advertises a misleading `status` filter.

4. **Saved-search presentation is intentionally generic today.**
   - `SavedSearchesPanel.tsx` can only summarize category/tag filters generically; it does not resolve category names or tag labels from saved criteria.
   - That is a real completion/polish gap, but not a missing system.

## Not needed / false positives

- Do **not** schedule query-param persistence as missing work; `MarketplacePage.tsx` already persists search state in the URL.
- Do **not** add saved-search CRUD as backlog; it already exists.
- Do **not** add recommendation/ranking or location-engine scope here.

## Intentionally deferred

- Search-engine replacement and geospatial search remain deferred and are not required to finish the current implementation.

## Implementation-ready backlog

### Backend / API

1. Tighten validation behavior for unsupported filter values so clients can distinguish bad input from empty results where appropriate.
2. Finish status-contract normalization before touching public filter surface area.

### Frontend

3. Improve saved-search summaries so applied criteria are visible as concrete labels instead of placeholders.
4. Revisit result-state messaging only for the remaining ambiguous cases: invalid filter input vs genuinely empty results.

## Dependencies and follow-on impact

- Depends directly on `priority-0/04-search-status-contract-consistency.md`.
- Pairs well with `priority-1/04-frontend-test-coverage-expansion.md` once the public search contract stops drifting.

## Exit criteria

- The roadmap no longer treats core search/filter functionality as unfinished when it is already present.
- Remaining work is limited to validation clarity, saved-search polish, and post-P0 contract cleanup.
- Search 2.0 stays incremental and does not reopen completed discovery features.
