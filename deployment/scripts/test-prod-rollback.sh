#!/usr/bin/env bash
# Local-only rollback argument/preflight checks. A fake docker binary prevents service actions.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
ENV_FILE="${REPO_DIR}/deployment/env/prod.env"
CREATED_ENV="false"

cleanup() {
  if [[ "${CREATED_ENV}" == "true" ]]; then rm -f "${ENV_FILE}"; fi
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

[[ ! -e "${ENV_FILE}" ]] || { echo 'Refusing to overwrite existing prod.env.' >&2; exit 2; }
CREATED_ENV="true"
cat > "${ENV_FILE}" <<'ENV'
CADDY_IMAGE=example.invalid/barter-caddy@sha256:0000000000000000000000000000000000000000000000000000000000000000
BACKEND_IMAGE=example.invalid/backend:1.3.1
FRONTEND_IMAGE=example.invalid/frontend:1.3.1
LANDING_IMAGE=example.invalid/landing:1.3.1
ENV

mkdir -p "${TMP_DIR}/bin"
cat > "${TMP_DIR}/bin/docker" <<'SH'
#!/usr/bin/env bash
if [[ "$*" == *" config --quiet" ]]; then exit 0; fi
if [[ "$*" == inspect* && "$*" == *".State.Health"* ]]; then echo healthy; exit 0; fi
if [[ "$*" == inspect* && "$*" == *"barter-prod-caddy"* ]]; then echo 'dragisahub1984/barter-caddy@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'; exit 0; fi
if [[ "$*" == *" pull backend frontend landing"* ]]; then exit 0; fi
if [[ "$*" == *" up -d --no-deps --force-recreate backend"* ]]; then exit 0; fi
if [[ "$*" == *" up -d --no-deps --force-recreate frontend landing"* ]]; then exit 0; fi
if [[ "$*" == *" ps -q "* ]]; then echo test-container-id; exit 0; fi
if [[ "$*" == "compose"*" ps"* ]]; then exit 0; fi
echo "Unexpected docker command in rollback test." >&2
exit 90
SH
chmod +x "${TMP_DIR}/bin/docker"
cat > "${TMP_DIR}/bin/curl" <<'SH'
#!/usr/bin/env bash
exit 0
SH
chmod +x "${TMP_DIR}/bin/curl"

output="$(PATH="${TMP_DIR}/bin:${PATH}" bash "${SCRIPT_DIR}/rollback-prod.sh" 1.2.3 \
  --confirm-migration-state-reviewed 2>&1)" || {
  echo "${output}" >&2
  exit 1
}

grep -q 'Caddy remains unchanged' <<<"${output}"
grep -q 'Production ROLLBACK completed successfully' <<<"${output}"
if grep -q 'BARTER_VERIFICATION_CODE_SECRET' "${ENV_FILE}"; then
  echo 'Rollback test unexpectedly introduced the new dedicated secret.' >&2
  exit 1
fi
echo 'PASS rollback preflight and target sequencing do not require BARTER_VERIFICATION_CODE_SECRET.'
