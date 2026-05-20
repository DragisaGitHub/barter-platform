# Launch Blocker 01 — Rate Limiting

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 launch risk reduction / security  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Add pragmatic rate limiting and brute-force protection for public write/auth surfaces before open public registration.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap identifies missing rate limits as the most direct abuse blocker.
- Auth, password reset, uploads, messages, offers, favorites, and future reports can be spammed or brute-forced without additional controls.

# Current State

- Spring Security and JWT authentication exist.
- No evidence of application-level rate limits or backoff controls.
- DEV deployment has reverse proxying but no documented production WAF or edge limiting.

# Risks

- Credential stuffing and password-reset abuse.
- Message, offer, favorite, upload, or report spam.
- Accidental denial of service if limits are too strict for NATed users.
- Operational blind spots if rate-limit rejections are not logged/observable.

# Proposed Solution

- Use an in-process limiter first, such as Bucket4j or equivalent Spring filter/interceptor, backed by memory for launch and designed for optional Redis later.
- Apply separate policies by endpoint category: login, registration, password reset, verification resend, message send, offer create, image upload, favorites, and report submit.
- Key unauthenticated requests by IP plus normalized identifier where relevant; key authenticated requests by user UUID plus IP as secondary signal.
- Return HTTP 429 with a consistent API error body and safe `Retry-After` guidance.
- Log rate-limit events without secrets, reset codes, tokens, or full credentials.

# Simpler Alternatives

- Start with Caddy/nginx coarse IP limits only, but this cannot distinguish user-level abuse after login.
- Temporarily launch invite-only, but still add auth/reset limits before larger beta.
- Use a managed WAF later only if attack volume justifies the operational/cost overhead.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- Add a small cross-cutting security component in `barter-web` or shared application support; avoid touching domain model unless audit persistence is added later.
- Keep policy values externalized through production configuration.

# Operational Impact

- Operators need a documented way to adjust limits without rebuilding images.
- 429 counts become a basic abuse/health metric.
- Support docs should explain legitimate user recovery from temporary throttling.

# Security Impact

- Directly reduces brute force, spam, and resource exhaustion risk.
- Must avoid leaking whether an email/username exists in throttling responses.
- Rate-limit logs must not become sensitive-data stores.

# Developer Velocity Impact

- Adds a reusable security guard for future endpoints.
- May require test helper updates where high-volume test loops hit limits.
- Keeps implementation simpler than distributed rate limiting until multiple backend replicas exist.

# Backend Changes

- Add dependency/configuration for the selected limiter.
- Define endpoint groups and default thresholds in typed configuration.
- Add filter/interceptor and tests for allowed, exhausted, and reset behavior.
- Ensure OpenAPI documents 429 responses where appropriate.

# Frontend Changes

- Handle HTTP 429 globally in the API client with user-friendly retry messaging.
- Show contextual throttling messages on login, password reset, registration, upload, message, offer, and report flows.

# Database Changes

- None for launch if using in-memory buckets.
- Optional later table or Redis-backed counters only if audit/compliance or multi-replica consistency requires it.

# Deployment Changes

- Add production environment variables for limit thresholds and trusted proxy/IP handling.
- Document recommended Caddy/nginx complementary coarse limits if used.
- Expose throttling metrics/log search in observability workstream.

# Testing Strategy

- Unit test policy selection and bucket exhaustion.
- MVC/integration test 429 behavior for representative endpoints.
- Regression test that unauthenticated and authenticated identities are keyed correctly.
- Smoke test production config starts with explicit thresholds.

# Rollout Plan

- Ship disabled or lenient in DEV.
- Enable for staging/production with conservative limits.
- Monitor 429 volume during beta and tune thresholds.
- After validation, make rate limits a launch gate.

# Future Improvements

- Move counters to Redis if multiple backend replicas are introduced.
- Add progressive backoff for repeated auth failures.
- Add CAPTCHA/challenge only when real abuse proves rate limits insufficient.

# Explicitly Deferred

- Full bot-management platform.
- Distributed WAF-first architecture.
- Kafka/event streaming for abuse events.
- Complex fraud scoring.
