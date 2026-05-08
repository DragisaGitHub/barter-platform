# Data Model

## Modeling Principles

- Use PostgreSQL as the primary database
- Use `BIGINT` internal primary keys
- Use `UUID` public identifiers
- Never expose internal numeric IDs through the API
- Use soft delete where business history matters
- Track creation and update timestamps on all important entities
- Use status fields instead of physical deletion for core business objects
- Keep the model generic enough to support future categories beyond Kinder toys

---

## Main Data Areas

The database model is organized into the following areas:

1. Identity & Access
2. User Profile & Contact
3. Catalog
4. Wishlist & Matching
5. Trade Offers
6. Messaging
7. Reputation & Trust
8. Moderation
9. Monetization
10. Notifications
11. Audit & History

---

## Identity & Access

Planned tables:

- users
- roles
- permissions
- user_roles
- role_permissions
- oauth_accounts
- user_mfa_settings
- user_mfa_recovery_codes

---

## User Profile & Contact

Planned tables:

- user_profiles
- user_contact_methods
- user_contact_preferences

---

## Catalog

Planned tables:

- categories
- category_attributes
- tags
- collections
- items
- item_images
- item_tags
- item_attribute_values
- item_contact_preferences

---

## Wishlist & Matching

Planned tables:

- wishlists
- wishlist_entries
- wishlist_entry_tags
- match_candidates

---

## Trade Offers

Planned tables:

- trade_offers
- trade_offer_items
- trade_offer_status_history

---

## Messaging

Planned tables:

- conversations
- conversation_participants
- messages
- message_attachments
- message_read_receipts

---

## Reputation & Trust

Planned tables:

- ratings
- reviews

---

## Moderation

Planned tables:

- reports
- moderation_actions

---

## Monetization

Planned tables:

- promotion_plans
- item_promotions
- ad_slots
- ad_campaigns
- ad_impressions
- ad_clicks

---

## Notifications

Planned tables:

- notifications
- notification_preferences

---

## Audit & History

Planned tables:

- audit_logs
- entity_change_history