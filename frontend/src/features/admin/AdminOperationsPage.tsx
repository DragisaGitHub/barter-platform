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
  helper?: string;
  badgeVariant?: BadgeVariant;
}

interface OperationsCardProps {
  title: string;
  description: string;
  icon: typeof Activity;
  metrics: MetricItem[];
}

export function AdminOperationsPage() {
  const { data, isLoading, isError, error, refetch, isFetching } = useAdminOperationsOverview();
  const healthStatus = data?.health.overallStatus;

  return (
    <AdminPageShell
      title="Operational dashboard"
      description="ADMIN-only runtime visibility for launch and beta operations. This summarizes safe counters and status signals without exposing secrets, raw configuration, or PII-heavy lists."
      badges={
        <>
          <Badge variant="primary">Admin only</Badge>
          <Badge variant={healthStatus === "UP" ? "success" : healthStatus ? "warning" : "default"}>
            {healthStatus ?? "Not loaded"}
          </Badge>
        </>
      }
      actions={
        <Button variant="outline" size="sm" onClick={() => refetch()} isLoading={isFetching}>
          <RefreshCw className="size-4" />
          Refresh
        </Button>
      }
    >
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState message={error instanceof Error ? error.message : undefined} onRetry={() => refetch()} /> : null}
      {data ? <OperationsOverview data={data} /> : null}
    </AdminPageShell>
  );
}

function LoadingState() {
  return (
    <AdminSurface contentClassName="flex min-h-64 items-center justify-center">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-sm text-slate-600 dark:text-slate-400">Loading operational overview…</p>
      </div>
    </AdminSurface>
  );
}

function ErrorState({ message, onRetry }: { message?: string; onRetry: () => void }) {
  return (
    <AdminSurface contentClassName="p-0">
      <div className="rounded-2xl border border-red-200 bg-red-50 p-5 dark:border-red-900/60 dark:bg-red-950/30">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300">
              <AlertTriangle className="size-5" />
            </div>
            <div>
              <h2 className="font-semibold text-red-950 dark:text-red-100">Operational overview unavailable</h2>
              <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                {message ?? "The admin operations endpoint could not be reached."}
              </p>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={onRetry}>
            Try again
          </Button>
        </div>
      </div>
    </AdminSurface>
  );
}

function OperationsOverview({ data }: { data: AdminOperationsOverviewResponse }) {
  const cards: OperationsCardProps[] = [
    {
      title: "System Health",
      description: "Runtime identity and lightweight backend status.",
      icon: Activity,
      metrics: [
        { label: "Application", value: data.system.applicationName },
        { label: "Version", value: data.system.applicationVersion },
        { label: "Profiles", value: data.system.activeProfiles.join(", ") },
        { label: "Server time", value: formatDateTime(data.system.serverTime) },
        { label: "Uptime", value: formatDuration(data.system.uptimeSeconds) },
        { label: "Overall status", value: data.health.overallStatus, badgeVariant: statusVariant(data.health.overallStatus) },
        { label: "Database", value: data.health.databaseStatus, badgeVariant: statusVariant(data.health.databaseStatus) },
      ],
    },
    {
      title: "Users & Security",
      description: "Account state counters for identity operations.",
      icon: Users,
      metrics: [
        { label: "Total users", value: data.users.totalUsers },
        { label: "Active", value: data.users.activeUsers, badgeVariant: "success" },
        { label: "Pending verification", value: data.users.pendingVerificationUsers, badgeVariant: "warning" },
        { label: "Suspended", value: data.users.suspendedUsers, badgeVariant: "warning" },
        { label: "Banned", value: data.users.bannedUsers, badgeVariant: "danger" },
      ],
    },
    {
      title: "Marketplace Activity",
      description: "Listing and trade-offer counters for operational awareness.",
      icon: Boxes,
      metrics: [
        { label: "Total items", value: data.marketplace.totalItems },
        { label: "Active listings", value: data.marketplace.activeListings, badgeVariant: "success" },
        { label: "Removed listings", value: data.marketplace.removedListings, badgeVariant: "warning" },
        { label: "Open trade offers", value: data.marketplace.openTradeOffers, badgeVariant: "primary" },
        { label: "Completed trades", value: data.marketplace.completedTrades, badgeVariant: "success" },
      ],
    },
    {
      title: "Moderation Queue",
      description: "Report queue state and negative review signal.",
      icon: ShieldAlert,
      metrics: [
        { label: "Open reports", value: data.moderation.openReports, badgeVariant: data.moderation.openReports > 0 ? "warning" : "success" },
        { label: "In review", value: data.moderation.inReviewReports, badgeVariant: "primary" },
        { label: "Resolved", value: data.moderation.resolvedReports },
        { label: "Dismissed", value: data.moderation.dismissedReports },
        { label: "Negative reviews", value: data.moderation.negativeReviews, badgeVariant: data.moderation.negativeReviews > 0 ? "warning" : "success" },
      ],
    },
    {
      title: "Storage",
      description: "Image metadata counts and configured provider type.",
      icon: HardDrive,
      metrics: [
        { label: "Provider", value: data.storage.storageProviderType, badgeVariant: "secondary" },
        { label: "Provider health", value: data.health.storageStatus, helper: data.health.storageStatusDetail ?? undefined },
        { label: "Image records", value: data.storage.totalImageRecords },
        { label: "Primary images", value: data.storage.primaryImageCount },
      ],
    },
    {
      title: "Deployment",
      description: "Safe environment metadata available to the application.",
      icon: Clock,
      metrics: [
        { label: "Environment", value: data.deployment.environment, badgeVariant: "primary" },
        { label: "Deployment state", value: data.deployment.deploymentStateAvailability },
        { label: "Last deployment", value: formatDateTime(data.deployment.lastDeploymentTimestamp) },
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
                <h2 className="text-lg font-semibold text-slate-950 dark:text-white">Runtime overview</h2>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  Counts are loaded from simple aggregate queries. Storage health avoids expensive remote blob calls.
                </p>
              </div>
            </div>
            <div className="grid gap-2 text-sm sm:grid-cols-2 lg:min-w-80">
              <SummaryPill label="Database" value={data.health.databaseStatus} variant={statusVariant(data.health.databaseStatus)} />
              <SummaryPill label="Storage" value={data.storage.storageProviderType} variant="secondary" />
            </div>
          </div>
        </div>
      </AdminSurface>

      <div className="grid gap-4 xl:grid-cols-2">
        {cards.map((card) => (
          <OperationsCard key={card.title} {...card} />
        ))}
      </div>
    </>
  );
}

function OperationsCard({ title, description, icon: Icon, metrics }: OperationsCardProps) {
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
                <Badge variant={metric.badgeVariant}>{formatMetric(metric.value)}</Badge>
              ) : (
                <span>{formatMetric(metric.value)}</span>
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

function formatMetric(value: string | number | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return "Unavailable";
  }
  if (typeof value === "number") {
    return new Intl.NumberFormat().format(value);
  }
  return value;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Unavailable";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "Unavailable";
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(parsed);
}

function formatDuration(seconds: number | null | undefined) {
  if (seconds === null || seconds === undefined) {
    return "Unavailable";
  }
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    return `${hours}h ${minutes % 60}m`;
  }
  const days = Math.floor(hours / 24);
  return `${days}d ${hours % 24}h`;
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

