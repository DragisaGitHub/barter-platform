import { Link } from "react-router-dom";
import {
  ArrowRight,
  Flag,
  FolderTree,
  KeyRound,
  Lock,
  Settings,
  ShieldCheck,
  Tags,
  Users,
  type LucideIcon,
} from "lucide-react";
import { Badge } from "../../components/ui/Badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/Card";
import { routePaths } from "@/routes/routePaths.ts";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";
import { useTranslation } from "react-i18next";
import { useAdminReportQueueSummary } from "./useAdminReports";

interface AdminModule {
  titleKey: string;
  descriptionKey: string;
  to: string;
  icon: LucideIcon;
  tone: string;
  status?: string;
}

const adminModules: AdminModule[] = [
  {
    titleKey: "users",
    descriptionKey: "dashboard.modules.users",
    to: routePaths.admin.users,
    icon: Users,
    tone: "bg-indigo-100 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-300",
  },
  {
    titleKey: "roles",
    descriptionKey: "dashboard.modules.roles",
    to: routePaths.admin.roles,
    icon: KeyRound,
    tone: "bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-300",
  },
  {
    titleKey: "permissions",
    descriptionKey: "dashboard.modules.permissions",
    to: routePaths.admin.permissions,
    icon: Lock,
    tone: "bg-violet-100 text-violet-600 dark:bg-violet-900/30 dark:text-violet-300",
  },
  {
    titleKey: "system",
    descriptionKey: "dashboard.modules.system",
    to: routePaths.admin.system,
    icon: Settings,
    tone: "bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-300",
  },
  {
    titleKey: "reports",
    descriptionKey: "dashboard.modules.reports",
    to: routePaths.admin.reports,
    icon: Flag,
    tone: "bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300",
  },
  {
    titleKey: "categories",
    descriptionKey: "dashboard.modules.categories",
    to: routePaths.admin.categories,
    icon: FolderTree,
    tone: "bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
  },
  {
    titleKey: "tags",
    descriptionKey: "dashboard.modules.tags",
    to: routePaths.admin.tags,
    icon: Tags,
    tone: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300",
  },
];

export function AdminDashboardPage() {
  const { t } = useTranslation("admin");
  const summaryQuery = useAdminReportQueueSummary();
  const unresolvedReports = (summaryQuery.data?.openCount ?? 0) + (summaryQuery.data?.inReviewCount ?? 0);

  return (
    <AdminPageShell
      title={t("adminControlPanel")}
      description={t("dashboard.description")}
      badges={
        <>
          <Badge variant="primary">{t("dashboard.badges.operations")}</Badge>
          <Badge>{t("dashboard.badges.internalWorkspace")}</Badge>
        </>
      }
      actions={
        <div className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-300 xl:max-w-md">
          {t("dashboard.actionsHelper")}
        </div>
      }
    >
      <AdminSurface
        title={t("dashboard.operationalModules")}
        description={t("dashboard.operationalModulesDescription")}
        contentClassName="grid gap-4 md:grid-cols-2 xl:grid-cols-3"
      >
        {adminModules.map((module) => {
          const isReportsModule = module.to === routePaths.admin.reports;
          const content = (
            <Card className="flex h-full flex-col justify-between border-slate-200 bg-gradient-to-br from-white to-slate-50/70 transition-all hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:from-slate-900 dark:to-slate-950">
              <CardHeader>
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div className={`flex size-12 items-center justify-center rounded-xl ${module.tone}`}>
                    <module.icon className="size-5" />
                  </div>
                  {isReportsModule ? (
                    <Badge variant={unresolvedReports > 0 ? "warning" : "success"}>
                      {summaryQuery.isLoading
                        ? "…"
                        : t("dashboard.pendingReportsCount", { count: unresolvedReports })}
                    </Badge>
                  ) : (
                    <Badge variant="success">{t("dashboard.ready")}</Badge>
                  )}
                </div>
                <CardTitle>{t(module.titleKey)}</CardTitle>
                <CardDescription>{t(module.descriptionKey)}</CardDescription>
                {isReportsModule && summaryQuery.data ? (
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    {t("dashboard.reportCounters", {
                      open: summaryQuery.data.openCount,
                      inReview: summaryQuery.data.inReviewCount,
                      stale: summaryQuery.data.staleOpenCount,
                    })}
                  </p>
                ) : null}
              </CardHeader>
              <CardContent className="mt-auto flex items-center justify-between pt-2 text-sm font-medium text-indigo-600 dark:text-indigo-300">
                <span>{t("dashboard.openModule")}</span>
                <ArrowRight className="size-4" />
              </CardContent>
            </Card>
          );

          return (
            <Link
              key={module.to}
              to={module.to}
              className="block h-full rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:focus:ring-offset-slate-950"
            >
              {content}
            </Link>
          );
        })}
      </AdminSurface>

      <AdminSurface
        title={t("dashboard.foundationStatus")}
        description={t("dashboard.foundationStatusDescription")}
        contentClassName="grid gap-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]"
      >
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-300">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                {t("dashboard.dedicatedExperience.title")}
              </h3>
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                {t("dashboard.dedicatedExperience.description")}
              </p>
            </div>
          </div>
        </div>

        <div className="grid gap-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">{t("dashboard.currentScope.title")}</p>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
              {t("dashboard.currentScope.description")}
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">{t("dashboard.preservedBehavior.title")}</p>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
              {t("dashboard.preservedBehavior.description")}
            </p>
          </div>
        </div>
      </AdminSurface>
    </AdminPageShell>
  );
}
