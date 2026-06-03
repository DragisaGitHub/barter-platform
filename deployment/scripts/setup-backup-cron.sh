#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_DIR="$(cd "${DEPLOYMENT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"
LOG_DIR="${DEPLOYMENT_DIR}/logs"
CRON_TAG="# barter-platform-postgres-backup"

usage() {
  cat <<EOF
Usage: $(basename "$0")

Installs or updates the current user's cron entry for PostgreSQL backups.
Uses BACKUP_SCHEDULE when set, otherwise maps BACKUP_FREQUENCY to a default cron expression.

Frequency defaults:
  monthly -> 0 4 1 * *
  weekly  -> 0 4 * * 0
  daily   -> 0 3 * * *
EOF
}

fail() {
  echo "$*" >&2
  exit 1
}

read_env_value() {
  local key="$1"
  local value

  value="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "${ENV_FILE}" | tail -n 1 | cut -d '=' -f 2- || true)"
  value="${value%$'\r'}"

  if [[ "${value}" =~ ^\".*\"$ ]] || [[ "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:-1}"
  fi

  printf '%s' "${value}"
}

resolve_schedule() {
  local frequency="$1"

  case "${frequency,,}" in
    daily)
      printf '%s' '0 3 * * *'
      ;;
    weekly)
      printf '%s' '0 4 * * 0'
      ;;
    monthly|'')
      printf '%s' '0 4 1 * *'
      ;;
    *)
      fail "Unsupported BACKUP_FREQUENCY='${frequency}'. Use daily, weekly, monthly, or set BACKUP_SCHEDULE explicitly."
      ;;
  esac
}

while [[ $# -gt 0 ]]; do
  case "$1" in
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
  fail "Missing env file: ${ENV_FILE}"
fi

if ! command -v crontab >/dev/null 2>&1; then
  fail "crontab is required to install the backup schedule."
fi

BACKUP_ENABLED="${BACKUP_ENABLED:-$(read_env_value BACKUP_ENABLED)}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-$(read_env_value BACKUP_FREQUENCY)}"
BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-$(read_env_value BACKUP_SCHEDULE)}"
BACKUP_ENABLED="${BACKUP_ENABLED:-true}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-monthly}"
BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-$(resolve_schedule "${BACKUP_FREQUENCY}")}"

mkdir -p "${LOG_DIR}"

BACKUP_COMMAND="cd '${REPO_DIR}' && ENV_FILE='${ENV_FILE}' '${DEPLOYMENT_DIR}/scripts/backup-db.sh' >> '${LOG_DIR}/backup-db.log' 2>&1"
CRON_LINE="${BACKUP_SCHEDULE} ${BACKUP_COMMAND} ${CRON_TAG}"

TMP_CRON_FILE="$(mktemp)"
trap 'rm -f "${TMP_CRON_FILE}"' EXIT

crontab -l 2>/dev/null | grep -Fv "${CRON_TAG}" > "${TMP_CRON_FILE}" || true

if [[ "${BACKUP_ENABLED,,}" == "true" || "${BACKUP_ENABLED}" == "1" || "${BACKUP_ENABLED,,}" == "yes" || "${BACKUP_ENABLED,,}" == "on" ]]; then
  echo "${CRON_LINE}" >> "${TMP_CRON_FILE}"
  crontab "${TMP_CRON_FILE}"
  echo "Installed PostgreSQL backup cron entry: ${BACKUP_SCHEDULE}"
else
  crontab "${TMP_CRON_FILE}"
  echo "BACKUP_ENABLED=false. Removed tagged PostgreSQL backup cron entry, if present."
fi

echo "Cron log file: ${LOG_DIR}/backup-db.log"


