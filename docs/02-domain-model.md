# Domain Model

## Core Domains

The platform is divided into the following core domains.

---

# Identity & Access

Responsible for:
- user accounts
- authentication
- authorization
- profile management
- contact visibility

Core entities:
- User
- UserProfile
- UserContactMethod
- Role
- Permission

---

# Catalog

Responsible for:
- items
- categories
- tags
- collections
- item metadata
- item visibility

Core entities:
- Item
- Category
- Tag
- Collection
- ItemImage
- ItemAttribute

---

# Trades

Responsible for:
- trade offers
- trade negotiations
- trade states
- trade history

Core entities:
- TradeOffer
- TradeOfferItem
- TradeStatusHistory

---

# Messaging

Responsible for:
- conversations
- messages
- attachments

Core entities:
- Conversation
- ConversationParticipant
- Message
- MessageAttachment

---

# Notifications

Responsible for:
- system notifications
- unread counters
- event delivery

Core entities:
- Notification
- NotificationPreference

---

# Reviews & Reports

Responsible for:
- ratings
- reviews
- user trust
- reports
- moderation

Core entities:
- Rating
- Review
- Report
- ModerationAction
