# Deployment Documentation Index — Barter Platform

This directory contains all operational documentation for the Barter Platform production and
development deployments on **zameni.rs**.

---

## Production Documents

| Document | Purpose |
|----------|---------|
| [production-runbook.md](production-runbook.md) | First-time setup, daily operations, and tag-based release flow |
| [SERVER_HARDENING.md](SERVER_HARDENING.md) | OS-level hardening guide for the production Ubuntu server |
| [GITHUB_SECRETS.md](GITHUB_SECRETS.md) | Every GitHub Actions secret required for CI/CD — what each is, where to find it |
| [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md) | Step-by-step operator checklist before and after every production deployment |
| [ROLLBACK_CHECKLIST.md](ROLLBACK_CHECKLIST.md) | When and how to roll back, including database migration considerations |
| [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) | Recovery procedures for VM failure, Docker failure, DB restore, DNS issues |

## Development Documents

| Document | Purpose |
|----------|---------|
| [DEV_DEPLOYMENT.md](DEV_DEPLOYMENT.md) | DEV server setup, OCI VM, Caddy HTTPS, image rollback |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Actuator endpoints, correlation IDs, request logging |

---

## Quick Reference

### Production URLs

| URL | Purpose |
|-----|---------|
| `https://zameni.rs` | Landing page |
| `https://www.zameni.rs` | Landing page (www redirect) |
| `https://app.zameni.rs` | React SPA |
| `https://app.zameni.rs/api/v1/actuator/health/readiness` | Backend readiness probe |
| `https://zameni.rs/health` | Caddy health (landing) |
| `https://app.zameni.rs/health` | Caddy health (frontend) |

### Key Paths on the Production Server

| Path | Contents |
|------|---------|
| `/opt/barter-platform/` | Repository root |
| `/opt/barter-platform/deployment/env/prod.env` | Runtime secrets (never committed, `chmod 600`) |
| `/opt/barter-platform/deployment/compose/docker-compose.prod.yml` | Production Compose file |
| `/opt/barter-platform/deployment/scripts/` | `deploy-prod.sh`, `rollback-prod.sh`, etc. |
| `/opt/barter-platform/deployment/logs/` | Script log files (e.g., `backup-db.log`) |

### Deployment Scripts

```bash
# Deploy a new release
bash deployment/scripts/deploy-prod.sh 1.1.0

# Roll back to a previous release
bash deployment/scripts/rollback-prod.sh 1.0.0
```

---

## Document Ownership

These documents live in `deployment/docs/` in the repository and are versioned alongside
the deployment infrastructure. Update them when deployment procedures change.

