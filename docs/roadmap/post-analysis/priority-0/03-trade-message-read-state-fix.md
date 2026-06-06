# Priority 0 — Trade Message Read-State Fix

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Why this is still Priority 0

Trade messages are already live, but the current read model is only partially surfaced. The database and API carry `isRead` / `readAt`, while the frontend mostly treats the thread as a polling chat log and does not expose any per-thread or global unread semantics.

## Verified current implementation

### Backend

- `backend/barter-domain/src/main/java/com/barterplatform/domain/trade/entity/TradeOfferMessageEntity.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/trade/service/impl/TradeOfferMessageServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/trade/mapper/TradeOfferMessageMapper.java`
- `backend/barter-infrastructure/src/main/java/com/barterplatform/infrastructure/trade/repository/TradeOfferMessageRepository.java`

### Frontend

- `frontend/src/api/tradeOfferMessagesApi.ts`
- `frontend/src/features/trade/useTradeOfferMessages.ts`
- `frontend/src/features/trade/TradeOfferMessagesPanel.tsx`
- `frontend/src/features/trade/TradeOfferDetailPage.tsx`

### OpenAPI / schema / database

- `backend/barter-api/src/main/resources/openapi/paths/trade-offer-messages.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/trade/TradeOfferMessageResponse.yaml`
- `backend/barter-web/src/main/resources/db/migration/V009__trade_offer_messages.sql`

## Already implemented

- Message rows persist `is_read` and `read_at` in `trade_offer_messages`.
- `TradeOfferMessageEntity.markAsRead()` is already idempotent.
- `TradeOfferMessageServiceImpl.listMessages(...)` marks unread recipient messages as read when the participant loads the thread.
- The repository already contains `countByRecipientUserIdAndReadFalse(...)`, so the persistence layer can support unread aggregates.
- The frontend message panel already polls every 15 seconds, renders loading/error/empty states, and performs optimistic sends.

## Confirmed missing

1. **The API has no explicit unread aggregate contract.**
   - `trade-offer-messages.yaml` exposes only list + send.
   - There is no message unread-count endpoint, no thread unread summary endpoint, and no navigation-level aggregate endpoint.

2. **The repository unread count query is currently unused.**
   - `TradeOfferMessageRepository.countByRecipientUserIdAndReadFalse(...)` exists, but nothing in the service/controller layer exposes it.

3. **Read-state semantics are implicit only.**
   - The only read transition happens as a side effect of `listMessages(...)`.
   - There is no contract documenting whether opening a thread, fetching a thread in the background, or explicit user action should be the canonical read trigger.

4. **The frontend does not actually use `isRead` / `readAt`.**
   - `TradeOfferMessagesPanel.tsx` renders message content and timestamps, but does not display read state or unread state.
   - No other frontend trade file consumes message unread metadata.

5. **No thread-list or global unread indicator exists for messages.**
   - The only message-specific count shown today is `messages.count` inside `TradeOfferMessagesPanel.tsx`, which is total messages, not unread messages.

## Not needed / false positives

- Do **not** rebuild messaging around websockets or push delivery.
- Do **not** redesign the composer; validation and optimistic send are already present.
- Do **not** create a message event store; the existing table is sufficient for this backlog item.

## Intentionally deferred

- Realtime message delivery is still outside scope.
- Cross-channel delivery (email / mobile push) belongs to later notification work, not this fix.

## Implementation-ready backlog

### Backend

1. Decide whether implicit read-on-list remains the source of truth or whether a dedicated mark-read action is required.
2. If implicit read remains, document it in the API and ensure background fetches cannot silently change semantics.
3. Expose unread aggregates using the existing repository capability so other surfaces stop guessing.

### API

4. Extend the message contract with thread-level/global unread metadata, or add dedicated unread endpoints.
5. Keep `TradeOfferMessageResponse` stable; do not overload per-message read fields to stand in for thread counters.

### Frontend

6. Update trade-offer surfaces to consume server unread data rather than inferring read state from “thread was opened”.
7. Add UI rules for when a thread transitions from unread to read, especially under 15-second polling and background refetches.

## Dependencies and follow-on impact

- This document is the prerequisite for `priority-1/02-message-notifications-and-unread-indicators.md`.
- Any unread badge work should wait until the read contract stops being implicit-only.

## Exit criteria

- One server-defined rule explains when a trade message becomes read.
- The backend exposes unread information beyond the raw per-message list.
- Frontend trade surfaces stop treating unread state as “not yet implemented”.
