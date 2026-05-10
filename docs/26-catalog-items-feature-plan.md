# Catalog / Items — Feature Plan (v1)

## References

- [02-domain-model.md](02-domain-model.md)
- [03-data-model.md](03-data-model.md)
- [09-database-conventions.md](09-database-conventions.md)
- [14-enum-strategy.md](14-enum-strategy.md)
- [17-pagination-search-strategy.md](17-pagination-search-strategy.md)

---

# 1. Domain Overview

The Catalog domain is the core of the marketplace. It lets users create, browse, and manage item listings they want to barter.

Key responsibilities:
- item listings (create, update, archive)
- categories and tags for organization
- ownership — every item belongs to a user
- public browsing and searching
- item lifecycle (draft → active → reserved → archived)

Relations to other domains:
- **Identity & Access** — item owner is a `User`; RBAC controls who can create/update/archive
- **Trade (future)** — trade offers reference items
- **Wishlist (future)** — wishlist entries match against catalog items
- **Messaging (future)** — conversations may be linked to items

### v1 Scope

v1 delivers text-only item listings with a single flat category, multiple tags, owner CRUD, public search, and archive flow. Images, collections, attributes, trades, and favorites are deferred.

---

# 2. Core Entities

## v1 Entities

| Entity   | Description                          |
|----------|--------------------------------------|
| Category | Grouping for items (flat in v1 API, hierarchy-ready in DB) |
| Tag      | Descriptive label; admin-curated     |
| Item     | A user's listing for barter          |
| ItemTag  | Join table: Item ↔ Tag (many-to-many) |

## Deferred Entities

| Entity         | When           |
|----------------|----------------|
| ItemImage      | Image upload feature |
| Collection     | User-curated groupings |
| ItemAttribute  | Category-specific metadata |
| CategoryAttribute | Template for item attributes |

---

# 3. Suggested Database Tables

Migration file: `V003__catalog_schema.sql` (not created yet).

## categories

| Column      | Type                         | Notes                        |
|-------------|------------------------------|------------------------------|
| id          | BIGSERIAL PK                 |                              |
| uuid        | UUID NOT NULL UNIQUE         |                              |
| parent_id   | BIGINT NULL                  | FK → categories(id); nullable; unused in v1 API but present for future hierarchy |
| name        | VARCHAR(120) NOT NULL        |                              |
| slug        | VARCHAR(120) NOT NULL        | URL-friendly, unique         |
| description | TEXT                         |                              |
| sort_order  | INT NOT NULL DEFAULT 0       |                              |
| created_at  | TIMESTAMPTZ NOT NULL         |                              |
| updated_at  | TIMESTAMPTZ                  |                              |

Constraints:
- `uq_categories_uuid` UNIQUE(uuid)
- `uq_categories_slug` UNIQUE(slug)
- `fk_categories_parent` FOREIGN KEY(parent_id) REFERENCES categories(id)

Indexes:
- `idx_categories_parent_id`(parent_id)

## tags

| Column     | Type                         | Notes                |
|------------|------------------------------|----------------------|
| id         | BIGSERIAL PK                 |                      |
| uuid       | UUID NOT NULL UNIQUE         |                      |
| name       | VARCHAR(80) NOT NULL         |                      |
| slug       | VARCHAR(80) NOT NULL         | URL-friendly, unique |
| created_at | TIMESTAMPTZ NOT NULL         |                      |

Constraints:
- `uq_tags_uuid` UNIQUE(uuid)
- `uq_tags_slug` UNIQUE(slug)

## items

| Column      | Type                         | Notes                                    |
|-------------|------------------------------|------------------------------------------|
| id          | BIGSERIAL PK                 |                                          |
| uuid        | UUID NOT NULL UNIQUE         |                                          |
| owner_id    | BIGINT NOT NULL              | FK → users(id)                           |
| category_id | BIGINT NOT NULL              | FK → categories(id)                      |
| title       | VARCHAR(255) NOT NULL        |                                          |
| description | TEXT                         |                                          |
| status      | VARCHAR(40) NOT NULL         | ItemStatus enum                          |
| condition   | VARCHAR(40) NOT NULL         | ItemCondition enum                       |
| created_at  | TIMESTAMPTZ NOT NULL         |                                          |
| updated_at  | TIMESTAMPTZ                  |                                          |
| deleted_at  | TIMESTAMPTZ                  | Soft delete                              |

Constraints:
- `uq_items_uuid` UNIQUE(uuid)
- `fk_items_owner` FOREIGN KEY(owner_id) REFERENCES users(id)
- `fk_items_category` FOREIGN KEY(category_id) REFERENCES categories(id)

Indexes:
- `idx_items_owner_id`(owner_id)
- `idx_items_category_id`(category_id)
- `idx_items_status`(status)
- `idx_items_status_created_at`(status, created_at DESC)

## item_tags

| Column  | Type         | Notes              |
|---------|--------------|---------------------|
| item_id | BIGINT NOT NULL | FK → items(id)   |
| tag_id  | BIGINT NOT NULL | FK → tags(id)    |

Constraints:
- `pk_item_tags` PRIMARY KEY(item_id, tag_id)
- `fk_item_tags_item` FOREIGN KEY(item_id) REFERENCES items(id)
- `fk_item_tags_tag` FOREIGN KEY(tag_id) REFERENCES tags(id)

Indexes:
- `idx_item_tags_tag_id`(tag_id)

---

# 4. Enum Strategy

All enums follow project conventions: uppercase values, stored as VARCHAR, `@Enumerated(EnumType.STRING)`.

## ItemStatus

| Value    | Meaning                             |
|----------|-------------------------------------|
| DRAFT    | Created but not published           |
| ACTIVE   | Visible in public search            |
| RESERVED | Owner marked as reserved (in negotiation) |
| ARCHIVED | Owner archived; no longer available |
| REMOVED  | Moderator/admin removed             |

## ItemCondition

| Value     | Meaning                    |
|-----------|----------------------------|
| NEW       | Unused, in original packaging |
| LIKE_NEW  | Barely used, excellent state |
| GOOD      | Normal wear                |
| USED      | Noticeable wear            |
| FOR_PARTS | Damaged, useful for parts  |

## Permissions

Existing permissions already seeded in `V002__identity_access_seed.sql`:
- `ITEM_VIEW` — view active item listings
- `ITEM_CREATE` — create item listings
- `ITEM_UPDATE` — update owned item listings
- `ITEM_DELETE` — delete or archive owned item listings

New permissions to add in the catalog seed migration:
- `ITEM_ARCHIVE` — explicitly archive an item (distinct from delete)
- `ITEM_VIEW_ANY` — view any item regardless of status (moderator/admin)

Role assignments for new permissions:
- USER → `ITEM_ARCHIVE`
- MODERATOR → `ITEM_ARCHIVE`, `ITEM_VIEW_ANY`
- ADMIN → `ITEM_ARCHIVE`, `ITEM_VIEW_ANY`

---

# 5. API Endpoints

Base path: `/api/v1/catalog`

## Public (no auth)

| Method | Path                        | Description                        |
|--------|-----------------------------|------------------------------------|
| GET    | `/catalog/categories`       | List all categories (flat)         |
| GET    | `/catalog/tags`             | List all tags                      |
| GET    | `/catalog/items`            | Search/browse items (paged)        |
| GET    | `/catalog/items/{uuid}`     | Get item detail                    |

### GET /catalog/items query parameters

| Param        | Type     | Notes                         |
|--------------|----------|-------------------------------|
| q            | String   | Free-text search on title     |
| categoryUuid | UUID     | Filter by category            |
| tagUuids     | UUID[]   | Filter by one or more tags    |
| condition    | String   | Filter by ItemCondition       |
| page         | int      | Default 0                     |
| size         | int      | Default 20, max 100           |
| sort         | String   | e.g. `createdAt,desc`         |

Public search only returns items with status = ACTIVE.

## Authenticated (Bearer token)

| Method | Path                              | Permission   | Description                 |
|--------|-----------------------------------|--------------|-----------------------------|
| POST   | `/catalog/items`                  | ITEM_CREATE  | Create a new item           |
| PATCH  | `/catalog/items/{uuid}`           | ITEM_UPDATE  | Update own item             |
| POST   | `/catalog/items/{uuid}/archive`   | ITEM_ARCHIVE | Archive own item            |
| GET    | `/catalog/items/mine`             | ITEM_VIEW    | List own items (paged, all statuses) |

## Moderator / Admin

| Method | Path                        | Permission    | Description                      |
|--------|-----------------------------|---------------|----------------------------------|
| DELETE | `/catalog/items/{uuid}`     | ITEM_DELETE   | Soft-remove item (sets REMOVED)  |

---

# 6. OpenAPI DTOs

## Request DTOs

**CreateItemRequest**
- title (string, required, max 255)
- description (string, optional)
- categoryUuid (UUID, required)
- tagUuids (UUID[], optional)
- condition (ItemCondition, required)
- status (ItemStatus, optional — default DRAFT)

**UpdateItemRequest**
- title (string, optional)
- description (string, optional)
- categoryUuid (UUID, optional)
- tagUuids (UUID[], optional)
- condition (ItemCondition, optional)
- status (ItemStatus, optional — only DRAFT↔ACTIVE transitions allowed by owner)

## Response DTOs

**CategoryResponse**
- uuid, name, slug, description, sortOrder

**TagResponse**
- uuid, name, slug

**ItemSummaryResponse**
- uuid, title, status, condition, categoryUuid, categoryName, ownerUuid, ownerUsername, createdAt

**ItemDetailResponse**
- uuid, title, description, status, condition, category (CategoryResponse), tags (TagResponse[]), ownerUuid, ownerUsername, createdAt, updatedAt

**ItemPagedResponse**
- content (ItemSummaryResponse[]), page, size, totalElements, totalPages, first, last, sort

## Enum Schemas

- ItemStatus: DRAFT, ACTIVE, RESERVED, ARCHIVED, REMOVED
- ItemCondition: NEW, LIKE_NEW, GOOD, USED, FOR_PARTS

Reuse existing pagination parameters and `ErrorResponse` from the current OpenAPI spec.

---

# 7. Backend Implementation Order

1. **OpenAPI spec** — add catalog endpoints, DTOs, and enum schemas to `barter-api` spec; regenerate code
2. **Enums** — add `ItemStatus` and `ItemCondition` enums in `barter-domain`
3. **JPA entities** — `CategoryEntity`, `TagEntity`, `ItemEntity`, `ItemTagEntity` (with `ItemTagId` composite key) in `barter-domain`
4. **Flyway migration** — `V003__catalog_schema.sql` for tables; `V004__catalog_seed.sql` for initial categories, tags, and new permissions (`ITEM_ARCHIVE`, `ITEM_VIEW_ANY`)
5. **Repositories** — `CategoryRepository`, `TagRepository`, `ItemRepository`, `ItemTagRepository` in `barter-infrastructure`
6. **Services & mappers** — in `barter-application`:
   - `CatalogQueryService` — list categories, tags; search items (paged); get item detail; list own items
   - `ItemCommandService` — create, update, archive item
   - `ItemMapper`, `CategoryMapper`, `TagMapper` (MapStruct)
7. **Controllers** — `CatalogController` in `barter-web`; apply `@PreAuthorize` with permission checks
8. **Integration tests** — CRUD lifecycle, search filters, authorization checks

---

# 8. Frontend Integration Order

1. **Regenerate TypeScript client** — run OpenAPI generator after spec update
2. **Marketplace browse page** — public item grid/list with search, category filter, tag filter, pagination
3. **Item detail page** — public item view with category, tags, description, owner info
4. **My Items page** — authenticated list of own items with status badges; link to create/edit
5. **Create Item form** — category selector, tag multi-select, condition picker, title, description
6. **Edit Item form** — pre-populated; status toggle (DRAFT ↔ ACTIVE)
7. **Archive action** — confirmation dialog, calls archive endpoint
8. **Navigation** — add "Marketplace" to public nav; add "My Items" to authenticated sidebar
9. **Image placeholders** — show placeholder/icon where images will go later

---

# 9. v1 vs Later

## In v1

- Text-only item listings (no images)
- Single category per item (flat list in API)
- Multiple tags per item (admin-curated tags)
- Owner CRUD: create, update, archive
- Item lifecycle: DRAFT → ACTIVE → RESERVED → ARCHIVED
- Moderator/admin soft-remove (REMOVED status)
- Public search with filters (q, category, tags, condition)
- Paginated search following existing pagination contract
- RBAC with existing + new permissions
- Flyway migration for catalog schema and seed data

## Later

| Feature                  | Notes                                              |
|--------------------------|----------------------------------------------------|
| Image upload             | `item_images` table + file storage (S3/MinIO)      |
| Category hierarchy       | Expose `parent_id` in API; tree navigation          |
| Category attributes      | `category_attributes` + `item_attribute_values`    |
| Collections              | User-curated item groups                           |
| Geo/city filters         | Location-based search                              |
| Full-text search         | PostgreSQL tsvector or external search engine       |
| Trade offer integration  | `trade_offers` + `trade_offer_items` referencing items |
| Favorites / watchlist    | User-saved items                                   |
| Reporting / moderation   | Flag items for review                              |
| Promoted listings        | Monetization: boost visibility                     |

