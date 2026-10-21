#!/usr/bin/env bash
# Local-only deploy-order test. Fake docker prevents backup, image pull, or service actions.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
ENV_FILE="${REPO_DIR}/deployment/env/prod.env"
CREATED_ENV="false"
trap 'if [[ "${CREATED_ENV}" == true ]]; then rm -f "${ENV_FILE}"; fi; rm -rf "${TMP_DIR}"' EXIT

[[ ! -e "${ENV_FILE}" ]] || { echo 'Refusing to overwrite existing prod.env.' >&2; exit 2; }
CREATED_ENV="true"
TEST_SECRET="preflight-test-only-secret-never-production-7b39d2"
cat > "${ENV_FILE}" <<ENV
CADDY_IMAGE=dragisahub1984/barter-caddy@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
BACKEND_IMAGE=dragisahub1984/barter-backend:1.0.0
FRONTEND_IMAGE=dragisahub1984/barter-frontend:1.0.0
LANDING_IMAGE=dragisahub1984/barter-landing:1.0.0
DB_URL=jdbc:postgresql://db.invalid:5432/barter
DB_USERNAME=placeholder
DB_PASSWORD=placeholder
JWT_SECRET=placeholder-jwt-secret-value
BARTER_VERIFICATION_CODE_SECRET=${TEST_SECRET}
SMTP_HOST=smtp.invalid
SMTP_USERNAME=placeholder
SMTP_PASSWORD=placeholder
AZURE_STORAGE_CONNECTION_STRING_PROD=placeholder
BACKUP_ENABLED=false
ENV

mkdir -p "${TMP_DIR}/bin"
cat > "${TMP_DIR}/bin/docker" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${DOCKER_COMMAND_LOG}"
if [[ "$*" == *" config --quiet" ]]; then exit 0; fi
if [[ "$*" == *" pull caddy backend frontend landing"* ]]; then exit 0; fi
if [[ "$*" == *" up -d --no-deps --force-recreate caddy"* ]]; then exit 0; fi
if [[ "$*" == *" up -d --no-deps --force-recreate backend"* ]]; then exit 0; fi
if [[ "$*" == *" up -d --no-deps --force-recreate frontend landing"* ]]; then exit 0; fi
if [[ "$*" == *" ps -q "* ]]; then echo test-container-id; exit 0; fi
if [[ "$*" == inspect* && "$*" == *".State.Health"* ]]; then echo healthy; exit 0; fi
if [[ "$*" == *" ps"* ]]; then exit 0; fi
echo "Unexpected docker command in deploy test." >&2
exit 90
SH
cat > "${TMP_DIR}/bin/curl" <<'SH'
#!/usr/bin/env bash
exit 0
SH
chmod +x "${TMP_DIR}/bin/docker" "${TMP_DIR}/bin/curl"
export DOCKER_COMMAND_LOG="${TMP_DIR}/docker.log"

output="$(PATH="${TMP_DIR}/bin:${PATH}" BACKUP_ENABLED=false \
  bash "${SCRIPT_DIR}/deploy-prod.sh" 1.2.3 \
  sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb 2>&1)" || {
  echo "${output}" >&2
  exit 1
}

if grep -q "${TEST_SECRET}" <<<"${output}" || grep -q "${TEST_SECRET}" "${DOCKER_COMMAND_LOG}"; then
  echo 'Production secret leaked into output or command log.' >&2
  exit 1
fi
commands="$(cat "${DOCKER_COMMAND_LOG}")"
caddy_line="$(grep -n 'up -d --no-deps --force-recreate caddy' <<<"${commands}" | cut -d: -f1)"
backend_line="$(grep -n 'up -d --no-deps --force-recreate backend' <<<"${commands}" | cut -d: -f1)"
frontend_line="$(grep -n 'up -d --no-deps --force-recreate frontend landing' <<<"${commands}" | cut -d: -f1)"
[[ -n "${caddy_line}" && -n "${backend_line}" && -n "${frontend_line}" ]] \
  || { echo 'Expected explicit service targets were not invoked.' >&2; exit 1; }
(( caddy_line < backend_line && backend_line < frontend_line )) \
  || { echo 'Service order was not Caddy, backend, frontend/landing.' >&2; exit 1; }
! grep -q -- '--remove-orphans' <<<"${commands}" \
  || { echo 'Unexpected orphan removal.' >&2; exit 1; }
echo 'PASS production deploy preflight, secret redaction, explicit targets, and Caddy-first order.'
