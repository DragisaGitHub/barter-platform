#!/usr/bin/env bash
# harden-server.sh — Production server hardening for Ubuntu LTS.
#
# MODES
#   Plan mode (default — ZERO changes, read-only survey):
#     bash deployment/scripts/harden-server.sh
#
#   Apply mode (makes changes):
#     sudo bash deployment/scripts/harden-server.sh --apply
#
# OPTIONAL FLAGS (only meaningful with --apply)
#   --reset-firewall      Wipe ALL existing UFW rules before adding new ones.
#                         ⚠  DANGEROUS on a shared server — will destroy the
#                            5432 rule used by bitcoin-postgres/Fabric.
#                            Requires typing YES at a second prompt.
#
#   --restrict-ssh-users  Write "AllowUsers <user>" to the sshd drop-in.
#                         ⚠  DANGEROUS — locks out every SSH user except
#                            <user>.  Only use AFTER verifying the target
#                            user can log in.  Requires typing YES.
#
#   --disable-root-login  Write "PermitRootLogin no" to the sshd drop-in.
#                         ⚠  Only use AFTER confirming the deploy user has a
#                            working SSH key on this server.
#                            Requires typing YES.
#
# ENVIRONMENT OVERRIDES
#   BARTER_DEPLOY_USER   Deployment username   (default: barter)
#   BARTER_DEPLOY_PATH   Repository root       (default: /opt/barter-platform)
#   BARTER_SSH_PORT      SSH port              (default: 22)
#
# SHARED SERVER CONTEXT
#   The current production candidate also runs:
#     • bitcoin-tracker (k3s / Kubernetes)
#     • bitcoin-postgres (Docker, port 5432 exposed for Microsoft Fabric)
#   This script detects those workloads and will NOT silently break them.
#
# WHAT THIS SCRIPT DOES NOT DO (manual steps after running)
#   • Add SSH key to barter authorized_keys
#   • Populate deployment/env/prod.env
#   • Clone the repository
#   • Configure DNS
#   • Deploy the application  (use deploy-prod.sh)
#
# IDEMPOTENCY
#   Safe to run in plan mode any number of times.
#   Apply mode is also idempotent for most steps.
#
# See deployment/docs/SERVER_HARDENING.md for the full manual guide.

set -euo pipefail

# ─── Flag parsing ────────────────────────────────────────────────────────────

DRY_RUN="true"          # default: plan mode — no changes
RESET_FIREWALL="false"  # --reset-firewall: wipe and rebuild UFW rules
RESTRICT_SSH="false"    # --restrict-ssh-users: write AllowUsers directive
DISABLE_ROOT="false"    # --disable-root-login: write PermitRootLogin no

usage() {
  sed -n '2,58p' "$0" | sed 's/^# \?//'
  exit 0
}

for _ARG in "$@"; do
  case "${_ARG}" in
    --apply)               DRY_RUN="false" ;;
    --reset-firewall)      RESET_FIREWALL="true" ;;
    --restrict-ssh-users)  RESTRICT_SSH="true" ;;
    --disable-root-login)  DISABLE_ROOT="true" ;;
    --help|-h)             usage ;;
    *) echo "[ERROR] Unknown argument: ${_ARG}" >&2; usage ;;
  esac
done
unset _ARG

# ─── Configuration ───────────────────────────────────────────────────────────

DEPLOY_USER="${BARTER_DEPLOY_USER:-barter}"
DEPLOY_PATH="${BARTER_DEPLOY_PATH:-/opt/barter-platform}"
SSH_PORT="${BARTER_SSH_PORT:-22}"

# ─── Colour helpers ──────────────────────────────────────────────────────────

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

log()    { echo -e "${BLUE}[INFO]${NC}   $*"; }
ok()     { echo -e "${GREEN}[OK]${NC}     $*"; }
warn()   { echo -e "${YELLOW}[WARN]${NC}   $*"; }
fail()   { echo -e "${RED}[ERROR]${NC}  $*" >&2; exit 1; }
plan()   { echo -e "${CYAN}[PLAN]${NC}   $*"; }
skip()   { echo -e "${DIM}[SKIP]${NC}   $*"; }
action() { echo -e "${BOLD}[ACTION]${NC} $*"; }

section() {
  echo
  echo -e "${BOLD}══════════════════════════════════════════════════════════${NC}"
  echo -e "${BOLD}  $*${NC}"
  echo -e "${BOLD}══════════════════════════════════════════════════════════${NC}"
}

# ─── Confirmation helpers ────────────────────────────────────────────────────

# confirm_dangerous: require the operator to type YES (all-caps) to proceed.
# Returns 0 on YES or 1 on anything else.
confirm_dangerous() {
  local prompt="$1"
  echo
  echo -e "${RED}${BOLD}⚠  DANGER — ${prompt}${NC}"
  echo -e "${RED}   Type ${BOLD}YES${NC}${RED} (all caps) to confirm, or press Enter to skip:${NC}"
  read -rp "   > " _REPLY
  if [[ "${_REPLY}" == "YES" ]]; then
    return 0
  else
    warn "Skipped by operator."
    return 1
  fi
}

# ─── Server survey (read-only — always runs first) ───────────────────────────
#
# All survey results are stored in SURVEY_* globals so that both print_survey()
# and the apply-mode functions can use them without re-running commands.

SURVEY_OS_ID=""
SURVEY_OS_VERSION=""
SURVEY_K3S_DETECTED="false"
SURVEY_K3S_STATUS=""
SURVEY_BITCOIN_CONTAINERS=""       # newline-separated container lines
SURVEY_ALL_CONTAINERS=""           # all running containers for reference
SURVEY_PORT_5432_PUBLIC="false"    # true if 5432 bound on a non-loopback address
SURVEY_PORT_5432_DETAIL=""         # ss output line
SURVEY_UFW_ACTIVE="false"
SURVEY_UFW_RULES=""
SURVEY_SSH_PASS_AUTH="(unknown)"
SURVEY_SSH_PERMIT_ROOT="(unknown)"
SURVEY_SSH_ALLOW_USERS="(not set)"
SURVEY_DEPLOY_USER_EXISTS="false"
SURVEY_DOCKER_INSTALLED="false"
SURVEY_SHARED_SERVER="false"       # true if any co-tenant workload detected
SURVEY_SSH_SERVICE="ssh"           # ssh.service (Ubuntu 24.04) or sshd.service (Ubuntu 22.04)

_detect_os() {
  if [[ -f /etc/os-release ]]; then
    # shellcheck source=/dev/null
    source /etc/os-release
    SURVEY_OS_ID="${ID:-unknown}"
    SURVEY_OS_VERSION="${VERSION_ID:-unknown}"
  fi
}

_detect_k3s() {
  if command -v k3s &>/dev/null || \
     systemctl list-units --type=service --no-pager 2>/dev/null | grep -q 'k3s'; then
    SURVEY_K3S_DETECTED="true"
    SURVEY_K3S_STATUS="$(systemctl is-active k3s 2>/dev/null || echo 'unknown')"
    SURVEY_SHARED_SERVER="true"
  fi
}

_detect_docker_workloads() {
  if command -v docker &>/dev/null; then
    SURVEY_DOCKER_INSTALLED="true"
    SURVEY_ALL_CONTAINERS="$(
      docker ps --format '  {{.Names}}  image={{.Image}}  ports={{.Ports}}' 2>/dev/null || true
    )"
    SURVEY_BITCOIN_CONTAINERS="$(
      echo "${SURVEY_ALL_CONTAINERS}" | grep -iE 'bitcoin|postgres' || true
    )"
    if [[ -n "${SURVEY_BITCOIN_CONTAINERS}" ]]; then
      SURVEY_SHARED_SERVER="true"
    fi
  fi
}

_detect_open_ports() {
  # Check if 5432 is bound on a non-loopback address
  local _port_line
  _port_line="$(ss -tlnp 2>/dev/null | grep ':5432 ' | grep -v '127\.0\.0\.1\|::1' || true)"
  if [[ -n "${_port_line}" ]]; then
    SURVEY_PORT_5432_PUBLIC="true"
    SURVEY_PORT_5432_DETAIL="${_port_line}"
    SURVEY_SHARED_SERVER="true"
  fi
}

_detect_ufw() {
  if command -v ufw &>/dev/null; then
    local _status
    _status="$(ufw status 2>/dev/null || true)"
    if echo "${_status}" | grep -q "Status: active"; then
      SURVEY_UFW_ACTIVE="true"
      SURVEY_UFW_RULES="$(echo "${_status}" | tail -n +5 || true)"
    fi
  fi
}

_detect_sshd() {
  # sshd -T requires root; skip gracefully if not root
  [[ "$(id -u)" -ne 0 ]] && return
  SURVEY_SSH_PASS_AUTH="$(
    sshd -T 2>/dev/null | grep '^passwordauthentication ' | awk '{print $2}' || echo 'unknown'
  )"
  SURVEY_SSH_PERMIT_ROOT="$(
    sshd -T 2>/dev/null | grep '^permitrootlogin ' | awk '{print $2}' || echo 'unknown'
  )"
  local _au
  _au="$(sshd -T 2>/dev/null | grep '^allowusers ' | awk '{$1=""; print $0}' | xargs 2>/dev/null || true)"
  SURVEY_SSH_ALLOW_USERS="${_au:-"(not set — all users allowed)"}"
}

_detect_deploy_user() {
  id "${DEPLOY_USER}" &>/dev/null && SURVEY_DEPLOY_USER_EXISTS="true" || true
}

_detect_ssh_service() {
  # Ubuntu 24.04 ships the daemon as ssh.service; Ubuntu 22.04 uses sshd.service.
  if systemctl list-units --type=service --no-pager 2>/dev/null \
       | grep -qE '^\s*sshd\.service'; then
    SURVEY_SSH_SERVICE="sshd"
  else
    SURVEY_SSH_SERVICE="ssh"
  fi
}

survey_server() {
  _detect_os
  _detect_k3s
  _detect_docker_workloads
  _detect_open_ports
  _detect_ufw
  _detect_ssh_service
  _detect_sshd
  _detect_deploy_user
}

print_survey() {
  section "Server Survey (read-only)"

  log "OS: ${SURVEY_OS_ID} ${SURVEY_OS_VERSION}"

  # ── Shared workloads ───────────────────────────────────────────────────────
  echo
  if [[ "${SURVEY_SHARED_SERVER}" == "true" ]]; then
    echo -e "${YELLOW}${BOLD}⚠  SHARED SERVER — other workloads detected on this host${NC}"
    echo
    if [[ "${SURVEY_K3S_DETECTED}" == "true" ]]; then
      warn "k3s detected (status: ${SURVEY_K3S_STATUS}) — bitcoin-tracker is running"
    fi
    if [[ -n "${SURVEY_BITCOIN_CONTAINERS}" ]]; then
      warn "Docker containers matching bitcoin/postgres:"
      echo "${SURVEY_BITCOIN_CONTAINERS}" | while IFS= read -r _line; do
        [[ -n "${_line}" ]] && echo -e "    ${YELLOW}${_line}${NC}"
      done
    fi
    if [[ "${SURVEY_PORT_5432_PUBLIC}" == "true" ]]; then
      warn "Port 5432 is OPEN on a public interface (required for Fabric/bitcoin-postgres):"
      echo -e "    ${YELLOW}${SURVEY_PORT_5432_DETAIL}${NC}"
      warn "This port must NOT be removed until Microsoft Fabric access is migrated."
      warn "Barter uses MANAGED Azure PostgreSQL — it does not need local port 5432."
    fi
  else
    ok "No shared workloads detected (dedicated server)"
  fi

  # ── All running containers ─────────────────────────────────────────────────
  if [[ -n "${SURVEY_ALL_CONTAINERS}" ]]; then
    echo
    log "All running Docker containers:"
    echo "${SURVEY_ALL_CONTAINERS}" | while IFS= read -r _line; do
      [[ -n "${_line}" ]] && echo "  ${_line}"
    done
  fi

  # ── Firewall ───────────────────────────────────────────────────────────────
  echo
  log "UFW: active=${SURVEY_UFW_ACTIVE}"
  if [[ -n "${SURVEY_UFW_RULES}" ]]; then
    echo "${SURVEY_UFW_RULES}" | while IFS= read -r _line; do
      [[ -n "${_line}" ]] && echo "  ${_line}"
    done
  fi

  # ── SSH ────────────────────────────────────────────────────────────────────
  echo
  log "SSH daemon config:"
  log "  PasswordAuthentication : ${SURVEY_SSH_PASS_AUTH}"
  log "  PermitRootLogin        : ${SURVEY_SSH_PERMIT_ROOT}"
  log "  AllowUsers             : ${SURVEY_SSH_ALLOW_USERS}"
  [[ "$(id -u)" -ne 0 ]] && \
    warn "  (sshd -T requires root — some values unavailable)"

  # ── Deploy user ────────────────────────────────────────────────────────────
  echo
  [[ "${SURVEY_DEPLOY_USER_EXISTS}" == "true" ]] \
    && ok "Deploy user '${DEPLOY_USER}' exists" \
    || log "Deploy user '${DEPLOY_USER}' does not exist — will be created"
}

# ─── Plan display ─────────────────────────────────────────────────────────────

print_plan() {
  section "Plan"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo -e "${CYAN}${BOLD}MODE: PLAN  (read-only — no changes will be made)${NC}"
    echo -e "${CYAN}To apply:  sudo bash $0 --apply${NC}"
  else
    echo -e "${YELLOW}${BOLD}MODE: APPLY (changes will be made to this server)${NC}"
  fi
  echo

  plan "PACKAGES     apt-get install: curl wget git ca-certificates gnupg ufw fail2ban"
  plan "             unattended-upgrades logrotate htop jq gzip openssl"
  echo

  [[ "${SURVEY_DEPLOY_USER_EXISTS}" == "true" ]] \
    && plan "USER         '${DEPLOY_USER}' exists — verify .ssh dir only" \
    || plan "USER         Create '${DEPLOY_USER}', set up ~/.ssh/authorized_keys (empty)"
  echo

  plan "SSH          Write /etc/ssh/sshd_config.d/99-barter-hardening.conf"
  plan "               PasswordAuthentication no"
  plan "               PubkeyAuthentication yes  /  PermitEmptyPasswords no"
  plan "               ClientAliveInterval 300  /  ClientAliveCountMax 3"
  plan "               Banner none  /  LogLevel VERBOSE"
  if [[ "${RESTRICT_SSH}" == "true" ]]; then
    warn "SSH    ⚠   AllowUsers ${DEPLOY_USER}  ← WILL BE WRITTEN (--restrict-ssh-users)"
    warn "           Will require typing YES — locks out ALL other SSH users"
    warn "           Current AllowUsers: ${SURVEY_SSH_ALLOW_USERS}"
  else
    skip "SSH          AllowUsers: NOT written — use --restrict-ssh-users to enable"
    skip "             Safe default for shared server — add AllowUsers manually later"
  fi
  if [[ "${DISABLE_ROOT}" == "true" ]]; then
    warn "SSH    ⚠   PermitRootLogin no  ← WILL BE WRITTEN (--disable-root-login)"
    warn "           Will require typing YES"
    warn "           Current PermitRootLogin: ${SURVEY_SSH_PERMIT_ROOT}"
  else
    skip "SSH          PermitRootLogin no: NOT written — use --disable-root-login to enable"
    skip "             Keep root login until '${DEPLOY_USER}' key is verified working"
  fi
  echo

  if [[ "${RESET_FIREWALL}" == "true" ]]; then
    warn "UFW    ⚠   FULL RESET (ufw --force reset) — ALL existing rules will be wiped"
    [[ "${SURVEY_PORT_5432_PUBLIC}" == "true" ]] && \
      warn "           Port 5432 is OPEN — reset WILL break bitcoin-postgres/Fabric"
    warn "           Will require typing YES"
  else
    plan "UFW          Additive mode — existing rules PRESERVED (no --reset-firewall)"
    if [[ "${SURVEY_PORT_5432_PUBLIC}" == "true" ]]; then
      if [[ "${SURVEY_UFW_ACTIVE}" == "true" ]] && \
         echo "${SURVEY_UFW_RULES}" | grep -q "5432"; then
        ok   "UFW          Port 5432 rule exists — will be preserved (bitcoin-postgres/Fabric safe)"
      else
        warn "UFW          Port 5432 has no UFW rule — will add ufw allow 5432/tcp"
        warn "             (TEMP: bitcoin-postgres/Fabric — remove after Fabric migration)"
      fi
    fi
    plan "UFW          Add rules:"
    plan "               + ${SSH_PORT}/tcp (SSH)"
    plan "               + 80/tcp  (HTTP — Caddy ACME+redirect)"
    plan "               + 443/tcp (HTTPS)"
    plan "               + 443/udp (HTTP/3 QUIC)"
  fi
  echo

  plan "FAIL2BAN     Write /etc/fail2ban/jail.d/barter-sshd.conf"
  plan "AUTO-UPD     Write /etc/apt/apt.conf.d/20barter-auto-upgrades"
  plan "DOCKER       Install if missing / add '${DEPLOY_USER}' to docker group"
  plan "DAEMON.JSON  Write /etc/docker/daemon.json (10m×5 rotation, live-restore)"
  plan "LOGROTATE    Write /etc/logrotate.d/barter-platform"
  plan "FILESYSTEM   Create ${DEPLOY_PATH}/deployment/{logs,backups/postgres}"
}

# ─── Step functions ───────────────────────────────────────────────────────────

install_packages() {
  section "Packages"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  export DEBIAN_FRONTEND=noninteractive
  apt-get update -q
  apt-get upgrade -y -q
  apt-get install -y -q \
    curl wget git ca-certificates gnupg lsb-release \
    ufw fail2ban unattended-upgrades apt-listchanges \
    logrotate htop jq gzip openssl
  ok "Packages installed"
}

create_deploy_user() {
  section "Deployment user: ${DEPLOY_USER}"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  if id "${DEPLOY_USER}" &>/dev/null; then
    ok "User '${DEPLOY_USER}' already exists"
  else
    adduser --disabled-password --gecos "Barter Platform Deployment" "${DEPLOY_USER}"
    ok "User '${DEPLOY_USER}' created"
  fi

  local _SSH_DIR="/home/${DEPLOY_USER}/.ssh"
  mkdir -p "${_SSH_DIR}"
  chmod 700 "${_SSH_DIR}"
  [[ ! -f "${_SSH_DIR}/authorized_keys" ]] && touch "${_SSH_DIR}/authorized_keys"
  chmod 600 "${_SSH_DIR}/authorized_keys"
  chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${_SSH_DIR}"
  ok ".ssh directory secured for '${DEPLOY_USER}'"

  action "Add the deploy public key to ${_SSH_DIR}/authorized_keys:"
  action "  echo 'ssh-ed25519 AAA...' >> ${_SSH_DIR}/authorized_keys"
}

harden_ssh() {
  section "SSH hardening"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  local _SSHD_CONF="/etc/ssh/sshd_config.d/99-barter-hardening.conf"

  # ── AllowUsers gate ───────────────────────────────────────────────────────
  # NEVER written without explicit opt-in + YES confirmation.
  # On a shared server, writing AllowUsers immediately locks out other admins.
  local _ALLOW_USERS_LINE=""
  if [[ "${RESTRICT_SSH}" == "true" ]]; then
    echo
    warn "AllowUsers restriction requested."
    warn "Current AllowUsers : ${SURVEY_SSH_ALLOW_USERS}"
    warn "Currently logged in: $(who 2>/dev/null | awk '{print $1}' | sort -u | tr '\n' ' ' || echo '(unknown)')"
    warn ""
    warn "After sshd restarts with 'AllowUsers ${DEPLOY_USER}', every SSH user"
    warn "other than '${DEPLOY_USER}' will be LOCKED OUT — including root."
    warn ""
    warn "Only proceed if you have tested 'ssh ${DEPLOY_USER}@<host>' successfully"
    warn "in ANOTHER terminal right now."
    if confirm_dangerous "Write 'AllowUsers ${DEPLOY_USER}' to ${_SSHD_CONF}?"; then
      _ALLOW_USERS_LINE="AllowUsers ${DEPLOY_USER}"
      ok "AllowUsers will be written"
    else
      warn "AllowUsers SKIPPED — all users retain SSH access"
    fi
  else
    skip "AllowUsers not written (--restrict-ssh-users not set)"
    warn "Add admin/operator usernames to ${_SSHD_CONF} AllowUsers manually when ready."
  fi

  # ── PermitRootLogin gate ──────────────────────────────────────────────────
  # NEVER disabled without explicit opt-in + YES confirmation.
  local _PERMIT_ROOT_LINE="# PermitRootLogin intentionally NOT changed"
  local _PERMIT_ROOT_HINT="# Pass --disable-root-login (with --apply) only after verifying ${DEPLOY_USER} SSH works."
  if [[ "${DISABLE_ROOT}" == "true" ]]; then
    echo
    warn "PermitRootLogin disable requested."
    warn "Current PermitRootLogin: ${SURVEY_SSH_PERMIT_ROOT}"
    warn ""
    warn "If '${DEPLOY_USER}' key login is broken after this change, you will"
    warn "need emergency console/VNC access to recover SSH."
    warn ""
    warn "Confirm 'ssh ${DEPLOY_USER}@<host>' works in ANOTHER terminal right now."
    if confirm_dangerous "Write 'PermitRootLogin no' to ${_SSHD_CONF}?"; then
      _PERMIT_ROOT_LINE="PermitRootLogin no"
      _PERMIT_ROOT_HINT=""
      ok "PermitRootLogin no will be written"
    else
      warn "PermitRootLogin SKIPPED — root SSH access preserved"
    fi
  else
    skip "PermitRootLogin no: not written (--disable-root-login not set)"
  fi

  # ── Write the drop-in config ──────────────────────────────────────────────
  cat > "${_SSHD_CONF}" << EOF
# Barter Platform production SSH hardening
# Managed by: deployment/scripts/harden-server.sh
# Manual guide: deployment/docs/SERVER_HARDENING.md
# Written: $(date -u +%Y-%m-%dT%H:%M:%SZ)

Port ${SSH_PORT}

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

# ── Conditional directives ────────────────────────────────────────────────────
# These are intentionally NOT set by default on a shared server so other tenants
# retain SSH access.  Re-run with --restrict-ssh-users and --disable-root-login
# ONLY after verifying '${DEPLOY_USER}' key login works from an independent terminal.
${_PERMIT_ROOT_HINT}
${_PERMIT_ROOT_LINE}
EOF

  if [[ -n "${_ALLOW_USERS_LINE}" ]]; then
    echo "${_ALLOW_USERS_LINE}" >> "${_SSHD_CONF}"
    ok "AllowUsers appended to ${_SSHD_CONF}"
  fi

  ok "SSH drop-in written: ${_SSHD_CONF}"

  if sshd -t 2>/dev/null; then
    ok "SSH config syntax is valid"
  else
    warn "SSH config validation FAILED — NOT restarting ${SURVEY_SSH_SERVICE}."
    warn "Fix errors in ${_SSHD_CONF}, then: systemctl restart ${SURVEY_SSH_SERVICE}"
    return
  fi

  echo
  warn "About to restart ${SURVEY_SSH_SERVICE}.  Keep a SECOND SSH session open as a fallback."
  warn "Cloud provider emergency console is your recovery path if locked out."
  read -rp "  Restart ${SURVEY_SSH_SERVICE} now? [y/N] " _RESTART_REPLY
  if [[ "${_RESTART_REPLY,,}" == "y" ]]; then
    systemctl restart "${SURVEY_SSH_SERVICE}"
    ok "${SURVEY_SSH_SERVICE} restarted"
  else
    warn "Skipped — run 'systemctl restart ${SURVEY_SSH_SERVICE}' manually when ready"
  fi
}

configure_ufw() {
  section "UFW firewall"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  # Show current state before touching anything
  if [[ "${SURVEY_SHARED_SERVER}" == "true" ]]; then
    log "Shared server — current UFW state before changes:"
    ufw status verbose 2>/dev/null || true
    echo
  fi

  # ── Optional full reset gate ──────────────────────────────────────────────
  # Default is ADDITIVE — never silently destroy existing rules.
  if [[ "${RESET_FIREWALL}" == "true" ]]; then
    echo
    warn "Full UFW reset requested."
    [[ "${SURVEY_PORT_5432_PUBLIC}" == "true" ]] && \
      warn "Port 5432 is OPEN — resetting WILL break bitcoin-postgres / Fabric access."
    warn "This removes ALL existing rules including any not added by this script."
    if confirm_dangerous "Reset ALL UFW rules? Cannot be undone."; then
      ufw --force reset > /dev/null
      ok "UFW rules reset"
    else
      warn "UFW reset ABORTED — proceeding in additive mode"
    fi
  else
    log "Additive mode: existing UFW rules are PRESERVED"
  fi

  # ── Set defaults and add Barter rules ────────────────────────────────────
  ufw default deny incoming  2>/dev/null || true
  ufw default allow outgoing 2>/dev/null || true

  ufw allow "${SSH_PORT}/tcp" comment "SSH (Barter deploy)"
  ufw allow 80/tcp            comment "HTTP Caddy ACME+redirect"
  ufw allow 443/tcp           comment "HTTPS"
  ufw allow 443/udp           comment "HTTP/3 QUIC"

  # ── Preserve 5432 for bitcoin-postgres/Fabric on shared server ───────────
  # When UFW was previously inactive there is no pre-existing 5432 rule.
  # Enabling UFW with "deny incoming" default would silently block Fabric.
  # Detect this and add the rule explicitly so nothing breaks.
  if [[ "${SURVEY_PORT_5432_PUBLIC}" == "true" && "${RESET_FIREWALL}" != "true" ]]; then
    ufw allow 5432/tcp comment "TEMP: bitcoin-postgres/Fabric — remove after Fabric migration"
    warn "Added 5432/tcp rule (bitcoin-postgres/Fabric — temporary until Fabric migration)"
    warn "Remove once Fabric access is migrated: ufw delete allow 5432/tcp"
  fi

  ufw --force enable
  ok "Barter UFW rules added and firewall enabled"

  echo
  log "UFW state after changes:"
  ufw status verbose
}

configure_fail2ban() {
  section "Fail2Ban"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  cat > /etc/fail2ban/jail.d/barter-sshd.conf << EOF
# Barter Platform SSH brute-force protection
# Managed by: deployment/scripts/harden-server.sh
[sshd]
enabled   = true
port      = ${SSH_PORT}
filter    = sshd
logpath   = /var/log/auth.log
maxretry  = 3
bantime   = 3600
findtime  = 600
EOF

  systemctl enable --now fail2ban > /dev/null 2>&1
  systemctl restart fail2ban
  ok "Fail2Ban configured"
  fail2ban-client status sshd 2>/dev/null && ok "sshd jail active" \
    || warn "sshd jail check failed — see /var/log/fail2ban.log"
}

configure_auto_updates() {
  section "Automatic security updates"
  if [[ "${DRY_RUN}" == "true" ]]; then skip "Plan mode — skipping"; return; fi

  cat > /etc/apt/apt.conf.d/20barter-auto-upgrades << 'EOF'
// Barter Platform automatic security update policy
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Download-Upgradeable-Packages "1";
APT::Periodic::AutocleanInterval "7";
APT::Periodic::Unattended-Upgrade "1";
EOF

  systemctl enable --now unattended-upgrades > /dev/null 2>&1
  systemctl is-active --quiet unattended-upgrades \
    && ok "unattended-upgrades active" \
    || warn "not active — check: systemctl status unattended-upgrades"
}

check_time_sync() {
  section "Time synchronization"
  if [[ "${DRY_RUN}" == "true" ]]; then
    local _sync
    _sync="$(timedatectl show 2>/dev/null | grep NTPSynchronized | cut -d= -f2 || echo unknown)"
    log "NTP synchronized: ${_sync}"
    [[ "${_sync}" != "yes" ]] && plan "Would enable systemd-timesyncd"
    return
  fi
  systemctl enable --now systemd-timesyncd > /dev/null 2>&1
  timedatectl set-ntp true
  timedatectl show 2>/dev/null | grep -q "NTPSynchronized=yes" \
    && ok "NTP synchronized" \
    || warn "NTP sync pending — verify: timedatectl status"
}

install_docker() {
  section "Docker Engine"
  if [[ "${DRY_RUN}" == "true" ]]; then
    [[ "${SURVEY_DOCKER_INSTALLED}" == "true" ]] \
      && skip "Docker already installed — would skip" \
      || plan "Would install Docker Engine"
    plan "Would add '${DEPLOY_USER}' to docker group"
    return
  fi

  if command -v docker &>/dev/null; then
    ok "Docker already installed: $(docker --version)"
  else
    log "Installing Docker Engine..."
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
      | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    # shellcheck source=/dev/null
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" \
      | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update -q
    apt-get install -y -q \
      docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    ok "Docker installed: $(docker --version)"
  fi

  getent group docker | grep -qw "${DEPLOY_USER}" \
    && ok "'${DEPLOY_USER}' already in docker group" \
    || { usermod -aG docker "${DEPLOY_USER}"; ok "'${DEPLOY_USER}' added to docker group"; }

  systemctl enable --now docker > /dev/null 2>&1
  ok "Docker service enabled and running"
}

configure_docker_logging() {
  section "Docker daemon log rotation"
  local _DAEMON_JSON="/etc/docker/daemon.json"
  if [[ "${DRY_RUN}" == "true" ]]; then
    { [[ -f "${_DAEMON_JSON}" ]] && grep -q "max-size" "${_DAEMON_JSON}" 2>/dev/null; } \
      && skip "daemon.json already configured — would skip" \
      || plan "Would write ${_DAEMON_JSON} (10m×5 rotation, live-restore)"
    return
  fi
  if ! command -v docker &>/dev/null; then
    warn "Docker not installed — skipping daemon.json"; return
  fi
  if [[ -f "${_DAEMON_JSON}" ]] && grep -q "max-size" "${_DAEMON_JSON}" 2>/dev/null; then
    ok "daemon.json already configured — skipping"
  else
    cat > "${_DAEMON_JSON}" << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5"
  },
  "live-restore": true
}
EOF
    systemctl reload docker 2>/dev/null || systemctl restart docker
    ok "daemon.json written: 10m×5 log rotation, live-restore enabled"
  fi
}

configure_logrotate() {
  section "System log rotation"
  if [[ "${DRY_RUN}" == "true" ]]; then plan "Would write /etc/logrotate.d/barter-platform"; return; fi
  cat > /etc/logrotate.d/barter-platform << EOF
${DEPLOY_PATH}/deployment/logs/*.log {
    weekly
    rotate 12
    compress
    delaycompress
    missingok
    notifempty
    create 640 ${DEPLOY_USER} ${DEPLOY_USER}
}
EOF
  ok "logrotate configured for ${DEPLOY_PATH}/deployment/logs/"
}

setup_filesystem() {
  section "Deployment filesystem"
  if [[ "${DRY_RUN}" == "true" ]]; then
    plan "Would create ${DEPLOY_PATH}/deployment/{logs,backups/postgres}"
    return
  fi
  mkdir -p "${DEPLOY_PATH}"
  chown "${DEPLOY_USER}:${DEPLOY_USER}" "${DEPLOY_PATH}"
  local -a _DIRS=(
    "${DEPLOY_PATH}/deployment/logs"
    "${DEPLOY_PATH}/deployment/backups/postgres"
  )
  for _DIR in "${_DIRS[@]}"; do
    if [[ ! -d "${_DIR}" ]]; then mkdir -p "${_DIR}"; ok "Created ${_DIR}"
    else ok "Exists:  ${_DIR}"; fi
  done
  chown -R "${DEPLOY_USER}:${DEPLOY_USER}" \
    "${DEPLOY_PATH}/deployment/logs" \
    "${DEPLOY_PATH}/deployment/backups" 2>/dev/null || true
  chmod 750 "${DEPLOY_PATH}/deployment/logs"            2>/dev/null || true
  chmod 700 "${DEPLOY_PATH}/deployment/backups"         2>/dev/null || true
  chmod 700 "${DEPLOY_PATH}/deployment/backups/postgres" 2>/dev/null || true
  ok "Filesystem layout configured"
  action "Clone repository as '${DEPLOY_USER}':"
  action "  su - ${DEPLOY_USER} -c 'git clone https://github.com/your-org/barter-platform.git ${DEPLOY_PATH}'"
}

# ─── Summary ──────────────────────────────────────────────────────────────────

print_summary() {
  section "Summary"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo -e "${CYAN}${BOLD}Plan mode complete.  No changes were made.${NC}"
    echo
    echo "Apply changes:"
    echo "  sudo bash $0 --apply"
    echo
    echo "Additional flags (combine with --apply):"
    printf "  %-30s %s\n" "--reset-firewall"      "Wipe all UFW rules (⚠ destroys 5432 / Fabric rule)"
    printf "  %-30s %s\n" "--restrict-ssh-users"  "Write AllowUsers ${DEPLOY_USER} (⚠ locks out all other SSH users)"
    printf "  %-30s %s\n" "--disable-root-login"  "Write PermitRootLogin no (⚠ only after verifying barter SSH)"
    echo
    return
  fi

  local _all_ok=true
  _chk() {
    eval "${2}" &>/dev/null && ok "${1}" || { warn "FAILED: ${1}"; _all_ok=false; }
  }
  _chk "UFW active"                 "ufw status | grep -q 'Status: active'"
  _chk "Fail2Ban active"            "systemctl is-active fail2ban"
  _chk "unattended-upgrades active" "systemctl is-active unattended-upgrades"
  _chk "Docker active"              "systemctl is-active docker"
  _chk "SSH service (${SURVEY_SSH_SERVICE}) running" "systemctl is-active ${SURVEY_SSH_SERVICE}"
  _chk "NTP enabled"                "systemctl is-active systemd-timesyncd"
  _chk "Deploy user exists"         "id ${DEPLOY_USER}"
  _chk "Deploy path exists"         "test -d ${DEPLOY_PATH}"
  getent group docker | grep -qw "${DEPLOY_USER}" \
    && ok "'${DEPLOY_USER}' in docker group" \
    || { warn "'${DEPLOY_USER}' NOT in docker group"; _all_ok=false; }

  echo
  [[ "${_all_ok}" == "true" ]] \
    && echo -e "${GREEN}${BOLD}All hardening checks passed.${NC}" \
    || echo -e "${YELLOW}${BOLD}Some items need attention — review warnings above.${NC}"

  echo
  echo -e "${BOLD}══ MANUAL STEPS REMAINING ══${NC}"
  echo
  echo "  1. Add deploy SSH key:"
  echo "       echo 'ssh-ed25519 AAA...' >> /home/${DEPLOY_USER}/.ssh/authorized_keys"
  echo
  echo "  2. TEST '${DEPLOY_USER}' SSH login from a SEPARATE terminal:"
  echo "       ssh ${DEPLOY_USER}@<host>"
  echo
  echo "  3. ONLY AFTER step 2 succeeds — restrict SSH access:"
  echo "       sudo bash $0 --apply --restrict-ssh-users --disable-root-login"
  echo
  echo "  4. Clone the repository as '${DEPLOY_USER}':"
  echo "       su - ${DEPLOY_USER}"
  echo "       git clone https://github.com/your-org/barter-platform.git ${DEPLOY_PATH}"
  echo
  echo "  5. Create prod.env:"
  echo "       cp ${DEPLOY_PATH}/deployment/env/prod.env.example ${DEPLOY_PATH}/deployment/env/prod.env"
  echo "       chmod 600 ${DEPLOY_PATH}/deployment/env/prod.env"
  echo "       nano ${DEPLOY_PATH}/deployment/env/prod.env"
  echo
  echo "  6. Run first deployment:"
  echo "       cd ${DEPLOY_PATH} && bash deployment/scripts/deploy-prod.sh <TAG>"
  echo
  echo "  See: deployment/docs/SERVER_HARDENING.md"
  echo "       deployment/docs/PRODUCTION_CHECKLIST.md"
  echo
}

# ─── Main ─────────────────────────────────────────────────────────────────────

main() {
  echo
  echo -e "${BOLD}Barter Platform — Production Server Hardening${NC}"
  echo -e "${BOLD}══════════════════════════════════════════════${NC}"
  echo -e "  Deploy user : ${DEPLOY_USER}"
  echo -e "  Deploy path : ${DEPLOY_PATH}"
  echo -e "  SSH port    : ${SSH_PORT}"
  echo

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo -e "${CYAN}${BOLD}PLAN MODE — read-only, zero changes.  Pass --apply to execute.${NC}"
  else
    echo -e "${YELLOW}${BOLD}APPLY MODE — this server WILL be modified.${NC}"
  fi
  echo

  # ── Root check ─────────────────────────────────────────────────────────────
  if [[ "$(id -u)" -ne 0 ]]; then
    if [[ "${DRY_RUN}" == "true" ]]; then
      warn "Not root — some survey data will be unavailable (sshd -T, ss process names)"
      warn "For a complete survey: sudo bash $0"
    else
      fail "Apply mode requires root.  Run: sudo bash $0 --apply"
    fi
  fi

  # ── OS check ───────────────────────────────────────────────────────────────
  if [[ -f /etc/os-release ]]; then
    # shellcheck source=/dev/null
    source /etc/os-release
    [[ "${ID}" != "ubuntu" ]] && fail "Ubuntu only. Detected OS: ${ID}"
    if [[ "${VERSION_ID}" == "22.04" || "${VERSION_ID}" == "24.04" ]]; then
      ok "Ubuntu ${VERSION_ID}"
    else
      warn "Tested on Ubuntu 22.04/24.04.  Detected: ${VERSION_ID}"
    fi
  else
    warn "/etc/os-release missing — cannot verify OS"
  fi

  # ── Always-run survey ─────────────────────────────────────────────────────
  survey_server
  print_survey
  print_plan

  # ── Plan mode exits here ───────────────────────────────────────────────────
  if [[ "${DRY_RUN}" == "true" ]]; then
    print_summary
    exit 0
  fi

  # ── Apply mode: one last gate before any writes ───────────────────────────
  echo
  if [[ "${SURVEY_SHARED_SERVER}" == "true" ]]; then
    warn "SHARED SERVER detected.  Review the plan above before continuing."
    warn "bitcoin-tracker, bitcoin-postgres, and port 5432 must not be disrupted."
    echo
  fi

  read -rp "Apply all changes listed above? [y/N] " _GLOBAL_CONFIRM
  if [[ "${_GLOBAL_CONFIRM,,}" != "y" ]]; then
    echo "Aborted."
    exit 0
  fi

  install_packages
  create_deploy_user
  harden_ssh
  configure_ufw
  configure_fail2ban
  configure_auto_updates
  check_time_sync
  install_docker
  configure_docker_logging
  configure_logrotate
  setup_filesystem
  print_summary
}

main "$@"
