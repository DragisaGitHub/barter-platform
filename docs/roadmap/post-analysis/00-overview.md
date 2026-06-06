# Post-Analysis Roadmap Overview

> Scope: roadmap and implementation planning only. No production-code, migration, test, or branch changes are part of this roadmap area.
> 
> Source inputs: recent launch gap analysis, recent verification findings, and existing roadmap structure under `docs/roadmap/`.

## Purpose

This roadmap area captures the **confirmed remaining work after the latest launch-gap analysis and verification pass**. It is intentionally separate from the earlier thematic roadmap folders because the remaining work is now driven less by greenfield feature discovery and more by:

- launch hardening;
- contract and state consistency;
- moderation safety improvements;
- UX polish for already-built flows; and
- targeted test coverage expansion.

## Why this is a separate roadmap area

The existing roadmap folders are organized by broad theme:

- `launch-blockers/`
- `production-hardening/`
- `growth/`
- `devops/`
- `deferred/`

The new work identified by the recent analysis spans multiple themes at once. For example, one item may include moderation workflow hardening, contract cleanup, API normalization, and frontend polish in the same execution unit. Because of that, extending only one existing folder would scatter the current execution plan across unrelated categories.

A dedicated `post-analysis/` area is the most consistent structure for the current phase.

## Folder structure

```text
`docs/roadmap/post-analysis/`
  `00-overview.md`
  `01-launch-feature-gap-analysis.md`
  `02-launch-feature-gap-verification.md`
  `03-priority-matrix.md`
  `04-recommended-execution-order.md`
  `priority-0/`
  `priority-1/`
  `priority-2/`
  `priority-3/`
```

## Planning principles

1. Prefer hardening and consistency work over net-new feature breadth.
2. Preserve the modular monolith and existing feature boundaries.
3. Use the recent verification outcome as the gate for what remains in scope.
4. Sequence work so that contract safety and moderation integrity land before polish.
5. Treat test coverage expansion as a launch-enabler for the confirmed remaining work, not as a separate quality vanity project.

## Priority groups in this roadmap area

### Priority 0 — launch integrity and safety correctness

- Moderation Queue Hardening
- Report Audit Trail
- Trade Message Read-State Fix
- Search Status Contract Consistency

### Priority 1 — launch polish and confidence for already-built user flows

- Trade Messaging Launch Polish
- Message Notifications & Unread Indicators
- Search & Filters 2.0 Completion
- Frontend Test Coverage Expansion

### Priority 2 — secondary hardening and consistency improvements

- Favorites State Consistency
- Reviews & Reputation Hardening
- Non-Item Moderation Actions
- Admin Review Governance Improvements

### Priority 3 — cleanup and cross-document consistency

- Roadmap Cleanup
- Documentation Synchronization
- Minor UX Consistency Improvements

## Expected usage

- Read `01-launch-feature-gap-analysis.md` for the consolidated residual-gap summary.
- Read `02-launch-feature-gap-verification.md` for the confirmation pass that narrowed scope to the items in this roadmap area.
- Use `03-priority-matrix.md` to evaluate urgency, dependency, and blast radius.
- Use `04-recommended-execution-order.md` as the execution sequence for actual implementation later.
- Treat each priority document as one controlled implementation unit unless the execution plan explicitly splits it.

