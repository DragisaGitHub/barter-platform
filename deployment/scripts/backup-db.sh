#!/usr/bin/env bash
set -euo pipefail
umask 077   # Backup files and directories are owner-only (no world/group read)

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

Backup modes (controlled by BACKUP_DB_MODE in the env file or environment):
  compose   (default) — runs pg_dump inside the running Docker Compose PostgreSQL container.
  external            — runs pg_dump directly on the host against a managed/external PostgreSQL
                        instance using DB_URL, DB_USERNAME, and DB_PASSWORD from the env file.
                        Use this mode for production (BACKUP_DB_MODE=external in prod.env).

Options:
  --force   Run even when BACKUP_ENABLED=false.
  --help    Show this help.
EOF
}

fail() {
  echo "$*" >&2
  exit 1
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
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

upload_backup_with_host_az() {
  echo "Uploading backup to Azure Blob Storage using host Azure CLI..."

  # Pass the connection string via environment variable so it never appears
  # in the process argument list (ps aux) on the host.
  AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
  az storage blob upload \
    --container-name "${BACKUP_AZURE_CONTAINER}" \
    --name "${BACKUP_BLOB_NAME}" \
    --file "${BACKUP_FILE}" \
    --overwrite true \
    --content-type application/gzip \
    --no-progress \
    --only-show-errors >/dev/null
}

upload_backup_with_docker_az() {
  local backup_dir
  local backup_name

  backup_dir="$(dirname "${BACKUP_FILE}")"
  backup_name="$(basename "${BACKUP_FILE}")"

  echo "Uploading backup to Azure Blob Storage using Azure CLI Docker image (${AZURE_CLI_DOCKER_IMAGE})..."

  AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
  BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER}" \
  BACKUP_BLOB_NAME="${BACKUP_BLOB_NAME}" \
  BACKUP_FILE_NAME="${backup_name}" \
    docker run --rm \
      -e AZURE_STORAGE_CONNECTION_STRING \
      -e BACKUP_AZURE_CONTAINER \
      -e BACKUP_BLOB_NAME \
      -e BACKUP_FILE_NAME \
      -v "${backup_dir}:/backup:ro" \
      "${AZURE_CLI_DOCKER_IMAGE}" \
      sh -c 'az storage blob upload \
        --container-name "$BACKUP_AZURE_CONTAINER" \
        --name "$BACKUP_BLOB_NAME" \
        --file "/backup/$BACKUP_FILE_NAME" \
        --overwrite true \
        --content-type application/gzip \
        --no-progress \
        --only-show-errors >/dev/null'
}

upload_backup_to_azure() {
  case "${AZURE_UPLOAD_MODE}" in
    host-az)
      upload_backup_with_host_az
      ;;
    docker-az)
      upload_backup_with_docker_az
      ;;
    *)
      fail "Internal error: unsupported Azure upload mode '${AZURE_UPLOAD_MODE}'."
      ;;
  esac
}

# ─── Argument parsing ──────────────────────────────────────────────────────────

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

# ─── Env file check ────────────────────────────────────────────────────────────

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Missing env file: ${ENV_FILE}"
fi

# ─── Read common backup settings ──────────────────────────────────────────────

BACKUP_ENABLED="${BACKUP_ENABLED:-$(read_env_value BACKUP_ENABLED)}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-$(read_env_value BACKUP_FREQUENCY)}"
BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-$(read_env_value BACKUP_SCHEDULE)}"
BACKUP_LOCAL_RETENTION_COUNT="${BACKUP_LOCAL_RETENTION_COUNT:-$(read_env_value BACKUP_LOCAL_RETENTION_COUNT)}"
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-$(read_env_value BACKUP_AZURE_CONTAINER)}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-$(read_env_value BACKUP_AZURE_PREFIX)}"
BACKUP_WORK_DIR="${BACKUP_WORK_DIR:-$(read_env_value BACKUP_WORK_DIR)}"
BACKUP_AZURE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING:-$(read_env_value BACKUP_AZURE_CONNECTION_STRING)}"
BACKUP_DB_MODE="${BACKUP_DB_MODE:-$(read_env_value BACKUP_DB_MODE)}"

# ─── Read mode-specific settings ──────────────────────────────────────────────

# compose mode vars
POSTGRES_SERVICE="${POSTGRES_SERVICE:-$(read_env_value POSTGRES_SERVICE)}"
POSTGRES_DB="${POSTGRES_DB:-$(read_env_value POSTGRES_DB)}"
POSTGRES_USER="${POSTGRES_USER:-$(read_env_value POSTGRES_USER)}"
AZURE_STORAGE_CONNECTION_STRING_DEV="${AZURE_STORAGE_CONNECTION_STRING_DEV:-$(read_env_value AZURE_STORAGE_CONNECTION_STRING_DEV)}"

# external mode vars (read but never printed)
DB_URL="${DB_URL:-$(read_env_value DB_URL)}"
DB_USERNAME="${DB_USERNAME:-$(read_env_value DB_USERNAME)}"
DB_PASSWORD="${DB_PASSWORD:-$(read_env_value DB_PASSWORD)}"

# ─── Apply defaults ────────────────────────────────────────────────────────────

BACKUP_ENABLED="${BACKUP_ENABLED:-true}"
BACKUP_FREQUENCY="${BACKUP_FREQUENCY:-monthly}"
BACKUP_LOCAL_RETENTION_COUNT="${BACKUP_LOCAL_RETENTION_COUNT:-2}"
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-postgres-backups}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-dev/postgres}"
BACKUP_WORK_DIR="${BACKUP_WORK_DIR:-${DEPLOYMENT_DIR}/backups/postgres}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
BACKUP_DB_MODE="${BACKUP_DB_MODE:-compose}"
AZURE_CLI_DOCKER_IMAGE="mcr.microsoft.com/azure-cli"

BACKUP_AZURE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING:-${AZURE_STORAGE_CONNECTION_STRING_DEV:-}}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX#/}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX%/}"

if [[ "${BACKUP_WORK_DIR}" != /* ]]; then
  BACKUP_WORK_DIR="${REPO_DIR}/${BACKUP_WORK_DIR}"
fi

# ─── Enabled check ─────────────────────────────────────────────────────────────

if ! is_truthy "${BACKUP_ENABLED}" && ! is_truthy "${FORCE_RUN}"; then
  echo "BACKUP_ENABLED=false. Skipping PostgreSQL backup."
  exit 0
fi

# ─── Mode validation ───────────────────────────────────────────────────────────

case "${BACKUP_DB_MODE}" in
  compose|external)
    ;;
  *)
    fail "Unsupported BACKUP_DB_MODE='${BACKUP_DB_MODE}'. Valid values: compose, external."
    ;;
esac

# ─── Mode-specific requirement checks ─────────────────────────────────────────

if [[ "${BACKUP_DB_MODE}" == "external" ]]; then
  # Fail fast on missing required values — never print their contents
  [[ -z "${DB_URL}" ]]      && fail "external mode requires DB_URL to be set in ${ENV_FILE}."
  [[ -z "${DB_USERNAME}" ]] && fail "external mode requires DB_USERNAME to be set in ${ENV_FILE}."
  [[ -z "${DB_PASSWORD}" ]] && fail "external mode requires DB_PASSWORD to be set in ${ENV_FILE}."

  if ! command_exists pg_dump; then
    fail "pg_dump is required for external PostgreSQL backup. Install postgresql-client on the host."
  fi

  # Parse the database name from the JDBC URL
  # Format: jdbc:postgresql://host:port/dbname?params  OR  postgresql://host:port/dbname?params
  _pg_uri_for_parse="${DB_URL#jdbc:}"   # strip jdbc: prefix if present
  _pg_uri_path="${_pg_uri_for_parse#*://}"  # host:port/dbname?params
  _pg_uri_path="${_pg_uri_path#*/}"         # dbname?params
  BACKUP_DB_NAME="${_pg_uri_path%%\?*}"     # dbname (strip query string)

  if [[ -z "${BACKUP_DB_NAME}" ]]; then
    fail "Could not parse database name from DB_URL. Verify the URL format: jdbc:postgresql://host:port/dbname?params"
  fi
else
  # compose mode
  if [[ -z "${POSTGRES_DB}" || -z "${POSTGRES_USER}" ]]; then
    fail "compose mode requires POSTGRES_DB and POSTGRES_USER to be set in ${ENV_FILE}."
  fi

  if ! command_exists docker; then
    fail "Docker is not installed or not on PATH."
  fi

  BACKUP_DB_NAME="${POSTGRES_DB}"
fi

# ─── gzip check ───────────────────────────────────────────────────────────────

if ! command_exists gzip; then
  fail "gzip is required for compressed PostgreSQL backups."
fi

# ─── Azure upload mode ────────────────────────────────────────────────────────

if command_exists az; then
  AZURE_UPLOAD_MODE="host-az"
elif command_exists docker; then
  AZURE_UPLOAD_MODE="docker-az"
else
  fail "Azure Blob upload requires either local Azure CLI ('az') or Docker to run ${AZURE_CLI_DOCKER_IMAGE}."
fi

# ─── Common validation ────────────────────────────────────────────────────────

if [[ -z "${BACKUP_AZURE_CONNECTION_STRING}" ]]; then
  fail "Set BACKUP_AZURE_CONNECTION_STRING in ${ENV_FILE}."
fi

if ! [[ "${BACKUP_LOCAL_RETENTION_COUNT}" =~ ^[0-9]+$ ]]; then
  fail "BACKUP_LOCAL_RETENTION_COUNT must be a non-negative integer."
fi

# ─── Prepare work directory ───────────────────────────────────────────────────

mkdir -p "${BACKUP_WORK_DIR}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_BASENAME="barter-${BACKUP_DB_NAME}-${TIMESTAMP}.dump.gz"
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

# ─── Summary (no secrets printed) ─────────────────────────────────────────────

echo "Backup mode:        ${BACKUP_DB_MODE}"
if [[ "${BACKUP_DB_MODE}" == "compose" ]]; then
  echo "Using compose file: ${COMPOSE_FILE}"
fi
echo "Using env file:     ${ENV_FILE}"
echo "Backup frequency:   ${BACKUP_FREQUENCY}${BACKUP_SCHEDULE:+ (schedule: ${BACKUP_SCHEDULE})}"
echo "Work directory:     ${BACKUP_WORK_DIR}"
echo "Azure container:    ${BACKUP_AZURE_CONTAINER}"
echo "Azure blob path:    ${BACKUP_BLOB_NAME}"
echo "Azure CLI mode:     ${AZURE_UPLOAD_MODE}"
if [[ "${BACKUP_DB_MODE}" == "external" ]]; then
  echo "Database name:      ${BACKUP_DB_NAME}"
fi
echo "Creating compressed PostgreSQL backup: ${BACKUP_FILE}"

# ─── Run the backup dump ──────────────────────────────────────────────────────

if [[ "${BACKUP_DB_MODE}" == "external" ]]; then
  # Strip jdbc: prefix so pg_dump receives a valid libpq connection URI
  _pg_conn_uri="${DB_URL#jdbc:}"

  # PGPASSWORD is passed via environment — never echoed or logged
  PGPASSWORD="${DB_PASSWORD}" pg_dump \
    --dbname="${_pg_conn_uri}" \
    --username="${DB_USERNAME}" \
    --format=custom \
    --no-owner \
    --no-privileges \
    | gzip -c > "${TEMP_BACKUP_FILE}"
else
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T \
    -e BACKUP_POSTGRES_DB="${POSTGRES_DB}" \
    -e BACKUP_POSTGRES_USER="${POSTGRES_USER}" \
    "${POSTGRES_SERVICE}" \
    sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h localhost -U "$BACKUP_POSTGRES_USER" -d "$BACKUP_POSTGRES_DB" --format=custom --no-owner --no-privileges' \
    | gzip -c > "${TEMP_BACKUP_FILE}"
fi

mv "${TEMP_BACKUP_FILE}" "${BACKUP_FILE}"

# ─── Upload and retention ─────────────────────────────────────────────────────

upload_backup_to_azure

echo "Upload complete. Applying local retention policy..."
cleanup_local_backups "${BACKUP_LOCAL_RETENTION_COUNT}" "barter-${BACKUP_DB_NAME}-*.dump.gz"

echo "Backup complete."
if [[ -f "${BACKUP_FILE}" ]]; then
  echo "Local file retained: ${BACKUP_FILE}"
else
  echo "Local file retained: none (removed by retention policy)"
fi
echo "Azure blob:          ${BACKUP_BLOB_NAME}"
echo
if [[ "${BACKUP_DB_MODE}" == "compose" ]]; then
  echo "Restore example (compose/dev):"
  echo "  ./deployment/scripts/restore-db.sh --file ${BACKUP_FILE} --target-db ${BACKUP_DB_NAME}_restore_test --recreate-target-db --yes"
else
  echo "Restore reference (external/prod):"
  echo "  See deployment/docs/PRODUCTION_BACKUP.md for the production restore procedure."
fi
