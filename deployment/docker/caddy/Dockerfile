FROM caddy@sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f

ARG TARGETARCH

RUN set -eu; \
    test "$(cut -d. -f1,2 /etc/alpine-release)" = "3.23"; \
    case "${TARGETARCH:-amd64}" in \
      amd64) apk_arch=x86_64 ;; \
      *) echo "Unsupported target architecture for the pinned Alpine CA artifacts." >&2; exit 1 ;; \
    esac; \
    certs="ca-certificates-20261020-r0.apk"; \
    bundle="ca-certificates-bundle-20261020-r0.apk"; \
    base="https://dl-cdn.alpinelinux.org/alpine/v3.23/main/${apk_arch}"; \
    wget -q "${base}/${certs}" -O "/tmp/${certs}"; \
    wget -q "${base}/${bundle}" -O "/tmp/${bundle}"; \
    echo '0ff2550833ee3e5ace48fab67147b49f80b77dfe34ab85e5a3ba63b178710d53  /tmp/ca-certificates-20261020-r0.apk' | sha256sum -c -; \
    echo 'a4c5de1bf37652cd98cbe71aec209512ae0508a964961957a6c9650f795e46c4  /tmp/ca-certificates-bundle-20261020-r0.apk' | sha256sum -c -; \
    apk add --no-cache "/tmp/${bundle}" "/tmp/${certs}"; \
    apk list -I | grep -F 'ca-certificates-20261020-r0'; \
    apk list -I | grep -F 'ca-certificates-bundle-20261020-r0'; \
    rm -f "/tmp/${certs}" "/tmp/${bundle}"
