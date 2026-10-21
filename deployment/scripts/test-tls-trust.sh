#!/usr/bin/env bash
# Isolated TLS regression checks for Caddy's Go transport and the default Java SSLContext.
# This script does not use Compose or connect to application/production infrastructure.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CADDY_IMAGE="${CADDY_IMAGE:-barter-caddy-ca-hotfix:workspace}"
CURRENT_JAVA_IMAGE="${CURRENT_JAVA_IMAGE:-dragisahub1984/barter-backend@sha256:64e0398c1a90c21f7eb8c26be39d28975ff7e5d3858bfa0899810d19f522f723}"
CANDIDATE_JAVA_IMAGE="${CANDIDATE_JAVA_IMAGE:-barter-backend-tls-candidate:local}"
JDK_IMAGE="eclipse-temurin@sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438"
PORT_BASE="${TLS_TEST_PORT_BASE:-18180}"
TMP_DIR="$(mktemp -d)"
CONTAINER_PREFIX="barter-tls-probe-$$"
TEST_NETWORK="${CONTAINER_PREFIX}-network"
MODE="${1:-all}"
NETWORK_BLOCKERS=0
TEST_FAILURES=0

case "${MODE}" in
  all|--caddy-only|--java-only) ;;
  *) echo "Usage: $0 [--caddy-only|--java-only]" >&2; exit 2 ;;
esac

fail() { echo "[FAIL] $*" >&2; exit 1; }

cleanup() {
  docker ps -aq --filter "name=${CONTAINER_PREFIX}-" | xargs -r docker rm -f >/dev/null 2>&1 || true
  docker image rm "${CONTAINER_PREFIX}-current" "${CONTAINER_PREFIX}-candidate" >/dev/null 2>&1 || true
  docker network rm "${TEST_NETWORK}" >/dev/null 2>&1 || true
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

for tool in docker curl; do
  command -v "${tool}" >/dev/null 2>&1 || fail "Required command not found: ${tool}"
done

if [[ "${MODE}" != "--java-only" ]]; then
  docker image inspect "${CADDY_IMAGE}" >/dev/null 2>&1 \
    || fail "Caddy candidate image is unavailable locally: ${CADDY_IMAGE}"
fi
if [[ "${MODE}" != "--caddy-only" ]]; then
  docker image inspect "${CURRENT_JAVA_IMAGE}" >/dev/null 2>&1 \
    || docker pull "${CURRENT_JAVA_IMAGE}"

  if ! docker image inspect "${CANDIDATE_JAVA_IMAGE}" >/dev/null 2>&1; then
    echo "Building local backend TLS candidate image (no push)."
    docker build --platform linux/amd64 \
      --file "${ROOT_DIR}/deployment/docker/backend/Dockerfile" \
      --tag "${CANDIDATE_JAVA_IMAGE}" "${ROOT_DIR}"
  fi
fi

cat > "${TMP_DIR}/TlsProbe.java" <<'JAVA'
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URL;

public final class TlsProbe {
    public static void main(String[] args) {
        String endpoint = args[0].contains("://") ? args[0] : "https://" + args[0] + "/";
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
            connection.setSSLSocketFactory(SSLContext.getDefault().getSocketFactory());
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            System.out.println("TLS_ACCEPTED " + endpoint + " HTTP=" + responseCode);
            connection.disconnect();
        } catch (SSLHandshakeException error) {
            System.out.println("TLS_REJECTED " + endpoint + " " + error.getClass().getSimpleName());
            System.exit(2);
        } catch (IOException error) {
            System.out.println("NETWORK_OR_IO_ERROR " + endpoint + " " + error.getClass().getSimpleName());
            System.exit(3);
        } catch (Exception error) {
            System.out.println("CLIENT_ERROR " + endpoint + " " + error.getClass().getSimpleName());
            System.exit(4);
        }
    }
}
JAVA

if [[ "${MODE}" != "--caddy-only" ]]; then
cat > "${TMP_DIR}/Dockerfile.probe" <<EOF
ARG BASE_IMAGE=dragisahub1984/barter-backend@sha256:64e0398c1a90c21f7eb8c26be39d28975ff7e5d3858bfa0899810d19f522f723
FROM ${JDK_IMAGE} AS compiler
WORKDIR /source
COPY TlsProbe.java ./TlsProbe.java
RUN javac -d /out TlsProbe.java
FROM \${BASE_IMAGE} AS probe
COPY --from=compiler /out/TlsProbe.class /work/TlsProbe.class
EOF

docker build --platform linux/amd64 --file "${TMP_DIR}/Dockerfile.probe" \
  --build-arg "BASE_IMAGE=${CURRENT_JAVA_IMAGE}" \
  --tag "${CONTAINER_PREFIX}-current" "${TMP_DIR}"
docker build --platform linux/amd64 --file "${TMP_DIR}/Dockerfile.probe" \
  --build-arg "BASE_IMAGE=${CANDIDATE_JAVA_IMAGE}" \
  --tag "${CONTAINER_PREFIX}-candidate" "${TMP_DIR}"
fi

run_caddy_case() {
  local case_name="$1" host="$2" expected="$3" port="$4"
  local container="${CONTAINER_PREFIX}-${case_name}" config="${TMP_DIR}/${case_name}.Caddyfile"
  cat > "${config}" <<EOF
:8080 {
    reverse_proxy https://${host} {
        header_up Host ${host}
    }
}
EOF
  docker run -d --platform linux/amd64 --name "${container}" \
    --publish "127.0.0.1:${port}:8080" \
    --volume "${config}:/etc/caddy/Caddyfile:ro" \
    "${CADDY_IMAGE}" caddy run --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null

  local code="000" attempt
  for attempt in $(seq 1 30); do
    code="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
      --connect-timeout 2 --max-time 8 "http://127.0.0.1:${port}/" 2>/dev/null || true)"
    [[ "${code}" != "000" ]] && break
    sleep 1
  done

  local logs
  logs="$(docker logs "${container}" 2>&1)"
  if [[ "${code}" != "${expected}" ]]; then
    if grep -Eq 'dial tcp|i/o timeout|no such host|connection refused' <<<"${logs}"; then
      echo "BLOCKED Caddy ${case_name}: upstream network/I/O error (HTTP ${code}); not counted as TLS verification." >&2
      NETWORK_BLOCKERS=$((NETWORK_BLOCKERS + 1))
      docker rm -f "${container}" >/dev/null
      return 0
    fi
    echo "${logs}" >&2
    echo "FAIL Caddy ${case_name}: expected HTTP ${expected}, got ${code}." >&2
    TEST_FAILURES=$((TEST_FAILURES + 1))
    docker rm -f "${container}" >/dev/null
    return 0
  fi
  if [[ "${expected}" == "502" ]]; then
    if ! grep -q 'tls: failed to verify certificate' <<<"${logs}"; then
      if grep -Eq 'dial tcp|i/o timeout|no such host|connection refused' <<<"${logs}"; then
        echo "BLOCKED Caddy ${case_name}: upstream network/I/O error; not counted as TLS verification." >&2
        NETWORK_BLOCKERS=$((NETWORK_BLOCKERS + 1))
      else
        echo "${logs}" >&2
        echo "FAIL Caddy ${case_name}: 502 did not include a certificate-verification error." >&2
        TEST_FAILURES=$((TEST_FAILURES + 1))
      fi
      docker rm -f "${container}" >/dev/null
      return 0
    fi
  fi
  echo "PASS Caddy ${case_name}: HTTP ${code}; upstream TLS verification remained enabled."
  docker rm -f "${container}" >/dev/null
}

run_java_case() {
  local label="$1" image="$2" host="$3" expected="$4"
  local output status endpoint network="${5:-}"
  local -a docker_network=()
  [[ -n "${network}" ]] && docker_network=(--network "${network}")
  endpoint="https://${host}/"
  set +e
  output="$(docker run --rm --platform linux/amd64 \
    "${docker_network[@]}" \
    --entrypoint java "${image}" -cp /work TlsProbe "${endpoint}" 2>&1)"
  status=$?
  set -e
  case "${expected}:${status}:${output}" in
    accepted:0:*TLS_ACCEPTED*) echo "PASS Java ${label}: TLS accepted for ${host}." ;;
    rejected:2:*TLS_REJECTED*) echo "PASS Java ${label}: TLS rejected for ${host}." ;;
    *)
      if [[ "${status}" == 3 ]]; then
        echo "BLOCKED Java ${label}: network/I/O failure for ${host}; not counted as TLS verification." >&2
        NETWORK_BLOCKERS=$((NETWORK_BLOCKERS + 1))
        return 0
      fi
      echo "${output}" >&2
      echo "FAIL Java ${label}: expected ${expected} for ${host}, process status ${status}." >&2
      TEST_FAILURES=$((TEST_FAILURES + 1))
      ;;
  esac
}

if [[ "${MODE}" != "--java-only" ]]; then
  base_port=$((PORT_BASE))
  run_caddy_case emudhra-g3b dv-g3b-emudhra.we-have-a-problem.com 502 "$((base_port))"
  run_caddy_case emudhra-g3b-aaa dv-g3b-aaa-emudhra.we-have-a-problem.com 502 "$((base_port + 1))"
  run_caddy_case valid-control example.com 200 "$((base_port + 2))"
  run_caddy_case invalid-control self-signed.badssl.com 502 "$((base_port + 3))"
fi

if [[ "${MODE}" != "--caddy-only" ]]; then
  run_java_case current "${CONTAINER_PREFIX}-current" dv-g3b-emudhra.we-have-a-problem.com accepted
  run_java_case current "${CONTAINER_PREFIX}-current" dv-g3b-aaa-emudhra.we-have-a-problem.com rejected
  run_java_case current "${CONTAINER_PREFIX}-current" example.com accepted
  run_java_case candidate "${CONTAINER_PREFIX}-candidate" dv-g3b-emudhra.we-have-a-problem.com rejected
  run_java_case candidate "${CONTAINER_PREFIX}-candidate" dv-g3b-aaa-emudhra.we-have-a-problem.com rejected
  run_java_case candidate "${CONTAINER_PREFIX}-candidate" example.com accepted

  docker network create "${TEST_NETWORK}" >/dev/null
  cat > "${TMP_DIR}/self-signed.Caddyfile" <<'EOF'
https://selfsigned.test:8443 {
    tls internal
    respond "self-signed TLS control" 200
}
EOF
  docker run -d --platform linux/amd64 --name "${CONTAINER_PREFIX}-selfsigned" \
    --network "${TEST_NETWORK}" --network-alias selfsigned.test \
    --volume "${TMP_DIR}/self-signed.Caddyfile:/etc/caddy/Caddyfile:ro" \
    "${CADDY_IMAGE}" caddy run --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
  ready=false
  for _attempt in $(seq 1 20); do
    if docker logs "${CONTAINER_PREFIX}-selfsigned" 2>&1 | grep -q 'server running'; then
      ready=true
      break
    fi
    sleep 1
  done
  [[ "${ready}" == "true" ]] || fail "Local self-signed TLS control did not start."
  run_java_case current "${CONTAINER_PREFIX}-current" selfsigned.test:8443 rejected "${TEST_NETWORK}"
  run_java_case candidate "${CONTAINER_PREFIX}-candidate" selfsigned.test:8443 rejected "${TEST_NETWORK}"
fi

if (( TEST_FAILURES > 0 )); then
  fail "${TEST_FAILURES} TLS verification assertion(s) failed; network blockers=${NETWORK_BLOCKERS}."
fi
if (( NETWORK_BLOCKERS > 0 )); then
  echo "TLS assertions passed where reachable; ${NETWORK_BLOCKERS} check(s) were blocked by network/I/O."
  exit 3
fi
printf '\nAll isolated TLS trust checks passed.\n'
