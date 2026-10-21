#!/usr/bin/env bash
# Local-only checks for production secret preflight. Uses a fake docker command and temp env files.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="${SCRIPT_DIR}/deploy-prod.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

mkdir -p "${TMP_DIR}/bin"
cat > "${TMP_DIR}/bin/docker" <<'SH'
#!/usr/bin/env bash
if [[ "$*" == *" config --quiet" ]]; then exit 0; fi
echo "Unexpected docker command in preflight test." >&2
exit 90
SH
chmod +x "${TMP_DIR}/bin/docker"

run_preflight() {
  local env_file="$1" output status
  set +e
  output="$(PATH="${TMP_DIR}/bin:${PATH}" ENV_FILE="${env_file}" \
    bash "${DEPLOY_SCRIPT}" 1.2.3 \
    sha256:0000000000000000000000000000000000000000000000000000000000000000 \
    --preflight-only 2>&1)"
  status=$?
  set -e
  printf '%s\n' "${status}" > "${TMP_DIR}/status"
  printf '%s\n' "${output}" > "${TMP_DIR}/output"
}

printf 'BARTER_VERIFICATION_CODE_SECRET=%064d\n' 0 > "${TMP_DIR}/valid.env"
run_preflight "${TMP_DIR}/valid.env"
[[ "$(cat "${TMP_DIR}/status")" == 0 ]] || { cat "${TMP_DIR}/output" >&2; exit 1; }
! grep -q '0000000000000000000000000000000000000000000000000000000000000000' "${TMP_DIR}/output" \
  || { echo 'Preflight output exposed the test secret.' >&2; exit 1; }
echo 'PASS valid dedicated secret accepted without outputting its value.'

printf 'BARTER_VERIFICATION_CODE_SECRET=too-short-secret\n' > "${TMP_DIR}/short.env"
run_preflight "${TMP_DIR}/short.env"
[[ "$(cat "${TMP_DIR}/status")" != 0 ]] || { echo 'Short secret unexpectedly passed.' >&2; exit 1; }
! grep -q 'too-short-secret' "${TMP_DIR}/output" \
  || { echo 'Preflight output exposed the test secret.' >&2; exit 1; }
echo 'PASS weak dedicated secret rejected without outputting its value.'

printf 'BARTER_VERIFICATION_CODE_SECRET=this test secret contains spaces\n' > "${TMP_DIR}/spaces.env"
run_preflight "${TMP_DIR}/spaces.env"
[[ "$(cat "${TMP_DIR}/status")" != 0 ]] || { echo 'Secret containing spaces unexpectedly passed.' >&2; exit 1; }
! grep -q 'this test secret contains spaces' "${TMP_DIR}/output" \
  || { echo 'Preflight output exposed the test secret.' >&2; exit 1; }
echo 'PASS secret containing whitespace rejected without outputting its value.'

: > "${TMP_DIR}/missing.env"
run_preflight "${TMP_DIR}/missing.env"
[[ "$(cat "${TMP_DIR}/status")" != 0 ]] || { echo 'Missing secret unexpectedly passed.' >&2; exit 1; }
echo 'PASS missing dedicated secret rejected.'
