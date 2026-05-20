# Deferred Decision — Microservice Extraction Triggers

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: Deferred / architecture governance  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Define measurable triggers for possible future extraction while reaffirming the modular monolith as the default architecture.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap states current domains are transactionally coupled and should remain in the monolith for launch.
- Premature extraction would slow delivery and increase consistency/deployment risk.

# Current State

- Backend is a multi-module Spring Boot modular monolith.
- Domains include users, catalog, offers, messages, notifications, reviews, profiles, and admin moderation.
- The roadmap identifies possible future candidates: image processing worker, search index service, notification delivery worker, analytics/recommendation jobs.

# Risks

- Extracting core trade/catalog/review domains would complicate transactions and tests.
- Never defining triggers can lead to ad hoc architecture debates.
- Extraction without observability and deployment maturity is high risk.

# Proposed Solution

- Keep all core product domains in the monolith.
- Use triggers before extraction: measured scaling bottleneck, distinct deployment cadence, failure isolation need, data ownership clarity, team ownership capacity, and mature monitoring/rollback.
- Prefer internal module boundary cleanup before service extraction.
- Start with workers/batch jobs, not core transactional domains, if pressure emerges.

# Simpler Alternatives

- Add application ports around painful dependencies before any service split.
- Use scheduled jobs inside the monolith before external workers.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- This document is an ADR guardrail, not an implementation task.
- Future extraction requires a separate design doc and migration plan.

# Operational Impact

- Prevents unnecessary operational load.
- Clarifies when additional deployables may be worth the cost.

# Security Impact

- Fewer services reduce network, secrets, and identity surfaces for launch.
- Future services would require service-to-service auth and secrets management.

# Developer Velocity Impact

- Maintains fast local development and simple CI/CD.
- Provides decision criteria to avoid repeated speculative debates.

# Backend Changes

- None now.
- Future internal refactors may add ports around high-pressure boundaries.

# Frontend Changes

- None.

# Database Changes

- None now.
- Future extraction would require data ownership/migration design.

# Deployment Changes

- None now.
- Future services require independent image, config, deploy, monitoring, and rollback plans.

# Testing Strategy

- No implementation tests now.
- If extraction is proposed later, require contract, integration, failure-mode, and rollback tests.

# Rollout Plan

- Adopt as architecture decision record.
- Review quarterly or when scaling/ownership pain becomes measurable.
- Require explicit trigger evidence before extraction work starts.

# Future Improvements

- Image transformation worker.
- Notification delivery worker/outbox.
- Search index service after PostgreSQL proves insufficient.
- Analytics/recommendation batch jobs.

# Explicitly Deferred

- Identity service extraction.
- Catalog CRUD service.
- Trade-offer microservice.
- Admin moderation microservice.
