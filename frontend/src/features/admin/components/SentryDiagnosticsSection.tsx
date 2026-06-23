import { useState } from "react";
import * as Sentry from "@sentry/react";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AlertTriangle, Bug, CheckCircle2 } from "lucide-react";
import { isSentryEnabled } from "@/lib/sentry";
import { AdminSurface } from "./AdminPageShell";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";

const ENVIRONMENT = import.meta.env.VITE_SENTRY_ENVIRONMENT || "development";

export function SentryDiagnosticsSection() {
  const { t } = useTranslation("admin");
  const location = useLocation();
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);

  const sentryActive = isSentryEnabled();

  function handleSendTestEvent() {
    if (!sentryActive) return;

    setSending(true);
    try {
      Sentry.withScope((scope) => {
        scope.setTag("source", "admin-diagnostics");
        scope.setTag("environment", ENVIRONMENT);
        scope.setContext("diagnostics", {
          source: "admin-diagnostics",
          environment: ENVIRONMENT,
          route: location.pathname,
          timestamp: new Date().toISOString(),
        });
        Sentry.captureException(new Error("Manual Sentry diagnostics test"));
      });
      setSent(true);
    } finally {
      setSending(false);
    }
  }

  return (
    <AdminSurface
      title={t("sentryDiagnostics.title")}
      description={t("sentryDiagnostics.description")}
      actions={
        sentryActive ? (
          <Badge variant="success">{t("sentryDiagnostics.statusActive")}</Badge>
        ) : (
          <Badge variant="danger">{t("sentryDiagnostics.statusDisabled")}</Badge>
        )
      }
      contentClassName="space-y-5"
    >
      {!sentryActive ? (
        <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-5 dark:border-amber-800/50 dark:bg-amber-900/20">
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-600 dark:text-amber-400" />
          <div>
            <p className="text-sm font-semibold text-amber-900 dark:text-amber-200">
              {t("sentryDiagnostics.disabledTitle")}
            </p>
            <p className="mt-1 text-sm text-amber-700 dark:text-amber-300">
              {t("sentryDiagnostics.disabledDescription")}
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
              {t("sentryDiagnostics.metadataSent")}
            </p>
            <dl className="mt-3 grid gap-2 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-slate-500 dark:text-slate-400">source</dt>
                <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">admin-diagnostics</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">environment</dt>
                <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">{ENVIRONMENT}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">route</dt>
                <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">{location.pathname}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">timestamp</dt>
                <dd className="font-mono font-medium text-slate-900 dark:text-slate-100">
                  {t("sentryDiagnostics.capturedAtSend")}
                </dd>
              </div>
            </dl>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <Button
              variant="outline"
              size="sm"
              onClick={handleSendTestEvent}
              isLoading={sending}
              disabled={sending}
            >
              <Bug className="size-4" />
              {t("sentryDiagnostics.sendTestEvent")}
            </Button>

            {sent && (
              <div
                className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-200"
                role="status"
                aria-live="polite"
              >
                <CheckCircle2 className="size-4 shrink-0" />
                <span className="font-medium">{t("sentryDiagnostics.successTitle")}</span>
                <span className="text-emerald-700 dark:text-emerald-300">
                  — {t("sentryDiagnostics.successDescription")}
                </span>
              </div>
            )}
          </div>

          <p className="text-xs text-slate-500 dark:text-slate-400">
            {t("sentryDiagnostics.privacyNote")}
          </p>
        </div>
      )}
    </AdminSurface>
  );
}

