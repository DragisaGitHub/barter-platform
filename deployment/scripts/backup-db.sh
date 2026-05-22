#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_DIR="$(cd "${DEPLOYMENT_DIR}/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml}"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--force]

Creates a PostgreSQL pg_dump backup, compresses it with gzip, uploads it to Azure Blob Storage,
and prunes older local backup files according to BACKUP_LOCAL_RETENTION_COUNT.

Options:
  --force   Run even when BACKUP_ENABLED=false.
  --help    Show this help.
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

is_truthy() {
  case "${1,,}" in
    1|true|yes|y|on)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

cleanup_local_backups() {
  local retention="$1"
  local pattern="$2"
  local -a files=()
  local index

  shopt -s nullglob
  files=("${BACKUP_WORK_DIR}"/${pattern})
  shopt -u nullglob

  if (( ${#files[@]} <= retention )); then
    return 0
  fi

  mapfile -t files < <(printf '%s\n' "${files[@]}" | sort -r)

  for (( index=retention; index<${#files[@]}; index++ )); do
    rm -f "${files[${index}]}"
    echo "Removed old local backup: ${files[${index}]}"
  done
}

FORCE_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE_RUN="true"
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
  fail "Missing env file: ${ENV_FILE}"
fi

BACKUP_ENABLED="${BACKUP_ENABLED:-$(read_env_value BACKUP_ENABLED)}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-$(read_env_value BACKUP_FREQUENCY)}"
BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-$(read_env_value BACKUP_SCHEDULE)}"
BACKUP_LOCAL_RETENTION_COUNT="${BACKUP_LOCAL_RETENTION_COUNT:-$(read_env_value BACKUP_LOCAL_RETENTION_COUNT)}"
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-$(read_env_value BACKUP_AZURE_CONTAINER)}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-$(read_env_value BACKUP_AZURE_PREFIX)}"
BACKUP_WORK_DIR="${BACKUP_WORK_DIR:-$(read_env_value BACKUP_WORK_DIR)}"
BACKUP_AZURE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING:-$(read_env_value BACKUP_AZURE_CONNECTION_STRING)}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-$(read_env_value POSTGRES_SERVICE)}"
POSTGRES_DB="${POSTGRES_DB:-$(read_env_value POSTGRES_DB)}"
POSTGRES_USER="${POSTGRES_USER:-$(read_env_value POSTGRES_USER)}"
AZURE_STORAGE_CONNECTION_STRING_DEV="${AZURE_STORAGE_CONNECTION_STRING_DEV:-$(read_env_value AZURE_STORAGE_CONNECTION_STRING_DEV)}"

BACKUP_ENABLED="${BACKUP_ENABLED:-true}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-monthly}"
BACKUP_LOCAL_RETENTION_COUNT="${BACKUP_LOCAL_RETENTION_COUNT:-2}"
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-postgres-backups}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-dev/postgres}"
BACKUP_WORK_DIR="${BACKUP_WORK_DIR:-${DEPLOYMENT_DIR}/backups/postgres}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"

BACKUP_AZURE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING:-${AZURE_STORAGE_CONNECTION_STRING_DEV:-}}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX#/}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX%/}"

if [[ "${BACKUP_WORK_DIR}" != /* ]]; then
  BACKUP_WORK_DIR="${REPO_DIR}/${BACKUP_WORK_DIR}"
fi

if ! is_truthy "${BACKUP_ENABLED}" && ! is_truthy "${FORCE_RUN}"; then
  echo "BACKUP_ENABLED=false. Skipping PostgreSQL backup."
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed or not on PATH."
fi

if ! command -v gzip >/dev/null 2>&1; then
  fail "gzip is required for compressed PostgreSQL backups."
fi

if ! command -v az >/dev/null 2>&1; then
  fail "Azure CLI ('az') is required for backup upload."
fi

if [[ -z "${POSTGRES_DB}" || -z "${POSTGRES_USER}" ]]; then
  fail "POSTGRES_DB and POSTGRES_USER must be set in ${ENV_FILE}."
fi

if [[ -z "${BACKUP_AZURE_CONNECTION_STRING}" ]]; then
  fail "Set BACKUP_AZURE_CONNECTION_STRING (or reuse AZURE_STORAGE_CONNECTION_STRING_DEV) in ${ENV_FILE}."
fi

if ! [[ "${BACKUP_LOCAL_RETENTION_COUNT}" =~ ^[0-9]+$ ]]; then
  fail "BACKUP_LOCAL_RETENTION_COUNT must be a non-negative integer."
fi

mkdir -p "${BACKUP_WORK_DIR}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_BASENAME="barter-${POSTGRES_DB}-${TIMESTAMP}.dump.gz"
BACKUP_FILE="${BACKUP_WORK_DIR}/${BACKUP_BASENAME}"
TEMP_BACKUP_FILE="${BACKUP_FILE}.partial"
BACKUP_BLOB_NAME="${BACKUP_BASENAME}"

if [[ -n "${BACKUP_AZURE_PREFIX}" ]]; then
  BACKUP_BLOB_NAME="${BACKUP_AZURE_PREFIX}/${BACKUP_BASENAME}"
fi

cleanup_partial_backup() {
  if [[ -f "${TEMP_BACKUP_FILE}" ]]; then
    rm -f "${TEMP_BACKUP_FILE}"
  fi
}

trap cleanup_partial_backup EXIT

echo "Using compose file: ${COMPOSE_FILE}"
echo "Using env file:     ${ENV_FILE}"
echo "Backup frequency:   ${BACKUP_FREQUENCY}${BACKUP_SCHEDULE:+ (schedule: ${BACKUP_SCHEDULE})}"
echo "Work directory:     ${BACKUP_WORK_DIR}"
echo "Azure container:    ${BACKUP_AZURE_CONTAINER}"
echo "Azure blob path:    ${BACKUP_BLOB_NAME}"
echo "Creating compressed PostgreSQL backup: ${BACKUP_FILE}"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T \
  -e BACKUP_POSTGRES_DB="${POSTGRES_DB}" \
  -e BACKUP_POSTGRES_USER="${POSTGRES_USER}" \
  "${POSTGRES_SERVICE}" \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h localhost -U "$BACKUP_POSTGRES_USER" -d "$BACKUP_POSTGRES_DB" --format=custom --no-owner --no-privileges' \
  | gzip -c > "${TEMP_BACKUP_FILE}"

mv "${TEMP_BACKUP_FILE}" "${BACKUP_FILE}"

echo "Uploading backup to Azure Blob Storage..."
az storage blob upload \
  --connection-string "${BACKUP_AZURE_CONNECTION_STRING}" \
  --container-name "${BACKUP_AZURE_CONTAINER}" \
  --name "${BACKUP_BLOB_NAME}" \
  --file "${BACKUP_FILE}" \
  --overwrite true \
  --content-type application/gzip \
  --no-progress \
  --only-show-errors >/dev/null

echo "Upload complete. Applying local retention policy..."
cleanup_local_backups "${BACKUP_LOCAL_RETENTION_COUNT}" "barter-${POSTGRES_DB}-*.dump.gz"

echo "Backup complete."
if [[ -f "${BACKUP_FILE}" ]]; then
  echo "Local file retained: ${BACKUP_FILE}"
else
  echo "Local file retained: none (removed by retention policy)"
fi
echo "Azure blob:          ${BACKUP_BLOB_NAME}"
echo
echo "Restore example:"
echo "  ./deployment/scripts/restore-db.sh --file ${BACKUP_FILE} --target-db ${POSTGRES_DB}_restore_test --recreate-target-db --yes"

