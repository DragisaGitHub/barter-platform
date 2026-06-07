#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"
STATE_CAPTURE_SCRIPT="${SCRIPT_DIR}/capture-deployment-state.sh"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"
DRY_RUN="false"
SKIP_STATE_CAPTURE="false"
SKIP_PULL="false"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--dry-run] [--skip-state-capture] [--skip-pull]

Pulls the configured backend/frontend images, captures the current running image state,
recreates the DEV stack, and waits for backend/frontend health checks.

Options:
  --dry-run             Print the actions without changing containers.
  --skip-state-capture  Do not capture the current backend/frontend deployment state first.
  --skip-pull           Skip 'docker compose pull' and reuse locally available configured images.
  --help                Show this help.
EOF
}

fail() {
  echo "[ERROR] $*" >&2
  exit 1
}

log() {
  echo
  echo "=== $* ==="
}

print_command() {
  printf '+ '
  printf '%q ' "$@"
  printf '\n'
}

run_cmd() {
  print_command "$@"

  if [[ "${DRY_RUN}" == "true" ]]; then
    return 0
  fi

  "$@"
}

compose_cmd() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

print_image_info() {
  local label="$1"
  local image="$2"

  echo "${label}: ${image}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] Would inspect ${image}"
    return 0
  fi

  docker image inspect "${image}" --format '  Id={{.Id}} Created={{.Created}} RepoDigests={{join .RepoDigests ", "}}' 2>/dev/null || echo "  Image not available locally."
}

print_container_image_info() {
  local service="$1"
  local container_id
  local image_id
  local created

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] Would inspect running container for service ${service}"
    return 0
  fi

  container_id="$(compose_cmd ps -q "${service}" | head -n 1 || true)"

  if [[ -z "${container_id}" ]]; then
    echo "Service ${service}: no running container found."
    return 0
  fi

  image_id="$(docker inspect --format '{{.Image}}' "${container_id}" 2>/dev/null || true)"
  created="$(docker inspect --format '{{.Created}}' "${container_id}" 2>/dev/null || true)"

  echo "Service ${service}:"
  echo "  ContainerId=${container_id}"
  echo "  ContainerCreated=${created}"
  echo "  ContainerImage=${image_id}"
}

wait_for_service_health() {
  local service="$1"
  local timeout_seconds="$2"
  local poll_interval=3
  local elapsed=0
  local container_id
  local health_status

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] Would wait up to ${timeout_seconds}s for '${service}' health."
    return 0
  fi

  echo "Waiting for '${service}' to become healthy (timeout: ${timeout_seconds}s)..."

  while (( elapsed < timeout_seconds )); do
    container_id="$(compose_cmd ps -q "${service}" | head -n 1)"

    if [[ -z "${container_id}" ]]; then
      sleep "${poll_interval}"
      elapsed=$((elapsed + poll_interval))
      continue
    fi

    health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}" 2>/dev/null || true)"

    case "${health_status}" in
      healthy|running)
        echo "Service '${service}' is ${health_status}."
        return 0
        ;;
      unhealthy|exited|dead)
        fail "Service '${service}' entered '${health_status}'. Inspect logs with: docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs --tail=200 ${service}"
        ;;
      *)
        sleep "${poll_interval}"
        elapsed=$((elapsed + poll_interval))
        ;;
    esac
  done

  fail "Timed out after ${timeout_seconds}s waiting for '${service}' health. Inspect logs with: docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs --tail=200 ${service}"
}

record_successful_deployment_metadata() {
  local state_file="${DEPLOYMENT_DIR}/state/dev/public/latest.env"
  local temp_state_file="${state_file}.tmp"
  local deployed_at

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] Would record safe public deployment metadata in ${state_file}."
    return 0
  fi

  deployed_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  mkdir -p "$(dirname "${state_file}")"

  if [[ -f "${state_file}" ]]; then
    grep -v '^DEPLOYED_AT_UTC=' "${state_file}" > "${temp_state_file}" || true
  else
    {
      echo "#!/usr/bin/env bash"
      printf '%s=%s\n' "STATE_VERSION" "1"
    } > "${temp_state_file}"
  fi

  printf '%s=%s\n' "DEPLOYED_AT_UTC" "${deployed_at}" >> "${temp_state_file}"
  mv "${temp_state_file}" "${state_file}"
  echo "Safe public deployment metadata updated: ${state_file}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --skip-state-capture)
      SKIP_STATE_CAPTURE="true"
      shift
      ;;
    --skip-pull)
      SKIP_PULL="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage >&2
      fail "Unknown argument: $1"
      ;;
  esac
done

log "DEV deployment script started"
echo "Started at UTC:       $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "Working directory:    $(pwd)"
echo "Script directory:     ${SCRIPT_DIR}"
echo "Deployment directory: ${DEPLOYMENT_DIR}"
echo "Compose file:         ${COMPOSE_FILE}"
echo "Env file:             ${ENV_FILE}"
echo "Dry run:              ${DRY_RUN}"
echo "Skip state capture:   ${SKIP_STATE_CAPTURE}"
echo "Skip pull:            ${SKIP_PULL}"

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Missing env file: ${ENV_FILE}. Create it from ${DEPLOYMENT_DIR}/env/dev.env.example and fill DEV secrets on the server."
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  fail "Missing compose file: ${COMPOSE_FILE}"
fi

if [[ "${DRY_RUN}" != "true" ]] && ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed or not on PATH."
fi

log "Docker version"
run_cmd docker --version
run_cmd docker compose version

log "Configured images"
compose_cmd config | grep -A3 -B3 "image:" || true

log "Current local images before pull"
print_image_info "Backend latest before pull" "dragisahub1984/barter-backend:latest"
print_image_info "Frontend latest before pull" "dragisahub1984/barter-frontend:latest"

log "Current running containers before deploy"
print_container_image_info backend
print_container_image_info frontend
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

if [[ "${SKIP_STATE_CAPTURE}" != "true" ]]; then
  if [[ ! -f "${STATE_CAPTURE_SCRIPT}" ]]; then
    fail "Missing state capture script: ${STATE_CAPTURE_SCRIPT}"
  fi

  log "Capturing current deployment state"
  if [[ "${DRY_RUN}" == "true" ]]; then
    run_cmd bash "${STATE_CAPTURE_SCRIPT}" --dry-run
  else
    run_cmd bash "${STATE_CAPTURE_SCRIPT}"
  fi
fi

if [[ "${SKIP_PULL}" != "true" ]]; then
  log "Pulling configured images"
  run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" pull
else
  log "Skipping image pull"
fi

log "Current local images after pull"
print_image_info "Backend latest after pull" "dragisahub1984/barter-backend:latest"
print_image_info "Frontend latest after pull" "dragisahub1984/barter-frontend:latest"

log "Starting Barter Platform DEV stack"
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --force-recreate --remove-orphans

log "Current running containers after recreate"
print_container_image_info backend
print_container_image_info frontend
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

log "Waiting for service health"
wait_for_service_health backend "${HEALTH_TIMEOUT_SECONDS}"
wait_for_service_health frontend "${HEALTH_TIMEOUT_SECONDS}"

log "Recording deployment metadata"
record_successful_deployment_metadata

log "Final service status"
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

log "Useful log commands"
echo "docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f caddy"
echo "docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f backend"
echo "docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f frontend"
echo "docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f postgres"

log "DEV deployment script finished"
echo "Finished at UTC: $(date -u +%Y-%m-%dT%H:%M:%SZ)"