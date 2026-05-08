# Identity & Access Model

## Goals

The Identity & Access module is responsible for:
- user registration
- authentication
- authorization
- account lifecycle management
- role-based access control
- MFA support
- OAuth2 integration readiness

---

# Authentication Strategy

Initial authentication:
- email/password authentication
- JWT access token
- refresh token support

Future authentication support:
- Google OAuth2
- Apple OAuth2
- authenticator app MFA
- recovery codes

---

# Authorization Strategy

The system uses RBAC (Role-Based Access Control).

Users can have multiple roles.

Roles contain permissions.

Permissions define access to application features.

---

# Main Entities

## User

Represents a platform account.

Responsibilities:
- authentication
- account state
- security state

Important properties:
- internal ID
- public UUID
- email
- username
- password hash
- account status
- email verification status
- MFA enabled flag

---

## Role

Represents a logical security role.

Examples:
- USER
- MODERATOR
- ADMIN

---

## Permission

Represents a granular system capability.

Examples:
- ITEM_CREATE
- ITEM_DELETE
- REPORT_REVIEW
- USER_BAN

---

## OAuthAccount

Represents external OAuth2 identity linkage.

Examples:
- Google account
- Apple account

---

## UserMfaSettings

Represents MFA configuration.

Supports:
- TOTP authenticator apps
- recovery codes

---

# User Lifecycle

Planned states:

- PENDING_VERIFICATION
- ACTIVE
- SUSPENDED
- BANNED
- DELETED

---

# Security Principles

- Never expose internal numeric IDs
- Store password hashes only
- Use short-lived access tokens
- Support token revocation
- Audit sensitive security actions
- Use rate limiting on authentication endpoints

---

# Future Scalability

The model should support:
- multiple login providers
- advanced permission systems
- audit logging
- account recovery
- device/session tracking