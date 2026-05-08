# Database Conventions

## General Principles

The database model must prioritize:
- consistency
- scalability
- auditability
- maintainability
- predictable naming

---

# Primary Keys

All major tables use:

- BIGINT internal primary key
- UUID public identifier

Rules:
- internal numeric IDs are never exposed publicly
- APIs expose UUID values only

Example:

- id = 15
- uuid = 8f7b4b7c-0f22-41d8-95ef-2d891e1f1111

---

# UUID Strategy

UUID columns:
- type: UUID
- must be unique
- must be NOT NULL

UUID generation:
- backend-generated
- never database-generated initially

---

# Timestamp Columns

All major business tables should contain:

- created_at
- updated_at

Optional:
- deleted_at

Rules:
- timestamps use UTC
- timestamps use TIMESTAMP WITH TIME ZONE where appropriate

---

# Soft Delete Strategy

Core business entities should prefer soft delete.

Soft delete uses:

- deleted_at

Rules:
- deleted rows remain queryable for audit/history
- business-critical records are not physically removed immediately

---

# Naming Conventions

## Tables

Rules:
- lowercase
- snake_case
- plural names

Examples:
- users
- trade_offers
- message_read_receipts

---

## Columns

Rules:
- lowercase
- snake_case

Examples:
- created_at
- user_id
- provider_user_id

---

## Foreign Keys

Rules:
- use <entity>_id naming

Examples:
- user_id
- role_id
- conversation_id

---

## Unique Constraints

Rules:
- explicitly named

Examples:
- uq_users_email
- uq_users_username

---

## Indexes

Rules:
- explicitly named

Examples:
- idx_items_owner_id
- idx_messages_conversation_id

---

# Audit Strategy

Business-critical tables should support:
- created_at
- updated_at
- deleted_at

Sensitive operations should later support:
- created_by
- updated_by

---

# Status Fields

Business entities should use status fields instead of booleans where state evolution is expected.

Preferred:

- status = ACTIVE

Avoid:

- is_active = true

---

# Enum Strategy

Business enums should:
- be stable
- use uppercase values
- avoid database-specific enum types initially

Preferred:
- VARCHAR columns with application-level enums

---

# Migration Strategy

Database changes must be managed through Flyway migrations.

Rules:
- never manually modify production schema
- migrations are immutable
- migrations are append-only

Naming convention:

    V001__initial_identity_schema.sql
    V002__catalog_schema.sql
    V003__wishlist_schema.sql

---

# Relationship Strategy

Use:
- foreign keys
- explicit indexes
- cascading carefully

Avoid:
- uncontrolled cascade delete

---

# Performance Principles

Plan indexes early for:
- UUID lookup
- foreign keys
- status fields
- timestamps
- search/filter fields

---

# Multi-Tenancy

Not supported initially.

The model should remain flexible enough for future tenant support if needed.