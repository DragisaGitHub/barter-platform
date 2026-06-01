# Growth 06 — Listing Templates and Smart Creation Flow

> Source: roadmap growth wave 2  
> Priority: P2 product usability / listing quality  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Improve item creation UX by introducing structured listing templates and adaptive creation flows.
- Allow users to create listings faster and with better metadata quality.
- Support template-driven listing creation for common barter scenarios.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

The platform now supports:

- standard listings
- flexible multi-item listings
- bundle / pick-any flows
- saved searches
- recommendations

However, item creation is still generic.

Users should not need to manually figure out how to structure:

- sticker collections
- card collections
- book bundles
- electronics bundles
- duplicate-item exchanges
- “choose what you need” listings

A guided creation flow improves:

- listing quality
- marketplace consistency
- recommendation quality
- saved-search usefulness
- conversion to successful trades

# Current State

The catalog supports:

- standard item creation
- multi-item flexible listings
- listing modes
- exchange location
- item images
- category/tag classification

The item creation form is still generic and manually configured.

No category-aware or scenario-aware creation assistance exists.

# Risks

## Product risks

- Overcomplicating listing creation
- Creating too many templates too early
- Confusing users with excessive branching

## Technical risks

- Template logic tightly coupling frontend and backend
- Breaking existing item create/edit flows
- Schema bloat

## Rollout risks

- Existing listings must remain fully compatible
- Edit flow must support legacy listings

# Proposed Solution

Introduce lightweight listing templates with adaptive form behavior.

Users first select listing type:

- Standard Item
- Bundle
- Pick From Collection
- Collection Album
- Wishlist / Looking For

The creation form dynamically adapts.

Examples:

## Standard Item

Current default flow.

## Bundle

User offers multiple related items as one exchange unit.

Additional fields:

- bundle title
- item grouping description

## Pick From Collection

User offers a larger collection.

Other users may select specific entries.

Supports:

- sticker duplicates
- trading cards
- collectible subsets

## Collection Album

Specialized for collectible exchanges.

Fields:

- collection name
- total owned
- duplicates
- missing / wanted entries
- exchange rules

## Wishlist

User expresses demand without offering an immediate item.

Useful for future matching and saved-search evolution.

# Simpler Alternatives

A simpler option would be only:

- add listingType enum
- minor UI copy changes

This is acceptable only if adaptive forms become too large.

Preferred solution:

lightweight adaptive UI without heavy schema specialization.

# Architecture Impact

Preserve modular monolith.

Changes remain inside catalog module.

No:

- separate template service
- dynamic form engine
- plugin architecture
- external schema registry

Templates are explicit product-level listing modes.

# Operational Impact

Minimal.

No new infrastructure.

No runtime operational complexity.

# Security Impact

Validate template-specific payload fields.

Prevent:

- malformed template metadata
- unsupported combinations
- oversized collection payloads

Authorization remains unchanged.

Ownership enforcement remains unchanged.

# Developer Velocity Impact

Positive.

Future specialized listing experiences become easier.

Reduces ad-hoc UI branching.

Creates a clean path for domain-specific marketplace experiences.

# Backend Changes

Add listing template support to catalog domain.

Potential additions:

- ListingTemplateType enum
- optional template metadata object

Update:

- create item API
- update item API
- item response DTOs
- item validation service

Add tests for:

- valid template combinations
- invalid template payload rejection
- backward compatibility

# Frontend Changes

Update item creation/edit flow.

Add template selector step.

Implement adaptive form sections.

Add localized helper text.

Improve item preview rendering.

Update item cards/detail views to display template context.

Examples:

- “Choose any from collection”
- “Duplicate sticker bundle”
- “Wishlist request”

# Database Changes

Add nullable template fields.

Possible migration:

listing_template_type  
template_metadata_json

Backward compatible.

Existing listings default to STANDARD.

# Deployment Changes

No infrastructure changes.

Standard migration rollout.

# Testing Strategy

Backend:

- validation tests
- serialization tests
- compatibility tests

Frontend:

- template switching tests
- create/edit flow tests
- localization rendering tests

Manual:

- create each template type
- edit existing listings
- verify display in marketplace
- verify offer flow compatibility

# Rollout Plan

## Phase 1

Introduce template model + backend validation.

## Phase 2

Implement adaptive frontend creation flow.

## Phase 3

Update item display surfaces.

## Phase 4

Validate all trade interactions.

Deploy to DEV.

Manual verification.

# Future Improvements

- category-specific presets
- collectible-series helpers
- smart field suggestions
- import collection lists
- duplicate detection assistance
- CSV collection import

# Explicitly Deferred

Not part of this unit:

- AI-assisted listing generation
- OCR/image parsing
- external collectible databases
- automatic album recognition
- recommendation retraining
- advanced marketplace analytics