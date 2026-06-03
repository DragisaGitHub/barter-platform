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
  echo "$*" >&2
  exit 1
}

print_command() {
  printf '  '
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

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Missing env file: ${ENV_FILE}
Create it from ${DEPLOYMENT_DIR}/env/dev.env.example and fill DEV secrets on the server."
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  fail "Missing compose file: ${COMPOSE_FILE}"
fi

if [[ "${DRY_RUN}" != "true" ]] && ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed or not on PATH."
fi

echo "Using compose file: ${COMPOSE_FILE}"
echo "Using env file:     ${ENV_FILE}"
echo "Dry run:            ${DRY_RUN}"

if [[ "${SKIP_STATE_CAPTURE}" != "true" ]]; then
  if [[ ! -f "${STATE_CAPTURE_SCRIPT}" ]]; then
    fail "Missing state capture script: ${STATE_CAPTURE_SCRIPT}"
  fi

  echo
  echo "Capturing current deployment state before pulling new images..."
  if [[ "${DRY_RUN}" == "true" ]]; then
    run_cmd bash "${STATE_CAPTURE_SCRIPT}" --dry-run
  else
    run_cmd bash "${STATE_CAPTURE_SCRIPT}"
  fi
fi

if [[ "${SKIP_PULL}" != "true" ]]; then
  echo
  echo "Pulling configured images..."
  run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" pull
else
  echo
  echo "Skipping image pull and reusing locally available images."
fi

echo
echo "Starting Barter Platform DEV stack..."
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --remove-orphans

echo
wait_for_service_health backend "${HEALTH_TIMEOUT_SECONDS}"
wait_for_service_health frontend "${HEALTH_TIMEOUT_SECONDS}"
record_successful_deployment_metadata

echo
echo "Current service status:"
run_cmd docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

echo
echo "Useful log commands:"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f caddy"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f backend"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f frontend"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f postgres"

