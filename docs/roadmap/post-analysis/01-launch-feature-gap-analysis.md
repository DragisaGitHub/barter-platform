# Post-Analysis 01 — Launch Feature Gap Analysis

> Scope: planning document only.
> 
> Purpose in this roadmap area: establish the residual implementation gaps that remain after earlier roadmap items and feature work already landed.

## Summary

The latest analysis indicates that the codebase is **no longer primarily missing core launch features**. Instead, most remaining work falls into four buckets:

1. **Hardening** — moderation, auditability, and edge-case handling.
2. **Consistency** — state, contract, and status alignment across backend and frontend.
3. **Safety improvements** — governance, triage quality, and admin accountability.
4. **Coverage expansion** — targeted frontend tests around launch-critical flows.

## Confirmed residual work

### Priority 0

- Moderation Queue Hardening
- Report Audit Trail
- Trade Message Read-State Fix
- Search Status Contract Consistency

### Priority 1

- Trade Messaging Launch Polish
- Message Notifications & Unread Indicators
- Search & Filters 2.0 Completion
- Frontend Test Coverage Expansion

### Priority 2

- Favorites State Consistency
- Reviews & Reputation Hardening
- Non-Item Moderation Actions
- Admin Review Governance Improvements

### Priority 3

- Roadmap Cleanup
- Documentation Synchronization
- Minor UX Consistency Improvements

## Cross-cutting findings

### What appears already implemented enough to avoid replanning as net-new work

- Core trade messaging flow exists.
- Search and filtering foundations exist.
- Favorites, reviews, reputation, and admin review surfaces exist.
- Moderation foundations exist, but remain uneven outside the strongest item/listing paths.

### What still creates launch or post-launch risk

- Queue and audit gaps reduce moderator confidence and traceability.
- Read-state and unread-indicator drift can create user confusion and support noise.
- Search status mismatches create contract fragility between backend and frontend.
- Existing features need finishing passes more than they need expansion into bigger systems.

## Planning consequence

The roadmap should now optimize for **execution clarity**, not feature ideation. That means:

- tightly scoped implementation units;
- priority-based sequencing;
- explicit out-of-scope boundaries;
- acceptance criteria centered on consistency and safety; and
- minimal overlap with already completed roadmap work.

