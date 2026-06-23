import { useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertTriangle, CheckCircle2, Server } from "lucide-react";
import { isAxiosError } from "axios";
import { apiClient } from "@/api/axios.ts";
import { AdminSurface } from "./AdminPageShell";
import { Button } from "@/components/ui/Button";

export function BackendSentryDiagnosticsSection() {
  const { t } = useTranslation("admin");
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [unexpectedError, setUnexpectedError] = useState(false);

  async function handleSendBackendTestEvent() {
    setSending(true);
    setSent(false);
    setUnexpectedError(false);

    try {
      await apiClient.post("/admin/system/sentry-test");
      // Should not reach here — the endpoint always throws 500 by design.
      // Treat it as a success regardless.
      setSent(true);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 500) {
        // HTTP 500 is the designed and expected response for this diagnostic endpoint.
        // It means the backend threw the exception and GlobalExceptionHandler captured it.
        setSent(true);
      } else {
        // Unexpected failure: auth error (401/403) or network issue.
        setUnexpectedError(true);
      }
    } finally {
      setSending(false);
    }
  }

  return (
    <AdminSurface
      title={t("backendSentryDiagnostics.title")}
      description={t("backendSentryDiagnostics.description")}
      contentClassName="space-y-5"
    >
      <div className="space-y-4">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
            {t("backendSentryDiagnostics.endpointLabel")}
          </p>
          <dl className="mt-3 grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-slate-500 dark:text-slate-400">endpoint</dt>
              <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">
                POST /admin/system/sentry-test
              </dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">expected response</dt>
              <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">HTTP 500</dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">exception</dt>
              <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">
                RuntimeException
              </dd>
            </div>
            <div>
              <dt className="text-slate-500 dark:text-slate-400">captured by</dt>
              <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">
                GlobalExceptionHandler
              </dd>
            </div>
          </dl>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <Button
            variant="outline"
            size="sm"
            onClick={handleSendBackendTestEvent}
            isLoading={sending}
            disabled={sending}
          >
            <Server className="size-4" />
            {t("backendSentryDiagnostics.sendTestEvent")}
          </Button>

          {sent && (
            <div
              className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-200"
              role="status"
              aria-live="polite"
            >
              <CheckCircle2 className="size-4 shrink-0" />
              <span className="font-medium">{t("backendSentryDiagnostics.successTitle")}</span>
              <span className="text-emerald-700 dark:text-emerald-300">
                — {t("backendSentryDiagnostics.successDescription")}
              </span>
            </div>
          )}

          {unexpectedError && (
            <div
              className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-800 dark:border-red-800/50 dark:bg-red-900/20 dark:text-red-200"
              role="alert"
              aria-live="polite"
            >
              <AlertTriangle className="size-4 shrink-0" />
              <span>{t("backendSentryDiagnostics.errorDescription")}</span>
            </div>
          )}
        </div>

        <p className="text-xs text-slate-500 dark:text-slate-400">
          {t("backendSentryDiagnostics.privacyNote")}
        </p>
      </div>
    </AdminSurface>
  );
}

