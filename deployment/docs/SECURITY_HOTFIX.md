# Combined security hotfix — CA trust, email verification, and release operations

This document records the locally verified hotfix and its production procedure. No image was published
and no production system was contacted during implementation.

## Verified image inputs

### Caddy

- Stable upstream release observed on 2026-10-21: **Caddy v2.11.7**, released 2026-10-03:
  <https://github.com/caddyserver/caddy/releases/tag/v2.11.7>
- Official base: `caddy:2.11.7-alpine`, manifest digest
  `sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f`.
  The inspected amd64 image reported Alpine **3.23.6** and `ca-certificates-20260909-r0`.
- Alpine 3.23 is a supported stable branch (official lifecycle lists support through 2027-11-01):
  <https://alpinelinux.org/releases/>.
- The official fixed package is `ca-certificates-20261020-r0`; its separately-versioned trust bundle
  must also be updated to `ca-certificates-bundle-20261020-r0`. Installing only the meta-package
  left the old bundle installed, so the Dockerfile pins both APK artifacts from the v3.23 main
  repository and verifies their SHA-256 values before APK signature verification/install:
  - `ca-certificates-20261020-r0.apk` SHA-256
    `0ff2550833ee3e5ace48fab67147b49f80b77dfe34ab85e5a3ba63b178710d53`
  - `ca-certificates-bundle-20261020-r0.apk` SHA-256
    `a4c5de1bf37652cd98cbe71aec209512ae0508a964961957a6c9650f795e46c4`
  - Official package record: <https://pkgs.alpinelinux.org/package/v3.23/main/x86_64/ca-certificates>
  - Official APK archive: <https://dl-cdn.alpinelinux.org/alpine/v3.23/main/x86_64/>
    - Comparing the `caddy:2.11.7-alpine` September bundle with the fixed October bundle on the same
      Alpine 3.23 base showed 121 → 118 roots: exactly three removals and no additions. All three
      removals are eMudhra anchors (`emSign Root CA - C1`, `emSign ECC Root CA - C3`, and
      `emSign ECC Root CA - G3`); no unrelated root was removed by this fixed package update.
- The checked-in Caddy Dockerfile uses only the Caddy digest above and the Alpine v3.23 package
  artifacts. It fails if the base is not Alpine 3.23, verifies package checksums and installed
  versions, and supports the tested `linux/amd64` platform only.
- Actual Caddy Go reverse-proxy tests against both incident hosts returned **502** and logged
  `tls: failed to verify certificate: x509: certificate signed by unknown authority`. The valid
  `example.com` control returned **200**; the self-signed control returned **502** with a TLS
  verification error. The old `caddy:2.8-alpine` client returned **200** for the G3B test host.
- One intermediate repeat encountered outbound timeouts to both incident hosts and was classified
  as blocked. The subsequent final run with the checked-in workspace image passed both eMudhra
  endpoints and both controls. The final image's CA bundle is byte-identical to the earlier candidate
  bundle. The harness continues to report network timeouts separately from TLS rejection.
- The final registry digest is deliberately not fabricated. Docker Publish emits the immutable
  manifest digest in its Actions summary. Copy that digest to the `caddy_digest` input of PROD
  Deploy; the deploy script writes `CADDY_IMAGE=<repo>@<digest>` only after the new Caddy becomes
  healthy. Do not substitute a tag for that production digest.
- The unauthenticated Docker Hub lookup returned 404 for `dragisahub1984/barter-caddy` at review
  time. Before the approved publish, create that repository and grant the existing Docker Hub token
  push access. This was not created automatically because publishing is outside the authorized scope.

### Backend/JVM

- Current published backend identity inspected locally:
  `dragisahub1984/barter-backend:1.3.1`, digest
  `sha256:64e0398c1a90c21f7eb8c26be39d28975ff7e5d3858bfa0899810d19f522f723`, `linux/amd64`,
  Temurin 21.0.11+10, Ubuntu 22.04.5, `ca-certificates 20260601~22.04.1`.
- Candidate Temurin JRE: `eclipse-temurin:21.0.12_8-jre-jammy`, manifest digest
  `sha256:61d6c7b34d36aee3f45d043101259f97f3c6d428dc2a6f75513789983c5e254f`.
  Candidate JDK build image: `eclipse-temurin:21.0.12_8-jdk-jammy`, digest
  `sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438`.
  The manifests include `linux/amd64`; backend CI builds this platform.
- Ubuntu Jammy's available CA package was `20260816ubuntu0.22.04.1`. Updating this OS package
  made system curl reject both eMudhra endpoints with verification error 60, while a valid endpoint
  succeeded. That OS update alone **did not** alter Temurin's default JVM trust store: the current
  backend and clean Temurin 21.0.12 both accepted the G3B endpoint.
- The served chain was observed as leaf → `EM DV TLS CA - G3B` → `emSign Root TLS CA - G3` →
  `emSign ECC Root CA - G3`. The latter was trusted by both JDK trust stores (SHA-256 fingerprint
  `86:A1:EC:BA:08:9C:4A:8D:3B:BE:27:34:C6:12:BA:34:1D:81:3E:04:3C:F9:E8:A8:62:CD:5C:57:A3:6B:BE:6B`).
- The backend Dockerfile removes only the three exact eMudhra aliases removed by the fixed official
  Alpine bundle (`emSign Root CA - C1`, `emSign ECC Root CA - C3`, and `emSign ECC Root CA - G3`).
  It asserts all three are absent and the unrelated eMudhra G1 root remains. The candidate runtime
  rejected both incident hosts, accepted `example.com`, and rejected a locally generated internal-
  CA control. No global TLS-verification bypass or unrelated root deletion is used.
- The final isolated Java harness passed: current backend accepted G3B and `example.com` but
  rejected G3-AAA; candidate backend rejected both incident hosts and the local self-signed control
  while accepting `example.com`. Earlier transient timeouts were classified as network-blocked,
  not certificate rejection; the final rerun reached all controls.

## Dedicated production verification secret

- `BARTER_VERIFICATION_CODE_SECRET` is required in production and must be at least 32 printable
  ASCII characters. Use the redirect-based procedure in `production-runbook.md` to write it
  directly to the server-only `deployment/env/prod.env` without displaying it.
- Keep it stable across releases. Never commit it, pass it through GitHub Actions, print it, hash it,
  or include it in diagnostics. Development/test retain the existing JWT-derived fallback.
- Production startup validation and `deploy-prod.sh` preflight use the same rule. The deploy
  preflight checks it before backup, env edits, pulls, or service changes. The rollback script does
  not require the new secret, so rolling back an old image is not blocked by a key it never used.

## Proxy identity and shared routing

- Existing Caddy directives in both authoritative barter-platform Caddyfiles overwrite `X-Real-IP`
  with `{client_ip}`; no broad `trusted_proxies` ranges are added. Production consumes only
  `X-Real-IP` for rate-limit keys. `X-Forwarded-For` changes do not bypass that limiter.
- Spring remains on `server.forward-headers-strategy: framework`; no backend port is published by
  Compose. Do not expose the backend directly or make it trust client-supplied forwarding headers.
- The active `Caddyfile.prod` remains authoritative for Wivaulty. Verify routes after staging:
  `api-test.wivaulty.com`, `api.wivaulty.com`, `/.well-known/assetlinks.json`, `/invite`,
  `/invite/*`, `/shared-collection-invitations`, and `/s/*`. Do not replace the shared file with a
  Wivaulty repository template.
- Validate both files locally with the Caddy binary: `caddy adapt --adapter caddyfile --validate`.
  The existing security headers, compression, health routes, and reverse proxies must remain intact.

## Deployment order and rollback

1. Record and verify the published Caddy digest in Docker Publish's run summary.
2. On the production host, confirm a recent managed-PostgreSQL backup and the server-only dedicated
   verification secret. `deploy-prod.sh <semver> <caddy-sha256-digest> --preflight-only` performs
   a non-mutating secret/Compose check.
3. A normal deploy repeats preflight, then backup and pulls only `caddy`, `backend`, `frontend`,
   and `landing` target images.
4. Caddy is recreated first; wait for its health before backend recreation. A brief edge interruption
   is expected. Backend health gates the subsequent frontend/landing recreation.
5. Neither deployment nor rollback uses `--remove-orphans`, renames the Compose project, recreates
   unrelated services, deletes volumes, or changes the external `wivaulty-edge` network.
6. Rollback changes backend/frontend/landing images only; Caddy remains at the verified digest.
   V030/V031 are not reversed. If an application rollback is insufficient, recover forward. Any DB
   restore is a separate operator-approved action; do not run destructive SQL automatically.
7. If Caddy does not become healthy, the backend is not activated and `prod.env` retains its old
   Caddy reference. Do not automatically revert to `caddy:2.8-alpine`, which accepted the affected
   chain; diagnose and recover forward with the verified CA-remediated image.

## Flyway V030/V031

- **V030 is immutable** and was already applied to a local/test PostgreSQL database before this
  hotfix continuation. It adds attempt/issuance state and indexes and invalidates outstanding legacy
  codes with `invalidated_at`. I briefly changed the SQL accidentally, then restored it exactly to
  the version present at the start of this continuation; no migration/test was run while it was
  changed. The restored source matches the earlier Gradle resource copy byte-for-byte (SHA-256
  `289ECD2E94FC5CE3090FB40746FC825E5EF498C0087EB0553726CDE365C58FE5`); with runtime Flyway
  11.14.1, its offline CRC32 is `1059031375`. The checksum stored in the already-applied database cannot be confirmed without
  reading `flyway_schema_history`; no local PostgreSQL service/listener or `DB_URL` was available,
  and no production connection was attempted.
- V030's additive defaults permit the previous backend to start, but that version only checks
  `used_at`; by itself it would not honor V030's invalidation marker. V031 therefore updates
  `used_at = invalidated_at` for already-invalidated rows. It adds no schema objects, preserves the
  codes as unusable, and is forward-only. The integration test exercises V030+V031 in a fresh
  disposable PostgreSQL container, not an existing database.
- Users with outstanding legacy verification codes must request a fresh code after V030 retires
  the old SHA-256 values. During rollback to an older application, codes created by the new HMAC
  implementation are not compatible with the old verifier; this is a release rollback limitation,
  not a reason to reverse the migrations.
- If deployment stops after V030 but before V031 is applied, do not activate an older backend image:
  it ignores `invalidated_at`. Complete V031 with the corrected hotfix/forward application release.
- V030 adds a constraint and creates two regular (non-concurrent) indexes and updates the active
  code rows. These operations can wait for locks and cause write blocking proportional to row count.
  Stage first and confirm the verification-code table is small/backup current before the production
  migration. No production row counts or lock impact were inspected in this local task.

## Local verification commands

```bash
bash deployment/scripts/test-tls-trust.sh
bash deployment/scripts/test-client-ip-proxy.sh
bash deployment/scripts/test-prod-preflight.sh
bash deployment/scripts/test-prod-rollback.sh
bash deployment/scripts/test-prod-deploy-sequence.sh
```

The TLS script tests actual Caddy and default Java TLS clients; it distinguishes certificate errors
from network/I/O failures. These scripts and the Docker Publish workflow do not publish/deploy by
being run locally. After changes, run backend tests and the Testcontainers migration test separately.
