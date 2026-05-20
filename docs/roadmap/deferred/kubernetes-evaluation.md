# Deferred Decision — Kubernetes Evaluation

> Source: `docs/28-production-readiness-roadmap.md`  
> Priority: Deferred / Phase 2+  
> Execution mode: one controlled Agent-mode implementation unit unless explicitly split below.

# Goal

- Document why Kubernetes is intentionally not a launch requirement and define future triggers for reevaluation.
- Agent-mode scope: implement only this document, update tests/docs, and stop for review before starting another roadmap item.

# Why It Matters

- The roadmap repeatedly states Kubernetes-first delivery is premature.
- Avoiding unnecessary orchestration preserves small-team velocity and focuses effort on launch blockers.

# Current State

- Compose + Caddy + backend + frontend + Postgres deployment exists for DEV-like environments.
- Dockerfiles and health checks exist.
- Monitoring, backups, rollback, and production hardening are more urgent than orchestration migration.

# Risks

- Adopting Kubernetes now adds cluster operations before the app has basic production safety.
- Avoiding Kubernetes forever could become limiting if replicas, self-healing, or managed cloud operations become necessary.

# Proposed Solution

- Keep Compose/Caddy for launch if backed by monitoring, backup/restore, security hardening, and rollback.
- Reevaluate Kubernetes only when clear triggers appear: multiple backend replicas, Compose unreliability, managed cloud migration, advanced rollout needs, or team operational capacity.
- Prefer managed container platforms before self-managing complexity if cloud migration happens.

# Simpler Alternatives

- Use improved Compose deployment with immutable tags and health checks.
- Use a managed VM/container app platform later before full AKS if requirements fit.

# Architecture Impact

- Keep this inside the modular monolith. Favor transparent product behavior over speculative algorithms or distributed architecture.
- No current code changes should assume Kubernetes.
- Keep app twelve-factor enough to move later: env config, health endpoints, stateless backend, externalized storage.

# Operational Impact

- Avoids immediate cluster cost and maintenance.
- Requires discipline to make current deployment production-safe.

# Security Impact

- Kubernetes does not replace app security, backup, or observability work.
- Later cluster adoption would require RBAC, network policy, image scanning, secrets, and ingress security.

# Developer Velocity Impact

- Deferral keeps contributors focused on product and launch safety.
- Future migration should be planned only when benefits exceed learning/ops cost.

# Backend Changes

- None now.
- Maintain health/readiness endpoints for future portability.

# Frontend Changes

- None now.

# Database Changes

- None now; keep database externalization in mind.

# Deployment Changes

- No Kubernetes manifests now.
- Keep Docker images and env-based config portable.

# Testing Strategy

- No Kubernetes testing now.
- Validate Compose deployment, rollback, and smoke tests instead.

# Rollout Plan

- Mark as deferred ADR.
- Review after public beta or when operational triggers occur.
- If revisited, produce a separate migration assessment before implementation.

# Future Improvements

- AKS/managed container evaluation.
- Helm/Kustomize only if Kubernetes is selected.
- Horizontal scaling and rolling deployment strategy.

# Explicitly Deferred

- Kubernetes implementation.
- Helm charts.
- Service mesh.
- Cluster autoscaling work.
