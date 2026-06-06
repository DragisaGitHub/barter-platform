# Priority 1 — Trade Messaging Launch Polish

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-application/src/main/java/com/barterplatform/application/trade/service/impl/TradeOfferMessageServiceImpl.java`
- `backend/barter-api/src/main/resources/openapi/paths/trade-offer-messages.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/trade/TradeOfferMessageResponse.yaml`

### Frontend

- `frontend/src/features/trade/TradeOfferMessagesPanel.tsx`
- `frontend/src/features/trade/useTradeOfferMessages.ts`
- `frontend/src/features/trade/TradeOfferDetailPage.tsx`
- `frontend/src/api/tradeOfferMessagesApi.ts`

## Already implemented

- The message panel already ships with:
  - loading and error states
  - empty conversation state
  - 15-second polling indicator
  - optimistic send state
  - character counter and 2000-character limit
  - read-only mode when the trade is not writable
  - a "new messages" jump button when the user is scrolled up
  - report affordances on inbound messages
- `TradeOfferMessageServiceImpl` already validates blank content, max length, participant access, and writable trade statuses.
- Accessibility is not absent: the panel already uses `role="log"`, `aria-live="polite"`, and disabled states.

## Confirmed missing

1. **Send failures are not surfaced inside the messaging UI.**
   - `TradeOfferMessagesPanel.tsx` catches failed sends only to reset scroll intent.
   - There is no inline error banner, retry affordance, or last-failed message state in the panel.

2. **Polling health is hinted, but stale-state recovery is still thin.**
   - The panel shows "syncing updates" / "polling every 15s", but there is no explicit stale-data warning or last-synced indicator if polling falls behind.

3. **There is no launch-ready polish checklist for cross-page messaging behavior.**
   - The conversation panel is polished, but there is no documented pass tying `TradeOfferDetailPage.tsx`, offer status transitions, and message error states together.

## Not needed / false positives

- Do **not** reopen empty/loading-state work as a general backlog item; those states already exist.
- Do **not** add attachments, reactions, or realtime transport under this document.
- Do **not** move unread semantics into this item; that belongs in `priority-0/03-trade-message-read-state-fix.md` and `priority-1/02-message-notifications-and-unread-indicators.md`.

## Intentionally deferred

- Rich-media messaging and websocket delivery remain out of scope.

## Implementation-ready backlog

### Frontend

1. Add an inline failed-send state to `TradeOfferMessagesPanel.tsx` so a dropped request is visible where the user was typing.
2. Decide whether failed sends should be retryable in-place or fully retyped; the current UI does neither explicitly.
3. Add a stale-sync indicator that distinguishes “polling normally” from “latest fetch failed / delayed”.

### Backend / API

4. Keep backend work limited to clarifying error semantics if the UI needs a more structured retry contract.

## Dependencies and follow-on impact

- Depends on `priority-0/03-trade-message-read-state-fix.md` for authoritative read semantics.
- Should be executed before or alongside targeted frontend tests for the message panel.

## Exit criteria

- A failed send is visible and understandable without checking the browser console.
- Polling lag has a user-visible fallback state, not just a background spinner label.
- The messaging polish backlog stays small and does not reopen finished work.
