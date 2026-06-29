# Production Server Hardening Guide — Barter Platform

This guide documents every OS-level security and reliability configuration that must be applied
to the production server **before** the first `deploy-prod.sh` run.  All commands are written
for **Ubuntu 22.04 LTS** and **Ubuntu 24.04 LTS**.  Run them as `root` or prefix each with
`sudo`.

> See also `deployment/scripts/harden-server.sh` — an interactive script that surveys the
> server, prints a plan, and applies changes only when `--apply` is passed.

---

## ⚠ Shared Server Mode (Current Production Context)

> **Read this section first if you are hardening the current production candidate server.**

The server currently running as the production candidate also hosts **two other workloads**
that must continue to operate after Barter is deployed:

| Workload | Runtime | Status |
|----------|---------|--------|
| `bitcoin-tracker` | k3s (Kubernetes) | Must keep running |
| `bitcoin-postgres` | Docker container | Must keep running |
| Port 5432 (TCP) | bitcoin-postgres → Microsoft Fabric | Must stay open temporarily |

### What this means for hardening

| Topic | Adjustment for shared server |
|-------|------------------------------|
| **UFW firewall** | Use **Mode B** (§6) — additive rules only, keep port 5432 open |
| **`ufw --force reset`** | **Never run** without `--reset-firewall` flag + explicit YES confirmation |
| **AllowUsers in sshd** | **Do not set** until bitcoin-tracker admin access strategy is documented |
| **PermitRootLogin no** | **Do not set** until `barter` key login is verified from a second terminal |
| **`harden-server.sh`** | Always run in **plan mode** first (`bash harden-server.sh`); apply with `--apply` only |

### Port 5432 status

Port 5432 is intentionally exposed on a public interface so Microsoft Fabric can read from
`bitcoin-postgres`.  This is a **temporary** arrangement.

- **Barter does NOT need port 5432** — it uses Azure managed PostgreSQL exclusively.
- Do not close port 5432 without coordinating with the bitcoin-tracker operator.
- When Fabric access is migrated or the servers are separated, port 5432 can be closed.
  Until then, use UFW **Mode B** (§6) to keep the existing 5432 rule intact.

### k3s co-existence

k3s manages its own CNI networking and modifies iptables rules.  The Barter Docker stack
and k3s should not conflict on ports (Barter uses 80/443 via Caddy; bitcoin-tracker uses
its own k3s NodePort or LoadBalancer assignments).  Verify with `ss -tlnp` that there are
no port conflicts before starting the Barter stack.

### Transition plan

The shared server arrangement is acceptable for initial production launch.  The long-term
goal is to separate Barter onto a dedicated server.  When that happens:
- Remove the 5432 note from this section
- Apply UFW **Mode A** (§6)
- Apply `AllowUsers barter` and `PermitRootLogin no` without restriction

---

## 1. Operating System

### Supported versions

| Version | Support until | Recommended |
|---------|--------------|-------------|
| Ubuntu 22.04 LTS (Jammy) | April 2027 (standard) / 2032 (ESM) | ✓ |
| Ubuntu 24.04 LTS (Noble) | April 2029 (standard) / 2034 (ESM) | ✓ |

Do **not** use non-LTS Ubuntu releases on the production server.

### Initial system update

```bash
apt-get update
apt-get upgrade -y
apt-get dist-upgrade -y
apt-get autoremove -y
apt-get autoclean
```

### Required packages

```bash
apt-get install -y \
  curl \
  wget \
  git \
  ca-certificates \
  gnupg \
  lsb-release \
  ufw \
  fail2ban \
  unattended-upgrades \
  apt-listchanges \
  logrotate \
  htop \
  jq \
  gzip
```

---

## 2. Deployment User

Production must **never** run Docker commands as `root`.  Create a dedicated non-root user.

```bash
# Create the deployment user (no password login — SSH key only)
useradd --system --create-home --shell /bin/bash barter

# Or, if you prefer a regular user with a home directory
adduser --disabled-password --gecos "" barter
```

The user will be added to the `docker` group after Docker is installed (see §7).

> ⚠️ **Security note**: membership of the `docker` group grants effective root access to the
> host because Docker can mount the host filesystem and run privileged containers.  Limit the
> `docker` group to the minimum required users and treat it as equivalent to sudo.

---

## 3. SSH Hardening

### Create the `~/.ssh` directory for the deployment user

```bash
mkdir -p /home/barter/.ssh
chmod 700 /home/barter/.ssh
touch /home/barter/.ssh/authorized_keys
chmod 600 /home/barter/.ssh/authorized_keys
chown -R barter:barter /home/barter/.ssh
```

Add the GitHub Actions deploy public key to `authorized_keys`:

```bash
echo "ssh-ed25519 AAAA...your-deploy-public-key... barter-platform-deploy" \
  >> /home/barter/.ssh/authorized_keys
```

### SSH daemon configuration

Create a drop-in configuration file so the main `sshd_config` remains distribution-managed:

```bash
cat > /etc/ssh/sshd_config.d/99-barter-hardening.conf << 'EOF'
# Barter Platform production SSH hardening

# SSH key only — disable all password authentication
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
UsePAM yes
PubkeyAuthentication yes
PermitEmptyPasswords no

# Hide SSH banner
Banner none

# Idle session timeout: 15 minutes
ClientAliveInterval 300
ClientAliveCountMax 3

# Log key fingerprints for audit
LogLevel VERBOSE

# ── AllowUsers and PermitRootLogin ────────────────────────────────────────────
# See warnings below before uncommenting these on a shared server.
# PermitRootLogin no
# AllowUsers barter
EOF
```

> ⚠️ **Before restarting sshd**: open a second SSH session in a different terminal and keep it
> open.  If the restart fails or locks you out, you still have the fallback session.
> On cloud VMs, know where the provider's emergency console is before proceeding.

Validate the configuration before restarting:

```bash
sshd -t && echo "SSH config is valid"

# The SSH service name changed between Ubuntu releases:
#   Ubuntu 22.04 — sshd.service:  systemctl restart sshd
#   Ubuntu 24.04 — ssh.service:   systemctl restart ssh
# Detect and restart automatically:
SSH_SVC=$(systemctl list-units --type=service | grep -oE 'ssh[d]?\.service' | head -1 | sed 's/\.service//')
systemctl restart "${SSH_SVC:-ssh}"
```

### AllowUsers — shared server warning

`AllowUsers barter` restricts SSH login to **only the listed users**.  On the current shared
server, this would immediately lock out the bitcoin-tracker operator and any other admins when
sshd restarts.

**Do not add `AllowUsers`** until:

1. All current admin users and their SSH strategies are documented.
2. Every admin user is listed in `AllowUsers` (e.g., `AllowUsers barter admin-alice`).
3. You have verified that **all listed users** can SSH in from a separate terminal.

When ready, add `AllowUsers` via `harden-server.sh --apply --restrict-ssh-users` which
requires typing `YES` before writing the directive, or add it manually:

```bash
echo "AllowUsers barter admin-alice" >> /etc/ssh/sshd_config.d/99-barter-hardening.conf
sshd -t && systemctl restart ssh   # use 'sshd' on Ubuntu 22.04
```

### PermitRootLogin — staged approach

**Do not disable root login** until the `barter` user's SSH key login is confirmed working
from a separate terminal.  The staged approach:

```
Step 1: harden-server.sh --apply          # installs config WITHOUT PermitRootLogin no
Step 2: Test barter key login separately  # ssh barter@<host>
Step 3: harden-server.sh --apply --disable-root-login  # adds PermitRootLogin no (requires YES)
```

### Optional: change the SSH port

If you change the port from 22, update the UFW rule and the `PROD_SSH_PORT` GitHub secret:

```bash
# In /etc/ssh/sshd_config.d/99-barter-hardening.conf add:
Port 2222

# Update UFW
ufw allow 2222/tcp comment "SSH custom port"
ufw delete allow 22/tcp
```

---

## 4. Automatic Security Updates

```bash
# Enable unattended-upgrades
dpkg-reconfigure --priority=low unattended-upgrades
```

Edit `/etc/apt/apt.conf.d/50unattended-upgrades` to ensure security updates are applied
automatically and the system reboots if required:

```bash
cat > /etc/apt/apt.conf.d/20barter-auto-upgrades << 'EOF'
// Barter Platform automatic security update policy
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Download-Upgradeable-Packages "1";
APT::Periodic::AutocleanInterval "7";
APT::Periodic::Unattended-Upgrade "1";
EOF
```

Verify the service is running:

```bash
systemctl is-enabled unattended-upgrades
systemctl status unattended-upgrades --no-pager
```

Test dry-run:

```bash
unattended-upgrades --dry-run --debug 2>&1 | head -40
```

---

## 5. Time Synchronization

Ubuntu 22.04+ ships `systemd-timesyncd`.  Verify it is active:

```bash
timedatectl status
# Expected: "System clock synchronized: yes" and "NTP service: active"
```

If not active:

```bash
systemctl enable --now systemd-timesyncd
timedatectl set-ntp true
```

Verify synchronization:

```bash
timedatectl show-timesync --all
```

The NTP pool defaults (`ntp.ubuntu.com`) are appropriate.  You may override to a regional pool
in `/etc/systemd/timesyncd.conf` if required.

---

## 6. UFW Firewall

Choose the mode that matches your server context.

---

### Mode A — Dedicated production server (strict)

Use this when Barter is the **only** workload on the server.

```bash
# ⚠  Do NOT run ufw --force reset on a shared server — it removes all existing rules.
# Only run this block on a fresh or dedicated server.

ufw --force reset

ufw default deny incoming
ufw default allow outgoing

# SSH
ufw allow 22/tcp  comment "SSH"

# HTTP — required for Caddy ACME HTTP-01 challenges and redirects
ufw allow 80/tcp  comment "HTTP (Caddy ACME + redirect)"

# HTTPS
ufw allow 443/tcp comment "HTTPS"

# HTTP/3 QUIC (used by Caddy)
ufw allow 443/udp comment "HTTP/3 QUIC"

ufw --force enable
ufw status verbose
```

Expected output (Mode A):

```
Status: active
To                         Action      From
--                         ------      ----
22/tcp                     ALLOW IN    Anywhere
80/tcp                     ALLOW IN    Anywhere
443/tcp                    ALLOW IN    Anywhere
443/udp                    ALLOW IN    Anywhere
```

---

### Mode B — Shared transition server (current context)

Use this when bitcoin-tracker, bitcoin-postgres, and Microsoft Fabric 5432 access must
be preserved alongside Barter.  **This is the mode for the current production candidate.**

**Do not reset existing rules.**  Add Barter rules incrementally:

```bash
# DO NOT run ufw --force reset — it would remove the 5432 rule.

ufw default deny incoming
ufw default allow outgoing

# SSH (if not already present)
ufw allow 22/tcp  comment "SSH"

# HTTP — Caddy ACME challenges and HTTP→HTTPS redirect
ufw allow 80/tcp  comment "HTTP (Caddy ACME + redirect)"

# HTTPS
ufw allow 443/tcp comment "HTTPS"

# HTTP/3 QUIC
ufw allow 443/udp comment "HTTP/3 QUIC"

# Port 5432 — required for bitcoin-postgres / Microsoft Fabric access.
# ⚠ If UFW was previously INACTIVE there is no pre-existing 5432 rule.
# Enabling UFW with "deny incoming" default would silently block Fabric.
# Always add this rule explicitly on the shared server.
# Barter does NOT need port 5432 (it uses Azure managed PostgreSQL).
# Remove only after confirming Fabric access is migrated off this server.
ufw allow 5432/tcp comment "TEMP: bitcoin-postgres/Fabric — remove after Fabric migration"

ufw --force enable
ufw status verbose
```

Expected output (Mode B) — 5432 rule explicitly added for bitcoin-postgres/Fabric:

```
Status: active
To                         Action      From
--                         ------      ----
22/tcp                     ALLOW IN    Anywhere
80/tcp                     ALLOW IN    Anywhere
443/tcp                    ALLOW IN    Anywhere
443/udp                    ALLOW IN    Anywhere
5432/tcp                   ALLOW IN    Anywhere    ← bitcoin-postgres / Fabric (temporary)
```

> **Do NOT open port 8080 (backend) or 3000 (frontend).**  These are internal-only Docker
> network services proxied by Caddy.
>
> **Note on Docker and UFW**: Docker containers with published ports (`-p 5432:5432`) modify
> iptables directly and bypass UFW FORWARD rules.  If bitcoin-postgres is a published Docker
> container, the port may be accessible from outside even without an explicit UFW rule.
> Verify with `ss -tlnp | grep 5432`.

---

## 7. Docker Engine

### Installation (official Docker repository)

```bash
# Add Docker's GPG key
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Add the Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine and Compose plugin
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Verify:

```bash
docker --version       # Docker Engine 26+
docker compose version # Docker Compose v2.x
```

### Add the deployment user to the docker group

```bash
usermod -aG docker barter
```

The session change takes effect on the next login.  Verify with:

```bash
su - barter -c "docker info" 2>&1 | head -5
```

### Enable Docker on boot

```bash
systemctl enable --now docker
systemctl enable --now containerd
```

---

## 8. Docker Log Retention

Without log limits, Docker `json-file` logs will consume all available disk space.
Configure global limits before starting any containers:

```bash
cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5"
  },
  "live-restore": true
}
EOF
```

Apply the configuration:

```bash
systemctl reload docker
# Verify
docker info --format '{{.LoggingDriver}}'
```

> `live-restore: true` allows containers to continue running during a Docker daemon restart,
> which is useful when applying daemon updates without causing a service outage.

---

## 9. System Log Rotation

Docker container logs are managed by Docker itself (see §8).  Script output logs under
`deployment/logs/` need logrotate:

```bash
# BARTER_DEPLOY_PATH defaults to /opt/barter-platform but is configurable.
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

cat > /etc/logrotate.d/barter-platform << EOF
${DEPLOY_PATH}/deployment/logs/*.log {
    weekly
    rotate 12
    compress
    delaycompress
    missingok
    notifempty
    create 640 barter barter
}
EOF
```

Test the configuration:

```bash
logrotate --debug /etc/logrotate.d/barter-platform
```

---

## 10. Fail2Ban

### Enable and configure

```bash
systemctl enable --now fail2ban
```

Create a local jail override (never edit `jail.conf` directly):

```bash
cat > /etc/fail2ban/jail.d/barter-sshd.conf << 'EOF'
[sshd]
enabled   = true
port      = 22
filter    = sshd
logpath   = /var/log/auth.log
maxretry  = 3
bantime   = 3600
findtime  = 600
EOF
```

If you changed the SSH port, update `port` above accordingly.

Restart and verify:

```bash
systemctl restart fail2ban
fail2ban-client status
fail2ban-client status sshd
```

### Fail2Ban operations

```bash
# List currently banned IPs
fail2ban-client status sshd

# Unban a specific IP
fail2ban-client set sshd unbanip 1.2.3.4

# Check Fail2Ban logs
tail -f /var/log/fail2ban.log
```

---

## 11. Filesystem Layout

The production server hosts a single checkout of the repository at a configurable path.
The default is `/opt/barter-platform`.  Override with the `BARTER_DEPLOY_PATH` environment
variable before running `harden-server.sh` or any deployment script:

```bash
export BARTER_DEPLOY_PATH=/opt/barter-platform   # default — change if needed
```

> All examples below use `$BARTER_DEPLOY_PATH`.  If you did not export the variable,
> substitute `/opt/barter-platform` directly.

```
$BARTER_DEPLOY_PATH/                             ← repository root (git checkout)
├── deployment/
│   ├── backups/
│   │   └── postgres/                        ← pg_dump files (future backup implementation)
│   ├── compose/
│   │   └── docker-compose.prod.yml          ← production Compose file
│   ├── docker/
│   │   └── caddy/Caddyfile.prod             ← Caddy reverse proxy config
│   ├── docs/                                ← operational documentation
│   ├── env/
│   │   ├── prod.env.example                 ← reference template (versioned)
│   │   └── prod.env                         ← runtime secrets (NOT in git, chmod 600)
│   ├── logs/
│   │   └── backup-db.log                    ← cron/script output log
│   └── scripts/
│       ├── deploy-prod.sh
│       ├── rollback-prod.sh
│       ├── backup-db.sh
│       ├── setup-backup-cron.sh
│       ├── restore-db.sh
│       └── harden-server.sh
└── (source code — not read by the runtime)
```

**Docker-managed volumes** (stored under `/var/lib/docker/volumes/`, not host-mounted):

| Volume name (approximate) | Contents |
|--------------------------|----------|
| `compose_caddy_data` | Let's Encrypt certificates and ACME account material |
| `compose_caddy_config` | Caddy runtime configuration state |

> The exact volume names are determined by Docker Compose project name.  Check with:
> `docker volume ls | grep caddy`
>
> ⚠️ **Never delete Caddy volumes** unless you intentionally want fresh certificates.
> Aggressive certificate re-requests may trigger Let's Encrypt rate limits.

### Create the directory structure

```bash
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

# Create root directory and set ownership
mkdir -p "${DEPLOY_PATH}"
chown barter:barter "${DEPLOY_PATH}"

# As the barter user, clone the repository
su - barter -c "git clone https://github.com/your-org/barter-platform.git ${DEPLOY_PATH}"

# Create runtime directories not tracked by git
mkdir -p "${DEPLOY_PATH}/deployment/logs"
mkdir -p "${DEPLOY_PATH}/deployment/backups/postgres"
chown -R barter:barter "${DEPLOY_PATH}/deployment/logs"
chown -R barter:barter "${DEPLOY_PATH}/deployment/backups"
```

---

## 12. File Permissions

```bash
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

# Repository root — deployment user owns everything
chown -R barter:barter "${DEPLOY_PATH}"

# Secret env file — readable only by owner
install -o barter -g barter -m 600 \
  "${DEPLOY_PATH}/deployment/env/prod.env.example" \
  "${DEPLOY_PATH}/deployment/env/prod.env"
# Then fill in real values

# Scripts must be executable
chmod +x "${DEPLOY_PATH}/deployment/scripts/"*.sh

# Backup directory — private (future use)
chmod 700 "${DEPLOY_PATH}/deployment/backups"
chmod 700 "${DEPLOY_PATH}/deployment/backups/postgres"

# Logs directory
chmod 750 "${DEPLOY_PATH}/deployment/logs"
```

Verify the key files:

```bash
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

ls -la "${DEPLOY_PATH}/deployment/env/"
# Expected: -rw------- 1 barter barter ... prod.env

ls -la "${DEPLOY_PATH}/deployment/backups/"
# Expected: drwx------ 2 barter barter ... postgres
```

---

## 13. Caddy TLS Verification

Caddy automatically obtains TLS certificates from Let's Encrypt when:

1. DNS A records for `zameni.rs`, `www.zameni.rs`, and `app.zameni.rs` all point to the server IP.
2. UFW allows port 80 (ACME HTTP-01 challenge) and port 443.
3. No other process is bound to ports 80 or 443 on the host.

Check certificate status after first startup:

```bash
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

docker compose -f "${DEPLOY_PATH}/deployment/compose/docker-compose.prod.yml" \
  --env-file "${DEPLOY_PATH}/deployment/env/prod.env" \
  logs --tail=50 caddy | grep -i "certificate\|tls\|acme\|error"
```

Verify TLS from outside:

```bash
curl -sv https://zameni.rs/health 2>&1 | grep -E "SSL|TLS|certificate|expire"
echo | openssl s_client -connect zameni.rs:443 -servername zameni.rs 2>/dev/null \
  | openssl x509 -noout -dates
```

---

## 14. Security Verification Checklist

Run these checks after completing setup and after every server maintenance window:

```bash
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"

# ── SSH ────────────────────────────────────────────────────────────────────

# Confirm password authentication is disabled
sshd -T | grep -E "passwordauthentication|permitrootlogin|pubkeyauthentication"
# Expected: passwordauthentication no | permitrootlogin no | pubkeyauthentication yes

# ── SSH service name ───────────────────────────────────────────────────────
# Ubuntu 22.04 uses sshd.service; Ubuntu 24.04 uses ssh.service.
SSH_SVC=$(systemctl list-units --type=service | grep -oE 'ssh[d]?\.service' | head -1 | sed 's/\.service//')
systemctl is-active "${SSH_SVC:-ssh}"
# Expected: active

# ── Firewall ───────────────────────────────────────────────────────────────

ufw status verbose
# Dedicated server (Mode A): only 22/tcp, 80/tcp, 443/tcp, 443/udp ALLOW IN
# Shared server (Mode B):    above + 5432/tcp for bitcoin-postgres/Fabric

# ── Fail2Ban ───────────────────────────────────────────────────────────────

fail2ban-client status
fail2ban-client status sshd
# Expected: sshd jail is active

# ── Automatic updates ──────────────────────────────────────────────────────

systemctl is-enabled unattended-upgrades
# Expected: enabled

# ── Time sync ──────────────────────────────────────────────────────────────

timedatectl | grep "synchronized"
# Expected: System clock synchronized: yes

# ── Docker ─────────────────────────────────────────────────────────────────

docker info --format '{{.LoggingDriver}}'
# Expected: json-file

docker info --format '{{.LoggingDriver}} {{json .LoggingConfig}}'
# Expected: json-file map with max-size and max-file

# ── File permissions ───────────────────────────────────────────────────────

stat -c "%n  %a  %U:%G" "${DEPLOY_PATH}/deployment/env/prod.env"
# Expected: 600  barter:barter

stat -c "%n  %a  %U:%G" "${DEPLOY_PATH}/deployment/backups"
# Expected: 700  barter:barter

# ── Docker group ───────────────────────────────────────────────────────────

getent group docker
# Expected: docker:x:<gid>:barter  (only barter, no root or other users)

# ── No unexpected open ports ────────────────────────────────────────────────

ss -tlnp | grep -v "127.0.0.1\|::1"
# On a DEDICATED server: should show nothing on 5432, 8080, 3000, etc.
# On the SHARED server: 5432 is expected (bitcoin-postgres / Fabric).
# Verify: 8080 (backend) and 3000 (frontend) must NOT appear — these stay
# inside Docker's bridge network and are proxied by Caddy only.
```

---

## 15. Ongoing Maintenance

### Monthly tasks

- [ ] Review `fail2ban-client status sshd` for unusual ban patterns
- [ ] Check `df -h` — ensure disk usage is below 80%
- [ ] Review Docker volume sizes: `docker system df`
- [ ] Check `docker ps` — all containers should be `healthy`
- [ ] Verify TLS certificate expiry (Caddy auto-renews, but confirm): `echo | openssl s_client -connect zameni.rs:443 -servername zameni.rs 2>/dev/null | openssl x509 -noout -enddate`

### After any kernel update

```bash
# Check if reboot is required
cat /var/run/reboot-required 2>/dev/null && echo "REBOOT REQUIRED" || echo "No reboot needed"

# Coordinate maintenance window before rebooting
reboot
```

### After every OS security patch

Re-run the security verification checklist (§14).

---

## See Also

- [`deployment/scripts/harden-server.sh`](../scripts/harden-server.sh) — Automated hardening script
- [`production-runbook.md`](production-runbook.md) — First-time deployment and ongoing operations
- [`PRODUCTION_CHECKLIST.md`](PRODUCTION_CHECKLIST.md) — Pre-deployment operator checklist

