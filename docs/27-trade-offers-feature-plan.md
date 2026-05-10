# 27 — Trade Offers Feature Plan (v1)

## 1. Domain Overview

The Trade Offers domain is the first real barter workflow on the platform. With Catalog/Items v1 complete, users can list items; this feature lets them propose **item-for-item swaps**.

A **trade offer** is a directed proposal from one user (the *sender*) to another (the *receiver*) suggesting: *"Trade your item X for my item Y."* The receiver can accept, reject, or let it expire. The sender can cancel while still pending. An accepted offer is the v1 trade outcome: both items leave the marketplace (auto-archived), and no other pending offer involving either item can transition to accepted.

v1 is intentionally narrow:
- One sender item, one receiver item (no multi-item bundles).
- No cash component, no fees, no escrow.
- No counter-offers (rejecting ends the negotiation; sender may create a new offer).
- No notifications/messaging integration yet (offer carries an optional message).

## 2. Core Entities

| Entity        | Purpose                                                                   |
|---------------|---------------------------------------------------------------------------|
| `TradeOffer`  | The proposal itself: sender, receiver, sender item, receiver item, status, optional message, timestamps. |
| `TradeOfferStatusHistory` *(optional v1)* | Audit log for status transitions; can defer to v2 if it adds noise. |

References (no new persisted models):
- `User` — both `senderUserId` and `receiverUserId` reference existing users.
- `Item` — both `senderItemId` and `receiverItemId` reference existing catalog items.

Invariants encoded in the entity:
- `senderUserId != receiverUserId`
- `senderItemId != receiverItemId`
- sender owns sender item; receiver owns receiver item (denormalized check, validated at service layer)
- status transitions are restricted (state machine, see §4)

## 3. Suggested Database Tables

Follow existing conventions from `09-database-conventions.md` (snake_case, `uuid` public id + `bigserial` internal id, `created_at`/`updated_at`, soft state via status enum).

### `trade_offer`

| Column                  | Type                       | Notes                                               |
|-------------------------|----------------------------|-----------------------------------------------------|
| `id`                    | `bigserial PK`             | Internal id                                         |
| `uuid`                  | `uuid UNIQUE NOT NULL`     | Public id                                           |
| `sender_user_id`        | `bigint NOT NULL FK user`  | Index                                               |
| `receiver_user_id`      | `bigint NOT NULL FK user`  | Index                                               |
| `sender_item_id`        | `bigint NOT NULL FK item`  | Index                                               |
| `receiver_item_id`      | `bigint NOT NULL FK item`  | Index                                               |
| `status`                | `varchar(32) NOT NULL`     | Enum string (`PENDING`, ...)                        |
| `message`               | `text NULL`                | Max 1000 chars (validated app-side)                 |
| `responded_at`          | `timestamptz NULL`         | Set when accepted/rejected/cancelled                |
| `expires_at`            | `timestamptz NULL`         | Optional v1; default e.g. `created_at + 14 days`    |
| `created_at`            | `timestamptz NOT NULL`     |                                                     |
| `updated_at`            | `timestamptz NULL`         |                                                     |

Constraints / indexes:
- `CHECK (sender_user_id <> receiver_user_id)`
- `CHECK (sender_item_id <> receiver_item_id)`
- Composite index `(receiver_user_id, status)` — incoming list query
- Composite index `(sender_user_id, status)` — sent list query
- **Partial unique index** to enforce "one accepted offer per item":
    - `UNIQUE (sender_item_id) WHERE status = 'ACCEPTED'`
    - `UNIQUE (receiver_item_id) WHERE status = 'ACCEPTED'`
- *(Optional)* Partial unique to prevent duplicate pending pairs:
    - `UNIQUE (sender_user_id, sender_item_id, receiver_item_id) WHERE status = 'PENDING'`

### `trade_offer_status_history` *(optional, can defer)*

| Column           | Type                       |
|------------------|----------------------------|
| `id`             | `bigserial PK`             |
| `trade_offer_id` | `bigint NOT NULL FK`       |
| `from_status`    | `varchar(32) NULL`         |
| `to_status`      | `varchar(32) NOT NULL`     |
| `actor_user_id`  | `bigint NOT NULL FK user`  |
| `created_at`     | `timestamptz NOT NULL`     |

## 4. Enum Strategy

Follow `14-enum-strategy.md`:
- Java enum `TradeOfferStatus` in `barter-domain`.
- Persisted as `varchar` via `@Enumerated(EnumType.STRING)`.
- Mirrored in OpenAPI as `TradeOfferStatus` schema.

Values + allowed transitions (state machine):

```
PENDING ──► ACCEPTED   (terminal)
   │
   ├──► REJECTED       (terminal)
   ├──► CANCELLED      (terminal)
   └──► EXPIRED        (terminal, system)
```

Rules:
- Only `PENDING` is mutable.
- `ACCEPTED` triggers side effects (archive both items, void competing pending offers).
- `EXPIRED` is set by a scheduled job (out of v1 scope if `expires_at` is omitted).

## 5. API Endpoints

All under `/api/v1`. All endpoints require auth.

| Method | Path                              | Purpose                              | Authorization                       |
|--------|-----------------------------------|--------------------------------------|-------------------------------------|
| POST   | `/trade-offers`                   | Create a new offer                   | Authenticated, not own item         |
| GET    | `/trade-offers/incoming`          | Paged offers received by current user | Authenticated; filter `?status=`   |
| GET    | `/trade-offers/sent`              | Paged offers sent by current user    | Authenticated; filter `?status=`    |
| GET    | `/trade-offers/{uuid}`            | Detail of a single offer             | Sender or receiver only             |
| POST   | `/trade-offers/{uuid}/accept`     | Receiver accepts a pending offer     | Receiver only, status = PENDING     |
| POST   | `/trade-offers/{uuid}/reject`     | Receiver rejects a pending offer     | Receiver only, status = PENDING     |
| POST   | `/trade-offers/{uuid}/cancel`     | Sender cancels a pending offer       | Sender only, status = PENDING       |

Pagination/sort parameters reuse the existing `Page`/`Size`/`Sort` parameters (see `17-pagination-search-strategy.md`).

Error semantics:
- `400` — invalid payload, sender owns receiver item, items not ACTIVE, self-offer.
- `403` — caller is not sender (cancel) or not receiver (accept/reject), or not a participant (detail).
- `404` — offer not found.
- `409` — illegal state transition (e.g., accept on non-PENDING), or accept conflicts with already-accepted offer on either item.

## 6. OpenAPI DTOs

New schemas in `backend/barter-api/src/main/resources/openapi/components/schemas/trade/`:

- **`TradeOfferStatus.yaml`** — enum: `PENDING | ACCEPTED | REJECTED | CANCELLED | EXPIRED`.
- **`CreateTradeOfferRequest.yaml`**
    - `receiverItemUuid: uuid` (required) — derives receiver user
    - `senderItemUuid: uuid` (required) — must belong to sender
    - `message: string` (optional, ≤1000 chars)
- **`TradeOfferUserSummary.yaml`** — minimal `{ uuid, username }` for embedded participants.
- **`TradeOfferItemSummary.yaml`** — minimal `{ uuid, title, status, condition, categoryName }`.
- **`TradeOfferResponse.yaml`** — full detail for `GET /{uuid}` and action responses:
    - `uuid`, `status`, `message?`
    - `sender: TradeOfferUserSummary`, `receiver: TradeOfferUserSummary`
    - `senderItem: TradeOfferItemSummary`, `receiverItem: TradeOfferItemSummary`
    - `createdAt`, `respondedAt?`, `expiresAt?`
- **`TradeOfferSummaryResponse.yaml`** — same as detail but compact (used in lists).
- **`TradeOfferPagedResponse.yaml`** — paged wrapper following existing pattern.

New parameter:
- **`TradeOfferUuid.yaml`** — path parameter for `{uuid}`.

Path file:
- `paths/trade-offers.yaml` — defines `list`, `incoming`, `sent`, `byUuid`, `accept`, `reject`, `cancel` operations referencing the schemas above.

Register all new schemas/paths/parameters in `openapi.yaml`.

## 7. Backend Implementation Order

Layer-by-layer to keep PRs small. Follows `06-backend-architecture.md` and `15-domain-entity-strategy.md`.

1. **Domain (`barter-domain`)**
    - `TradeOfferStatus` enum.
    - `TradeOffer` entity + state-transition methods (`accept`, `reject`, `cancel`).
    - Unit tests for transitions + invariants.
2. **Infrastructure (`barter-infrastructure`)**
    - Flyway migration for `trade_offer` table + indexes (and history table if included).
    - `TradeOfferRepository` (Spring Data JPA) with finders for incoming/sent + status filter.
3. **Application (`barter-application`)**
    - `TradeOfferService` orchestrating: create, list, get, accept, reject, cancel.
    - `acceptOffer` is the transactional hot spot:
        - Lock both items (or use partial unique index + retry on conflict).
        - Verify both still `ACTIVE`.
        - Set offer to `ACCEPTED`.
        - Archive both items via existing `ItemService.archive(...)` (system-initiated, reason = `"trade-accepted"`).
        - Optionally auto-reject other `PENDING` offers referencing either item.
    - Authorization checks (sender/receiver/participant).
    - Service tests covering happy path + each forbidden/conflict case.
4. **Web (`barter-web`)**
    - DTO mappers (entity ↔ OpenAPI generated DTO).
    - `TradeOfferController` with the 7 endpoints.
    - `@WebMvcTest` slice tests.
5. **API (`barter-api`)**
    - OpenAPI YAML changes (§6).
    - Regenerate generator outputs; ensure CI build passes.
6. **Integration tests**
    - End-to-end flow: create → accept → both items ARCHIVED → second accept on same item returns 409.

## 8. Frontend Integration Order

Mirrors the catalog rollout (`docs/26`).

1. **Schema regen**
    - `yarn generate:api` after backend OpenAPI lands.
    - Add friendly types in `src/api/generated/types.ts`: `TradeOfferStatus`, `TradeOfferResponse`, `TradeOfferSummaryResponse`, `TradeOfferPagedResponse`, `CreateTradeOfferRequest`.
2. **API service** — `src/api/tradeOffersApi.ts` with the 7 functions.
3. **Hooks** — `src/features/trade-offers/useTradeOffers.ts`:
    - Queries: `useIncomingOffers`, `useSentOffers`, `useTradeOfferDetail`.
    - Mutations: `useCreateTradeOffer`, `useAcceptOffer`, `useRejectOffer`, `useCancelOffer` (invalidate offers + items caches).
4. **UI primitives**
    - `TradeOfferStatusBadge`.
    - `TradeOfferCard` (shows both items side-by-side with placeholder image area).
5. **Pages**
    - `OffersPage` (replaces "My Offers" placeholder) with two tabs: **Incoming** and **Sent**, status filter, pagination.
    - `TradeOfferDetailPage` at `/offers/:uuid` with action buttons (accept/reject/cancel) gated by role.
6. **Item detail integration**
    - On `ItemDetailPage`, when item is `ACTIVE` and not owned by the current user, show **"Propose Trade"** button.
    - Opens a modal listing the user's own `ACTIVE` items (via `useMyItems({ status: "ACTIVE" })`); on submit calls `useCreateTradeOffer`.
7. **Routing & nav**
    - Add `/offers/:uuid` route.
    - Sidebar "My Offers" already present — wire it to the real `OffersPage`.
8. **Marketplace cleanup**
    - Confirm public search defaults to `ACTIVE` only; archived items already disappear (no change expected, but verify after backend auto-archive lands).

## 9. v1 vs Later

### In v1
- Item ↔ item offers only (1:1).
- 5 statuses, sender/receiver actions, accept auto-archives both items.
- Partial unique indexes prevent double-accept races.
- Incoming/sent list, detail, create.
- "Propose Trade" entry point on item detail.

### Deferred (later versions)
- **Counter-offers** — receiver proposes alternate item.
- **Multi-item bundles** — N sender items for M receiver items.
- **Cash adjustment** — small monetary delta in either direction.
- **Expiration job** — scheduled task to flip `PENDING → EXPIRED`; v1 may ship without `expires_at` enforcement.
- **Auto-rejection of competing pending offers** on accept (can launch with manual orphan handling and add later).
- **Status history audit table** if not included in v1 migration.
- **Notifications** (in-app/email) for new/responded offers.
- **Messaging thread** attached to an offer.
- **Reputation / trade history** on user profiles.
- **Disputes & moderation** workflow.
- **Trade completion confirmation** (both parties confirm physical handoff) — v1 treats `ACCEPTED` as terminal.