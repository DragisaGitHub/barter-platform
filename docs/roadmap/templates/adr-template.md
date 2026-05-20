# Architecture Decision Record Template

Use this for deferred decisions and architecture guardrails derived from the roadmap.

> Copy this template into the appropriate roadmap folder and replace all prompts before implementation begins.
> Keep one document to one controlled Agent-mode implementation unit unless the rollout plan explicitly splits it.

# Goal

- State the decision to make or preserve.

# Why It Matters

- Explain the risk reduced by making this decision explicit.

# Current State

- Describe the present architecture, modules, infrastructure, or product state.

# Risks

- List risks of both acting now and deferring.

# Proposed Solution

- State the decision and the conditions under which it may change.

# Simpler Alternatives

- Identify simpler options that should be tried first.

# Architecture Impact

- Describe module/deployment/data-boundary consequences.
- Default to modular monolith and operational simplicity.

# Operational Impact

- Capture operations, monitoring, ownership, cost, and on-call implications.

# Security Impact

- Capture new or avoided security surfaces.

# Developer Velocity Impact

- Explain how the decision affects local dev, CI, review, and future delivery speed.

# Backend Changes

- Usually none for an ADR; list future changes only if the decision is later activated.

# Frontend Changes

- Usually none for an ADR; list future UX changes only if relevant.

# Database Changes

- Usually none for an ADR; list future data ownership/migration implications if relevant.

# Deployment Changes

- Usually none for an ADR; list future infrastructure implications if relevant.

# Testing Strategy

- Define what evidence would be needed before reversing or activating the decision.

# Rollout Plan

- Document how the decision is adopted, reviewed, and superseded.

# Future Improvements

- List possible later investments after triggers are met.

# Explicitly Deferred

- List architecture or implementation work intentionally excluded now.
