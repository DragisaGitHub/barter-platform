# Domain Model

## Core Domains

The platform is divided into several core domains.

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

Future-ready authentication capabilities:
- username/email and password authentication
- OAuth2 external login providers
- multi-factor authentication using authenticator applications
- recovery codes

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

# Wishlist & Matching

Responsible for:
- user wishes
- desired items
- automatic matching

Core entities:
- Wishlist
- WishlistEntry
- MatchCandidate

---

# Trade

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
- read receipts
- attachments

Core entities:
- Conversation
- ConversationParticipant
- Message
- MessageAttachment
- MessageReadReceipt

---

# Reputation & Trust

Responsible for:
- ratings
- reviews
- user trust
- reports

Core entities:
- Rating
- Review
- Report
- ModerationAction

---

# Monetization

Responsible for:
- advertisements
- promoted listings
- premium profiles

Core entities:
- PromotionPlan
- ItemPromotion
- AdCampaign
- AdPlacement

---

# Notification

Responsible for:
- system notifications
- unread counters
- event delivery

Core entities:
- Notification
- NotificationPreference

---

# Audit & History

Responsible for:
- auditing
- history tracking
- traceability

Core entities:
- AuditLog
- EntityChangeHistory