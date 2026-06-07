# Priority 0 — Search Status Contract Consistency

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Why this is still Priority 0

The public catalog search contract advertises a `status` filter, but the backend implementation currently ignores the incoming status and hardcodes public search to `ACTIVE` items only. That is a real API/implementation mismatch, not just a documentation nit.

## Verified current implementation

### Backend

- `backend/barter-web/src/main/java/com/barterplatform/web/catalog/controller/CatalogController.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/CatalogQueryService.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/catalog/service/impl/CatalogQueryServiceImpl.java`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/catalog/enums/ItemStatus.java`

### Frontend

- `frontend/src/features/catalog/MarketplacePage.tsx`
- `frontend/src/features/catalog/useCatalog.ts`
- `frontend/src/features/catalog/SavedSearchesPanel.tsx`
- `frontend/src/api/catalogApi.ts`
- `frontend/src/api/generated/types.ts`

### OpenAPI / schema / database

- `backend/barter-api/src/main/resources/openapi/paths/catalog.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemStatus.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemSummaryResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/catalog/ItemDetailResponse.yaml`

## Already implemented

- Public item search already supports text, category, tag, condition, location, paging, and sort.
- Owner/admin detail views already differentiate visibility from public browsing in `CatalogQueryServiceImpl.getItemByUuid(...)`.
- Frontend marketplace pages already assume public listings are active-only and additionally filter client-side to `item.status === "ACTIVE"`.

## Confirmed missing

1. **The public OpenAPI contract overstates status support.**
   - `paths/catalog.yaml` exposes a public `status` query parameter using the full `ItemStatus` enum.
   - The description says, “Public search defaults to ACTIVE only”, but the parameter remains publicly available.

2. **The backend ignores the incoming search status.**
   - `CatalogController.searchItems(...)` accepts `status` and passes it into `CatalogQueryService.searchItems(...)`.
   - `CatalogQueryServiceImpl.searchItems(...)` receives that parameter, but `buildSearchSpecification(...)` hardcodes `ItemStatus.ACTIVE` and never uses the incoming `status` argument.

3. **The frontend-generated types allow more than the public marketplace actually supports.**
   - `frontend/src/api/generated/types.ts` inherits the full `ItemStatus` enum from OpenAPI.
   - `MarketplacePage.tsx` has no public status selector and still performs its own `ACTIVE` filtering, so frontend behavior and generated contract are already drifting.

4. **Saved-search criteria do not model status at all.**
   - The current saved-search UI and criteria formatting cover query/category/tags/condition/location, not status.
   - That is fine for ACTIVE-only public search, but it becomes ambiguous while OpenAPI still advertises a broader status filter.

## Not needed / false positives

- Do **not** add database migrations for this item; the item status vocabulary already exists.
- Do **not** redesign search ranking or introduce a search engine.
- Do **not** conflate public marketplace status rules with admin listing management; admin listing status handling already lives on separate endpoints.

## Intentionally deferred

- There is no evidence that public users should browse `DRAFT`, `ARCHIVED`, or `REMOVED` items. Until that product decision change, the safe direction is likely to narrow the contract rather than broaden public behavior.

## Implementation-ready backlog

### API / backend

1. Make the public contract truthful:
   - either remove public `status` filtering entirely, or
   - explicitly constrain it to `ACTIVE` and reject anything else.
2. Stop accepting a parameter that the service ignores.
3. Document owner/admin status visibility on their dedicated endpoints instead of implying it on public search.

### Frontend

1. Align `catalogApi` usage and generated types with the final public contract.
2. Remove any remaining client-side assumptions that are compensating for a misleading API contract.

## Dependencies and follow-on impact

- This is the prerequisite for `priority-1/03-search-and-filters-2-0-completion.md`.
- It also reduces the risk for later frontend test coverage around search/filter URLs.

## Exit criteria

- The public search API no longer advertises status behavior it does not implement.
- Backend, generated types, and `MarketplacePage.tsx` all share the same status semantics.
- Public/admin/owner status differences are explicit instead of implied.
