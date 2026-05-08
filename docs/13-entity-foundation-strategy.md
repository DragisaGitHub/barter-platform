# Entity Foundation Strategy

## Goal

Define common entity foundation classes shared across the backend domain model.

The goal is:
- consistency
- reduced duplication
- predictable persistence behavior
- audit support
- UUID standardization

---

# Core Principles

- All business entities inherit common base classes.
- Internal database IDs use BIGINT.
- Public identifiers use UUID.
- Audit fields are standardized.
- Entities should remain clean and predictable.

---

# Planned Base Classes

## BaseEntity

Responsibilities:
- internal ID
- public UUID

Common fields:
- id
- uuid

---

## AuditableEntity

Extends BaseEntity.

Responsibilities:
- creation audit
- update audit
- soft delete support

Common fields:
- createdAt
- updatedAt
- deletedAt

---

# UUID Strategy

UUID values:
- generated in backend application
- immutable after creation
- exposed publicly through APIs

Rules:
- UUID must never change
- UUID must never be nullable
- internal IDs are not exposed externally

---

# Audit Strategy

Audit fields:
- createdAt
- updatedAt
- deletedAt

Rules:
- createdAt set automatically
- updatedAt updated automatically
- deletedAt used for soft delete

---

# Entity Design Rules

Entities should:
- represent business state
- avoid business orchestration logic
- avoid framework-heavy logic
- remain persistence-focused

Entities should not:
- contain controller logic
- contain API DTO logic
- contain HTTP concerns

---

# Lombok Strategy

Use Lombok carefully.

Preferred:
- getters/setters
- builders where appropriate

Avoid:
- excessive magic
- complex inheritance builders initially

---

# Equals and HashCode Strategy

Rules:
- avoid using mutable fields
- avoid using collections
- use UUID for business identity where needed

---

# Soft Delete Strategy

Soft delete uses:
- deletedAt timestamp

Physical delete should be avoided for:
- users
- trades
- messages
- moderation history

---

# Future Extensions

Foundation should later support:
- createdBy
- updatedBy
- tenant support
- optimistic locking
- audit history