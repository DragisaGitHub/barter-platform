#!/usr/bin/env bash
# rollback-prod.sh — Roll back production to a previous known-good release tag.
#
# Usage:  rollback-prod.sh <previous-tag> --confirm-migration-state-reviewed
# Example: rollback-prod.sh 1.0.0 --confirm-migration-state-reviewed
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

read_caddy_image() {
  local configured
  configured="$(grep -E '^CADDY_IMAGE=' "${ENV_FILE}" 2>/dev/null | tail -n1 | cut -d '=' -f 2- || true)"
  configured="${configured%$'\r'}"
  if [[ -n "${configured}" ]]; then
    printf '%s' "${configured}"
    return 0
  fi

  docker inspect --format '{{.Config.Image}}' barter-prod-caddy 2>/dev/null \
    || fail "CADDY_IMAGE is missing and the current Caddy container could not be inspected. Set CADDY_IMAGE to its current image reference before rollback."
}

compose() {
  CADDY_IMAGE="${CADDY_IMAGE_REF}" docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" "$@"
}

wait_for_compose_health() {
  local service="$1"
  local elapsed=0
  local container_id state
  while (( elapsed < HEALTH_TIMEOUT_SECONDS )); do
    container_id="$(compose ps -q "${service}" | head -n 1)"
    if [[ -n "${container_id}" ]]; then
      state="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}" 2>/dev/null || true)"
      case "${state}" in
        healthy|running) echo "Compose service '${service}' is ${state}."; return 0 ;;
        unhealthy|exited|dead) fail "Compose service '${service}' entered '${state}'; inspect logs before continuing." ;;
      esac
    fi
    sleep "${HEALTH_POLL_INTERVAL}"
    elapsed=$((elapsed + HEALTH_POLL_INTERVAL))
  done
  fail "Timed out waiting for Compose service '${service}' to become healthy."
}

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
    fail "Rollback tag must not be empty. Usage: $(basename "$0") <semver-tag> --confirm-migration-state-reviewed"
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
    fail "Missing env file: ${ENV_FILE}. Create it from ${DEPLOYMENT_DIR}/env/prod.env.example."
  fi

  if ! command -v docker >/dev/null 2>&1; then
    fail "Docker is not installed or not on PATH."
  fi
}

# ─── Capture Currently Deployed Tag ───────────────────────────────────────

read_current_tag() {
  # Reads the tag portion of BACKEND_IMAGE from prod.env (e.g. "1.1.0").
  # Returns empty string if the line is absent or has no colon.
  grep -E '^BACKEND_IMAGE=' "${ENV_FILE}" 2>/dev/null | tail -n1 | cut -d: -f2 || true
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

  grep -q '^BACKEND_IMAGE=' "${ENV_FILE}" || printf 'BACKEND_IMAGE=%s:%s\n' "${BACKEND_REPO}" "${tag}" >> "${ENV_FILE}"
  grep -q '^FRONTEND_IMAGE=' "${ENV_FILE}" || printf 'FRONTEND_IMAGE=%s:%s\n' "${FRONTEND_REPO}" "${tag}" >> "${ENV_FILE}"
  grep -q '^LANDING_IMAGE=' "${ENV_FILE}" || printf 'LANDING_IMAGE=%s:%s\n' "${LANDING_REPO}" "${tag}" >> "${ENV_FILE}"

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
MIGRATION_ACK="${2:-}"
validate_tag "${ROLLBACK_TAG}"
if [[ "${MIGRATION_ACK}" != "--confirm-migration-state-reviewed" ]]; then
  fail "Review the applied Flyway state before rolling back; preserve forward migrations and pass --confirm-migration-state-reviewed only after confirmation."
fi
validate_prerequisites
CADDY_IMAGE_REF="$(read_caddy_image)"

log "Validating rollback Compose configuration"
compose config --quiet || fail "Compose preflight failed; no containers or env files were changed."

CURRENT_TAG="$(read_current_tag)"

log "Production ROLLBACK started"
echo "Rolling back to tag: ${ROLLBACK_TAG}"
echo "Current tag:         ${CURRENT_TAG:-unknown}  ← currently deployed version"
echo "Compose file:        ${COMPOSE_FILE}"
echo "Env file:            ${ENV_FILE}"
echo "Started at (UTC):    $(date -u +%Y-%m-%dT%H:%M:%SZ)"

warn "This script rolls back application images only; it never reverses Flyway or restores a database."
warn "Keep V030/V031 forward. V031 marks V030-retired legacy codes used for older backend compatibility."
warn "For any other destructive migration, assess schema compatibility and consider a separately approved restore."
warn ""
warn "Caddy and unrelated services remain unchanged; only backend/frontend/landing are recreated."
echo

log "Pulling rollback application images only"
BACKEND_IMAGE="${BACKEND_REPO}:${ROLLBACK_TAG}" \
  FRONTEND_IMAGE="${FRONTEND_REPO}:${ROLLBACK_TAG}" \
  LANDING_IMAGE="${LANDING_REPO}:${ROLLBACK_TAG}" \
  compose pull backend frontend landing

log "Rolling back backend, then frontend and landing (Caddy remains unchanged)"
BACKEND_IMAGE="${BACKEND_REPO}:${ROLLBACK_TAG}" \
  compose up -d --no-deps --force-recreate backend
wait_for_compose_health backend
BACKEND_IMAGE="${BACKEND_REPO}:${ROLLBACK_TAG}" \
  FRONTEND_IMAGE="${FRONTEND_REPO}:${ROLLBACK_TAG}" \
  LANDING_IMAGE="${LANDING_REPO}:${ROLLBACK_TAG}" \
  compose up -d --no-deps --force-recreate frontend landing
wait_for_compose_health frontend
wait_for_compose_health landing

update_env_images "${ROLLBACK_TAG}"

log "Container status after rollback"
compose ps

run_health_checks

log "Production ROLLBACK completed successfully"
echo "Rolled back to tag:  ${ROLLBACK_TAG}"
echo "Previous tag was:    ${CURRENT_TAG:-unknown}"
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
