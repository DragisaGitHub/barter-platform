# Launch Blocker 03 — Observability and Monitoring

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: P0 operational readiness  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Make outages, degraded dependencies, and security-relevant symptoms visible before users become the monitoring system.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap marks observability as a critical launch blocker.
- Public traffic requires health checks, logs, metrics, alerts, and runbooks.

# Current State

- A `/ping`-style health signal exists.
- No documented Spring Actuator/Micrometer setup, request correlation, alerting, dashboards, or incident flow.
- Compose/Caddy deployment is simple and appropriate but needs production visibility.

# Risks

- Backend, database, disk, or proxy failures may go unnoticed.
- Security incidents may lack correlation IDs and useful logs.
- Unstructured logs slow debugging during launch.

# Proposed Solution

- Add Spring Actuator health/readiness endpoints with only safe public exposure.
- Add Micrometer metrics for JVM, HTTP status/latency, database health, rate-limit events, uploads, and report queue counts where practical.
- Add request correlation IDs across proxy, backend logs, and frontend error reporting conventions.
- Define minimal external uptime checks and alerts: backend down, frontend down, high 5xx, DB unavailable, disk high, backup failed.
- Write a short incident runbook.

# Simpler Alternatives

- Start with uptime checks plus structured logs if metrics dashboarding must wait.
- Use hosted monitoring only if available; otherwise use lightweight VM-level scripts temporarily.

# Architecture Impact

- Keep the existing modular Spring Boot monolith. Do not introduce microservices, event streaming, or Kubernetes for this workstream.
- Add cross-cutting observability configuration without changing domain boundaries.
- Avoid distributed tracing complexity until multiple services/workers exist.

# Operational Impact

- Creates actionable alerts for the small team.
- Requires alert recipients, thresholds, and escalation expectations.
- Improves deployment verification and support triage.

# Security Impact

- Logs must redact tokens, reset codes, verification codes, secrets, and storage connection strings.
- Actuator details must be restricted in production.
- Correlation IDs help investigate abuse without logging sensitive payloads.

# Developer Velocity Impact

- Improves debugging speed and confidence in future Agent-mode changes.
- Adds small maintenance cost for metric naming and runbooks.

# Backend Changes

- Add Actuator/Micrometer dependencies/configuration if not already present.
- Configure health groups and safe endpoint exposure.
- Add correlation ID filter and structured logging pattern.
- Add log redaction review for auth and upload paths.

# Frontend Changes

- Surface correlation/request IDs in generic error handling where available.
- Avoid logging sensitive tokens/client state in browser console.
- Optionally add a lightweight user-facing error reference code.

# Database Changes

- None required.
- Optional later table for operational events is deferred unless dashboards need persisted history.

# Deployment Changes

- Add healthcheck endpoints for Compose/Caddy monitoring.
- Configure log retention/rotation.
- Add external uptime checks and documented alert channels.
- Ensure production Actuator endpoints are not publicly overexposed.

# Testing Strategy

- Test health/readiness behavior with DB available/unavailable.
- Test correlation ID propagation.
- Smoke test container health endpoints.
- Verify sensitive values are not emitted in representative logs.

# Rollout Plan

- Add instrumentation in DEV/staging first.
- Configure alerts with non-paging thresholds initially.
- Tune thresholds during beta.
- Make observability green status a release checklist item.

# Future Improvements

- Dashboards for moderation volume, user growth, latency percentiles, and error budgets.
- Distributed tracing only if workers/services are later introduced.
- Frontend real-user monitoring if production issues justify it.

# Explicitly Deferred

- Full OpenTelemetry tracing platform.
- Enterprise SIEM integration.
- Service mesh metrics.
- Kubernetes-native observability stack.
