# Identity & Access Seed V1

## Goal

Define initial RBAC seed data for the identity and access module.

This document is the source for:

    V002__identity_access_seed.sql

---

## Roles

Initial roles:

| Code | Name | Description |
|---|---|---|
| USER | User | Standard registered platform user |
| MODERATOR | Moderator | User responsible for reviewing reports and moderating content |
| ADMIN | Administrator | Full platform administrator |

---

## Permissions

Initial permissions:

| Code | Name | Description |
|---|---|---|
| ITEM_VIEW | View items | Allows viewing active item listings |
| ITEM_CREATE | Create items | Allows creating item listings |
| ITEM_UPDATE | Update items | Allows updating owned item listings |
| ITEM_DELETE | Delete items | Allows deleting or archiving owned item listings |
| MESSAGE_SEND | Send messages | Allows sending messages to other users |
| TRADE_OFFER_CREATE | Create trade offers | Allows creating trade offers |
| TRADE_OFFER_RESPOND | Respond to trade offers | Allows accepting or rejecting received trade offers |
| PROFILE_UPDATE | Update profile | Allows updating own profile |
| REPORT_CREATE | Create reports | Allows reporting users, items or messages |
| REPORT_REVIEW | Review reports | Allows reviewing submitted reports |
| MODERATION_ACTION_CREATE | Create moderation actions | Allows performing moderation actions |
| USER_VIEW | View users | Allows viewing user accounts for administration/moderation |
| USER_SUSPEND | Suspend users | Allows suspending user accounts |
| USER_BAN | Ban users | Allows banning user accounts |
| ADMIN_ACCESS | Admin access | Allows access to administrator features |

---

## Role Permission Mapping

### USER

Permissions:

- ITEM_VIEW
- ITEM_CREATE
- ITEM_UPDATE
- ITEM_DELETE
- MESSAGE_SEND
- TRADE_OFFER_CREATE
- TRADE_OFFER_RESPOND
- PROFILE_UPDATE
- REPORT_CREATE

### MODERATOR

Includes all USER permissions plus:

- REPORT_REVIEW
- MODERATION_ACTION_CREATE
- USER_VIEW
- USER_SUSPEND

### ADMIN

Includes all MODERATOR permissions plus:

- USER_BAN
- ADMIN_ACCESS

---

## Seed Rules

- Seed data must be deterministic.
- Role codes must be unique.
- Permission codes must be unique.
- Seed migration must be safe to run once through Flyway.
- Do not create application users in this migration.
- Use fixed UUID values for roles and permissions.
- Use current timestamp for created_at.
- Do not update existing records in V002 unless explicitly required.

---

## Notes

This seed only defines platform authorization structure.

Initial admin user creation will be handled separately.