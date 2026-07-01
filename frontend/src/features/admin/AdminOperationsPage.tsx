import {
  Activity,
  AlertTriangle,
  Boxes,
  Clock,
  Database,
  HardDrive,
  RefreshCw,
  ShieldAlert,
  Users,
} from "lucide-react";
import type { TFunction } from "i18next";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";
import { useAdminOperationsOverview } from "./useAdminOperations";
import type { AdminOperationsOverviewResponse } from "@/api/generated/types";

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

export function AdminOperationsPage() {
  const { t, i18n } = useTranslation(["admin"]);
  const { data, isLoading, isError, error, refetch, isFetching } = useAdminOperationsOverview();
  const healthStatus = data?.health.overallStatus;
  const locale = toIntlLocale(i18n.language);

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
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isFetching}>
          <RefreshCw className="size-4" />
          {t("admin:refresh")}
        </Button>
      }
    >
      {isLoading ? <LoadingState t={t} /> : null}
      {isError ? <ErrorState t={t} message={error instanceof Error ? error.message : undefined} onRetry={() => refetch()} /> : null}
      {data ? <OperationsOverview data={data} t={t} locale={locale} /> : null}
    </AdminPageShell>
  );
}

function LoadingState({ t }: { t: TFunction }) {
  return (
    <AdminSurface contentClassName="flex min-h-64 items-center justify-center">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-sm text-slate-600 dark:text-slate-400">{t("admin:operationsPage.loading")}</p>
      </div>
    </AdminSurface>
  );
}

function ErrorState({ t, message, onRetry }: { t: TFunction; message?: string; onRetry: () => void }) {
  return (
    <AdminSurface contentClassName="p-0">
      <div className="rounded-2xl border border-red-200 bg-red-50 p-5 dark:border-red-900/60 dark:bg-red-950/30">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300">
              <AlertTriangle className="size-5" />
            </div>
            <div>
              <h2 className="font-semibold text-red-950 dark:text-red-100">{t("admin:operationsPage.errorTitle")}</h2>
              <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                {message ?? t("admin:operationsPage.errorDescription")}
              </p>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={onRetry}>
            {t("admin:operationsPage.tryAgain")}
          </Button>
        </div>
      </div>
    </AdminSurface>
  );
}

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

