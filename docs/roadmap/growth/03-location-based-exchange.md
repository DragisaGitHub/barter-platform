# Growth 03 — Location-Based Exchange

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P2 core barter/local value  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Add privacy-conscious location support so users can discover nearby exchange opportunities and coordinate safer local swaps.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- Locality is core to practical barter exchange.
- The roadmap lists location/city/distance as missing discovery dimensions but warns about privacy and geospatial complexity.

# Current State

- Catalog/search appears DB-driven with basic filters.
- No location/city/distance model is documented as implemented.
- Safety copy and local exchange UX are incomplete.

# Risks

- Precise location exposure can endanger users.
- Geospatial search can be overbuilt before community density exists.
- Location quality varies by user input and region.

# Proposed Solution

- Start with coarse user/listing location: city/region or approximate area, not exact address.
- Allow users to set an exchange area/radius preference.
- Add catalog filter/sort for nearby or same-city items using simple PostgreSQL capabilities first.
- Include safety reminders: public meeting places, no home address sharing by default, report unsafe exchanges.

# Simpler Alternatives

- Add city/region text fields and filters before distance search.
- Use manually selected area labels for beta communities.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- Use PostgreSQL indexes and optional PostGIS only if simple city filters are insufficient.
- Do not add a geospatial service.

# Operational Impact

- Requires content/privacy policy updates.
- May increase moderation needs around unsafe exchange reports.

# Security Impact

- Avoid storing or displaying exact home addresses.
- Treat location as personal data.
- Default to coarse public display and explicit user consent.

# Developer Velocity Impact

- City/region support is manageable; full geospatial radius adds complexity.
- A staged approach preserves velocity.

# Backend Changes

- Add safe location fields to users/items or item exchange preferences after data-model decision.
- Add catalog filters for city/region and optional approximate distance.
- Validate and normalize location inputs.

# Frontend Changes

- Add location fields to listing/profile flows.
- Add catalog location filters and display coarse location labels.
- Add safety reminders in listing/trade flows.

# Database Changes

- Add location columns and indexes.
- Consider PostGIS only after evaluating needs; do not require it by default.

# Deployment Changes

- No new infrastructure for city/region support.
- If PostGIS is adopted later, update DB image/managed DB requirements.

# Testing Strategy

- Backend tests for location privacy and filter behavior.
- Frontend tests for optional/required location states.
- Manual UX review for not exposing precise addresses.

# Rollout Plan

- Add coarse city/region first.
- Run beta in one/few communities.
- Add radius/distance only when inventory density makes it useful.

# Future Improvements

- Neighborhood circles.
- Safe exchange locations map.
- Approximate distance ranking.
- Community events tied to location.

# Explicitly Deferred

- Exact address sharing.
- Realtime geolocation.
- Geofencing engine.
- External maps dependency until UX need is proven.
