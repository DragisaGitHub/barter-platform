# Post-Analysis 03 — Priority Matrix

> Scope: planning document only.
> 
> Decision basis: launch risk, user trust impact, dependency weight, regression risk, and execution leverage.

## Priority definitions

- **Priority 0** — correctness, moderation safety, or contract integrity issues that can undermine launch confidence.
- **Priority 1** — user-facing completion work that should follow once Priority 0 establishes a stable foundation.
- **Priority 2** — important secondary hardening and state/governance consistency.
- **Priority 3** — cleanup, synchronization, and low-risk UX consistency work.

## Matrix

| Item | Priority | Why it belongs here | Depends on | Notes |
|---|---:|---|---|---|
| Moderation Queue Hardening | P0 | Moderator response quality directly affects safety operations | Existing moderation/reporting baseline | Best first moderation hardening unit |
| Report Audit Trail | P0 | Missing traceability weakens trust, reversibility, and accountability | Moderation/report flow baseline | Pairs with queue hardening |
| Trade Message Read-State Fix | P0 | State drift affects core trade communication correctness | Messaging baseline | Must stabilize before unread polish |
| Search Status Contract Consistency | P0 | Contract mismatches create API/frontend break risk | Search/filter baseline | Enables safer Search 2.0 completion |
| Trade Messaging Launch Polish | P1 | High-value user flow needs final polish after read-state correctness | Trade Message Read-State Fix | UX hardening, not a net-new system |
| Message Notifications & Unread Indicators | P1 | Depends on trustworthy read-state and notification alignment | Trade Message Read-State Fix | Avoid before read semantics stabilize |
| Search & Filters 2.0 Completion | P1 | Valuable completion pass after status contract alignment | Search Status Contract Consistency | Keep scope incremental |
| Frontend Test Coverage Expansion | P1 | Protects P0/P1 launch-critical flows | P0 item contracts and UX boundaries | Should track execution waves |
| Favorites State Consistency | P2 | Important, but lower launch risk than messaging/search/moderation | Existing favorites baseline | Good follow-on consistency pass |
| Reviews & Reputation Hardening | P2 | Important for trust, but not as urgent as P0 safety/correctness | Existing reviews baseline | Coordinate with admin governance |
| Non-Item Moderation Actions | P2 | Extends moderation beyond the strongest item path | Moderation Queue Hardening, Report Audit Trail | Better after core moderation hardening |
| Admin Review Governance Improvements | P2 | Improves review decision quality and accountability | Report Audit Trail | Works best once audit direction is clear |
| Roadmap Cleanup | P3 | Needed for planning clarity, but not launch-critical execution | None | Safer after P0/P1 scope settles |
| Documentation Synchronization | P3 | Should reflect implemented reality after higher-priority work lands | Roadmap Cleanup optional | Avoid churn during active change waves |
| Minor UX Consistency Improvements | P3 | Good polish, low launch risk | P1/P2 user-facing updates | Bundle into a final cleanup wave |

## Priority rationale summary

### Why the P0 set is first

The Priority 0 items protect the two things most likely to cause avoidable launch pain:

- **moderation/safety blind spots**, and
- **state/contract inconsistency in high-traffic user flows**.

### Why test coverage is P1 instead of P0

Test coverage expansion is strategically important, but it should be shaped by the final P0 contract and behavior decisions so that the added coverage locks down the correct behaviors instead of unstable ones.

### Why documentation and roadmap work are P3

These items matter, but they should consolidate a more stable implementation reality rather than compete with the higher-value execution units.

