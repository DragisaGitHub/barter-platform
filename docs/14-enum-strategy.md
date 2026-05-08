# Enum Strategy

## Goal

Define a consistent enum strategy across the backend.

Enums represent stable business states and system constants.

---

# General Principles

- Enums use uppercase values.
- Enums are stored as VARCHAR in the database.
- PostgreSQL enum types are avoided initially.
- Enum names should remain stable once used in production.
- Enums must be business-oriented and explicit.

---

# Persistence Strategy

JPA entities should use:

    @Enumerated(EnumType.STRING)

Never use:

    EnumType.ORDINAL

Reason:
- ordinal values are fragile
- ordinal changes break persistence compatibility

---

# Initial Planned Enums

## UserStatus

Values:
- PENDING_VERIFICATION
- ACTIVE
- SUSPENDED
- BANNED
- DELETED

---

## OAuthProvider

Values:
- GOOGLE
- APPLE

---

## PermissionCode

Values include:
- ITEM_CREATE
- ITEM_UPDATE
- ITEM_DELETE
- MESSAGE_SEND
- USER_BAN
- ADMIN_ACCESS

---

## RoleCode

Values:
- USER
- MODERATOR
- ADMIN

---

# Future Planned Enums

Examples:
- ItemStatus
- TradeOfferStatus
- ConversationStatus
- MessageType
- ReportStatus
- NotificationType

---

# Rules

- Avoid generic enum names.
- Prefer domain-specific naming.
- Avoid changing enum values after release.
- Add new values carefully with migration awareness.

---

# API Strategy

OpenAPI schemas should expose enum values explicitly.

Frontend applications should rely on API enum contracts where appropriate.