#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"
BACKUP_DIR="${BACKUP_DIR:-${DEPLOYMENT_DIR}/backups}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  exit 1
fi

read_env_value() {
  local key="$1"
  grep -E "^[[:space:]]*${key}[[:space:]]*=" "${ENV_FILE}" | tail -n 1 | cut -d '=' -f 2-
}

POSTGRES_DB="${POSTGRES_DB:-$(read_env_value POSTGRES_DB)}"
POSTGRES_USER="${POSTGRES_USER:-$(read_env_value POSTGRES_USER)}"

if [[ -z "${POSTGRES_DB}" || -z "${POSTGRES_USER}" ]]; then
  echo "POSTGRES_DB and POSTGRES_USER must be set in ${ENV_FILE}." >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/barter-${POSTGRES_DB}-${TIMESTAMP}.dump"

echo "Creating PostgreSQL backup: ${BACKUP_FILE}"
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" exec -T postgres \
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-privileges' \
  > "${BACKUP_FILE}"

echo "Backup complete."
echo
echo "Restore example:"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} exec -T postgres sh -c 'PGPASSWORD=\"\$POSTGRES_PASSWORD\" pg_restore -h localhost -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" --clean --if-exists --no-owner --no-privileges' < ${BACKUP_FILE}"

