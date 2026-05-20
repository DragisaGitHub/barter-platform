# Roadmap Implementation Spec Template

Use this for launch-blocker, hardening, and growth workstream specs.

> Copy this template into the appropriate roadmap folder and replace all prompts before implementation begins.
> Keep one document to one controlled Agent-mode implementation unit unless the rollout plan explicitly splits it.

# Goal

- State the concrete outcome and success criteria.
- Confirm this is documentation/planning or implementation work.

# Why It Matters

- Connect the work to launch risk, security, operations, trust/community value, simplicity, or developer velocity.

# Current State

- Summarize what exists today with file/module references where known.

# Risks

- List product, technical, operational, security, privacy, and rollout risks.

# Proposed Solution

- Describe the smallest pragmatic solution.
- Preserve the modular monolith unless there is measured pressure to change it.

# Simpler Alternatives

- List lower-complexity options and when they are acceptable.

# Architecture Impact

- Identify affected modules and boundaries.
- Explicitly reject premature microservices, event streaming, or Kubernetes unless justified.

# Operational Impact

- Define runbooks, alerts, ownership, maintenance, and support impact.

# Security Impact

- Define auth, authorization, data exposure, abuse, logging, and privacy considerations.

# Developer Velocity Impact

- Explain expected friction or simplification for future contributors.

# Backend Changes

- List API, service, domain, security, configuration, and test changes.

# Frontend Changes

- List route, component, API-client, UX, copy, accessibility, and error-state changes.

# Database Changes

- List migrations, indexes, constraints, retention, and rollback considerations.

# Deployment Changes

- List config, environment variables, scripts, CI/CD, smoke tests, and rollback needs.

# Testing Strategy

- Include unit, integration, contract, frontend, security, smoke, and manual test expectations.

# Rollout Plan

- Provide ordered implementation steps, feature flags/config switches if needed, and validation checkpoints.

# Future Improvements

- Capture known extensions that should not expand the current unit.

# Explicitly Deferred

- State what must not be implemented in this unit.
