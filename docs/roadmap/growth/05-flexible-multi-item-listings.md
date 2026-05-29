# Growth 05 — Flexible Multi-Item Listings

> Source: product idea from real-world barter behavior
> Priority: P2 core barter value / negotiation flexibility
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

* Let users create listings that represent more than one offered item, so barter offers can better match real-world exchange behavior.
* Support cases like:

    * “I offer these items as a bundle.”
    * “Pick one or more items from this set.”
    * “I have a small collection/album of items available for exchange.”
* Keep the existing single-item listing model working and avoid breaking current marketplace flows.

# Why It Matters

* Real barter is rarely strictly one item for one item.
* Users often want to show several things at once and let the other person choose what is useful.
* This is especially valuable for collectibles, cards, stickers, books, tools, toys, small electronics, and similar categories.
* It can increase successful matches because the receiving user has more choice inside one listing.

# Current State

* Marketplace currently works mainly around individual item listings.
* Item images already exist.
* Trade offers already support negotiation and item-based exchange.
* Users can browse, filter, save searches, and view public profiles.
* There is no dedicated multi-item listing / bundle / pick-from-set flow.

# Risks

* The model can become too complex if it tries to support every possible bundle negotiation upfront.
* Trade offer semantics may become unclear if users do not understand whether they are offering all items or only selected items.
* Search/listing cards may become visually noisy if many items are displayed at once.
* Inventory state can become harder to manage if sub-items need individual availability tracking.

# Proposed Solution

* Add a simple listing mode to distinguish normal listings from multi-item listings.
* Keep the existing item/listing concept as the parent listing.
* Support lightweight child entries inside a listing, without turning them into full independent marketplace items initially.

Suggested listing modes:

* `SINGLE` — existing behavior.
* `BUNDLE` — all included entries are offered together.
* `PICK_ANY` — the other user may choose one or more entries from the listing.

For MVP:

* A multi-item listing has one parent title, description, category, tags, location, and status.
* The listing can contain multiple child entries with:

    * title/name
    * optional short description
    * optional quantity
    * optional image reference or use parent images
    * sort order
* Trade negotiation can reference the parent listing first.
* Fine-grained selection inside the offer can be handled through messages or a simple selected-entry payload if practical.

# Simpler Alternatives

* Let users upload many images and describe the bundle only in free text.
* Add “bundle” as a category/tag only.
* Add no backend model and rely on description text.

These are faster but less structured, harder to search, and harder to evolve later.

# Architecture Impact

* Keep this inside the modular monolith.
* Do not introduce a separate inventory system.
* Do not split every child entry into a full marketplace item unless the implementation analysis proves it is simpler and safer.
* Preserve existing single-item behavior.
* Prefer additive schema changes.

# Operational Impact

* No new infrastructure.
* Existing image storage should be reused.
* Slightly more database storage for multi-item entries.
* No background jobs required.

# Security Impact

* Same ownership rules as normal items.
* Users can only create/update/delete multi-item entries for their own listings.
* Do not expose private metadata.
* Keep moderation/reporting at parent listing level for MVP.
* Avoid allowing hidden/private child entries that are visible only during negotiation.

# Developer Velocity Impact

* Moderate feature if limited to parent listing plus child entries.
* High complexity if full per-child inventory, availability, and offer selection are implemented immediately.
* MVP should intentionally avoid full inventory management.

# Backend Changes

* Add listing mode enum, for example:

    * `SINGLE`
    * `BUNDLE`
    * `PICK_ANY`

* Add database support:

    * new column on items/listings for listing mode
    * new table for multi-item listing entries, if needed

Suggested table:

`item_bundle_entries` or `item_listing_entries`

Fields:

* id

* uuid

* item_id / parent_item_id

* title

* description

* quantity

* image_id nullable

* sort_order

* created_at

* updated_at

* Update OpenAPI:

    * item create request
    * item update request
    * item detail response
    * item summary response if needed
    * optional dedicated entry schemas

* Update service logic:

    * create single listing as today by default
    * create multi-item listing with validated entries
    * update entries only for owner
    * preserve existing status rules
    * validate maximum number of entries per listing

Suggested MVP limits:

* max 20 entries per listing

* title required for each entry

* description optional

* quantity optional or minimum 1

* no exact pricing fields

* Update search/listing response:

    * show listing mode
    * show entry count
    * optionally show first few entry names/images

# Frontend Changes

* Update item create/edit form:

    * choose listing type:

        * single item
        * bundle
        * pick from a set
    * allow adding/removing/reordering entries
    * keep helper text clear:

        * Bundle: “All listed items are offered together.”
        * Pick from set: “The other user can choose what they need.”

* Update marketplace cards:

    * show a badge:

        * Bundle
        * Pick from set
    * show entry count, for example:

        * “5 items included”
        * “Choose from 8 items”
    * show thumbnail preview if available

* Update item detail page:

    * show full entry list
    * show bundle/pick-any explanation
    * keep existing offer flow available

* Update trade offer UI if practical:

    * minimally show listing mode in the selected item summary
    * optionally allow the sender to mention selected entries
    * defer complex per-entry locking/reservation

# Database Changes

* Add listing mode column to existing item/listing table.
* Add child entry table only if required for structured entries.
* Add indexes by parent item ID and sort order.
* Ensure cascade delete or controlled cleanup when parent listing is deleted.

# Deployment Changes

* Standard Flyway migration only.
* No infrastructure changes.

# Testing Strategy

Backend:

* Create normal single listing remains unchanged.
* Create bundle listing with entries.
* Create pick-any listing with entries.
* Reject multi-item listing with no entries.
* Reject too many entries.
* Only owner can update entries.
* Public item detail includes safe entry data.
* Marketplace listing includes listing mode and entry count.
* Existing item tests remain green.

Frontend:

* Create/edit form supports single and multi-item modes.
* Entry add/remove UI works.
* Listing card displays bundle/pick-any badge.
* Detail page renders entries.
* Serbian and English translations are complete.
* Existing single-item create/edit flow remains intact.

# Rollout Plan

* Ship as an MVP for structured multi-item listings.
* Keep trade negotiation simple.
* Collect feedback from real barter scenarios.
* Later decide whether selected child entries should become first-class trade offer selections.

# Future Improvements

* Let trade offers explicitly select child entries from a `PICK_ANY` listing.
* Track availability per child entry.
* Mark individual entries as reserved/exchanged.
* Support collection-specific templates, for example cards/stickers/books.
* Add bulk import for collectible lists.
* Add better gallery layout for large albums.
* Add category-specific fields for collectibles.

# Explicitly Deferred

* Full inventory management per child entry.
* Automatic matching between individual child entries and wanted items.
* Per-entry reservation/locking during negotiation.
* Maps, pricing, or valuation logic.
* AI-based bundle matching.
* Bulk spreadsheet import.
* Public marketplace auctions or bidding.
