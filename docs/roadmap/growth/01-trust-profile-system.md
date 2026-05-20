# Growth 01 — Trust Profile System

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P2 trust/community impact  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Improve public profiles with transparent trust signals that help users evaluate exchange partners without opaque scoring.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- Trust is central to barter/community exchange.
- The roadmap recommends profile trust panels and warns against opaque trust scores too early.

# Current State

- Public profiles, completed/cancelled trade counts, review counts, and reputation summary exist.
- Reviews are constrained to completed trades.
- Verification badges and richer trust UX are not yet implemented.

# Risks

- New users can appear untrusted if signals are presented poorly.
- Opaque trust scores can create fairness/support problems.
- Overpromising verification can damage credibility if not backed by real verification.

# Proposed Solution

- Add a profile trust panel with joined date, active listings, completed trades, cancelled trades, positive/negative review counts, and recent review snippets.
- Use clear labels such as `New trader` instead of hidden scores.
- Show email verification only if production verification is real and reliable.
- Add safety copy that encourages public-place exchanges and reporting suspicious behavior.

# Simpler Alternatives

- Improve copy around existing reputation summary before adding backend fields.
- Add review snippets only after profile layout is stable.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- Reuse existing profile/review/catalog data.
- Avoid trust-score service or ML ranking.

# Operational Impact

- Reduces support uncertainty by making trust signals visible.
- May increase report/review disputes, so moderation basics must exist first.

# Security Impact

- Avoid exposing private user data.
- Do not reveal sensitive verification details.
- Safety copy should direct users to report flows, not off-platform data sharing.

# Developer Velocity Impact

- Mostly frontend and aggregation work if existing data is sufficient.
- Avoiding opaque scoring keeps implementation and support simple.

# Backend Changes

- Extend public profile response only with safe aggregate fields not already exposed.
- Add review snippet endpoint/field if needed.
- Ensure privacy and status checks for suspended/banned users.

# Frontend Changes

- Add trust panel to public profile.
- Add neutral `new trader` state.
- Revise marketing/profile copy to match implemented trust features.

# Database Changes

- Likely none if aggregates use existing tables.
- Add indexes only if profile aggregate queries are slow.

# Deployment Changes

- No infrastructure changes.

# Testing Strategy

- Backend tests for aggregate correctness and privacy boundaries.
- Frontend tests for new, trusted, negative-review, and suspended-user states.
- Manual accessibility review of trust labels.

# Rollout Plan

- Ship transparent existing signals first.
- Add review snippets after moderation/reporting is ready.
- Collect user feedback before adding badges.

# Future Improvements

- Verified email/phone badges when flows are real.
- Community-member badges.
- Dispute-aware review weighting if disputes are implemented.

# Explicitly Deferred

- Opaque trust score.
- Automated fraud scoring.
- Identity verification vendor integration.
- Public ranking optimized for volume over community trust.
