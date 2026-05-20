# Growth 02 — Saved Searches and Wishlists

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P2 retention/discovery  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Let users save useful catalog searches or wants so they can return to relevant barter opportunities without rebuilding filters.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap identifies saved searches/wishlists as pragmatic retention features.
- Barter inventory is dynamic; users need a way to remember what they are looking for.

# Current State

- Catalog browsing and basic filters exist.
- Favorites/watchlist exists for specific items.
- Saved search/wishlist matching is not implemented.

# Risks

- Notification spam if saved searches immediately trigger alerts.
- Search schema can become overcomplicated before search behavior is proven.
- Saved searches without good catalog filters may provide limited value.

# Proposed Solution

- Add authenticated saved searches storing name, query/filter payload, enabled flag, and timestamps.
- Support manual revisit first; defer notifications until email/preferences are ready.
- Optionally add simple wishlist text/category/tags after saved search basics.
- Keep payload tied to current catalog filter model, not a new search engine.

# Simpler Alternatives

- Persist recent searches in browser local storage first, but that does not support cross-device use.
- Add wishlist text field to profile/listing creation before full saved-search records.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- Use PostgreSQL and existing catalog filters.
- Do not add OpenSearch or recommendation services for this milestone.

# Operational Impact

- Minimal operational overhead without notifications.
- Later alerting requires unsubscribe/preferences and rate limits.

# Security Impact

- Saved searches can reveal user interests; keep them private by default.
- Validate filter payload to prevent unsupported query injection patterns.

# Developer Velocity Impact

- Small CRUD feature if kept manual.
- Deferring alerts avoids email/background-job complexity.

# Backend Changes

- Add OpenAPI endpoints for create/list/update/delete saved searches.
- Validate and normalize supported filter fields.
- Return a link or payload that frontend can reapply to catalog.

# Frontend Changes

- Add save-search action on catalog results.
- Add saved-search management page/section.
- Allow opening a saved search back into catalog filters.

# Database Changes

- Add `saved_searches` table with user ID, UUID, name, filter payload JSON or structured columns, enabled, timestamps.
- Index by user and created/updated date.

# Deployment Changes

- No new infrastructure.

# Testing Strategy

- Backend validation and ownership tests.
- Frontend tests for saving, listing, deleting, and applying searches.
- Regression tests around unsupported filters.

# Rollout Plan

- Ship manual saved searches first.
- Review usage.
- Add wishlist/matching notifications only after notification preferences and email are ready.

# Future Improvements

- Email/in-app alerts for new matches.
- Wishlist matching with what users are looking for.
- PostgreSQL full-text search integration.
- Recommendation hooks after inventory density improves.

# Explicitly Deferred

- Realtime alerts.
- Search-engine migration.
- AI matching.
- Spammy growth notifications without preferences/unsubscribe.
