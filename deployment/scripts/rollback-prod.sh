#!/usr/bin/env bash
# rollback-prod.sh — Roll back production to a previous known-good release tag.
#
# Usage:  rollback-prod.sh <previous-tag>
# Example: rollback-prod.sh 1.0.0
#
# This script is functionally identical to deploy-prod.sh but provides explicit
# rollback warnings, especially regarding irreversible database migrations.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOYMENT_DIR}/compose/docker-compose.prod.yml"
ENV_FILE="${DEPLOYMENT_DIR}/env/prod.env"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-300}"
HEALTH_POLL_INTERVAL=5

BACKEND_REPO="dragisahub1984/barter-backend"
FRONTEND_REPO="dragisahub1984/barter-frontend"
LANDING_REPO="dragisahub1984/barter-landing"

# ─── Helpers ────────────────────────────────────────────────────────────────

fail() {
  echo "[ERROR] $*" >&2
  exit 1
}

log() {
  echo
  echo "=== $* ==="
}

warn() {
  echo
  echo "⚠️  WARNING: $*"
}

# ─── Tag Validation ────────────────────────────────────────────────────────

validate_tag() {
  local tag="$1"

  if [[ -z "${tag}" ]]; then
    fail "Rollback tag must not be empty. Usage: $(basename "$0") <semver-tag>"
  fi

  case "${tag}" in
    latest|main|master|dev|develop|staging|edge|nightly)
      fail "Tag '${tag}' is not allowed for production rollback. Use an immutable semver tag (e.g. 1.0.0)."
      ;;
  esac

  if ! [[ "${tag}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    fail "Tag '${tag}' is not a valid semver version. Expected format: 1.0.0 (no 'v' prefix)."
  fi
}

# ─── File Validation ───────────────────────────────────────────────────────

validate_prerequisites() {
  if [[ ! -f "${COMPOSE_FILE}" ]]; then
    fail "Missing compose file: ${COMPOSE_FILE}"
  fi

  if [[ ! -f "${ENV_FILE}" ]]; then
    fail "Missing env file: ${ENV_FILE}."
  fi

  if ! command -v docker >/dev/null 2>&1; then
    fail "Docker is not installed or not on PATH."
  fi
}

# ─── Update Image Tags in prod.env ─────────────────────────────────────────

update_env_images() {
  local tag="$1"

  log "Updating image tags in ${ENV_FILE} for rollback"

  sed -i \
    -e "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=${BACKEND_REPO}:${tag}|" \
    -e "s|^FRONTEND_IMAGE=.*|FRONTEND_IMAGE=${FRONTEND_REPO}:${tag}|" \
    -e "s|^LANDING_IMAGE=.*|LANDING_IMAGE=${LANDING_REPO}:${tag}|" \
    "${ENV_FILE}"

  echo "BACKEND_IMAGE  → ${BACKEND_REPO}:${tag}"
  echo "FRONTEND_IMAGE → ${FRONTEND_REPO}:${tag}"
  echo "LANDING_IMAGE  → ${LANDING_REPO}:${tag}"
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
  log "Running production health checks after rollback"

  wait_for_url "https://zameni.rs/health" "Landing (zameni.rs)"
  wait_for_url "https://app.zameni.rs/health" "Frontend (app.zameni.rs)"
  wait_for_url "https://app.zameni.rs/api/v1/actuator/health/readiness" "Backend readiness"
}

# ─── Main ──────────────────────────────────────────────────────────────────

ROLLBACK_TAG="${1:-}"
validate_tag "${ROLLBACK_TAG}"
validate_prerequisites

log "Production ROLLBACK started"
echo "Rolling back to tag: ${ROLLBACK_TAG}"
echo "Compose file:        ${COMPOSE_FILE}"
echo "Env file:            ${ENV_FILE}"
echo "Started at (UTC):    $(date -u +%Y-%m-%dT%H:%M:%SZ)"

warn "If the release you are rolling back FROM introduced irreversible database migrations"
warn "(e.g. dropped columns, renamed tables, deleted data), this rollback may cause"
warn "application errors. In that case, you must ALSO restore the database from a backup"
warn "taken BEFORE the forward deploy. Database restore is a separate manual step."
warn ""
warn "Proceed with caution. The rollback will now update images and recreate containers."
echo

update_env_images "${ROLLBACK_TAG}"

log "Pulling rollback images"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" pull

log "Recreating production stack with rollback images"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --force-recreate --remove-orphans

log "Container status after rollback"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

run_health_checks

log "Production ROLLBACK completed successfully"
echo "Rolled back to tag:  ${ROLLBACK_TAG}"
echo "Backend image:       ${BACKEND_REPO}:${ROLLBACK_TAG}"
echo "Frontend image:      ${FRONTEND_REPO}:${ROLLBACK_TAG}"
echo "Landing image:       ${LANDING_REPO}:${ROLLBACK_TAG}"
echo "Finished at (UTC):   $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo
echo "Verify manually:"
echo "  curl -s https://zameni.rs/health"
echo "  curl -s https://app.zameni.rs/health"
echo "  curl -s https://app.zameni.rs/api/v1/actuator/health/readiness"
echo
echo "If the application is not working correctly after rollback, check:"
echo "  1. Database compatibility with the rolled-back version"
echo "  2. Container logs: docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} logs --tail=200"
echo "  3. Consider restoring a database backup if migrations are incompatible"
