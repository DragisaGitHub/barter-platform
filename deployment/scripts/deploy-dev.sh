#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOYMENT_DIR}/compose/docker-compose.dev.yml"
ENV_FILE="${ENV_FILE:-${DEPLOYMENT_DIR}/env/dev.env}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  echo "Create it from ${DEPLOYMENT_DIR}/env/dev.env.example and fill DEV secrets on the server." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or not on PATH." >&2
  exit 1
fi

echo "Using compose file: ${COMPOSE_FILE}"
echo "Using env file:     ${ENV_FILE}"

echo "Pulling latest configured images..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" pull

echo "Starting Barter Platform DEV stack..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --remove-orphans

echo
echo "Current service status:"
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps

echo
echo "Useful log commands:"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f caddy"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f backend"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f frontend"
echo "  docker compose --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f postgres"

