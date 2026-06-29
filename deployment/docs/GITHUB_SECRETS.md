# GitHub Actions Secrets — Production CI/CD

This document describes every GitHub secret (and variable) required before the production
deployment workflow (`PROD Deploy`) and the Docker Publish workflow can run.

Secrets are stored in **Settings → Secrets and variables → Actions** in the GitHub repository.
The `production` environment (Settings → Environments) should be created with required-reviewers
protection before any production secret is added.

---

## Production Environment Secrets

These secrets are scoped to the `production` GitHub environment and are only available to
workflows that target that environment.

### `PROD_SSH_HOST`

| Field | Value |
|-------|-------|
| **Purpose** | IP address or hostname of the production server |
| **Used by** | `PROD Deploy` workflow — SSH connection target |
| **Example** | `203.0.113.42` or `prod.zameni.rs` |
| **How to get** | Your VPS/cloud provider dashboard (static IP recommended) |
| **Security note** | Use a static IP.  If the IP can change, update this secret and `PROD_SSH_KNOWN_HOSTS` together. |

---

### `PROD_SSH_PORT`

| Field | Value |
|-------|-------|
| **Purpose** | TCP port for SSH access to the production server |
| **Used by** | `PROD Deploy` workflow |
| **Default** | `22` (if not set, the workflow should default to 22) |
| **Example** | `22` or `2222` |
| **When to change** | Only if you moved SSH to a non-standard port in `SERVER_HARDENING.md §3` |

---

### `PROD_SSH_USER`

| Field | Value |
|-------|-------|
| **Purpose** | The non-root Linux user that owns the deployment directory on the server |
| **Used by** | `PROD Deploy` workflow — SSH login username |
| **Expected value** | `barter` (the deployment user created during server hardening) |
| **Security note** | This user must be in the `docker` group.  It must NOT be `root`. |

---

### `PROD_SSH_PRIVATE_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | The private half of an Ed25519 SSH key pair dedicated to production deployments |
| **Used by** | `PROD Deploy` workflow — authenticates the SSH connection |
| **Format** | Full PEM content of the private key, including `-----BEGIN OPENSSH PRIVATE KEY-----` header |
| **How to generate** | `ssh-keygen -t ed25519 -C "barter-platform-prod-deploy" -f ~/.ssh/barter_prod_deploy` |
| **Where public key goes** | Appended to `/home/barter/.ssh/authorized_keys` on the production server |
| **Security note** | Generate this key on your local machine.  Never generate or store it on the server.  Use a key dedicated to this deployment — do not reuse personal SSH keys. |

```bash
# Generate a dedicated deploy key pair (run locally, NOT on the server)
ssh-keygen -t ed25519 -C "barter-platform-prod-deploy" -f ~/.ssh/barter_prod_deploy -N ""

# Copy public key to the server
ssh-copy-id -i ~/.ssh/barter_prod_deploy.pub barter@<PROD_SSH_HOST>

# Add the private key content to GitHub secret PROD_SSH_PRIVATE_KEY
cat ~/.ssh/barter_prod_deploy
```

---

### `PROD_SSH_KNOWN_HOSTS`

| Field | Value |
|-------|-------|
| **Purpose** | SSH host key fingerprint(s) for strict host verification — prevents MITM attacks |
| **Used by** | `PROD Deploy` workflow — `StrictHostKeyChecking` configuration |
| **Strongly recommended** | Without this, the workflow may disable host key checking entirely, which is insecure |
| **How to generate** | `ssh-keyscan -H -p 22 <PROD_SSH_HOST>` |
| **Format** | Multi-line output from `ssh-keyscan` |

```bash
# Run from your local machine (not the server) after DNS/IP is confirmed
ssh-keyscan -H -p 22 <PROD_SSH_HOST>
# Copy the entire output into the PROD_SSH_KNOWN_HOSTS secret
```

> If `PROD_SSH_HOST` IP changes (e.g., server migration), regenerate this value immediately.
> A stale `known_hosts` entry will cause the deployment workflow to fail with a host key
> mismatch error, which is the intended security behavior.

---

### `PROD_DEPLOY_PATH`

| Field | Value |
|-------|-------|
| **Purpose** | Absolute path to the repository root on the production server |
| **Used by** | `PROD Deploy` workflow — working directory for `deploy-prod.sh` |
| **Expected value** | `/opt/barter-platform` |
| **How to verify** | `ssh barter@<PROD_SSH_HOST> "realpath /opt/barter-platform"` |
| **Security note** | The path must be owned by `PROD_SSH_USER`.  Avoid world-writable paths. |

---

## Repository-Scoped Secrets

These secrets are available to all workflows, not just the production environment.

### `DOCKERHUB_USERNAME`

| Field | Value |
|-------|-------|
| **Purpose** | Docker Hub account username for image push/pull authentication |
| **Used by** | Docker Publish workflow (`docker-publish.yml`) — logs in to Docker Hub before pushing |
| **Expected value** | `dragisahub1984` |
| **Security note** | Use the same account that owns `dragisahub1984/barter-backend`, `dragisahub1984/barter-frontend`, `dragisahub1984/barter-landing` |

---

### `DOCKERHUB_TOKEN`

| Field | Value |
|-------|-------|
| **Purpose** | Docker Hub access token (not the account password) for image publishing |
| **Used by** | Docker Publish workflow — `docker login` step |
| **How to generate** | Docker Hub → Account Settings → Security → New Access Token (scope: `Read, Write, Delete`) |
| **Security note** | Generate a token with the minimum required scope.  Rotate this token if it is ever exposed.  Never use your Docker Hub account password. |

---

### `VITE_SENTRY_DSN_PROD`

| Field | Value |
|-------|-------|
| **Purpose** | Sentry DSN for the production frontend — embedded at Docker image build time |
| **Used by** | Docker Publish workflow — passed as `--build-arg VITE_SENTRY_DSN=...` when building the frontend image |
| **Required** | No — leave empty to disable Sentry on the frontend |
| **How to get** | Sentry → Project → Settings → Client Keys (DSN) |
| **Security note** | The DSN is embedded in the public frontend bundle and is visible to any user who inspects the JavaScript.  This is expected and normal for Sentry frontend DSNs.  It is not a sensitive secret, but is stored as one to keep the workflow configuration consistent. |

---

## GitHub Environment Configuration

### Create the `production` environment

1. Go to **Settings → Environments → New environment**
2. Name it `production`
3. Add **Required reviewers** (at least one person must approve before the workflow runs)
4. Enable **Prevent self-review** if the team has more than one person
5. Set **Deployment branches and tags** to restrict to `v*` tags only:
   - Branch filter: tag name pattern `v*`

### Environment variable (not secret): none currently required

All production configuration is injected at runtime via `deployment/env/prod.env` on the server,
not via GitHub environment variables.

---

## Secret Rotation Policy

| Secret | Rotation trigger |
|--------|-----------------|
| `PROD_SSH_PRIVATE_KEY` | Annually, or immediately if the key file is ever exposed |
| `DOCKERHUB_TOKEN` | Annually, or immediately if compromised |
| `PROD_SSH_KNOWN_HOSTS` | When server IP or SSH host key changes |
| `VITE_SENTRY_DSN_PROD` | If Sentry project is reset or token is invalidated |

> Secrets in `deployment/env/prod.env` on the server (DB password, JWT secret, SMTP credentials,
> Azure connection strings) follow the rotation policy in the production runbook and are
> **not** stored in GitHub — they live only on the server.

---

## Verification

After adding all secrets, verify the workflows can authenticate:

```bash
# Test SSH connectivity from your local machine (simulate what GitHub Actions does)
ssh -i ~/.ssh/barter_prod_deploy \
    -o StrictHostKeyChecking=yes \
    -o UserKnownHostsFile=~/.ssh/known_hosts \
    -p 22 \
    barter@<PROD_SSH_HOST> \
    "cd /opt/barter-platform && git log --oneline -1 && docker compose version"
```

A successful response confirms:
- SSH key authentication works
- The deployment user can reach the repository
- Docker Compose is available for the deployment user

---

## See Also

- [`production-runbook.md`](production-runbook.md) — Full deployment flow
- [`SERVER_HARDENING.md`](SERVER_HARDENING.md) — How to set up `barter` user and SSH keys on the server
- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — Pre-deployment verification steps

