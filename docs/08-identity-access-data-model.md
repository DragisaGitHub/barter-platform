# Identity & Access Data Model

## Overview

This document defines the database-level design for identity, authentication, authorization and account security.

The model must support:
- local authentication
- OAuth2 authentication
- MFA
- RBAC authorization
- refresh tokens
- auditability
- future scalability

---

# Modeling Principles

- Every major table uses:
    - BIGINT internal primary key
    - UUID public identifier
- Public APIs expose UUID only
- Security-sensitive operations must be auditable
- Soft delete should be preferred for user lifecycle handling
- Usernames and emails must be unique
- Authentication providers must be extensible

---

# Planned Tables

## users

Core account table.

Responsibilities:
- authentication identity
- account state
- security state

Important fields:
- id
- uuid
- username
- email
- password_hash
- status
- email_verified
- mfa_enabled
- last_login_at
- created_at
- updated_at
- deleted_at

---

## roles

Defines logical platform roles.

Examples:
- USER
- MODERATOR
- ADMIN

Important fields:
- id
- uuid
- code
- name
- description

---

## permissions

Defines granular platform permissions.

Examples:
- ITEM_CREATE
- ITEM_DELETE
- USER_BAN
- REPORT_REVIEW

Important fields:
- id
- uuid
- code
- name
- description

---

## user_roles

Many-to-many relation between users and roles.

Important fields:
- user_id
- role_id
- assigned_at
- assigned_by

---

## role_permissions

Many-to-many relation between roles and permissions.

Important fields:
- role_id
- permission_id
- assigned_at

---

## oauth_accounts

Stores external authentication provider connections.

Examples:
- Google
- Apple

Important fields:
- id
- uuid
- user_id
- provider
- provider_user_id
- linked_at

---

## refresh_tokens

Stores refresh tokens for JWT authentication.

Important fields:
- id
- uuid
- user_id
- token_hash
- expires_at
- revoked_at
- created_at
- device_info
- ip_address

---

## user_mfa_settings

Stores MFA configuration.

Important fields:
- id
- uuid
- user_id
- secret
- enabled
- configured_at

---

## user_mfa_recovery_codes

Stores MFA recovery codes.

Important fields:
- id
- user_mfa_settings_id
- code_hash
- used_at

---

# User Lifecycle States

Planned values:

- PENDING_VERIFICATION
- ACTIVE
- SUSPENDED
- BANNED
- DELETED

---

# Security Rules

- Never store plaintext passwords
- Never store raw refresh tokens
- Store hashed refresh tokens only
- MFA secrets must be encrypted
- Emails must be verified before full account activation
- OAuth accounts must be unique per provider

---

# Future Extensions

The model should later support:
- session tracking
- device management
- login history
- suspicious activity detection
- account recovery workflows
- advanced permission groups