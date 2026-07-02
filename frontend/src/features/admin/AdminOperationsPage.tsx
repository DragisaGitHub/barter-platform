import {
  Activity,
  AlertTriangle,
  Boxes,
  CheckCircle2,
  Clock,
  CloudUpload,
  Database,
  DollarSign,
  HardDrive,
  RefreshCw,
  ShieldAlert,
  Users,
} from "lucide-react";
import { useState } from "react";
import * as RadixTabs from "@radix-ui/react-tabs";
import type { TFunction } from "i18next";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/utils";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";
import {
  useAdminOperationsBackups,
  useAdminOperationsCosts,
  useAdminOperationsDeployments,
  useAdminOperationsOverview,
} from "./useAdminOperations";
import type {
  AdminOperationsBackupsResponse,
  AdminOperationsCostsResponse,
  AdminOperationsDeploymentsResponse,
  AdminOperationsOverviewResponse,
} from "@/api/generated/types";

// ── Types ─────────────────────────────────────────────────────────────────────

type BadgeVariant = "default" | "primary" | "success" | "warning" | "danger" | "secondary";

interface MetricItem {
  label: string;
  value: string | number | null | undefined;
  displayValue?: string;
  helper?: string;
  badgeVariant?: BadgeVariant;
}

interface OperationsCardProps {
  title: string;
  description: string;
  icon: typeof Activity;
  metrics: MetricItem[];
}

interface OperationsCardRenderProps extends OperationsCardProps {
  t: TFunction;
  locale: string;
}

type TabId = "overview" | "backups" | "deployments" | "costs" | "monitoring" | "security";

// ── Page ──────────────────────────────────────────────────────────────────────

export function AdminOperationsPage() {
  const { t, i18n } = useTranslation(["admin"]);
  const { data: overviewData, isFetching: overviewFetching, refetch: refetchOverview } =
    useAdminOperationsOverview();
  const { isFetching: costsFetching, refetch: refetchCosts } = useAdminOperationsCosts();
  const healthStatus = overviewData?.health.overallStatus;
  const locale = toIntlLocale(i18n.language);
  const [activeTab, setActiveTab] = useState<TabId>("overview");

  function handleRefetch() {
    if (activeTab === "overview") void refetchOverview();
    if (activeTab === "costs") void refetchCosts();
  }

  const isRefetchingActive =
    (activeTab === "overview" && overviewFetching) ||
    (activeTab === "costs" && costsFetching);

  return (
    <AdminPageShell
      title={t("admin:operationsPage.title")}
      description={t("admin:operationsPage.description")}
      badges={
        <>
          <Badge variant="primary">{t("admin:operationsPage.badges.adminOnly")}</Badge>
          <Badge variant={healthStatus === "UP" ? "success" : healthStatus ? "warning" : "default"}>
            {healthStatus ? formatStatus(healthStatus, t) : t("admin:operationsPage.statusLabels.notLoaded")}
          </Badge>
        </>
      }
      actions={
        (activeTab === "overview" || activeTab === "costs") ? (
          <Button variant="outline" size="sm" onClick={handleRefetch} isLoading={isRefetchingActive}>
            <RefreshCw className="size-4" />
            {t("admin:refresh")}
          </Button>
        ) : undefined
      }
    >
      {/* Tab bar */}
      <RadixTabs.Root
        value={activeTab}
        onValueChange={(v) => setActiveTab(v as TabId)}
        className="flex flex-col gap-6"
      >
        <RadixTabs.List className="flex flex-wrap gap-1 rounded-2xl border border-slate-200 bg-white p-1 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          {(["overview", "backups", "deployments", "costs", "monitoring", "security"] as TabId[]).map(
            (tab) => (
              <RadixTabs.Trigger
                key={tab}
                value={tab}
                className={cn(
                  "rounded-xl px-4 py-2 text-sm font-medium transition-colors",
                  "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white",
                  "data-[state=active]:bg-indigo-50 data-[state=active]:text-indigo-700",
                  "dark:data-[state=active]:bg-indigo-900/30 dark:data-[state=active]:text-indigo-300",
                  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                  ["monitoring", "security"].includes(tab)
                    ? "cursor-default opacity-60"
                    : ""
                )}
              >
                {t(`admin:operationsPage.tabs.${tab}`)}
              </RadixTabs.Trigger>
            )
          )}
        </RadixTabs.List>

        {/* Overview tab */}
        <RadixTabs.Content value="overview" className="flex flex-col gap-6 outline-none">
          <OverviewTabContent t={t} locale={locale} />
        </RadixTabs.Content>

        {/* Backups tab */}
        <RadixTabs.Content value="backups" className="flex flex-col gap-6 outline-none">
          <BackupsTabContent t={t} locale={locale} />
        </RadixTabs.Content>

        {/* Deployments tab */}
        <RadixTabs.Content value="deployments" className="flex flex-col gap-6 outline-none">
          <DeploymentsTabContent t={t} locale={locale} />
        </RadixTabs.Content>

        {/* Costs tab */}
        <RadixTabs.Content value="costs" className="flex flex-col gap-6 outline-none">
          <CostsTabContent t={t} locale={locale} />
        </RadixTabs.Content>

        {/* Coming soon tabs */}
        {(["monitoring", "security"] as const).map((tab) => (
          <RadixTabs.Content key={tab} value={tab} className="outline-none">
            <ComingSoonState t={t} tabKey={tab} />
          </RadixTabs.Content>
        ))}
      </RadixTabs.Root>
    </AdminPageShell>
  );
}

// ── Overview tab ──────────────────────────────────────────────────────────────

function OverviewTabContent({ t, locale }: { t: TFunction; locale: string }) {
  const { data, isLoading, isError, error, refetch } = useAdminOperationsOverview();

  if (isLoading) return <TabLoadingState message={t("admin:operationsPage.loading")} />;
  if (isError) {
    return (
      <TabErrorState
        t={t}
        message={error instanceof Error ? error.message : undefined}
        onRetry={() => refetch()}
      />
    );
  }
  if (!data) return null;
  return <OperationsOverview data={data} t={t} locale={locale} />;
}

// ── Backups tab ───────────────────────────────────────────────────────────────

function BackupsTabContent({ t, locale }: { t: TFunction; locale: string }) {
  const { data, isLoading, isError, error, refetch, isFetching } = useAdminOperationsBackups();

  return (
    <>
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-semibold text-slate-950 dark:text-white">
            {t("admin:operationsPage.backupsTab.title")}
          </h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {t("admin:operationsPage.backupsTab.description")}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isFetching}>
          <RefreshCw className="size-4" />
          {t("admin:refresh")}
        </Button>
      </div>

      {isLoading ? <TabLoadingState message={t("admin:operationsPage.backupsTab.loading")} /> : null}
      {isError ? (
        <TabErrorState
          t={t}
          message={error instanceof Error ? error.message : undefined}
          onRetry={() => refetch()}
          titleKey="admin:operationsPage.backupsTab.errorTitle"
          descriptionKey="admin:operationsPage.backupsTab.errorDescription"
          retryKey="admin:operationsPage.backupsTab.tryAgain"
        />
      ) : null}
      {data ? <BackupsContent data={data} t={t} locale={locale} /> : null}
    </>
  );
}

function BackupsContent({
  data,
  t,
  locale,
}: {
  data: AdminOperationsBackupsResponse;
  t: TFunction;
  locale: string;
}) {
  const isConfigured = data.availability === "configured";
  const isPlaceholder = data.availability === "placeholder";
  const hasBlob = !!data.blobName;

  const availabilityVariant: BadgeVariant =
    isConfigured ? "success" : isPlaceholder ? "warning" : "danger";

  return (
    <div className="flex flex-col gap-4">
      {/* Status header card */}
      <AdminSurface contentClassName="p-0">
        <div className="rounded-2xl border border-slate-200 bg-linear-to-br from-white to-slate-50 p-5 dark:border-slate-800 dark:from-slate-950 dark:to-slate-900">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="flex items-start gap-4">
              <div
                className={cn(
                  "flex size-12 items-center justify-center rounded-2xl",
                  isConfigured && hasBlob
                    ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300"
                    : isConfigured
                    ? "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300"
                    : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
                )}
              >
                {isConfigured && hasBlob ? (
                  <CheckCircle2 className="size-6" />
                ) : (
                  <CloudUpload className="size-6" />
                )}
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-950 dark:text-white">
                  {t("admin:operationsPage.backupsTab.cardTitle")}
                </h3>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  {isConfigured && hasBlob
                    ? t("admin:operationsPage.backupsTab.configuredCardDescription")
                    : t("admin:operationsPage.backupsTab.cardDescription")}
                </p>
              </div>
            </div>
            <div className="flex flex-col items-end gap-2">
              <Badge variant={availabilityVariant}>
                {t(
                  `admin:operationsPage.backupsTab.availability.${data.availability}`,
                  { defaultValue: data.availability }
                )}
              </Badge>
              <Badge variant={data.scheduledBackupEnabled ? "success" : "default"}>
                {data.scheduledBackupEnabled
                  ? t("admin:operationsPage.backupsTab.enabled")
                  : t("admin:operationsPage.backupsTab.disabled")}
              </Badge>
            </div>
          </div>

          {/* Note banner */}
          {data.note && !hasBlob ? (
            <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-800/50 dark:bg-amber-950/30 dark:text-amber-300">
              {data.note}
            </div>
          ) : null}
        </div>
      </AdminSurface>

      {/* No backup found state */}
      {isConfigured && !hasBlob ? (
        <AdminSurface contentClassName="p-0">
          <div className="rounded-2xl border border-amber-200 bg-amber-50/50 p-8 text-center dark:border-amber-800/40 dark:bg-amber-950/20">
            <CloudUpload className="mx-auto size-10 text-amber-500 dark:text-amber-400" />
            <p className="mt-3 font-semibold text-amber-900 dark:text-amber-200">
              {t("admin:operationsPage.backupsTab.noBackupFound")}
            </p>
            <p className="mt-1 text-sm text-amber-700 dark:text-amber-400">
              {t("admin:operationsPage.backupsTab.noBackupFoundDescription")}
            </p>
          </div>
        </AdminSurface>
      ) : null}

      {/* Metadata grid — only shown when a backup blob was found */}
      {hasBlob ? (
        <AdminSurface
          title={t("admin:operationsPage.backupsTab.cardTitle")}
          description={t("admin:operationsPage.backupsTab.configuredCardDescription")}
          contentClassName="space-y-4"
        >
          <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.lastBackup")}
              value={formatDateTime(data.lastBackupTimestamp, t, locale)}
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.blobLastModified")}
              value={formatDateTime(data.blobLastModified, t, locale)}
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.size")}
              value={formatBytes(data.sizeBytes, t)}
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.blobPath")}
              value={data.blobName ?? null}
              mono
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.container")}
              value={data.container ?? null}
              mono
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.prefix")}
              value={data.prefix ?? null}
              mono
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.provider")}
              value={data.storageProvider ?? null}
            />
            <MetricCell
              label={t("admin:operationsPage.backupsTab.metrics.scheduledEnabled")}
              value={
                data.scheduledBackupEnabled
                  ? t("admin:operationsPage.backupsTab.enabled")
                  : t("admin:operationsPage.backupsTab.disabled")
              }
            />
          </dl>
        </AdminSurface>
      ) : null}
    </div>
  );
}

function MetricCell({
  label,
  value,
  mono,
}: {
  label: string;
  value: string | null | undefined;
  mono?: boolean;
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
      <dt className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{label}</dt>
      <dd
        className={cn(
          "mt-2 break-all text-sm font-medium text-slate-900 dark:text-white",
          mono ? "font-mono text-xs" : ""
        )}
      >
        {value ?? "—"}
      </dd>
    </div>
  );
}

// ── Deployments tab ───────────────────────────────────────────────────────────

function DeploymentsTabContent({ t, locale }: { t: TFunction; locale: string }) {
  const { data, isLoading, isError, error, refetch, isFetching } = useAdminOperationsDeployments();

  return (
    <>
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-semibold text-slate-950 dark:text-white">
            {t("admin:operationsPage.deploymentsTab.title")}
          </h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {t("admin:operationsPage.deploymentsTab.description")}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isFetching}>
          <RefreshCw className="size-4" />
          {t("admin:refresh")}
        </Button>
      </div>

      {isLoading ? <TabLoadingState message={t("admin:operationsPage.deploymentsTab.loading")} /> : null}
      {isError ? (
        <TabErrorState
          t={t}
          message={error instanceof Error ? error.message : undefined}
          onRetry={() => refetch()}
        />
      ) : null}
      {data ? <DeploymentsContent data={data} t={t} locale={locale} /> : null}
    </>
  );
}

function DeploymentsContent({
  data,
  t,
  locale,
}: {
  data: AdminOperationsDeploymentsResponse;
  t: TFunction;
  locale: string;
}) {
  return (
    <AdminSurface
      title={t("admin:operationsPage.deploymentsTab.cardTitle")}
      description={t("admin:operationsPage.deploymentsTab.cardDescription")}
      contentClassName="space-y-4"
    >
      <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <MetricCell
          label={t("admin:operationsPage.deploymentsTab.metrics.availability")}
          value={t(
            `admin:operationsPage.deploymentsTab.availability.${data.availability}`,
            { defaultValue: data.availability }
          )}
        />
        <MetricCell
          label={t("admin:operationsPage.deploymentsTab.metrics.environment")}
          value={data.environment}
        />
        <MetricCell
          label={t("admin:operationsPage.deploymentsTab.metrics.currentVersion")}
          value={data.currentVersion ?? null}
        />
        <MetricCell
          label={t("admin:operationsPage.deploymentsTab.metrics.lastDeployment")}
          value={formatDateTime(data.lastDeploymentTimestamp, t, locale)}
        />
        <MetricCell
          label={t("admin:operationsPage.deploymentsTab.metrics.deploymentSource")}
          value={data.deploymentSource ?? null}
        />
      </dl>
      {data.note ? (
        <p className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
          {data.note}
        </p>
      ) : null}
    </AdminSurface>
  );
}

// ── Costs tab ─────────────────────────────────────────────────────────────────

function CostsTabContent({ t, locale }: { t: TFunction; locale: string }) {
  const { data, isLoading, isError, error, refetch, isFetching } = useAdminOperationsCosts();

  return (
    <>
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-semibold text-slate-950 dark:text-white">
            {t("admin:operationsPage.costsTab.title")}
          </h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {t("admin:operationsPage.costsTab.description")}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isFetching}>
          <RefreshCw className="size-4" />
          {t("admin:refresh")}
        </Button>
      </div>

      {isLoading ? <TabLoadingState message={t("admin:operationsPage.costsTab.loading")} /> : null}
      {isError ? (
        <TabErrorState
          t={t}
          message={error instanceof Error ? error.message : undefined}
          onRetry={() => refetch()}
          titleKey="admin:operationsPage.costsTab.errorTitle"
          descriptionKey="admin:operationsPage.costsTab.errorDescription"
          retryKey="admin:operationsPage.costsTab.tryAgain"
        />
      ) : null}
      {data ? <CostsContent data={data} t={t} locale={locale} /> : null}
    </>
  );
}

function CostsContent({
  data,
  t,
  locale,
}: {
  data: AdminOperationsCostsResponse;
  t: TFunction;
  locale: string;
}) {
  const isConfigured = data.availability === "configured";
  const isPlaceholder = data.availability === "placeholder";

  const availabilityVariant: BadgeVariant =
    isConfigured ? "success" : isPlaceholder ? "warning" : "danger";

  return (
    <div className="flex flex-col gap-4">
      {/* Status header */}
      <AdminSurface contentClassName="p-0">
        <div className="rounded-2xl border border-slate-200 bg-linear-to-br from-white to-slate-50 p-5 dark:border-slate-800 dark:from-slate-950 dark:to-slate-900">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="flex items-start gap-4">
              <div
                className={cn(
                  "flex size-12 items-center justify-center rounded-2xl",
                  isConfigured
                    ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300"
                    : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
                )}
              >
                <DollarSign className="size-6" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-950 dark:text-white">
                  {t("admin:operationsPage.costsTab.cardTitle")}
                </h3>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  {isConfigured
                    ? t("admin:operationsPage.costsTab.configuredCardDescription")
                    : t("admin:operationsPage.costsTab.cardDescription")}
                </p>
              </div>
            </div>
            <div className="flex flex-col items-end gap-2">
              <Badge variant={availabilityVariant}>
                {t(
                  `admin:operationsPage.costsTab.availability.${data.availability}`,
                  { defaultValue: data.availability }
                )}
              </Badge>
              {data.currency ? (
                <Badge variant="secondary">{data.currency}</Badge>
              ) : null}
            </div>
          </div>

          {data.note ? (
            <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-800/50 dark:bg-amber-950/30 dark:text-amber-300">
              {data.note}
            </div>
          ) : null}
        </div>
      </AdminSurface>

      {/* Cost summary cards — only when configured */}
      {isConfigured ? (
        <>
          {/* Month totals */}
          <AdminSurface
            title={t("admin:operationsPage.costsTab.summaryTitle")}
            description={t("admin:operationsPage.costsTab.summaryDescription")}
            contentClassName="space-y-4"
          >
            <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <MetricCell
                label={t("admin:operationsPage.costsTab.metrics.currentMonth")}
                value={formatCurrency(data.currentMonthCost, data.currency, locale)}
              />
              <MetricCell
                label={t("admin:operationsPage.costsTab.metrics.previousMonth")}
                value={formatCurrency(data.previousMonthCost, data.currency, locale)}
              />
              <MetricCell
                label={t("admin:operationsPage.costsTab.metrics.projected")}
                value={
                  data.projectedMonthCost != null
                    ? formatCurrency(data.projectedMonthCost, data.currency, locale)
                    : t("admin:operationsPage.statusLabels.unavailable")
                }
              />
              <MetricCell
                label={t("admin:operationsPage.costsTab.metrics.scope")}
                value={data.scope ?? null}
                mono
              />
            </dl>
            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500">
              <span>
                {t("admin:operationsPage.costsTab.metrics.lastUpdated")}:{" "}
                {formatDateTime(data.lastUpdated, t, locale)}
              </span>
            </div>
          </AdminSurface>

          {/* Service breakdown */}
          {data.serviceBreakdown && data.serviceBreakdown.length > 0 ? (
            <AdminSurface
              title={t("admin:operationsPage.costsTab.serviceBreakdownTitle")}
              description={t("admin:operationsPage.costsTab.serviceBreakdownDescription")}
              contentClassName="p-0"
            >
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 dark:border-slate-800">
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                        {t("admin:operationsPage.costsTab.serviceBreakdownColumns.service")}
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                        {t("admin:operationsPage.costsTab.serviceBreakdownColumns.cost")}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.serviceBreakdown.map((entry, idx) => (
                      <tr
                        key={entry.serviceName ?? idx}
                        className="border-b border-slate-100 last:border-0 dark:border-slate-800/60"
                      >
                        <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">
                          {entry.serviceName}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-slate-700 dark:text-slate-300">
                          {formatCurrency(entry.cost, entry.currency, locale)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AdminSurface>
          ) : null}

          {/* Daily trend */}
          {data.dailyTrend && data.dailyTrend.length > 0 ? (
            <AdminSurface
              title={t("admin:operationsPage.costsTab.dailyTrendTitle")}
              description={t("admin:operationsPage.costsTab.dailyTrendDescription")}
              contentClassName="p-0"
            >
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 dark:border-slate-800">
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                        {t("admin:operationsPage.costsTab.dailyTrendColumns.date")}
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                        {t("admin:operationsPage.costsTab.dailyTrendColumns.cost")}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.dailyTrend.map((entry, idx) => (
                      <tr
                        key={entry.date ?? idx}
                        className="border-b border-slate-100 last:border-0 dark:border-slate-800/60"
                      >
                        <td className="px-4 py-3 font-mono text-slate-700 dark:text-slate-300">
                          {entry.date}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-slate-700 dark:text-slate-300">
                          {formatCurrency(entry.cost, data.currency, locale)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AdminSurface>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

// ── Coming soon placeholder ───────────────────────────────────────────────────

function ComingSoonState({ t, tabKey }: { t: TFunction; tabKey: "monitoring" | "security" }) {
  return (
    <AdminSurface contentClassName="p-0">
      <div className="rounded-2xl border border-slate-200 bg-linear-to-br from-white to-slate-50 p-10 text-center dark:border-slate-800 dark:from-slate-950 dark:to-slate-900">
        <Badge variant="secondary" className="mb-4">
          {t("admin:operationsPage.comingSoon.label")}
        </Badge>
        <h3 className="text-base font-semibold text-slate-950 dark:text-white">
          {t(`admin:operationsPage.comingSoon.${tabKey}.title`)}
        </h3>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
          {t(`admin:operationsPage.comingSoon.${tabKey}.description`)}
        </p>
      </div>
    </AdminSurface>
  );
}

// ── Shared states ─────────────────────────────────────────────────────────────

function TabLoadingState({ message }: { message: string }) {
  return (
    <AdminSurface contentClassName="flex min-h-52 items-center justify-center">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-sm text-slate-600 dark:text-slate-400">{message}</p>
      </div>
    </AdminSurface>
  );
}

function TabErrorState({
  t,
  message,
  onRetry,
  titleKey = "admin:operationsPage.errorTitle",
  descriptionKey = "admin:operationsPage.errorDescription",
  retryKey = "admin:operationsPage.tryAgain",
}: {
  t: TFunction;
  message?: string;
  onRetry: () => void;
  titleKey?: string;
  descriptionKey?: string;
  retryKey?: string;
}) {
  return (
    <AdminSurface contentClassName="p-0">
      <div className="rounded-2xl border border-red-200 bg-red-50 p-5 dark:border-red-900/60 dark:bg-red-950/30">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300">
              <AlertTriangle className="size-5" />
            </div>
            <div>
              <h2 className="font-semibold text-red-950 dark:text-red-100">{t(titleKey)}</h2>
              <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                {message ?? t(descriptionKey)}
              </p>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={onRetry}>
            {t(retryKey)}
          </Button>
        </div>
      </div>
    </AdminSurface>
  );
}

// ── Overview cards ────────────────────────────────────────────────────────────

function OperationsOverview({ data, t, locale }: { data: AdminOperationsOverviewResponse; t: TFunction; locale: string }) {
  const cards: OperationsCardProps[] = [
    {
      title: t("admin:operationsPage.cards.systemHealth.title"),
      description: t("admin:operationsPage.cards.systemHealth.description"),
      icon: Activity,
      metrics: [
        { label: t("admin:operationsPage.metrics.application"), value: data.system.applicationName },
        { label: t("admin:operationsPage.metrics.version"), value: data.system.applicationVersion },
        { label: t("admin:operationsPage.metrics.profiles"), value: data.system.activeProfiles.join(", ") },
        { label: t("admin:operationsPage.metrics.serverTime"), value: formatDateTime(data.system.serverTime, t, locale) },
        { label: t("admin:operationsPage.metrics.uptime"), value: formatDuration(data.system.uptimeSeconds, t) },
        {
          label: t("admin:operationsPage.metrics.overallStatus"),
          value: data.health.overallStatus,
          displayValue: formatStatus(data.health.overallStatus, t),
          badgeVariant: statusVariant(data.health.overallStatus),
        },
        {
          label: t("admin:operationsPage.metrics.database"),
          value: data.health.databaseStatus,
          displayValue: formatStatus(data.health.databaseStatus, t),
          badgeVariant: statusVariant(data.health.databaseStatus),
        },
      ],
    },
    {
      title: t("admin:operationsPage.cards.usersSecurity.title"),
      description: t("admin:operationsPage.cards.usersSecurity.description"),
      icon: Users,
      metrics: [
        { label: t("admin:operationsPage.metrics.totalUsers"), value: data.users.totalUsers },
        { label: t("admin:operationsPage.metrics.active"), value: data.users.activeUsers, badgeVariant: "success" },
        { label: t("admin:operationsPage.metrics.pendingVerification"), value: data.users.pendingVerificationUsers, badgeVariant: "warning" },
        { label: t("admin:operationsPage.metrics.suspended"), value: data.users.suspendedUsers, badgeVariant: "warning" },
        { label: t("admin:operationsPage.metrics.banned"), value: data.users.bannedUsers, badgeVariant: "danger" },
      ],
    },
    {
      title: t("admin:operationsPage.cards.marketplaceActivity.title"),
      description: t("admin:operationsPage.cards.marketplaceActivity.description"),
      icon: Boxes,
      metrics: [
        { label: t("admin:operationsPage.metrics.totalItems"), value: data.marketplace.totalItems },
        { label: t("admin:operationsPage.metrics.activeListings"), value: data.marketplace.activeListings, badgeVariant: "success" },
        { label: t("admin:operationsPage.metrics.removedListings"), value: data.marketplace.removedListings, badgeVariant: "warning" },
        { label: t("admin:operationsPage.metrics.openTradeOffers"), value: data.marketplace.openTradeOffers, badgeVariant: "primary" },
        { label: t("admin:operationsPage.metrics.completedTrades"), value: data.marketplace.completedTrades, badgeVariant: "success" },
      ],
    },
    {
      title: t("admin:operationsPage.cards.moderationQueue.title"),
      description: t("admin:operationsPage.cards.moderationQueue.description"),
      icon: ShieldAlert,
      metrics: [
        { label: t("admin:operationsPage.metrics.openReports"), value: data.moderation.openReports, badgeVariant: data.moderation.openReports > 0 ? "warning" : "success" },
        { label: t("admin:operationsPage.metrics.inReview"), value: data.moderation.inReviewReports, badgeVariant: "primary" },
        { label: t("admin:operationsPage.metrics.resolved"), value: data.moderation.resolvedReports },
        { label: t("admin:operationsPage.metrics.dismissed"), value: data.moderation.dismissedReports },
        { label: t("admin:operationsPage.metrics.negativeReviews"), value: data.moderation.negativeReviews, badgeVariant: data.moderation.negativeReviews > 0 ? "warning" : "success" },
      ],
    },
    {
      title: t("admin:operationsPage.cards.storage.title"),
      description: t("admin:operationsPage.cards.storage.description"),
      icon: HardDrive,
      metrics: [
        {
          label: t("admin:operationsPage.metrics.provider"),
          value: data.storage.storageProviderType,
          displayValue: formatStorageProvider(data.storage.storageProviderType, t),
          badgeVariant: "secondary",
        },
        {
          label: t("admin:operationsPage.metrics.providerHealth"),
          value: data.health.storageStatus,
          displayValue: formatStatus(data.health.storageStatus, t),
          helper: storageStatusHelper(data.health.storageStatus, t),
        },
        { label: t("admin:operationsPage.metrics.imageRecords"), value: data.storage.totalImageRecords },
        { label: t("admin:operationsPage.metrics.primaryImages"), value: data.storage.primaryImageCount },
      ],
    },
    {
      title: t("admin:operationsPage.cards.deployment.title"),
      description: t("admin:operationsPage.cards.deployment.description"),
      icon: Clock,
      metrics: [
        { label: t("admin:operationsPage.metrics.environment"), value: data.deployment.environment, badgeVariant: "primary" },
        {
          label: t("admin:operationsPage.metrics.currentRelease"),
          value: data.deployment.releaseVersion,
          badgeVariant: data.deployment.releaseVersion ? "success" : undefined,
        },
        { label: t("admin:operationsPage.metrics.buildVersion"), value: data.deployment.currentVersion },
        { label: t("admin:operationsPage.metrics.lastDeployed"), value: formatDateTime(data.deployment.lastDeploymentTimestamp, t, locale) },
        { label: t("admin:operationsPage.metrics.deploymentSource"), value: data.deployment.deploymentSource },
      ],
    },
  ];

  return (
    <>
      <AdminSurface contentClassName="p-0">
        <div className="rounded-2xl border border-slate-200 bg-linear-to-br from-white to-slate-50 p-5 dark:border-slate-800 dark:from-slate-950 dark:to-slate-900">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-start gap-4">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                <Database className="size-6" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-slate-950 dark:text-white">{t("admin:operationsPage.runtimeOverviewTitle")}</h2>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  {t("admin:operationsPage.runtimeOverviewDescription")}
                </p>
              </div>
            </div>
            <div className="grid gap-2 text-sm sm:grid-cols-2 lg:min-w-80">
              <SummaryPill label={t("admin:operationsPage.metrics.database")} value={formatStatus(data.health.databaseStatus, t)} variant={statusVariant(data.health.databaseStatus)} />
              <SummaryPill label={t("admin:operationsPage.cards.storage.title")} value={formatStorageProvider(data.storage.storageProviderType, t)} variant="secondary" />
            </div>
          </div>
        </div>
      </AdminSurface>

      <div className="grid gap-4 xl:grid-cols-2">
        {cards.map((card) => (
          <OperationsCard key={card.title} {...card} t={t} locale={locale} />
        ))}
      </div>
    </>
  );
}

function OperationsCard({ title, description, icon: Icon, metrics, t, locale }: OperationsCardRenderProps) {
  return (
    <AdminSurface contentClassName="space-y-4">
      <div className="flex items-start gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
          <Icon className="size-5" />
        </div>
        <div>
          <h3 className="font-semibold text-slate-950 dark:text-white">{title}</h3>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{description}</p>
        </div>
      </div>

      <dl className="grid gap-3 sm:grid-cols-2">
        {metrics.map((metric) => (
          <div key={metric.label} className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
            <dt className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{metric.label}</dt>
            <dd className="mt-2 flex flex-wrap items-center gap-2 text-lg font-semibold text-slate-950 dark:text-white">
              {metric.badgeVariant ? (
                <Badge variant={metric.badgeVariant}>{metric.displayValue ?? formatMetric(metric.value, t, locale)}</Badge>
              ) : (
                <span>{metric.displayValue ?? formatMetric(metric.value, t, locale)}</span>
              )}
            </dd>
            {metric.helper ? <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{metric.helper}</p> : null}
          </div>
        ))}
      </dl>
    </AdminSurface>
  );
}

function SummaryPill({ label, value, variant }: { label: string; value: string; variant: BadgeVariant }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2 dark:border-slate-800 dark:bg-slate-950">
      <span className="text-slate-500 dark:text-slate-400">{label}</span>
      <Badge variant={variant}>{value}</Badge>
    </div>
  );
}

// ── Formatters ────────────────────────────────────────────────────────────────

function formatCurrency(
  amount: number | null | undefined,
  currency: string | null | undefined,
  locale: string
): string {
  if (amount == null) return "—";
  try {
    return new Intl.NumberFormat(locale, {
      style: "currency",
      currency: currency ?? "USD",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency ?? "USD"}`;
  }
}

function formatMetric(value: string | number | null | undefined, t: TFunction, locale: string) {
  if (value === null || value === undefined || value === "") {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  if (typeof value === "number") {
    return new Intl.NumberFormat(locale).format(value);
  }
  return value;
}

function formatDateTime(value: string | null | undefined, t: TFunction, locale: string) {
  if (!value) {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(parsed);
}

function formatBytes(bytes: number | null | undefined, t: TFunction): string {
  if (bytes === null || bytes === undefined) {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function formatDuration(seconds: number | null | undefined, t: TFunction) {
  if (seconds === null || seconds === undefined) {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  if (seconds < 60) {
    return t("admin:operationsPage.duration.seconds", { value: seconds });
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return t("admin:operationsPage.duration.minutes", { value: minutes });
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    return t("admin:operationsPage.duration.hoursMinutes", { hours, minutes: minutes % 60 });
  }
  const days = Math.floor(hours / 24);
  return t("admin:operationsPage.duration.daysHours", { days, hours: hours % 24 });
}

function formatStatus(status: string | null | undefined, t: TFunction) {
  if (!status) {
    return t("admin:operationsPage.statusLabels.unknown");
  }
  return t(`admin:operationsPage.statusLabels.${status}`, { defaultValue: status });
}

function formatStorageProvider(value: string | null | undefined, t: TFunction) {
  if (!value) {
    return t("admin:operationsPage.statusLabels.unavailable");
  }
  return t(`admin:operationsPage.storageProviders.${value}`, { defaultValue: value });
}

function storageStatusHelper(status: string | null | undefined, t: TFunction) {
  if (status === "CONFIGURED_NOT_CHECKED") {
    return t("admin:operationsPage.helpers.storageConfiguredNotChecked");
  }
  return undefined;
}

function toIntlLocale(language: string) {
  return language.startsWith("sr") ? "sr-Latn-RS" : "en-US";
}

function statusVariant(status: string | null | undefined): BadgeVariant {
  if (status === "UP") {
    return "success";
  }
  if (status === "DOWN") {
    return "danger";
  }
  if (status === "DEGRADED" || status === "CONFIGURED_NOT_CHECKED") {
    return "warning";
  }
  return "default";
}

