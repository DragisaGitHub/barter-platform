# Priority 3 — Roadmap Cleanup

> Scope: documentation-only roadmap rewrite grounded in the current repository.
>
> Reviewed against the codebase on 2026-06-06.

## Verified current roadmap surface

### Active roadmap areas in the repository

- `docs/roadmap/post-analysis/`
- `docs/roadmap/launch-blockers/`
- `docs/roadmap/growth/`
- `docs/roadmap/devops/`
- `docs/roadmap/deferred/`
- `docs/roadmap/production-hardening/`

### Older roadmap files that now overlap implemented code

- `docs/roadmap/launch-blockers/02-reporting-system.md`
- `docs/roadmap/launch-blockers/06-admin-moderation-queue.md`
- `docs/roadmap/growth/02-saved-searches.md`
- `docs/roadmap/growth/03-location-based-exchange.md`
- `docs/roadmap/growth/04-recommendation-engine.md`
- `docs/roadmap/growth/05-flexible-multi-item-listings.md`
- `docs/roadmap/growth/06-listing-templates-and-smart-creation.md`
- `docs/roadmap/growth/08-wishlist-matching.md`

## Already implemented in the product but still represented as roadmap themes

- Reporting foundation exists.
- Admin moderation queue exists.
- Saved searches exist.
- Approximate exchange location exists.
- Lightweight recommendations exist.
- Flexible multi-item listings and listing templates exist.

This document exists because roadmap discoverability has not caught up with that implementation reality.

## Confirmed missing

1. **There is no roadmap status taxonomy in the files themselves.**
   - Older roadmap documents do not clearly say whether they are implemented, partially implemented, superseded, or still active.

2. **There is no cross-link convention from older roadmap themes to the post-analysis backlog.**
   - A reader can still land on pre-implementation roadmap docs without seeing which items are now the real residual backlog.

3. **The current roadmap structure overstates the size of the remaining product backlog.**
   - That is now a documentation organization problem, not a codebase implementation problem.

## Not needed / false positives

- Do **not** delete historical roadmap context.
- Do **not** rewrite product strategy from scratch.
- Do **not** treat this as an application-development work item.

## Intentionally deferred

- Cleanup should happen after the P0–P2 code-facing backlog settles enough that status labels will not churn every week.

## Implementation-ready backlog

1. Define a simple status vocabulary for roadmap docs, for example:
   - implemented
   - partially implemented
   - active residual work
   - superseded by post-analysis
   - intentionally deferred
2. Add cross-links from overlapping legacy roadmap files to the relevant `post-analysis` documents.
3. Update the roadmap entry points (`docs/roadmap/post-analysis/00-overview.md` and/or a root roadmap index) so a contributor can find the live execution backlog quickly.

## Exit criteria

- A contributor can tell which roadmap docs are historical context versus active backlog.
- Implemented themes no longer read like untouched future work.
- Cleanup remains documentation-only and does not bundle feature changes.
