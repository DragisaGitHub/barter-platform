#!/usr/bin/env bash
# Verify Caddy overwrites a client-supplied X-Real-IP before forwarding to an echo upstream.
# Local-only: no application or production service is contacted.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CADDY_IMAGE="${CADDY_IMAGE:-barter-caddy-ca-hotfix:local}"
PORT="${CLIENT_IP_TEST_PORT:-18190}"
UPSTREAM_PORT="${CLIENT_IP_UPSTREAM_PORT:-19190}"
TMP_DIR="$(mktemp -d)"
CONTAINER="barter-client-ip-probe-$$"
ECHO_CONTAINER="${CONTAINER}-echo"
NETWORK="${CONTAINER}-net"

cleanup() {
  docker rm -f "${CONTAINER}" >/dev/null 2>&1 || true
  docker rm -f "${ECHO_CONTAINER}" >/dev/null 2>&1 || true
  docker network rm "${NETWORK}" >/dev/null 2>&1 || true
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

if ! docker image inspect "${CADDY_IMAGE}" >/dev/null 2>&1; then
  docker build --platform linux/amd64 \
    --file "${ROOT_DIR}/deployment/docker/caddy/Dockerfile" \
    --tag "${CADDY_IMAGE}" "${ROOT_DIR}/deployment/docker/caddy"
fi

cat > "${TMP_DIR}/echo.py" <<'PY'
from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Echo(BaseHTTPRequestHandler):
    def do_GET(self):
        payload = json.dumps({
            "remote": self.client_address[0],
            "x_real_ip": self.headers.get("X-Real-IP"),
            "x_forwarded_for": self.headers.get("X-Forwarded-For"),
        }).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *_args):
        pass

HTTPServer(("0.0.0.0", int(__import__("sys").argv[1])), Echo).serve_forever()
PY

docker network create "${NETWORK}" >/dev/null
docker run -d --platform linux/amd64 --name "${ECHO_CONTAINER}" \
  --network "${NETWORK}" --network-alias echo \
  --volume "${TMP_DIR}/echo.py:/echo.py:ro" \
  python:3.12-alpine python /echo.py "${UPSTREAM_PORT}" >/dev/null

cat > "${TMP_DIR}/Caddyfile" <<EOF
:8080 {
    reverse_proxy echo:${UPSTREAM_PORT} {
        header_up X-Real-IP {client_ip}
    }
}
EOF

docker run -d --platform linux/amd64 --name "${CONTAINER}" \
  --network "${NETWORK}" \
  --publish "127.0.0.1:${PORT}:8080" \
  --volume "${TMP_DIR}/Caddyfile:/etc/caddy/Caddyfile:ro" \
  "${CADDY_IMAGE}" caddy run --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null

response=""
for _attempt in $(seq 1 30); do
  response="$(curl --silent --show-error --max-time 5 \
    -H 'X-Real-IP: 198.51.100.77' \
    -H 'X-Forwarded-For: 203.0.113.99' \
    "http://127.0.0.1:${PORT}/" 2>/dev/null || true)"
  [[ -n "${response}" ]] && break
  sleep 1
done

[[ -n "${response}" ]] || { docker logs "${CONTAINER}" >&2; echo 'Caddy echo-upstream request failed.' >&2; exit 1; }
docker run --rm --platform linux/amd64 --network "${NETWORK}" python:3.12-alpine \
  python -c 'import json,sys; d=json.loads(sys.argv[1]); spoof="198.51.100.77"; assert d["x_real_ip"] and d["x_real_ip"] != spoof, "X-Real-IP spoof was forwarded"; print("PASS X-Real-IP overwritten by Caddy; observed edge address:", d["x_real_ip"])' \
  "${response}"
