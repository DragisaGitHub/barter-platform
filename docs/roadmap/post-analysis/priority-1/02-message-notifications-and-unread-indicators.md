# Priority 1 — Message Notifications & Unread Indicators

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current implementation

### Backend

- `backend/barter-web/src/main/resources/db/migration/V008__notifications_schema.sql`
- `backend/barter-domain/src/main/java/com/barterplatform/domain/notification/enums/NotificationType.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/notification/service/NotificationService.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/notification/service/impl/NotificationServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/trade/service/impl/TradeOfferServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/reputation/service/impl/TradeReviewServiceImpl.java`
- `backend/barter-application/src/main/java/com/barterplatform/application/trade/service/impl/TradeOfferMessageServiceImpl.java`
- `backend/barter-infrastructure/src/main/java/com/barterplatform/infrastructure/trade/repository/TradeOfferMessageRepository.java`

### Frontend

- `frontend/src/features/notifications/NotificationBell.tsx`
- `frontend/src/features/notifications/NotificationsPage.tsx`
- `frontend/src/features/notifications/useNotifications.ts`
- `frontend/src/features/trade/TradeOfferMessagesPanel.tsx`
- `frontend/src/api/notificationsApi.ts`

### OpenAPI / schema

- `backend/barter-api/src/main/resources/openapi/components/schemas/notification/NotificationType.yaml`
- `backend/barter-api/src/main/resources/openapi/components/schemas/notification/NotificationUnreadCountResponse.yaml`
- `backend/barter-api/src/main/resources/openapi/paths/trade-offer-messages.yaml`

## Already implemented

- The platform already has a working in-app notification system with:
  - notification list
  - unread notification count
  - mark single read
  - mark all read
  - optimistic cache updates in React Query
- Trade-offer lifecycle events already generate notifications in `TradeOfferServiceImpl`.
- Review creation already generates `TRADE_REVIEW_RECEIVED` notifications in `TradeReviewServiceImpl`.
- Trade messages already persist read state and the repository can calculate unread message counts.

## Confirmed missing

1. **Message events do not generate notifications today.**
   - `TradeOfferMessageServiceImpl` sends/saves messages but never calls `NotificationService`.

2. **The notification enum has no message-specific type.**
   - `NotificationType.yaml` contains trade-offer, review, and listing events only.
   - There is no `TRADE_MESSAGE_RECEIVED` / equivalent type in OpenAPI or domain code.

3. **Message unread counts are not exposed anywhere user-visible.**
   - The message repository has `countByRecipientUserIdAndReadFalse(...)`, but there is no API endpoint using it.
   - `NotificationBell.tsx` and `NotificationsPage.tsx` only reflect notification unread count, not message unread count.

4. **There is no documented clearing contract between thread reads and notifications.**
   - Because message notifications do not exist yet, there is also no rule for when opening a thread clears a message notification versus just clearing a thread unread counter.

## Not needed / false positives

- Do **not** add email, mobile push, or preference-center work here.
- Do **not** rebuild the existing notification stack; the missing work is message integration, not notification-system absence.
- Do **not** use notification unread count as a substitute for message unread count.

## Intentionally deferred

- Cross-channel delivery remains deferred.
- Realtime delivery remains deferred.

## Implementation-ready backlog

### Backend / API

1. Add a dedicated message notification type if the product wants message events in the bell/page.
2. Decide whether message attention should be represented by:
   - thread unread counts only,
   - notification entries only, or
   - both, with explicit anti-double-count rules.
3. Expose message unread aggregates using the existing repository capability instead of forcing frontend inference.

### Frontend

4. Update navigation surfaces only after the backend contract defines whether message unread state lives in the bell, a dedicated badge, or both.
5. Ensure any badge-clearing behavior is based on confirmed server read state, not local optimistic assumptions.

## Dependencies and follow-on impact

- Depends on `priority-0/03-trade-message-read-state-fix.md`.
- Should stay coordinated with `priority-1/01-trade-messaging-launch-polish.md` so the message panel and notification surfaces do not drift.

## Exit criteria

- Message-generated attention cues use an explicit backend contract.
- There is no double-counting between notification unread totals and message unread totals.
- Navigation-level unread state clears according to the same read semantics used by the message thread itself.
