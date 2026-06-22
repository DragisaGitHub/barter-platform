import * as Sentry from "@sentry/react";

const SENTRY_DSN = import.meta.env.VITE_SENTRY_DSN;
const ENVIRONMENT = import.meta.env.VITE_SENTRY_ENVIRONMENT || "development";

// Injected at build time via vite.config.ts define
declare const __APP_VERSION__: string;
declare const __COMMIT_SHA__: string;

const appVersion = typeof __APP_VERSION__ !== "undefined" ? __APP_VERSION__ : "unknown";
const commitSha = typeof __COMMIT_SHA__ !== "undefined" ? __COMMIT_SHA__ : undefined;

const release = commitSha ? `${appVersion}+${commitSha}` : appVersion;

/** Headers that must never be sent to Sentry */
const SENSITIVE_HEADERS = new Set([
  "authorization",
  "cookie",
  "set-cookie",
  "x-csrf-token",
  "x-auth-token",
  "proxy-authorization",
]);

/** Keys whose values must be scrubbed from event data */
const SENSITIVE_KEYS = /password|token|secret|jwt|refreshToken|accessToken|credential|session/i;

/**
 * Recursively scrub sensitive keys from an object.
 * Returns a shallow-cleaned copy — does not mutate.
 */
function scrubObject(obj: unknown): unknown {
  if (obj === null || obj === undefined) return obj;
  if (typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(scrubObject);

  const cleaned: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
    if (SENSITIVE_KEYS.test(key)) {
      cleaned[key] = "[Filtered]";
    } else if (typeof value === "object" && value !== null) {
      cleaned[key] = scrubObject(value);
    } else {
      cleaned[key] = value;
    }
  }
  return cleaned;
}

/**
 * Strip sensitive headers from request/response breadcrumbs and event data
 */
function scrubHeaders(
  headers: Record<string, string> | undefined
): Record<string, string> | undefined {
  if (!headers) return headers;
  const cleaned: Record<string, string> = {};
  for (const [key, value] of Object.entries(headers)) {
    if (SENSITIVE_HEADERS.has(key.toLowerCase())) {
      cleaned[key] = "[Filtered]";
    } else {
      cleaned[key] = value;
    }
  }
  return cleaned;
}

export function initSentry(): void {
  if (!SENTRY_DSN) {
    return;
  }

  Sentry.init({
    dsn: SENTRY_DSN,
    environment: ENVIRONMENT,
    release,

    // Performance: disabled by default per requirements
    tracesSampleRate: 0,
    // No session replay
    replaysSessionSampleRate: 0,
    replaysOnErrorSampleRate: 0,

    // Do not send default PII (emails, IPs, etc.)
    sendDefaultPii: false,

    beforeSend(event) {
      // Scrub request bodies
      if (event.request) {
        delete event.request.data;
        delete event.request.cookies;
        event.request.headers = scrubHeaders(event.request.headers);
      }

      // Scrub user data — keep only non-sensitive ID
      if (event.user) {
        const safeUser: Sentry.User = {};
        if (event.user.id) {
          safeUser.id = event.user.id;
        }
        event.user = safeUser;
      }

      // Scrub any extra/context data
      if (event.extra) {
        event.extra = scrubObject(event.extra) as Record<string, unknown>;
      }

      // Scrub breadcrumb data
      if (event.breadcrumbs) {
        event.breadcrumbs = event.breadcrumbs.map((breadcrumb) => {
          if (breadcrumb.data) {
            // For HTTP breadcrumbs, keep only safe fields
            if (breadcrumb.category === "xhr" || breadcrumb.category === "fetch") {
              breadcrumb.data = {
                method: breadcrumb.data.method,
                url: breadcrumb.data.url,
                status_code: breadcrumb.data.status_code,
              };
            } else {
              breadcrumb.data = scrubObject(breadcrumb.data) as Record<string, unknown>;
            }
          }
          return breadcrumb;
        });
      }

      return event;
    },

    beforeBreadcrumb(breadcrumb) {
      // Allow navigation breadcrumbs
      if (breadcrumb.category === "navigation") {
        return breadcrumb;
      }

      // Allow UI click breadcrumbs (safe context for debugging)
      if (breadcrumb.category === "ui.click") {
        return breadcrumb;
      }

      // Allow console errors/warnings (no payloads)
      if (breadcrumb.category === "console") {
        if (breadcrumb.level === "error" || breadcrumb.level === "warning") {
          return breadcrumb;
        }
        return null;
      }

      // Allow XHR/fetch — data is sanitized in beforeSend
      if (breadcrumb.category === "xhr" || breadcrumb.category === "fetch") {
        if (breadcrumb.data) {
          breadcrumb.data = {
            method: breadcrumb.data.method,
            url: breadcrumb.data.url,
            status_code: breadcrumb.data.status_code,
          };
        }
        return breadcrumb;
      }

      // Drop all other breadcrumbs by default
      return null;
    },
  });
}

/**
 * Check if Sentry is initialized and active
 */
export function isSentryEnabled(): boolean {
  return !!SENTRY_DSN;
}

