#!/usr/bin/env bash
# deploy-prod.sh — Deploy a specific immutable release tag to production.
#
# Usage:  deploy-prod.sh <image-tag> <caddy-manifest-digest> [--preflight-only]
# Example: deploy-prod.sh 1.0.0 sha256:<64-hex-digits>
#
# The application tag must be semver; Caddy is pinned by immutable manifest digest.
# Preflight validates production secrets and the fully resolved Compose configuration
# before the backup, env-file update, or any service recreation.
#
# ⚠️  PRE-DEPLOYMENT CHECKLIST:
#   1. Ensure a recent managed-PostgreSQL backup exists (Azure automated backup
#      or a manual pg_dump) BEFORE deploying — especially for schema changes.
#   2. Confirm the target images are published and pass CI.
#   3. Note the CURRENT tag printed below — it is your rollback target if this
#      deployment fails.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOYMENT_DIR}/compose/docker-compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/prod.env}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-300}"
HEALTH_POLL_INTERVAL=5

BACKEND_REPO="dragisahub1984/barter-backend"
FRONTEND_REPO="dragisahub1984/barter-frontend"
LANDING_REPO="dragisahub1984/barter-landing"
CADDY_REPO="dragisahub1984/barter-caddy"

# ─── Helpers ────────────────────────────────────────────────────────────────

fail() {
  echo "[ERROR] $*" >&2
  exit 1
}

log() {
  echo
  echo "=== $* ==="
}

read_env_value_from_file() {
  local key="$1"
  local file="$2"
  local value

  value="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "${file}" | tail -n 1 | cut -d '=' -f 2- || true)"
  value="${value%$'\r'}"

  if [[ "${value}" =~ ^\".*\"$ ]] || [[ "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:-1}"
  fi

  printf '%s' "${value}"
}

validate_verification_secret() {
  local secret
  local LC_ALL=C
  secret="$(read_env_value_from_file BARTER_VERIFICATION_CODE_SECRET "${ENV_FILE}")"

  if ! [[ "${secret}" =~ ^[[:graph:]]{32,}$ ]]; then
    unset secret
    fail "BARTER_VERIFICATION_CODE_SECRET must be set to at least 32 printable ASCII characters; value withheld."
  fi

  unset secret
}

validate_caddy_digest() {
  local digest="$1"
  if ! [[ "${digest}" =~ ^sha256:[0-9a-f]{64}$ ]]; then
    fail "Caddy manifest digest must have the form sha256:<64 lowercase hex characters>."
  fi
}

# ─── Tag Validation ────────────────────────────────────────────────────────

validate_tag() {
  local tag="$1"

  if [[ -z "${tag}" ]]; then
    fail "Image tag must not be empty. Usage: $(basename "$0") <semver-tag>"
  fi

  case "${tag}" in
    latest|main|master|dev|develop|staging|edge|nightly)
      fail "Tag '${tag}' is not allowed for production. Use an immutable semver tag (e.g. 1.0.0)."
      ;;
  esac

  if ! [[ "${tag}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    fail "Tag '${tag}' is not a valid semver version. Expected format: 1.0.0 (no 'v' prefix — normalize before calling this script)."
  fi
}

# ─── File Validation ───────────────────────────────────────────────────────

validate_prerequisites() {
  if [[ ! -f "${COMPOSE_FILE}" ]]; then
    fail "Missing compose file: ${COMPOSE_FILE}"
  fi

  if [[ ! -f "${ENV_FILE}" ]]; then
    fail "Missing env file: ${ENV_FILE}. Create it from ${DEPLOYMENT_DIR}/env/prod.env.example."
  fi

  if ! command -v docker >/dev/null 2>&1; then
    fail "Docker is not installed or not on PATH."
  fi
}

validate_target_compose() {
  local tag="$1"
  local caddy_digest="$2"

  log "Preflight: validate dedicated production verification secret"
  validate_verification_secret
  validate_caddy_digest "${caddy_digest}"

  log "Preflight: validate resolved production Compose configuration"
  CADDY_IMAGE="${CADDY_REPO}@${caddy_digest}" \
    BACKEND_IMAGE="${BACKEND_REPO}:${tag}" \
    FRONTEND_IMAGE="${FRONTEND_REPO}:${tag}" \
    LANDING_IMAGE="${LANDING_REPO}:${tag}" \
    docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" config --quiet \
    || fail "Production Compose preflight failed; no services were changed."
  echo "Production Compose preflight succeeded; secret value was not displayed."
}

# ─── Capture Currently Deployed Tag ───────────────────────────────────────

read_current_tag() {
  # Reads the tag portion of BACKEND_IMAGE from prod.env (e.g. "1.0.0").
  # Returns empty string if the line is absent or has no colon.
  grep -E '^BACKEND_IMAGE=' "${ENV_FILE}" 2>/dev/null | tail -n1 | cut -d: -f2 || true
}

# ─── Update Image Tags in prod.env ─────────────────────────────────────────

update_env_images() {
  local tag="$1"

  log "Updating image tags in ${ENV_FILE}"

  # Use sed to update only the three image lines. Other content is preserved.
  sed -i \
    -e "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=${BACKEND_REPO}:${tag}|" \
    -e "s|^FRONTEND_IMAGE=.*|FRONTEND_IMAGE=${FRONTEND_REPO}:${tag}|" \
    -e "s|^LANDING_IMAGE=.*|LANDING_IMAGE=${LANDING_REPO}:${tag}|" \
    "${ENV_FILE}"

  grep -q '^BACKEND_IMAGE=' "${ENV_FILE}" || printf 'BACKEND_IMAGE=%s:%s\n' "${BACKEND_REPO}" "${tag}" >> "${ENV_FILE}"
  grep -q '^FRONTEND_IMAGE=' "${ENV_FILE}" || printf 'FRONTEND_IMAGE=%s:%s\n' "${FRONTEND_REPO}" "${tag}" >> "${ENV_FILE}"
  grep -q '^LANDING_IMAGE=' "${ENV_FILE}" || printf 'LANDING_IMAGE=%s:%s\n' "${LANDING_REPO}" "${tag}" >> "${ENV_FILE}"

  echo "BACKEND_IMAGE  → ${BACKEND_REPO}:${tag}"
  echo "FRONTEND_IMAGE → ${FRONTEND_REPO}:${tag}"
  echo "LANDING_IMAGE  → ${LANDING_REPO}:${tag}"
}

update_env_caddy_image() {
  local caddy_image="$1"
  sed -i -e "s|^CADDY_IMAGE=.*|CADDY_IMAGE=${caddy_image}|" "${ENV_FILE}"
  grep -q '^CADDY_IMAGE=' "${ENV_FILE}" || printf 'CADDY_IMAGE=%s\n' "${caddy_image}" >> "${ENV_FILE}"
  echo "CADDY_IMAGE updated to the verified manifest digest."
}

# ─── Write Release Metadata to prod.env ───────────────────────────────────

update_env_release_metadata() {
  local tag="$1"
  local deployed_at
  deployed_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

  log "Writing release metadata to ${ENV_FILE}"

  # Update existing lines if present, then append any that are missing.
  sed -i \
    -e "s|^BARTER_RELEASE_VERSION=.*|BARTER_RELEASE_VERSION=${tag}|" \
    -e "s|^BARTER_DEPLOYED_AT=.*|BARTER_DEPLOYED_AT=${deployed_at}|" \
    -e "s|^BARTER_DEPLOY_SOURCE=.*|BARTER_DEPLOY_SOURCE=github-actions-prod|" \
    "${ENV_FILE}"

  grep -q "^BARTER_RELEASE_VERSION=" "${ENV_FILE}" || echo "BARTER_RELEASE_VERSION=${tag}" >> "${ENV_FILE}"
  grep -q "^BARTER_DEPLOYED_AT="     "${ENV_FILE}" || echo "BARTER_DEPLOYED_AT=${deployed_at}" >> "${ENV_FILE}"
  grep -q "^BARTER_DEPLOY_SOURCE="   "${ENV_FILE}" || echo "BARTER_DEPLOY_SOURCE=github-actions-prod" >> "${ENV_FILE}"

  echo "BARTER_RELEASE_VERSION → ${tag}"
  echo "BARTER_DEPLOYED_AT     → ${deployed_at}"
  echo "BARTER_DEPLOY_SOURCE   → github-actions-prod"
}

# ─── Pre-deployment Database Backup ────────────────────────────────────────

run_pre_deploy_backup() {
  log "Pre-deployment production database backup"

  local backup_enabled
  backup_enabled="$(read_env_value_from_file BACKUP_ENABLED "${ENV_FILE}")"
  backup_enabled="${backup_enabled:-true}"

  case "${backup_enabled,,}" in
    1|true|yes|y|on)
      ;;
    *)
      echo "BACKUP_ENABLED=${backup_enabled} — skipping pre-deployment backup."
      echo "Set BACKUP_ENABLED=true in ${ENV_FILE} to enable automatic pre-deploy backups."
      return 0
      ;;
  esac

  echo "BACKUP_ENABLED=true — running production database backup before deploying..."
  echo "This protects data in case the deployment includes schema migrations or config changes."

  if BACKUP_DB_MODE=external \
     ENV_FILE="${ENV_FILE}" \
     COMPOSE_FILE="${COMPOSE_FILE}" \
     bash "${SCRIPT_DIR}/backup-db.sh"; then
    echo "Production backup completed. Continuing deploy..."
  else
    fail "Pre-deployment backup failed. Aborting deployment to protect database integrity."
  fi
}

# ─── Health Check ──────────────────────────────────────────────────────────

wait_for_url() {
  local url="$1"
  local label="$2"
  local elapsed=0

  echo "Checking ${label}: ${url}"

  while (( elapsed < HEALTH_TIMEOUT_SECONDS )); do
    if curl --fail --silent --show-error --max-time 10 "${url}" >/dev/null 2>&1; then
      echo "  ✓ ${label} is healthy."
      return 0
    fi

    sleep "${HEALTH_POLL_INTERVAL}"
    elapsed=$((elapsed + HEALTH_POLL_INTERVAL))
  done

  fail "Health check timed out after ${HEALTH_TIMEOUT_SECONDS}s for ${label} (${url})."
}

run_health_checks() {
  log "Running production health checks"

  wait_for_url "https://zameni.rs/health" "Landing (zameni.rs)"
  wait_for_url "https://app.zameni.rs/health" "Frontend (app.zameni.rs)"
  wait_for_url "https://app.zameni.rs/api/v1/actuator/health/readiness" "Backend readiness"
}

wait_for_compose_health() {
  local service="$1"
  local elapsed=0
  local container_id
  local state

  echo "Waiting for Compose service '${service}' to become healthy..."
  while (( elapsed < HEALTH_TIMEOUT_SECONDS )); do
    container_id="$(docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps -q "${service}" | head -n 1)"
    if [[ -n "${container_id}" ]]; then
      state="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}" 2>/dev/null || true)"
      case "${state}" in
        healthy|running)
          echo "Compose service '${service}' is ${state}."
          return 0
          ;;
        unhealthy|exited|dead)
          fail "Compose service '${service}' entered '${state}'; inspect its logs before continuing."
          ;;
      esac
    fi
    sleep "${HEALTH_POLL_INTERVAL}"
    elapsed=$((elapsed + HEALTH_POLL_INTERVAL))
  done

  fail "Timed out after ${HEALTH_TIMEOUT_SECONDS}s waiting for Compose service '${service}'."
}

# ─── Main ──────────────────────────────────────────────────────────────────

IMAGE_TAG="${1:-}"
CADDY_DIGEST="${2:-}"
PREFLIGHT_ONLY="${3:-}"
if [[ -n "${PREFLIGHT_ONLY}" && "${PREFLIGHT_ONLY}" != "--preflight-only" ]]; then
  fail "Unknown option '${PREFLIGHT_ONLY}'."
fi
validate_tag "${IMAGE_TAG}"
validate_prerequisites
validate_target_compose "${IMAGE_TAG}" "${CADDY_DIGEST}"
if [[ "${PREFLIGHT_ONLY}" == "--preflight-only" ]]; then
  echo "Preflight passed. No backup, env update, image pull, or service action was performed."
  exit 0
fi

PREVIOUS_TAG="$(read_current_tag)"
PREVIOUS_CADDY_IMAGE="$(read_env_value_from_file CADDY_IMAGE "${ENV_FILE}")"

log "Production deployment started"
echo "Tag:              ${IMAGE_TAG}"
echo "Previous tag:     ${PREVIOUS_TAG:-unknown}  ← use this if rollback is needed"
echo "Previous Caddy:   ${PREVIOUS_CADDY_IMAGE:-unknown}"
echo "Target Caddy:     ${CADDY_REPO}@${CADDY_DIGEST}"
echo "Compose file:     ${COMPOSE_FILE}"
echo "Env file:         ${ENV_FILE}"
echo "Started at (UTC): $(date -u +%Y-%m-%dT%H:%M:%SZ)"

run_pre_deploy_backup

export CADDY_IMAGE="${CADDY_REPO}@${CADDY_DIGEST}"

log "Pulling only target release services"
CADDY_IMAGE="${CADDY_REPO}@${CADDY_DIGEST}" \
  BACKEND_IMAGE="${BACKEND_REPO}:${IMAGE_TAG}" \
  FRONTEND_IMAGE="${FRONTEND_REPO}:${IMAGE_TAG}" \
  LANDING_IMAGE="${LANDING_REPO}:${IMAGE_TAG}" \
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" pull caddy backend frontend landing

log "Updating Caddy first (short planned edge interruption)"
CADDY_IMAGE="${CADDY_REPO}@${CADDY_DIGEST}" \
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --no-deps --force-recreate caddy
wait_for_compose_health caddy
update_env_caddy_image "${CADDY_REPO}@${CADDY_DIGEST}"

log "Updating backend only after Caddy is healthy"
CADDY_IMAGE="${CADDY_REPO}@${CADDY_DIGEST}" BACKEND_IMAGE="${BACKEND_REPO}:${IMAGE_TAG}" \
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --no-deps --force-recreate backend
wait_for_compose_health backend

log "Updating frontend and landing after backend is healthy"
CADDY_IMAGE="${CADDY_REPO}@${CADDY_DIGEST}" \
  BACKEND_IMAGE="${BACKEND_REPO}:${IMAGE_TAG}" \
  FRONTEND_IMAGE="${FRONTEND_REPO}:${IMAGE_TAG}" \
  LANDING_IMAGE="${LANDING_REPO}:${IMAGE_TAG}" \
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --no-deps --force-recreate frontend landing
wait_for_compose_health frontend
wait_for_compose_health landing

update_env_images "${IMAGE_TAG}"
update_env_release_metadata "${IMAGE_TAG}"

log "Container status"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

run_health_checks

log "Production deployment completed successfully"
echo "Tag deployed:      ${IMAGE_TAG}"
echo "Previous tag:      ${PREVIOUS_TAG:-unknown}  ← rollback target if needed"
echo "Caddy image:       ${CADDY_REPO}@${CADDY_DIGEST}"
echo "Backend image:     ${BACKEND_REPO}:${IMAGE_TAG}"
echo "Frontend image:    ${FRONTEND_REPO}:${IMAGE_TAG}"
echo "Landing image:     ${LANDING_REPO}:${IMAGE_TAG}"
echo "Finished at (UTC): $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo
echo "Verify manually:"
echo "  curl -s https://zameni.rs/health"
echo "  curl -s https://app.zameni.rs/health"
echo "  curl -s https://app.zameni.rs/api/v1/actuator/health/readiness"
echo
if [[ -n "${PREVIOUS_TAG}" && "${PREVIOUS_TAG}" != "${IMAGE_TAG}" ]]; then
  echo "To roll back to the previous release:"
  echo "  bash deployment/scripts/rollback-prod.sh ${PREVIOUS_TAG} --confirm-migration-state-reviewed"
fi
