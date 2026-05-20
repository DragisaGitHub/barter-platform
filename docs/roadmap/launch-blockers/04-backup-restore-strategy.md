# Launch Blocker 04 — Backup and Restore Strategy

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 data protection  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Establish verified backup, restore, retention, and recovery expectations for real user data before public launch.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- Listings, images, messages, offers, reviews, and accounts are user-created data with real trust value.
- The roadmap treats unverified backup/restore as an existential launch risk.

# Current State

- A database backup script exists under deployment scripts.
- No documented automated schedule, off-host encrypted retention, image backup coverage, or restore verification routine.

# Risks

- Host loss, disk corruption, operator error, failed migrations, or accidental deletion could destroy user data.
- Backups that are never restored may be unusable when needed.
- Images and database backups can drift if recovery procedures are not coordinated.

# Proposed Solution

- Define realistic RPO/RTO for beta and production.
- Automate PostgreSQL backups on a schedule with encrypted off-host storage.
- Document image storage backup/lifecycle coverage for local and Azure Blob modes.
- Run restore tests into a fresh database on a fixed cadence.
- Add backup success/failure observability and a restore runbook.

# Simpler Alternatives

- For invite-only beta, daily encrypted off-host DB backups plus monthly restore test may be enough.
- Use managed PostgreSQL backups if the deployment moves to managed DB, but still test application-level restore.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- No product architecture change is required.
- Prefer managed services or simple scripts over custom backup services.

# Operational Impact

- Adds recurring operational responsibility.
- Requires backup storage access control, retention policy, restore owner, and test schedule.
- Improves confidence before migrations/deployments.

# Security Impact

- Backups contain personal and potentially sensitive content.
- Encrypt backups at rest and in transit.
- Restrict backup credentials and avoid writing secrets into logs.

# Developer Velocity Impact

- Slight ops overhead, but restores reduce fear around migrations and production changes.
- Runbooks help future contributors act safely.

# Backend Changes

- None required for first milestone.
- Consider adding a maintenance endpoint only if protected and operationally justified; otherwise avoid.

# Frontend Changes

- None.

# Database Changes

- No schema changes.
- Create migration rollback/restore guidance for failed Flyway migration scenarios.

# Deployment Changes

- Update backup script/configuration for schedule, encryption, retention, and off-host destination.
- Add restore script or documented command sequence.
- Add alert on backup failure and disk usage.

# Testing Strategy

- Execute a restore into an empty database and verify Flyway/application startup.
- Validate restored core flows: login fixture, listing read, image references, offer/message/review records.
- Test backup failure alert path.

# Rollout Plan

- Inventory data stores: Postgres, image storage, deployment env/secrets references.
- Automate DB backup and off-host copy.
- Perform first restore test before public launch.
- Schedule recurring restore verification and record results.

# Future Improvements

- Managed PostgreSQL point-in-time recovery.
- Object storage lifecycle/versioning.
- Disaster recovery rehearsal after traffic grows.
- Automated anonymized staging refresh if privacy controls are in place.

# Explicitly Deferred

- Multi-region active-active recovery.
- Complex backup orchestration platform.
- Zero-data-loss RPO guarantees for early beta.
- Kubernetes backup tooling.
