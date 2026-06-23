import { useQuery } from "@tanstack/react-query";
import { Activity, CheckCircle, RefreshCw, XCircle } from "lucide-react";
import { apiClient } from "@/api/axios.ts";
import type { PingResponse } from "@/api/generated/types.ts";
import { Button } from "../../components/ui/Button";
import { Badge } from "../../components/ui/Badge";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";
import { SentryDiagnosticsSection } from "./components/SentryDiagnosticsSection";
import { BackendSentryDiagnosticsSection } from "./components/BackendSentryDiagnosticsSection";
import { useTranslation } from "react-i18next";

export function SystemPage() {
  const { t } = useTranslation("admin");
  const { data: ping, isLoading, isError, refetch } = useQuery({
    queryKey: ["ping"],
    queryFn: async () => {
      const response = await apiClient.get<PingResponse>("/ping");
      return response.data;
    },
    retry: 1,
  });

  const isHealthy = !!ping?.message;

  return (
    <AdminPageShell
      title={t("system")}
      description={t("systemPage.description")}
      badges={
        <>
          <Badge variant="primary">{t("operations")}</Badge>
          <Badge>{isHealthy ? t("systemPage.endpointReachable") : t("systemPage.healthCheckRequired")}</Badge>
        </>
      }
      actions={
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isLoading}>
          <RefreshCw className="size-4" />
          {t("refresh")}
        </Button>
      }
    >
      <AdminSurface
        title={t("systemPage.apiHealthTitle")}
        description={t("systemPage.apiHealthDescription")}
        contentClassName="space-y-6"
      >
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="flex items-start gap-4">
              <div
                className={`flex size-12 items-center justify-center rounded-2xl ${
                  isHealthy
                    ? "bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-300"
                    : "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-300"
                }`}
              >
                {isHealthy ? <CheckCircle className="size-6" /> : <XCircle className="size-6" />}
              </div>

              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-xl font-semibold text-slate-900 dark:text-white">
                    {isHealthy ? t("systemPage.systemOnline") : t("systemPage.systemStatusUnknown")}
                  </h2>
                  <Badge variant={isHealthy ? "success" : "danger"}>
                    {isHealthy ? t("systemPage.healthy") : t("systemPage.unavailable")}
                  </Badge>
                </div>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                  {isHealthy
                    ? t("systemPage.backendResponded")
                    : isError
                      ? t("systemPage.backendUnreachable")
                      : t("systemPage.invalidHealthResponse")}
                </p>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
              <div className="flex items-center gap-2 font-medium text-slate-900 dark:text-slate-100">
                <Activity className="size-4" />
                {t("systemPage.healthEndpoint")}
              </div>
              <p className="mt-1 font-mono text-xs text-slate-500 dark:text-slate-400">GET /ping</p>
            </div>
          </div>
        </div>

        <div className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
              {t("systemPage.responseSummary")}
            </p>
            <dl className="mt-4 space-y-3 text-sm">
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t("systemPage.message")}</dt>
                <dd className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                  {ping?.message ?? t("systemPage.noResponsePayload")}
                </dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t("systemPage.status")}</dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {isHealthy ? t("systemPage.healthyResponseReceived") : t("systemPage.healthResponseMissing")}
                </dd>
              </div>
            </dl>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
              {t("systemPage.rawPayload")}
            </p>
            <pre className="mt-4 overflow-x-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-100">
              {JSON.stringify(
                ping ?? { error: isError ? t("systemPage.unableToReachBackendApi") : t("systemPage.noPayload") },
                null,
                2
              )}
            </pre>
          </div>
        </div>
      </AdminSurface>

      <SentryDiagnosticsSection />

      <BackendSentryDiagnosticsSection />
    </AdminPageShell>
  );
}
