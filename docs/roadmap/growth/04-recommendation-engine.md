# Growth 04 — Recommendation Engine

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P3 growth after trust/data density  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Define a simple, explainable recommendation path that improves discovery only after launch safety, inventory density, and basic search are stable.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap lists recommendations as valuable long-term but warns advanced AI is a distraction before trust and safety basics.
- Recommendations can help barter matching once enough listings/trades exist.

# Current State

- Basic catalog browsing/filtering exists.
- Favorites, reviews, profiles, and trade history exist as potential future signals.
- No recommendation/matching engine is implemented.

# Risks

- AI/ML work can consume time before sufficient data exists.
- Opaque suggestions can harm trust.
- Recommendation jobs can burden the OLTP database if designed poorly.

# Proposed Solution

- Start with explainable rules: same category, same tags, recently active, nearby once location exists, users with positive review history, and wishlist match once saved searches exist.
- Implement as simple query-backed endpoints inside the monolith.
- Label recommendations transparently: `Similar category`, `Matches your saved search`, `Nearby`.
- Measure click/offer conversion before adding more complexity.

# Simpler Alternatives

- Improve category/tag browsing and popular categories first.
- Add `related items` on item detail using category/tags only.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- No ML service, vector database, or event stream initially.
- Potential future batch job can remain in the monolith or a simple scheduled task.

# Operational Impact

- Low if query-backed and limited.
- Must monitor query cost and avoid slowing core catalog pages.

# Security Impact

- Avoid exposing private saved searches or sensitive behavior.
- Do not recommend users/listings that are removed, banned, archived, or under moderation.

# Developer Velocity Impact

- Rule-based recommendations are easy to test and explain.
- Deferring ML protects launch roadmap focus.

# Backend Changes

- Add recommendation endpoint only after prerequisite data exists.
- Filter out inactive/moderated content.
- Limit query cost and paginate results.

# Frontend Changes

- Add related/recommended item sections with explanation labels.
- Avoid making recommendations central to UX until quality is proven.

# Database Changes

- Likely no initial schema changes.
- Add indexes for category/tag/location/recent activity if measured query plans require them.

# Deployment Changes

- No new infrastructure.
- Add monitoring for recommendation endpoint latency once introduced.

# Testing Strategy

- Unit/integration tests for rule ordering and visibility filters.
- Performance checks for representative catalog sizes.
- UX review to ensure explanations are clear.

# Rollout Plan

- Launch related-items by category/tags after safety milestones.
- Add saved-search/wishlist match after those features exist.
- Evaluate engagement before considering AI.

# Future Improvements

- Batch-computed recommendation candidates.
- Fairness/diversity controls.
- Trade fairness estimator.
- AI-assisted matching after sufficient data and safety review.

# Explicitly Deferred

- AI matching system.
- Vector database.
- Dedicated recommendation microservice.
- Event streaming pipeline for click behavior.
