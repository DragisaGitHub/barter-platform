# Deferred Decision — AI Matching System

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: Deferred / post-growth  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Record that AI-assisted matching is a future option, not a launch or early growth requirement.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap lists AI matching as attractive but premature before trust, moderation, inventory density, and basic recommendations are mature.
- AI can distract from safer, explainable matching primitives.

# Current State

- No AI matching system exists.
- Catalog, tags, favorites, profiles, reviews, and trades provide possible future signals.
- Saved searches, location, and simple recommendations are not yet complete.

# Risks

- Hallucinated or unsafe suggestions.
- Opaque matching can reduce user trust.
- AI infrastructure adds cost, privacy, evaluation, and moderation complexity.
- Insufficient data can make AI quality poor.

# Proposed Solution

- Defer AI until after launch blockers, trust profiles, saved searches, location, and simple rule-based recommendations are stable.
- When revisited, begin with explainable assistant features, not autonomous matching.
- Require data privacy review, safety evaluation, human-readable explanations, and opt-out controls.

# Simpler Alternatives

- Use rule-based recommendations and wishlist matching first.
- Improve search relevance and category/tag quality before AI.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- No AI service, vector database, or event streaming now.
- Future AI should be isolated behind a clear application boundary only after validated need.

# Operational Impact

- Deferral avoids model cost, quota, monitoring, and prompt-safety operations.
- Future AI would need evaluation, feedback loops, and incident handling.

# Security Impact

- Deferral avoids sending user/item data to model providers before privacy posture is ready.
- Future AI must avoid exposing private messages, precise locations, or sensitive user data.

# Developer Velocity Impact

- Keeps roadmap focused on launch and trust essentials.
- Avoids premature AI infrastructure and evaluation burden.

# Backend Changes

- None now.

# Frontend Changes

- None now.
- Avoid marketing copy that implies smart algorithms or AI matching before implementation.

# Database Changes

- None now.
- Future AI may need consent, feedback, and recommendation audit tables.

# Deployment Changes

- None now.
- Future AI would require provider credentials, quota planning, monitoring, and fallback behavior.

# Testing Strategy

- No implementation tests now.
- Future AI requires offline evaluation, safety tests, privacy review, and user-acceptance tests.

# Rollout Plan

- Keep deferred until simple recommendations and data density exist.
- Create a separate AI feasibility document when triggers are met.
- Pilot with opt-in users only if safety and value are demonstrated.

# Future Improvements

- Trade fairness estimator.
- Negotiation assistant with guardrails.
- Natural-language wishlist matching.
- Explainable bundle suggestions.

# Explicitly Deferred

- AI matching implementation.
- Vector database.
- LLM-powered negotiation assistant.
- Automated autonomous trade suggestions.
