#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml}"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"
STATE_DIR_DEFAULT="${DEPLOYMENT_DIR}/state/dev"
STATE_DIR="${STATE_DIR:-${STATE_DIR_DEFAULT}}"
STATE_FILE=""
DRY_RUN="false"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--state-dir <dir>] [--state-file <file>] [--dry-run]

Captures the current backend/frontend container image state so the operator can roll
back to the exact previously running images before a new deployment.

Options:
  --state-dir <dir>    Directory for timestamped state files (default: ${STATE_DIR_DEFAULT}).
  --state-file <file>  Exact output file path. Also refreshes latest.env beside it when possible.
  --dry-run            Print the state that would be captured without writing files.
  --help               Show this help.
EOF
}

fail() {
  echo "$*" >&2
  exit 1
}

compose_cmd() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

strip_image_tag_or_digest() {
  local image_ref="$1"
  local without_digest="${image_ref%@*}"
  local last_segment="${without_digest##*/}"

  if [[ "${last_segment}" == *:* ]]; then
    printf '%s' "${without_digest%:*}"
  else
    printf '%s' "${without_digest}"
  fi
}

resolve_rollback_image() {
  local config_image="$1"
  local image_id="$2"
  local image_repo="${3:-}"
  local repo_digests_output
  local preferred_digest=""
  local first_digest=""
  local digest

  repo_digests_output="$(docker image inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "${image_id}" 2>/dev/null || true)"

  while IFS= read -r digest; do
    [[ -n "${digest}" ]] || continue

    if [[ -z "${first_digest}" ]]; then
      first_digest="${digest}"
    fi

    if [[ -n "${image_repo}" && "${digest}" == "${image_repo}@"* ]]; then
      preferred_digest="${digest}"
      break
    fi
  done <<< "${repo_digests_output}"

  if [[ -n "${preferred_digest}" ]]; then
    printf '%s' "${preferred_digest}"
    return 0
  fi

  if [[ -n "${first_digest}" ]]; then
    printf '%s' "${first_digest}"
    return 0
  fi

  if [[ -n "${config_image}" ]]; then
    printf '%s' "${config_image}"
    return 0
  fi

  printf '%s' "${image_id}"
}

append_service_state() {
  local service="$1"
  local prefix="$2"
  local target_file="$3"
  local container_id
  local container_name
  local running
  local config_image
  local image_id
  local image_repo
  local rollback_image

  container_id="$(compose_cmd ps -q "${service}" | head -n 1)"
  if [[ -z "${container_id}" ]]; then
    echo "No existing '${service}' container found; skipping state capture for this service."
    return 1
  fi

  running="$(docker inspect --format '{{.State.Running}}' "${container_id}" 2>/dev/null || true)"
  container_name="$(docker inspect --format '{{.Name}}' "${container_id}" 2>/dev/null || true)"
  container_name="${container_name#/}"
  config_image="$(docker inspect --format '{{.Config.Image}}' "${container_id}" 2>/dev/null || true)"
  image_id="$(docker inspect --format '{{.Image}}' "${container_id}" 2>/dev/null || true)"
  image_repo="$(strip_image_tag_or_digest "${config_image}")"
  rollback_image="$(resolve_rollback_image "${config_image}" "${image_id}" "${image_repo}")"

  printf '%s=%q\n' "${prefix}_SERVICE" "${service}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_CONTAINER_ID" "${container_id}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_CONTAINER_NAME" "${container_name}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_RUNNING" "${running}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_CONFIG_IMAGE" "${config_image}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_IMAGE_REPOSITORY" "${image_repo}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_IMAGE_ID" "${image_id}" >> "${target_file}"
  printf '%s=%q\n' "${prefix}_ROLLBACK_IMAGE" "${rollback_image}" >> "${target_file}"

  echo "Captured ${service} state:"
  echo "  container:      ${container_name:-<unknown>} (${container_id})"
  echo "  config image:   ${config_image:-<unknown>}"
  echo "  image id:       ${image_id:-<unknown>}"
  echo "  rollback image: ${rollback_image:-<unknown>}"

  if [[ "${running}" != "true" ]]; then
    echo "  note: container is not currently running; rollback may not be safe until the cause is understood."
  fi

  return 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --state-dir)
      STATE_DIR="${2:-}"
      shift 2
      ;;
    --state-file)
      STATE_FILE="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
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

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  fail "Missing compose file: ${COMPOSE_FILE}"
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Missing env file: ${ENV_FILE}"
fi

if [[ "${DRY_RUN}" != "true" ]] && ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed or not on PATH."
fi

if [[ -z "${STATE_FILE}" ]]; then
  STATE_FILE="${STATE_DIR}/deployment-state-${TIMESTAMP}.env"
fi

LATEST_STATE_FILE="$(dirname "${STATE_FILE}")/latest.env"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "Using compose file: ${COMPOSE_FILE}"
  echo "Using env file:     ${ENV_FILE}"
  echo "[dry-run] Would inspect the currently running 'backend' and 'frontend' containers via docker compose + docker inspect."
  echo "[dry-run] Would write deployment state to: ${STATE_FILE}"
  echo "[dry-run] Would refresh latest state file: ${LATEST_STATE_FILE}"
  exit 0
fi

mkdir -p "$(dirname "${STATE_FILE}")"

TEMP_STATE_FILE="${STATE_FILE}.tmp"

trap 'rm -f "${TEMP_STATE_FILE}"' EXIT

{
  echo "#!/usr/bin/env bash"
  echo "# Generated by $(basename "$0") on ${TIMESTAMP}"
  printf '%s=%q\n' "STATE_VERSION" "1"
  printf '%s=%q\n' "CAPTURED_AT_UTC" "${TIMESTAMP}"
  printf '%s=%q\n' "COMPOSE_FILE" "${COMPOSE_FILE}"
  printf '%s=%q\n' "ENV_FILE" "${ENV_FILE}"
} > "${TEMP_STATE_FILE}"

captured_count=0

if append_service_state backend BACKEND "${TEMP_STATE_FILE}"; then
  captured_count=$((captured_count + 1))
fi

echo >> "${TEMP_STATE_FILE}"

if append_service_state frontend FRONTEND "${TEMP_STATE_FILE}"; then
  captured_count=$((captured_count + 1))
fi

if (( captured_count == 0 )); then
  echo "No existing backend/frontend containers were found. Nothing was written."
  exit 0
fi

mv "${TEMP_STATE_FILE}" "${STATE_FILE}"
cp "${STATE_FILE}" "${LATEST_STATE_FILE}"

echo
if [[ "${STATE_FILE}" == "${LATEST_STATE_FILE}" ]]; then
  echo "Deployment state updated: ${STATE_FILE}"
else
  echo "Deployment state written: ${STATE_FILE}"
  echo "Latest state refreshed:  ${LATEST_STATE_FILE}"
fi

