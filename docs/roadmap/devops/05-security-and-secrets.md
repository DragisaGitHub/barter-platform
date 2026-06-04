# DevOps Roadmap 05 — Security and Secrets

> Cross-cutting guardrails for the roadmap phases.

# Goal

- Define how CI/CD secrets, SSH access, registry credentials, and runtime env files should be handled as the DEV deploy and rollback workflows are introduced.
- Prevent secret sprawl while keeping the delivery flow practical for the current operating model.

# Scope

## In scope for this document

- GitHub-side secret categories for future DEV and production workflows.
- Server-side runtime secret boundaries.
- Minimum SSH and environment protection expectations.
- Logging and audit expectations for manual workflow dispatch.

## Secret-boundary model

### GitHub Actions should hold only automation access secrets

- SSH private key or equivalent connection secret for the DEV server.
- SSH known-hosts or server fingerprint material.
- Docker publishing credentials already required for image publication.
- Later: separate production deployment credentials.

### The target server should hold runtime application secrets

- `deployment/env/dev.env`
- later: `deployment/env/prod.env`
- database password
- JWT secret
- Azure storage connection strings
- SMTP credentials
- any later production-only secrets

### GitHub Actions should not become the runtime secret store

- Deploy workflows should connect to the host and invoke scripts.
- Deploy workflows should not reconstruct full runtime env files in YAML.
- Deploy workflows should not print sensitive env values in logs.

## Recommended protections

- Use environment-scoped secrets for DEV and later production.
- Require manual dispatch for both deploy and rollback workflows.
- Prefer protected environments with reviewer approval for production later.
- Rotate SSH keys and registry tokens on a documented cadence.
- Keep SSH host verification enabled.
- Restrict SSH access to the minimum account required for deployment tasks.

# Required files and secrets

## Files

- `deployment/env/dev.env.example`
- `deployment/env/prod.env.example`
- `deployment/docs/DEV_DEPLOYMENT.md`
- later workflow files once implementation begins

## GitHub secret categories

- Docker registry publish credentials
- DEV SSH connection secrets
- later production SSH connection secrets or equivalent deployment credentials

## Server-side secret files

- `deployment/env/dev.env`
- later `deployment/env/prod.env`

# Explicit non-goals

- Moving runtime secrets into GitHub Actions.
- Introducing HashiCorp Vault, Azure Key Vault, or another secret manager in this roadmap item.
- Changing application secret-loading behavior.
- Designing enterprise-grade identity federation for CI/CD.

# Validation checklist

- [ ] Runtime application secrets are documented to stay on the target host, not in repo files.
- [ ] GitHub secrets are limited to automation access and registry access.
- [ ] SSH host verification is explicitly required.
- [ ] DEV and future production credentials are documented as separate secret scopes.
- [ ] Workflow logging is documented to avoid printing secret values.
- [ ] Manual-trigger workflows remain part of the control model.

# Risks

- If runtime env values are copied into workflow YAML, secret exposure risk rises sharply.
- If the same SSH key is reused across DEV and production, environment isolation weakens.
- If operators use personal credentials instead of shared managed secrets, offboarding becomes risky.
- If secret rotation is ignored, long-lived credentials may persist unnoticed.
- If deploy logs include command tracing with sensitive env output, secrets may leak into GitHub log retention.

