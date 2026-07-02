#!/usr/bin/env bash
# restore-db.sh — PostgreSQL backup restore for Barter Platform
#
# Downloads a backup from Azure Blob Storage and restores it via a Docker
# PostgreSQL client image. Safe by default: restores to a test database unless
# --restore-to targets the production database (which requires additional flags
# and interactive confirmation).
#
# See deployment/docs/PRODUCTION_RESTORE.md for full usage documentation.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/prod.env}"
RESTORE_WORK_DIR="${RESTORE_WORK_DIR:-/tmp/barter-restore}"
AZURE_CLI_DOCKER_IMAGE="mcr.microsoft.com/azure-cli"

# ─── Flags ─────────────────────────────────────────────────────────────────────
ACTION_LIST="false"
ACTION_DOWNLOAD=""
ACTION_LATEST="false"
RESTORE_TO=""
BACKUP_FILE_ARG=""
VERIFY_ONLY="false"
ALLOW_PRODUCTION_RESTORE="false"
DRY_RUN="false"

# Set by runtime logic — must be declared before the EXIT trap is registered
LOCAL_DUMP_FILE=""

# ─── Usage ─────────────────────────────────────────────────────────────────────
usage() {
  cat <<'EOF'
Usage: restore-db.sh [OPTIONS]

Downloads and restores a PostgreSQL backup from Azure Blob Storage.
Default target: barter_restore_test on the same host as the production DB.

Source options (pick exactly one unless using --list):
  --list                      List available backup blobs and exit.
  --latest                    Auto-select the most recent backup blob.
  --download <blob-name>      Download a specific blob (full blob path).
  --backup-file <path>        Use a local .dump.gz file (skip Azure download).

Restore options:
  --restore-to <db-url>       Target connection URL (postgresql://host:port/dbname).
                              Credentials are always read from the env file.
                              Default: barter_restore_test on the production host.
  --verify-only               Download and inspect backup structure (pg_restore --list).
                              Does NOT restore into any database.
  --allow-production-restore  Required when --restore-to targets the production database.
                              Will also prompt for the confirmation phrase.
  --dry-run                   Print what would happen — no download or restore performed.
  --env-file <path>           Path to the env file (default: deployment/env/prod.env).
  --help                      Show this help.

Environment variables read from the env file:
  BACKUP_AZURE_CONNECTION_STRING   Azure Storage connection string (required)
  BACKUP_AZURE_CONTAINER           Blob container (default: postgres-backups)
  BACKUP_AZURE_PREFIX              Blob prefix    (default: prod/postgres)
  DB_URL                           Production JDBC database URL (required)
  DB_USERNAME                      Database username (required)
  DB_PASSWORD                      Database password (required)
  POSTGRES_CLIENT_DOCKER_IMAGE     Docker image for pg_restore (default: postgres:18)

Examples:
  List available backups:
    ./restore-db.sh --list

  Restore latest backup to test database (recommended first step):
    ./restore-db.sh --latest

  Restore specific blob to test database:
    ./restore-db.sh --download prod/postgres/barter-barter_db-20260701T030000Z.dump.gz

  Use a local file (no Azure download):
    ./restore-db.sh --backup-file /var/backups/barter/barter-barter_db-20260701T030000Z.dump.gz

  Verify backup structure without restoring:
    ./restore-db.sh --latest --verify-only

  Emergency production restore (deliberate — read the docs first):
    ./restore-db.sh --latest \
      --restore-to postgresql://your-server.postgres.database.azure.com:5432/barter_db \
      --allow-production-restore

EOF
}

# ─── Helpers ───────────────────────────────────────────────────────────────────

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

log() {
  echo "$*"
}

step() {
  echo ""
  echo "── $* ──────────────────────────────────────────────────────────"
}

read_env_value() {
  local key="$1"
  local value
  value="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "${ENV_FILE}" 2>/dev/null \
    | tail -n 1 | cut -d '=' -f 2- || true)"
  value="${value%$'\r'}"
  if [[ "${value}" =~ ^\".*\"$ ]] || [[ "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:-1}"
  fi
  printf '%s' "${value}"
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Extract host, port, or dbname from a PostgreSQL connection URL.
# Accepts jdbc:postgresql:// or postgresql:// with or without userinfo.
parse_pg_url() {
  local url="$1"
  local field="$2"

  # Strip jdbc: prefix
  local uri="${url#jdbc:}"
  # Strip scheme
  local rest="${uri#*://}"
  # Strip userinfo (user:pass@) if present
  local hostport_path
  if [[ "${rest}" == *"@"* ]]; then
    hostport_path="${rest#*@}"
  else
    hostport_path="${rest}"
  fi

  # hostport_path: host:port/dbname?params  OR  host/dbname?params
  local host="${hostport_path%%:*}"
  host="${host%%/*}"

  local port=""
  if [[ "${hostport_path}" == *":"* ]]; then
    local after_colon="${hostport_path#*:}"
    port="${after_colon%%/*}"
    port="${port%%\?*}"
  fi

  local dbname="${hostport_path#*/}"
  dbname="${dbname%%\?*}"

  case "$field" in
    host)   printf '%s' "${host}" ;;
    port)   printf '%s' "${port:-5432}" ;;
    dbname) printf '%s' "${dbname}" ;;
  esac
}

# ─── Argument parsing ──────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list)
      ACTION_LIST="true"
      shift
      ;;
    --download)
      ACTION_DOWNLOAD="${2:-}"
      [[ -z "${ACTION_DOWNLOAD}" ]] && { usage >&2; fail "--download requires a blob name."; }
      shift 2
      ;;
    --latest)
      ACTION_LATEST="true"
      shift
      ;;
    --backup-file)
      BACKUP_FILE_ARG="${2:-}"
      [[ -z "${BACKUP_FILE_ARG}" ]] && { usage >&2; fail "--backup-file requires a file path."; }
      shift 2
      ;;
    --restore-to)
      RESTORE_TO="${2:-}"
      [[ -z "${RESTORE_TO}" ]] && { usage >&2; fail "--restore-to requires a database URL."; }
      shift 2
      ;;
    --verify-only)
      VERIFY_ONLY="true"
      shift
      ;;
    --allow-production-restore)
      ALLOW_PRODUCTION_RESTORE="true"
      shift
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --env-file)
      ENV_FILE="${2:-}"
      [[ -z "${ENV_FILE}" ]] && { usage >&2; fail "--env-file requires a path."; }
      shift 2
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

# ─── Env file ──────────────────────────────────────────────────────────────────

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Missing env file: ${ENV_FILE}"
fi

# ─── Read env vars (secrets never printed) ─────────────────────────────────────

BACKUP_AZURE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING:-$(read_env_value BACKUP_AZURE_CONNECTION_STRING)}"
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-$(read_env_value BACKUP_AZURE_CONTAINER)}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-$(read_env_value BACKUP_AZURE_PREFIX)}"
DB_URL="${DB_URL:-$(read_env_value DB_URL)}"
DB_USERNAME="${DB_USERNAME:-$(read_env_value DB_USERNAME)}"
DB_PASSWORD="${DB_PASSWORD:-$(read_env_value DB_PASSWORD)}"
POSTGRES_CLIENT_DOCKER_IMAGE="${POSTGRES_CLIENT_DOCKER_IMAGE:-$(read_env_value POSTGRES_CLIENT_DOCKER_IMAGE)}"

# Apply defaults
BACKUP_AZURE_CONTAINER="${BACKUP_AZURE_CONTAINER:-postgres-backups}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX:-prod/postgres}"
POSTGRES_CLIENT_DOCKER_IMAGE="${POSTGRES_CLIENT_DOCKER_IMAGE:-postgres:18}"

# Normalise prefix: strip leading and trailing slashes
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX#/}"
BACKUP_AZURE_PREFIX="${BACKUP_AZURE_PREFIX%/}"

# ─── Prerequisites ─────────────────────────────────────────────────────────────

# All non-list operations require Docker to run pg_restore / psql
if [[ "${ACTION_LIST}" != "true" ]]; then
  if ! command_exists docker; then
    fail "Docker is required (runs ${POSTGRES_CLIENT_DOCKER_IMAGE} for pg_restore). Install Docker."
  fi
fi

# gzip is required to decompress .dump.gz (skip check for dry-run and verify-only)
if [[ "${ACTION_LIST}" != "true" && "${VERIFY_ONLY}" != "true" && "${DRY_RUN}" != "true" ]]; then
  if ! command_exists gzip; then
    fail "gzip is required to decompress .dump.gz backup files."
  fi
fi

# ─── Azure CLI mode detection ──────────────────────────────────────────────────

az_mode() {
  if command_exists az; then
    echo "host-az"
  elif command_exists docker; then
    echo "docker-az"
  else
    fail "Azure CLI operations require 'az' CLI or Docker (for ${AZURE_CLI_DOCKER_IMAGE})."
  fi
}

# ─── MODE: LIST ────────────────────────────────────────────────────────────────

if [[ "${ACTION_LIST}" == "true" ]]; then
  [[ -z "${BACKUP_AZURE_CONNECTION_STRING}" ]] && \
    fail "BACKUP_AZURE_CONNECTION_STRING is not set in ${ENV_FILE}."

  log "Listing backup blobs in ${BACKUP_AZURE_CONTAINER}/${BACKUP_AZURE_PREFIX}/"
  log ""

  if [[ "$(az_mode)" == "host-az" ]]; then
    AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
    az storage blob list \
      --container-name "${BACKUP_AZURE_CONTAINER}" \
      --prefix "${BACKUP_AZURE_PREFIX}/" \
      --query "[].{Name:name,LastModified:properties.lastModified,Bytes:properties.contentLength}" \
      --output table
  else
    AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
    _AZ_CONTAINER="${BACKUP_AZURE_CONTAINER}" \
    _AZ_PREFIX="${BACKUP_AZURE_PREFIX}" \
    docker run --rm \
      -e AZURE_STORAGE_CONNECTION_STRING \
      -e _AZ_CONTAINER \
      -e _AZ_PREFIX \
      "${AZURE_CLI_DOCKER_IMAGE}" \
      sh -c 'az storage blob list \
        --container-name "$_AZ_CONTAINER" \
        --prefix "$_AZ_PREFIX/" \
        --query "[].{Name:name,LastModified:properties.lastModified,Bytes:properties.contentLength}" \
        --output table'
  fi
  exit 0
fi

# ─── Validate source flags ─────────────────────────────────────────────────────

SOURCE_COUNT=0
[[ -n "${ACTION_DOWNLOAD}" ]]       && SOURCE_COUNT=$(( SOURCE_COUNT + 1 ))
[[ "${ACTION_LATEST}" == "true" ]]  && SOURCE_COUNT=$(( SOURCE_COUNT + 1 ))
[[ -n "${BACKUP_FILE_ARG}" ]]       && SOURCE_COUNT=$(( SOURCE_COUNT + 1 ))

if [[ "${SOURCE_COUNT}" -eq 0 ]]; then
  usage >&2
  fail "Specify one of: --latest, --download <blob>, or --backup-file <path>."
fi

if [[ "${SOURCE_COUNT}" -gt 1 ]]; then
  fail "--latest, --download, and --backup-file are mutually exclusive."
fi

# ─── Required env var checks ───────────────────────────────────────────────────

[[ -z "${DB_URL}" ]]      && fail "DB_URL is not set in ${ENV_FILE}."
[[ -z "${DB_USERNAME}" ]] && fail "DB_USERNAME is not set in ${ENV_FILE}."
[[ -z "${DB_PASSWORD}" ]] && fail "DB_PASSWORD is not set in ${ENV_FILE}."

if [[ -z "${BACKUP_FILE_ARG}" ]]; then
  [[ -z "${BACKUP_AZURE_CONNECTION_STRING}" ]] && \
    fail "BACKUP_AZURE_CONNECTION_STRING is not set in ${ENV_FILE}."
fi

# ─── Resolve backup source ─────────────────────────────────────────────────────

BLOB_USED="(local file)"
LOCAL_DUMP_GZ=""

if [[ -n "${BACKUP_FILE_ARG}" ]]; then
  [[ ! -f "${BACKUP_FILE_ARG}" ]] && fail "Backup file not found: ${BACKUP_FILE_ARG}"
  LOCAL_DUMP_GZ="${BACKUP_FILE_ARG}"
  BLOB_USED="(local file: ${BACKUP_FILE_ARG})"
else
  # Resolve --latest to a concrete blob name
  if [[ "${ACTION_LATEST}" == "true" ]]; then
    step "Finding latest backup blob"

    if [[ "${DRY_RUN}" == "true" ]]; then
      log "[dry-run] Would query latest blob from ${BACKUP_AZURE_CONTAINER}/${BACKUP_AZURE_PREFIX}/"
      ACTION_DOWNLOAD="${BACKUP_AZURE_PREFIX}/barter-barter_db-LATEST.dump.gz"
    else
      if [[ "$(az_mode)" == "host-az" ]]; then
        ACTION_DOWNLOAD="$(
          AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
          az storage blob list \
            --container-name "${BACKUP_AZURE_CONTAINER}" \
            --prefix "${BACKUP_AZURE_PREFIX}/" \
            --query "sort_by([], &properties.lastModified)[-1].name" \
            --output tsv
        )"
      else
        ACTION_DOWNLOAD="$(
          AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
          _AZ_CONTAINER="${BACKUP_AZURE_CONTAINER}" \
          _AZ_PREFIX="${BACKUP_AZURE_PREFIX}" \
          docker run --rm \
            -e AZURE_STORAGE_CONNECTION_STRING \
            -e _AZ_CONTAINER \
            -e _AZ_PREFIX \
            "${AZURE_CLI_DOCKER_IMAGE}" \
            sh -c 'az storage blob list \
              --container-name "$_AZ_CONTAINER" \
              --prefix "$_AZ_PREFIX/" \
              --query "sort_by([], &properties.lastModified)[-1].name" \
              --output tsv'
        )"
      fi
      # Strip trailing carriage-return / whitespace that Azure CLI can produce
      ACTION_DOWNLOAD="$(printf '%s' "${ACTION_DOWNLOAD}" | tr -d '\r' | awk 'NF{print;exit}')"
      [[ -z "${ACTION_DOWNLOAD}" ]] && \
        fail "No backup blobs found in ${BACKUP_AZURE_CONTAINER}/${BACKUP_AZURE_PREFIX}/. Run --list to check."
      log "Latest blob: ${ACTION_DOWNLOAD}"
    fi
  fi

  # Download the resolved blob
  BLOB_USED="${ACTION_DOWNLOAD}"
  BLOB_BASENAME="$(basename "${ACTION_DOWNLOAD}")"
  mkdir -p "${RESTORE_WORK_DIR}"
  LOCAL_DUMP_GZ="${RESTORE_WORK_DIR}/${BLOB_BASENAME}"

  step "Downloading backup blob"
  log "  Blob:        ${ACTION_DOWNLOAD}"
  log "  Container:   ${BACKUP_AZURE_CONTAINER}"
  log "  Destination: ${LOCAL_DUMP_GZ}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    log "[dry-run] Would download blob to ${LOCAL_DUMP_GZ}"
  else
    if [[ "$(az_mode)" == "host-az" ]]; then
      AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
      az storage blob download \
        --container-name "${BACKUP_AZURE_CONTAINER}" \
        --name "${ACTION_DOWNLOAD}" \
        --file "${LOCAL_DUMP_GZ}" \
        --no-progress \
        --only-show-errors
    else
      _DL_DIR="$(dirname "${LOCAL_DUMP_GZ}")"
      _DL_BASE="$(basename "${LOCAL_DUMP_GZ}")"
      AZURE_STORAGE_CONNECTION_STRING="${BACKUP_AZURE_CONNECTION_STRING}" \
      _AZ_CONTAINER="${BACKUP_AZURE_CONTAINER}" \
      _AZ_BLOB="${ACTION_DOWNLOAD}" \
      _AZ_DEST="${_DL_BASE}" \
      docker run --rm \
        -e AZURE_STORAGE_CONNECTION_STRING \
        -e _AZ_CONTAINER \
        -e _AZ_BLOB \
        -e _AZ_DEST \
        -v "${_DL_DIR}:/download" \
        "${AZURE_CLI_DOCKER_IMAGE}" \
        sh -c 'az storage blob download \
          --container-name "$_AZ_CONTAINER" \
          --name "$_AZ_BLOB" \
          --file "/download/$_AZ_DEST" \
          --no-progress \
          --only-show-errors'
    fi

    [[ ! -f "${LOCAL_DUMP_GZ}" ]] && fail "Download failed — file not found: ${LOCAL_DUMP_GZ}"
    log "Download complete."
  fi
fi

# ─── Determine restore target ──────────────────────────────────────────────────

PROD_HOST="$(parse_pg_url "${DB_URL}" host)"
PROD_PORT="$(parse_pg_url "${DB_URL}" port)"
PROD_DBNAME="$(parse_pg_url "${DB_URL}" dbname)"

if [[ -z "${RESTORE_TO}" ]]; then
  # Default: test database on the production host — never the production DB itself
  RESTORE_TO="postgresql://${PROD_HOST}:${PROD_PORT}/barter_restore_test?sslmode=require"
fi

TARGET_HOST="$(parse_pg_url "${RESTORE_TO}" host)"
TARGET_PORT="$(parse_pg_url "${RESTORE_TO}" port)"
TARGET_DBNAME="$(parse_pg_url "${RESTORE_TO}" dbname)"

# ─── Production restore guard ──────────────────────────────────────────────────

IS_PRODUCTION_RESTORE="false"
if [[ "${TARGET_HOST}" == "${PROD_HOST}" && "${TARGET_DBNAME}" == "${PROD_DBNAME}" ]]; then
  IS_PRODUCTION_RESTORE="true"
fi

if [[ "${IS_PRODUCTION_RESTORE}" == "true" ]]; then
  if [[ "${ALLOW_PRODUCTION_RESTORE}" != "true" ]]; then
    echo "" >&2
    echo "  ╔═══════════════════════════════════════════════════════════════╗" >&2
    echo "  ║  PRODUCTION RESTORE BLOCKED                                   ║" >&2
    echo "  ╠═══════════════════════════════════════════════════════════════╣" >&2
    echo "  ║  Target host:     ${TARGET_HOST}" >&2
    echo "  ║  Target database: ${TARGET_DBNAME}" >&2
    echo "  ║                                                               ║" >&2
    echo "  ║  This matches the production database in:                     ║" >&2
    echo "  ║    ${ENV_FILE}" >&2
    echo "  ║                                                               ║" >&2
    echo "  ║  To override, add --allow-production-restore to the command.  ║" >&2
    echo "  ║  You will be prompted for interactive confirmation.           ║" >&2
    echo "  ║                                                               ║" >&2
    echo "  ║  Read deployment/docs/PRODUCTION_RESTORE.md before           ║" >&2
    echo "  ║  performing an emergency production restore.                  ║" >&2
    echo "  ╚═══════════════════════════════════════════════════════════════╝" >&2
    echo "" >&2
    fail "Production restore requires --allow-production-restore."
  fi

  if [[ "${DRY_RUN}" != "true" ]]; then
    echo ""
    echo "  ┌───────────────────────────────────────────────────────────────┐"
    echo "  │  ⚠️   WARNING: PRODUCTION DATABASE RESTORE                     │"
    echo "  ├───────────────────────────────────────────────────────────────┤"
    echo "  │  Host:     ${TARGET_HOST}"
    echo "  │  Database: ${TARGET_DBNAME}"
    echo "  │                                                               │"
    echo "  │  This will OVERWRITE all production data with the backup.     │"
    echo "  │  Ensure the backend container is stopped before proceeding:   │"
    echo "  │                                                               │"
    echo "  │    docker compose -f deployment/compose/docker-compose.prod.yml \\"
    echo "  │      --env-file deployment/env/prod.env stop backend          │"
    echo "  │                                                               │"
    echo "  │  Type exactly (case-sensitive) and press Enter to confirm:    │"
    echo "  │                                                               │"
    echo "  │    RESTORE PRODUCTION DATABASE                                │"
    echo "  │                                                               │"
    echo "  │  Press Ctrl+C to abort.                                       │"
    echo "  └───────────────────────────────────────────────────────────────┘"
    echo ""
    printf "Confirmation: "
    read -r CONFIRMATION
    if [[ "${CONFIRMATION}" != "RESTORE PRODUCTION DATABASE" ]]; then
      fail "Confirmation phrase did not match. Aborting. No data was changed."
    fi
    echo "Confirmation accepted. Proceeding with production restore."
  else
    log "[dry-run] Would prompt for confirmation phrase: RESTORE PRODUCTION DATABASE"
  fi
fi

# ─── Dry-run summary and exit ──────────────────────────────────────────────────

if [[ "${DRY_RUN}" == "true" ]]; then
  echo ""
  echo "══════════════════════════════════════════════════════════════════════"
  echo "  DRY RUN — no changes will be made"
  echo "══════════════════════════════════════════════════════════════════════"
  echo "  Blob / source:   ${BLOB_USED}"
  echo "  Local .dump.gz:  ${LOCAL_DUMP_GZ}"
  echo "  Target host:     ${TARGET_HOST}:${TARGET_PORT}"
  echo "  Target database: ${TARGET_DBNAME}"
  echo "  Is production:   ${IS_PRODUCTION_RESTORE}"
  echo "  Verify only:     ${VERIFY_ONLY}"
  echo "  PG client image: ${POSTGRES_CLIENT_DOCKER_IMAGE}"
  echo "══════════════════════════════════════════════════════════════════════"
  exit 0
fi

# ─── Decompress backup ─────────────────────────────────────────────────────────

step "Decompressing backup"

LOCAL_DUMP_FILE="${LOCAL_DUMP_GZ%.gz}"
log "  Source: ${LOCAL_DUMP_GZ}"
log "  Output: ${LOCAL_DUMP_FILE}"

gzip -dc "${LOCAL_DUMP_GZ}" > "${LOCAL_DUMP_FILE}"
log "Decompression complete."

# Always clean up the uncompressed .dump on exit (it can be large)
cleanup_dump_file() {
  if [[ -n "${LOCAL_DUMP_FILE}" && -f "${LOCAL_DUMP_FILE}" ]]; then
    rm -f "${LOCAL_DUMP_FILE}"
  fi
}
trap cleanup_dump_file EXIT

DUMP_DIR="$(dirname "${LOCAL_DUMP_FILE}")"
DUMP_BASE="$(basename "${LOCAL_DUMP_FILE}")"

# ─── MODE: VERIFY ONLY ─────────────────────────────────────────────────────────
# Lists the backup object catalog via pg_restore --list.
# Does NOT connect to any database or modify any data.

if [[ "${VERIFY_ONLY}" == "true" ]]; then
  step "Verifying backup structure (pg_restore --list)"
  log "  File:  ${LOCAL_DUMP_FILE}"
  log "  Image: ${POSTGRES_CLIENT_DOCKER_IMAGE}"
  echo ""

  docker run --rm \
    --entrypoint sh \
    -e PG_DUMP_FILE="/restore/${DUMP_BASE}" \
    -v "${DUMP_DIR}:/restore:ro" \
    "${POSTGRES_CLIENT_DOCKER_IMAGE}" \
    -c 'pg_restore --list "$PG_DUMP_FILE"'

  echo ""
  echo "══════════════════════════════════════════════════════════════════════"
  echo "  VERIFY SUMMARY"
  echo "══════════════════════════════════════════════════════════════════════"
  echo "  Backup blob:  ${BLOB_USED}"
  echo "  Local file:   ${LOCAL_DUMP_GZ}"
  echo "  Verification: pg_restore --list passed — backup is structurally valid"
  echo "══════════════════════════════════════════════════════════════════════"
  echo ""
  echo "  For a full data verification (restore + row-count validation):"
  echo "    ./restore-db.sh --latest"
  echo "    ./restore-db.sh --backup-file ${LOCAL_DUMP_GZ}"
  exit 0
fi

# ─── Restore ───────────────────────────────────────────────────────────────────

step "Running pg_restore"
log "  Target: ${TARGET_HOST}:${TARGET_PORT}/${TARGET_DBNAME}"
log "  Image:  ${POSTGRES_CLIENT_DOCKER_IMAGE}"

RESTORE_START="$(date +%s)"

docker run --rm \
  --entrypoint sh \
  -e PGPASSWORD="${DB_PASSWORD}" \
  -e PG_HOST="${TARGET_HOST}" \
  -e PG_PORT="${TARGET_PORT}" \
  -e PG_USER="${DB_USERNAME}" \
  -e PG_DBNAME="${TARGET_DBNAME}" \
  -e PG_DUMP_FILE="/restore/${DUMP_BASE}" \
  -v "${DUMP_DIR}:/restore:ro" \
  "${POSTGRES_CLIENT_DOCKER_IMAGE}" \
  -c 'pg_restore \
    --host="$PG_HOST" \
    --port="$PG_PORT" \
    --username="$PG_USER" \
    --dbname="$PG_DBNAME" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    --single-transaction \
    "$PG_DUMP_FILE"'

RESTORE_END="$(date +%s)"
RESTORE_DURATION=$(( RESTORE_END - RESTORE_START ))
log "pg_restore complete (${RESTORE_DURATION}s)."

# ─── Validation queries ────────────────────────────────────────────────────────

step "Running validation queries on ${TARGET_DBNAME}"

run_count_query() {
  local label="$1"
  local query="$2"
  local result
  result="$(
    docker run --rm \
      --entrypoint sh \
      -e PGPASSWORD="${DB_PASSWORD}" \
      -e PG_HOST="${TARGET_HOST}" \
      -e PG_PORT="${TARGET_PORT}" \
      -e PG_USER="${DB_USERNAME}" \
      -e PG_DBNAME="${TARGET_DBNAME}" \
      -e PG_QUERY="${query}" \
      "${POSTGRES_CLIENT_DOCKER_IMAGE}" \
      -c 'psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --dbname="$PG_DBNAME" \
        --tuples-only \
        --no-align \
        -c "$PG_QUERY" 2>/dev/null' \
    | tr -d '[:space:]'
  )" || result="ERROR"
  result="${result:-N/A}"
  log "  ${label}: ${result}"
  printf '%s' "${result}"
}

COUNT_USERS="$(run_count_query        "users                 " "SELECT COUNT(*) FROM users;")"
COUNT_CATEGORIES="$(run_count_query   "categories            " "SELECT COUNT(*) FROM categories;")"
COUNT_TAGS="$(run_count_query         "tags                  " "SELECT COUNT(*) FROM tags;")"
COUNT_FLYWAY="$(run_count_query       "flyway_schema_history " "SELECT COUNT(*) FROM flyway_schema_history;")"

# ─── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo "══════════════════════════════════════════════════════════════════════"
echo "  RESTORE SUMMARY"
echo "══════════════════════════════════════════════════════════════════════"
echo "  Backup blob:       ${BLOB_USED}"
echo "  Local file:        ${LOCAL_DUMP_GZ}"
echo "  Target host:       ${TARGET_HOST}:${TARGET_PORT}"
echo "  Target database:   ${TARGET_DBNAME}"
echo "  Restore duration:  ${RESTORE_DURATION}s"
echo "  ────────────────────────────────────────────────────────────────────"
echo "  Validation counts:"
echo "    users:                    ${COUNT_USERS}"
echo "    categories:               ${COUNT_CATEGORIES}"
echo "    tags:                     ${COUNT_TAGS}"
echo "    flyway_schema_history:    ${COUNT_FLYWAY}"
echo "══════════════════════════════════════════════════════════════════════"

if [[ "${IS_PRODUCTION_RESTORE}" == "true" ]]; then
  echo ""
  echo "  ⚠️  Production restore complete. Restart the backend to resume traffic:"
  echo ""
  echo "    docker compose -f deployment/compose/docker-compose.prod.yml \\"
  echo "      --env-file deployment/env/prod.env start backend"
  echo ""
  echo "  Verify application health:"
  echo "    curl -sf https://app.zameni.rs/api/v1/actuator/health/readiness | jq .status"
else
  echo ""
  echo "  Test database restore complete."
  echo "  Inspect the data, then clean up the test database when finished:"
  echo ""
  echo "    PGPASSWORD='<DB_PASSWORD>' psql \\"
  echo "      --host=${TARGET_HOST} --username=${DB_USERNAME} --dbname=postgres \\"
  echo "      -c 'DROP DATABASE IF EXISTS \"${TARGET_DBNAME}\";'"
fi
