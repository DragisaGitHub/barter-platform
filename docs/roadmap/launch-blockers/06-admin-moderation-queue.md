# Launch Blocker 06 — Admin Moderation Queue

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 trust operations  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Make user reports and moderation actions actionable through a small, focused admin/moderator queue.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- A report model without an operational queue does not protect users.
- The roadmap identifies admin tooling as functional but not operationally sufficient for public launch.

# Current State

- Admin pages exist for dashboard, users, categories, listings, tags, and reviews.
- Listing moderation actions exist.
- No general report queue, queue counts, assignments, or report triage workflow exists.

# Risks

- Reports may be missed or resolved inconsistently.
- Admins may need direct DB queries to triage abuse.
- Lack of audit notes makes disputes hard to handle.

# Proposed Solution

- Add admin/moderator report list with filters by status, target type, reason, assignment, and age.
- Add report detail view showing reporter, target summary, reason, details, history, and safe action links.
- Support status transitions: open, in review, resolved, dismissed.
- Record resolution note and acting admin/moderator.
- Expose small dashboard counters for open and stale reports.

# Simpler Alternatives

- Start with admin-only list/detail/status update before moderator-specific UI.
- Skip assignment in first pass if the team is one operator, but keep schema ready for assignment.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- Build on reporting/moderation in the existing backend and admin frontend.
- Do not introduce a separate moderation service or workflow engine.

# Operational Impact

- Defines a daily safety workflow.
- Reduces manual investigation time.
- Creates a basis for escalation and accountability.

# Security Impact

- Queue must be restricted to admins/moderators.
- Reporter identity and report details must not be visible to report targets.
- Action history supports abuse and dispute investigation.

# Developer Velocity Impact

- Moderation UI reduces support/debug burden.
- Adds frontend/backend work but pays back quickly once public users arrive.

# Backend Changes

- Add admin report list/detail/update endpoints if not already delivered in reporting system.
- Add authorization for admin/moderator access.
- Add report summary projections for queue performance.

# Frontend Changes

- Add admin route(s) for report queue and report detail.
- Add filters, pagination, status badges, assignment/resolution controls, and links to target records.
- Add dashboard card/count for pending reports.

# Database Changes

- Use `reports` table from `02-reporting-system.md`.
- Add indexes for queue filters if not already included.
- Optional `admin_audit_events` can be added here only for report status changes if small.

# Deployment Changes

- No new infrastructure.
- Add route to admin navigation and include in smoke tests.

# Testing Strategy

- Backend authorization tests for user/moderator/admin.
- Integration tests for filters, pagination, and status transitions.
- Frontend tests for queue rendering and status update errors.
- Manual smoke test with seeded reports.

# Rollout Plan

- Build queue after report API exists.
- Seed sample reports in DEV for QA.
- Enable admin-only first, then moderator access when permissions are clear.
- Review report-handling cadence after beta.

# Future Improvements

- SLA/stale report indicators.
- Moderator assignment notifications.
- Duplicate report grouping.
- Escalation workflows for severe safety issues.

# Explicitly Deferred

- Enterprise moderation case management.
- AI report triage.
- Realtime queue collaboration.
- Separate moderation microservice.
