#!/usr/bin/env bash
# deploy-prod.sh — Deploy a specific immutable release tag to production.
#
# Usage:  deploy-prod.sh <image-tag>
# Example: deploy-prod.sh 1.0.0
#
# The tag must be a semver version (e.g. 1.0.0). Tags like "latest", "main",
# "dev", "develop", "master" are rejected. The script updates BACKEND_IMAGE,
# FRONTEND_IMAGE, and LANDING_IMAGE in env/prod.env, pulls the images, and
# recreates the production stack. Health checks are performed after deploy.
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
  log "Running production health checks"

  wait_for_url "https://zameni.rs/health" "Landing (zameni.rs)"
  wait_for_url "https://app.zameni.rs/health" "Frontend (app.zameni.rs)"
  wait_for_url "https://app.zameni.rs/api/v1/actuator/health/readiness" "Backend readiness"
}

# ─── Main ──────────────────────────────────────────────────────────────────

IMAGE_TAG="${1:-}"
validate_tag "${IMAGE_TAG}"
validate_prerequisites

log "Production deployment started"
echo "Tag:              ${IMAGE_TAG}"
echo "Compose file:     ${COMPOSE_FILE}"
echo "Env file:         ${ENV_FILE}"
echo "Started at (UTC): $(date -u +%Y-%m-%dT%H:%M:%SZ)"

update_env_images "${IMAGE_TAG}"

log "Pulling images"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" pull

log "Recreating production stack"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --force-recreate --remove-orphans

log "Container status"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

run_health_checks

log "Production deployment completed successfully"
echo "Tag deployed:      ${IMAGE_TAG}"
echo "Backend image:     ${BACKEND_REPO}:${IMAGE_TAG}"
echo "Frontend image:    ${FRONTEND_REPO}:${IMAGE_TAG}"
echo "Landing image:     ${LANDING_REPO}:${IMAGE_TAG}"
echo "Finished at (UTC): $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo
echo "Verify manually:"
echo "  curl -s https://zameni.rs/health"
echo "  curl -s https://app.zameni.rs/health"
echo "  curl -s https://app.zameni.rs/api/v1/actuator/health/readiness"
