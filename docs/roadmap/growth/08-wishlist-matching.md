# Growth 08 — Wishlist Matching

> Source: growth wave 3
> Priority: P1 barter value / marketplace liquidity
> Execution mode: split into small controlled Agent-mode implementation units.

# Goal

- Help users find listings that may satisfy their wishlist.
- Connect wishlist listings with collection and pick-from-collection listings.
- Increase successful barter interactions by surfacing potential matches.
- Keep the solution lightweight, explainable, and based on existing marketplace data.

# Why It Matters

The platform now supports:

- listing templates
- wishlist listings
- collection album listings
- pick-from-collection listings
- multi-item entries
- saved searches
- recommendations
- trade offers

This creates the foundation for real barter matching.

Users should not need to manually browse everything to find someone who may have what they need.

Wishlist matching helps answer:

- who might have the items I am looking for?
- which collections may contain my missing entries?
- where should I send my next offer?

This is a core product value for collectible, book, toy, and community barter scenarios.

# Current State

The platform supports wishlist listings through Growth 06.

Wishlist listings can describe:

- what the user is looking for
- wanted entries
- wanted condition notes

Collection and pick-from-collection listings can describe:

- collection name
- entries
- duplicates
- missing entries
- exchange rules

However, the platform does not yet actively connect these listing types.

Users must manually search or browse marketplace results.

# Risks

## Product Risks

- Showing weak or irrelevant matches
- Making users think matches are guaranteed
- Confusing wishlist matching with AI recommendations
- Encouraging spammy offers

## Technical Risks

- Creating a complex matching engine too early
- Adding full-text search complexity before it is needed
- Making matching logic hard to explain
- Overloading dashboard queries

## Rollout Risks

- Existing wishlist listings must remain compatible
- Existing recommendations must continue working
- Marketplace performance must remain stable

# Proposed Solution

Introduce lightweight wishlist matching.

The system should find candidate listings that may match a user's wishlist using existing data:

- listing title
- listing description
- category
- tags
- listing template type
- listing entries
- wishlist wanted text
- collection metadata

Matching should be explainable.

Examples:

- same category
- shared tags
- matching words in wanted entries
- matching words in listing entries
- collection listing with relevant title
- same exchange city or area

# Matching Scope

Wishlist matching should focus on these relationships:

## Wishlist -> Pick From Collection

A user wants specific entries.

Another user offers a collection where entries may be selected.

## Wishlist -> Collection Album

A user wants missing entries.

Another user has a collection album with duplicates or exchange rules.

## Wishlist -> Standard Item

A user wants one specific item.

Another user has a matching standard listing.

# Backend Changes

Add a lightweight wishlist matching service.

Possible endpoint:

GET /api/v1/catalog/items/{itemUuid}/wishlist-matches

Where itemUuid must refer to a wishlist listing owned by the current user.

The endpoint should return a paginated or limited list of candidate item summaries.

Recommended response fields:

- matched item summary
- match reasons
- score or rank
- owner summary if existing APIs already expose it safely

Match reasons should be human-readable through frontend i18n keys, not raw backend UI text if possible.

Backend rules:

- only active listings should be returned
- do not return the current user's own listings as matches
- wishlist listing must belong to current user
- inactive or non-wishlist listings should be rejected
- matches should be deterministic
- limit result size
- avoid expensive unbounded scans

# Matching Logic

Use simple scoring.

Example signals:

- same category
- shared tag
- title overlap
- description overlap
- wanted entries overlap
- listing entry title overlap
- same exchange city
- same exchange area
- compatible template type

Do not use:

- AI
- embeddings
- vector database
- external search services
- behavioral tracking pipeline

# Frontend Changes

Add wishlist match discovery UI.

Recommended surfaces:

## Wishlist Detail Page

When viewing own wishlist listing, show:

Potential matches

Each match card should show:

- listing title
- template type
- location summary
- match reasons
- action to view listing
- action to send offer if allowed

## Dashboard

Optionally show a compact section:

Matches for your wishlists

Only if this can be done without heavy backend work.

# OpenAPI Changes

Add OpenAPI contract for wishlist matching.

Add schemas for:

- WishlistMatchResponse
- WishlistMatchReason
- WishlistMatchPagedResponse or limited list response

Regenerate backend API and frontend API/types.

Controllers must implement generated Api interfaces.

No ad-hoc controller request/response classes.

# Database Changes

Prefer none for the first version.

Use existing item, tag, category, entry, and template metadata tables.

If performance becomes an issue later, add indexes or search tables in a future task.

# Security Impact

Access rules:

- user must be authenticated
- user can only request matches for their own wishlist listing
- response must not expose private data
- only active public listings should be returned

# Operational Impact

No new infrastructure.

No external services.

No background workers.

# Developer Velocity Impact

Positive.

Creates a foundation for:

- better recommendations
- smarter saved searches
- future search improvements
- barter-specific discovery

# Testing Strategy

Backend:

- wishlist owner can request matches
- non-owner cannot request matches
- non-wishlist item is rejected
- inactive wishlist is rejected
- own listings are excluded
- active matching listings are returned
- match reasons are generated
- deterministic ordering is preserved

Frontend:

- wishlist detail displays potential matches
- empty state appears when no matches exist
- match reasons are rendered through i18n
- action to open listing works
- action to send offer uses existing offer flow

Manual:

- create wishlist listing
- create matching collection listing from another user
- verify match appears
- verify reasons are understandable
- verify own listings do not appear
- verify normal listings still work

# Rollout Plan

## Phase 1

Backend OpenAPI contract and matching service.

## Phase 2

Frontend wishlist detail integration.

## Phase 3

Dashboard compact section only if low risk.

## Phase 4

Manual beta verification.

# Future Improvements

- PostgreSQL full-text search
- saved-search match notifications
- wishlist alerts
- match quality tuning
- collection-specific matching rules
- exclude ignored owners
- hide already contacted listings
- advanced marketplace match filters

# Explicitly Deferred

Not part of this unit:

- AI matching
- vector search
- external recommendation services
- email alerts
- push notifications
- real-time matching
- background matching jobs
- full-text search migration
- behavior tracking
- paid promotion
- complex ranking algorithms
- automatic offer generation