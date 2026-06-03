import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { MessageSquareQuote, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import type { BetaFeedbackCategory, BetaFeedbackStatus } from "@/api/generated/types";
import { Pagination } from "@/components/data/Pagination";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { parseApiError } from "@/utils";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useAdminBetaFeedback, useUpdateAdminBetaFeedbackStatus } from "./useAdminBetaFeedback";

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

const STATUS_OPTIONS: Array<{ value: BetaFeedbackStatus | ""; labelKey: string }> = [
  { value: "", labelKey: "admin:allStatuses" },
  { value: "NEW", labelKey: "feedback:admin.status.new" },
  { value: "REVIEWED", labelKey: "feedback:admin.status.reviewed" },
  { value: "RESOLVED", labelKey: "feedback:admin.status.resolved" },
];

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString();
}

function previewMessage(message?: string | null) {
  if (!message?.trim()) {
    return "—";
  }
  const trimmed = message.trim();
  return trimmed.length > 140 ? `${trimmed.slice(0, 137)}…` : trimmed;
}

function feedbackStatusBadgeVariant(status: BetaFeedbackStatus): "primary" | "warning" | "success" {
  switch (status) {
    case "NEW":
      return "warning";
    case "REVIEWED":
      return "primary";
    case "RESOLVED":
      return "success";
  }
}

function feedbackCategoryTranslationKey(category: BetaFeedbackCategory) {
  return `feedback:categories.${category}`;
}

export function AdminBetaFeedbackPage() {
  const { t } = useTranslation(["admin", "feedback", "common"]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [sort] = useState("createdAt,desc");
  const [status, setStatus] = useState<BetaFeedbackStatus | "">("NEW");

  const params = useMemo(
    () => ({
      page,
      size: pageSize,
      sort,
      status: status || undefined,
    }),
    [page, pageSize, sort, status],
  );

  const feedbackQuery = useAdminBetaFeedback(params);
  const updateStatusMutation = useUpdateAdminBetaFeedbackStatus();
  const data = feedbackQuery.data;
  const feedbackItems = data?.content ?? [];
  const totalItems = data?.totalElements ?? 0;
  const rangeStart = totalItems === 0 ? 0 : page * pageSize + 1;
  const rangeEnd = totalItems === 0 ? 0 : rangeStart + feedbackItems.length - 1;
  const hasFilters = !!status;

  const updateStatus = useMutation({
    mutationFn: ({ feedbackUuid, nextStatus }: { feedbackUuid: string; nextStatus: BetaFeedbackStatus }) =>
      updateStatusMutation.mutateAsync({ feedbackUuid, data: { status: nextStatus } }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.nextStatus === "REVIEWED"
          ? t("feedback:admin.reviewSuccess")
          : t("feedback:admin.resolveSuccess"),
      );
    },
    onError: (error) => {
      toast.error(parseApiError(error));
    },
  });

  const resetFilters = () => {
    setStatus("NEW");
    setPage(0);
  };

  return (
    <AdminPageShell
      title={t("feedback:admin.title")}
      description={t("feedback:admin.description")}
      badges={
        <>
          <Badge variant="primary">{t("navigation:betaFeedback")}</Badge>
          <Badge>{t("feedback:badge")}</Badge>
        </>
      }
      toolbar={
        <AdminToolbar>
          <div className="grid flex-1 gap-3 lg:grid-cols-2">
            <select
              value={status}
              onChange={(event) => {
                setPage(0);
                setStatus(event.target.value as BetaFeedbackStatus | "");
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.labelKey} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
              <span>{t("admin:rows")}</span>
              <select
                value={pageSize}
                onChange={(event) => {
                  setPage(0);
                  setPageSize(Number(event.target.value));
                }}
                className="bg-transparent text-sm font-medium text-slate-900 outline-none dark:text-slate-100"
                aria-label="Beta feedback rows per page"
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
            <Button variant="outline" onClick={resetFilters} disabled={!hasFilters}>
              {t("admin:resetFilters")}
            </Button>
          </div>
        </AdminToolbar>
      }
    >
      <AdminSurface
        title={t("feedback:admin.inboxTitle")}
        description={t("feedback:admin.inboxDescription", { count: totalItems })}
        contentClassName="space-y-0"
      >
        {feedbackQuery.isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : feedbackQuery.isError || !data ? (
          <EmptyState
            icon={<ShieldAlert className="size-12" />}
            title={t("feedback:admin.loadErrorTitle")}
            description={t("feedback:admin.loadErrorDescription")}
            action={
              <Button variant="outline" onClick={() => feedbackQuery.refetch()}>
                {t("common:tryAgain")}
              </Button>
            }
          />
        ) : feedbackItems.length === 0 ? (
          <EmptyState
            icon={<MessageSquareQuote className="size-12" />}
            title={t("feedback:admin.emptyTitle")}
            description={
              hasFilters
                ? t("feedback:admin.emptyFilteredDescription")
                : t("feedback:admin.emptyDescription")
            }
            action={
              hasFilters ? (
                <Button variant="outline" onClick={resetFilters}>
                  {t("admin:clearFilters")}
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 dark:bg-slate-900/60">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.status")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.category")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.message")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.username")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.sourcePage")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.createdAt")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("feedback:admin.columns.actions")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                  {feedbackItems.map((item) => {
                    const isUpdating = updateStatus.isPending && updateStatus.variables?.feedbackUuid === item.uuid;
                    return (
                      <tr key={item.uuid} className="hover:bg-slate-50 dark:hover:bg-slate-900/50">
                        <td className="px-4 py-3">
                          <Badge variant={feedbackStatusBadgeVariant(item.status)}>
                            {t(`feedback:admin.status.${item.status.toLowerCase()}`)}
                          </Badge>
                        </td>
                        <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                          {t(feedbackCategoryTranslationKey(item.category))}
                        </td>
                        <td className="max-w-sm px-4 py-3 text-slate-600 dark:text-slate-300">
                          <div className="font-medium text-slate-900 dark:text-slate-100">{previewMessage(item.message)}</div>
                          {item.email ? <div className="mt-1 text-xs text-slate-500">{item.email}</div> : null}
                        </td>
                        <td className="px-4 py-3 text-slate-700 dark:text-slate-300">{item.username}</td>
                        <td className="px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-400">{item.sourcePage || "—"}</td>
                        <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{formatDateTime(item.createdAt)}</td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-2">
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={item.status !== "NEW" || isUpdating}
                              onClick={() => updateStatus.mutate({ feedbackUuid: item.uuid, nextStatus: "REVIEWED" })}
                            >
                              {t("feedback:admin.actions.review")}
                            </Button>
                            <Button
                              size="sm"
                              disabled={item.status === "RESOLVED" || isUpdating}
                              onClick={() => updateStatus.mutate({ feedbackUuid: item.uuid, nextStatus: "RESOLVED" })}
                            >
                              {t("feedback:admin.actions.resolve")}
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <Pagination
              currentPage={data.page ?? 0}
              totalPages={Math.max(data.totalPages ?? 0, 1)}
              onPageChange={setPage}
              statusContent={
                <>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    {totalItems === 0
                      ? t("feedback:admin.noItemsMatched")
                      : t("admin:showingRange", { start: rangeStart, end: rangeEnd, total: totalItems })}
                  </span>
                  {feedbackQuery.isFetching && (
                    <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                      {t("admin:refreshing")}
                    </span>
                  )}
                </>
              }
            />
          </>
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}

