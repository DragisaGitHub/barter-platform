# Launch Blocker 02 — User Reporting System

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 launch risk reduction / trust and safety  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Create a minimal public reporting flow for unsafe listings, abusive users, messages, trade offers, and reviews.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap calls missing user reporting a critical safety gap.
- A barter/community product needs structured safety signals before public self-registration.

# Current State

- Admin listing moderation exists.
- No general report model appears for item, user, message, trade offer, or review targets.
- Frontend has item, profile, trade, message, and review surfaces where report actions can be added.

# Risks

- Users may have no trusted way to report scams, harassment, unsafe exchanges, no-shows, or prohibited items.
- Moderators may rely on ad hoc emails or manual database checks.
- False or malicious reports can be used to harass legitimate users if not rate-limited and audited.

# Proposed Solution

- Add a single `reports` domain with target type, target UUID, reporter, reason code, optional details, status, assignment, resolution note, and timestamps.
- Supported reason codes should start simple: prohibited item, spam/scam, harassment, misleading listing, unsafe exchange, no-show, and other.
- Expose authenticated report-create endpoints and admin/moderator report list/detail/status endpoints.
- Add report buttons on item detail, public profile, trade offer detail, message panel, and review cards.
- Integrate with rate limiting from `01-rate-limiting.md`.

# Simpler Alternatives

- Launch with a mailto safety contact, but this is not enough for public scale or moderation auditability.
- Start with item-only reporting if capacity is extremely constrained, then add users/messages/trades/reviews immediately after.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- Add reporting as a new moderation capability inside existing modules rather than as a separate case-management service.
- Reuse current RBAC/admin patterns while allowing future moderator-specific permissions.

# Operational Impact

- Creates a daily moderation inbox and triage workflow.
- Requires report status definitions and ownership expectations.
- Enables support handoff without direct database inspection.

# Security Impact

- Improves abuse detection and community safety.
- Must enforce authentication, target visibility checks, and no self-review/report edge-case leaks.
- Report details can contain sensitive content and must be protected in logs and UI.

# Developer Velocity Impact

- Provides one reusable pattern for future trust/safety features.
- Adds database/model/API/frontend work but reduces emergency moderation churn later.

# Backend Changes

- Add OpenAPI schemas and endpoints for report create/list/detail/update.
- Add domain entity/enums, repository, service, mapper, controller, and authorization checks.
- Validate target existence and reporter eligibility.
- Add admin/moderator status transition rules.

# Frontend Changes

- Add reusable report dialog component with target metadata and reason codes.
- Add report entry points to item, profile, trade detail, messages, and reviews.
- Show success state without exposing moderation internals.

# Database Changes

- Add Flyway migration for `reports` table and indexes by status, target type, reporter, created date, and assigned moderator.
- Use UUID public identifiers and internal numeric IDs consistent with existing conventions.

# Deployment Changes

- No new infrastructure.
- Ensure report endpoints are included in security smoke tests and rate-limit policies.

# Testing Strategy

- Unit test validation and status transitions.
- Integration test report creation for each target type.
- Authorization tests for anonymous/user/moderator/admin access.
- Frontend component tests for report dialog validation and 429 handling.

# Rollout Plan

- Implement backend model/API first.
- Add admin visibility next so reports are actionable before public buttons are broadly exposed.
- Enable report buttons per surface, starting with item and profile.
- Review report volume and reason-code quality after beta.

# Future Improvements

- Add attachments/evidence only if needed.
- Add duplicate report grouping.
- Add reporter abuse detection and appeal flows.
- Add email notifications to moderators after email infrastructure is reliable.

# Explicitly Deferred

- Enterprise case-management workflow.
- AI moderation classification.
- Realtime moderator notifications.
- External trust/safety vendor integration.
