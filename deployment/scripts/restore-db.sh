#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml}"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"

usage() {
  cat <<EOF
Usage: $(basename "$0") --file <backup.dump.gz> [--target-db <name>] [--recreate-target-db] [--allow-primary-db] --yes

Restores a compressed PostgreSQL backup created by backup-db.sh.

Options:
  --file <path>            Path to a local .dump.gz backup file.
  --target-db <name>       Database to restore into. Defaults to POSTGRES_DB from the env file.
  --recreate-target-db     Drop and recreate the target database before restoring.
  --allow-primary-db       Required when recreating the primary application database.
  --yes                    Confirm the restore operation.
  --help                   Show this help.
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

BACKUP_FILE=""
TARGET_DB=""
RECREATE_TARGET_DB="false"
ALLOW_PRIMARY_DB="false"
CONFIRMED="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --file)
      BACKUP_FILE="${2:-}"
      shift 2
      ;;
    --target-db)
      TARGET_DB="${2:-}"
      shift 2
      ;;
    --recreate-target-db)
      RECREATE_TARGET_DB="true"
      shift
      ;;
    --allow-primary-db)
      ALLOW_PRIMARY_DB="true"
      shift
      ;;
    --yes)
      CONFIRMED="true"
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

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed or not on PATH."
fi

if ! command -v gzip >/dev/null 2>&1; then
  fail "gzip is required to restore compressed PostgreSQL backups."
fi

if [[ -z "${BACKUP_FILE}" ]]; then
  usage >&2
  fail "--file is required."
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
  fail "Backup file not found: ${BACKUP_FILE}"
fi

POSTGRES_SERVICE="${POSTGRES_SERVICE:-$(read_env_value POSTGRES_SERVICE)}"
POSTGRES_DB="${POSTGRES_DB:-$(read_env_value POSTGRES_DB)}"
POSTGRES_USER="${POSTGRES_USER:-$(read_env_value POSTGRES_USER)}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
TARGET_DB="${TARGET_DB:-${POSTGRES_DB}}"

if [[ -z "${POSTGRES_DB}" || -z "${POSTGRES_USER}" ]]; then
  fail "POSTGRES_DB and POSTGRES_USER must be set in ${ENV_FILE}."
fi

if [[ "${CONFIRMED}" != "true" ]]; then
  fail "Restore not confirmed. Re-run with --yes after reviewing the target database."
fi

if [[ "${RECREATE_TARGET_DB}" == "true" && "${TARGET_DB}" == "${POSTGRES_DB}" && "${ALLOW_PRIMARY_DB}" != "true" ]]; then
  fail "Refusing to recreate the primary application database without --allow-primary-db."
fi

echo "Using compose file: ${COMPOSE_FILE}"
echo "Using env file:     ${ENV_FILE}"
echo "Backup file:        ${BACKUP_FILE}"
echo "Target database:    ${TARGET_DB}"

if [[ "${RECREATE_TARGET_DB}" == "true" ]]; then
  echo "Dropping and recreating target database: ${TARGET_DB}"
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T \
    -e RESTORE_TARGET_DB="${TARGET_DB}" \
    -e RESTORE_POSTGRES_USER="${POSTGRES_USER}" \
    "${POSTGRES_SERVICE}" \
    sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" dropdb --if-exists -h localhost -U "$RESTORE_POSTGRES_USER" "$RESTORE_TARGET_DB" && PGPASSWORD="$POSTGRES_PASSWORD" createdb -h localhost -U "$RESTORE_POSTGRES_USER" "$RESTORE_TARGET_DB"'
fi

echo "Restoring PostgreSQL backup into ${TARGET_DB}..."
gzip -dc "${BACKUP_FILE}" | docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T \
  -e RESTORE_TARGET_DB="${TARGET_DB}" \
  -e RESTORE_POSTGRES_USER="${POSTGRES_USER}" \
  "${POSTGRES_SERVICE}" \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h localhost -U "$RESTORE_POSTGRES_USER" -d "$RESTORE_TARGET_DB" --clean --if-exists --no-owner --no-privileges --single-transaction'

echo "Restore complete."

