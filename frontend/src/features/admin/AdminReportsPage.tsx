import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Clock3, ExternalLink, Flag, SearchCheck, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import type {
  AdminListingDetailResponse,
  ReportReasonCode,
  ReportStatus,
  ReportTargetType,
} from "@/api/generated/types";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/data/Pagination";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { parseApiError } from "@/utils";
import { routePaths } from "@/routes/routePaths";
import {
  REPORT_REASON_OPTIONS,
  REPORT_STATUS_OPTIONS,
  REPORT_TARGET_TYPE_OPTIONS,
  reportReasonTranslationKey,
  reportStatusBadgeVariant,
  reportStatusTranslationKey,
  reportTargetTypeTranslationKey,
} from "@/features/reports/reporting";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { AdminListingModerationDialog } from "./AdminListingModerationDialog";
import {
  useAdminReport,
  useAdminReportQueueSummary,
  useAdminReports,
  useUpdateAdminReport,
} from "./useAdminReports";
import {
  useAdminListing,
  useRemoveAdminListing,
  useRestoreAdminListing,
} from "./useAdminListings";
import { useTranslation } from "react-i18next";

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

function normalizeNote(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : "";
}

function isTerminalReportStatus(status: ReportStatus) {
  return status === "RESOLVED" || status === "DISMISSED";
}

function allowedStatusOptions(currentStatus: ReportStatus): ReportStatus[] {
  switch (currentStatus) {
    case "OPEN":
      return ["OPEN", "IN_REVIEW", "RESOLVED", "DISMISSED"];
    case "IN_REVIEW":
      return ["IN_REVIEW", "RESOLVED", "DISMISSED"];
    case "RESOLVED":
      return ["RESOLVED"];
    case "DISMISSED":
      return ["DISMISSED"];
  }
}

function reportAgeLabel(createdAt?: string | null) {
  if (!createdAt) {
    return "—";
  }

  const createdAtDate = new Date(createdAt).getTime();
  const ageMs = Date.now() - createdAtDate;
  const ageHours = Math.max(1, Math.floor(ageMs / (1000 * 60 * 60)));

  if (ageHours >= 48) {
    return `${Math.floor(ageHours / 24)}d`;
  }

  return `${ageHours}h`;
}

function isStaleOpenReport(createdAt: string | null | undefined, staleThresholdHours: number, status: ReportStatus) {
  if (!createdAt || status !== "OPEN") {
    return false;
  }

  const ageMs = Date.now() - new Date(createdAt).getTime();
  return ageMs >= staleThresholdHours * 60 * 60 * 1000;
}

function listingStatusBadgeVariant(listing?: AdminListingDetailResponse): "success" | "danger" | "secondary" {
  if (!listing) {
    return "secondary";
  }

  return listing.status === "REMOVED" ? "danger" : "success";
}

interface SummaryMetricCardProps {
  title: string;
  value: string | number;
  helper: string;
  toneClassName: string;
}

function SummaryMetricCard({ title, value, helper, toneClassName }: SummaryMetricCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-950">
      <div className={`inline-flex rounded-xl px-3 py-1 text-xs font-semibold ${toneClassName}`}>{title}</div>
      <div className="mt-3 text-3xl font-semibold text-slate-900 dark:text-slate-100">{value}</div>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{helper}</p>
    </div>
  );
}

export function AdminReportsPage() {
  const { t } = useTranslation(["admin", "reporting", "common"]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [sort] = useState("createdAt,desc");
  const [status, setStatus] = useState<ReportStatus | "">("");
  const [targetType, setTargetType] = useState<ReportTargetType | "">("");
  const [reasonCode, setReasonCode] = useState<ReportReasonCode | "">("");
  const [selectedReportUuid, setSelectedReportUuid] = useState<string>("");
  const [statusDraft, setStatusDraft] = useState<ReportStatus>("OPEN");
  const [resolutionNoteDraft, setResolutionNoteDraft] = useState("");
  const [removeOpen, setRemoveOpen] = useState(false);
  const [restoreOpen, setRestoreOpen] = useState(false);

  const params = useMemo(
    () => ({
      page,
      size: pageSize,
      sort,
      status: status || undefined,
      targetType: targetType || undefined,
      reasonCode: reasonCode || undefined,
    }),
    [page, pageSize, sort, status, targetType, reasonCode],
  );

  const summaryQuery = useAdminReportQueueSummary();
  const reportsQuery = useAdminReports(params);
  const detailQuery = useAdminReport(selectedReportUuid, !!selectedReportUuid);
  const updateReportMutation = useUpdateAdminReport();
  const removeListingMutation = useRemoveAdminListing();
  const restoreListingMutation = useRestoreAdminListing();

  const selectedReport = detailQuery.data;
  const selectedReportIsItem = selectedReport?.targetType === "ITEM";
  const listingQuery = useAdminListing(selectedReportIsItem ? selectedReport.targetUuid : "", !!selectedReportIsItem);

  const data = reportsQuery.data;
  const reports = data?.content ?? [];
  const totalReports = data?.totalElements ?? 0;
  const rangeStart = totalReports === 0 ? 0 : page * pageSize + 1;
  const rangeEnd = totalReports === 0 ? 0 : rangeStart + reports.length - 1;
  const hasFilters = !!status || !!targetType || !!reasonCode;
  const staleThresholdHours = summaryQuery.data?.staleThresholdHours ?? 48;
  const currentStatusOptions = selectedReport ? allowedStatusOptions(selectedReport.status) : REPORT_STATUS_OPTIONS;
  const canEditResolutionNote = isTerminalReportStatus(statusDraft);
  const normalizedResolutionNote = normalizeNote(resolutionNoteDraft);
  const existingResolutionNote = normalizeNote(selectedReport?.resolutionNote ?? "");
  const hasPendingChanges =
    !!selectedReport &&
    (statusDraft !== selectedReport.status || (canEditResolutionNote && normalizedResolutionNote !== existingResolutionNote));
  const canSaveStatusUpdate = hasPendingChanges && (!canEditResolutionNote || normalizedResolutionNote.length > 0);
  const selectedListing = listingQuery.data;

  const reporterProfilePath = selectedReport ? routePaths.publicProfile(selectedReport.reporter.uuid) : undefined;
  const targetLink = useMemo(() => {
    if (!selectedReport) {
      return undefined;
    }

    if (selectedReport.targetType === "ITEM") {
      return routePaths.admin.listingDetail(selectedReport.targetUuid);
    }

    if (selectedReport.targetType === "USER") {
      return routePaths.publicProfile(selectedReport.targetUuid);
    }

    return undefined;
  }, [selectedReport]);

  useEffect(() => {
    setPage(0);
  }, [status, targetType, reasonCode]);

  useEffect(() => {
    if (reports.length === 0) {
      if (selectedReportUuid) {
        setSelectedReportUuid("");
      }
      return;
    }

    const selectedStillVisible = reports.some((report) => report.uuid === selectedReportUuid);

    if (!selectedStillVisible) {
      setSelectedReportUuid(reports[0].uuid);
    }
  }, [reports, selectedReportUuid]);

  useEffect(() => {
    if (!selectedReport) {
      return;
    }

    setStatusDraft(selectedReport.status);
    setResolutionNoteDraft(selectedReport.resolutionNote ?? "");
  }, [selectedReport]);

  const resetFilters = () => {
    setStatus("");
    setTargetType("");
    setReasonCode("");
    setPage(0);
  };

  const saveStatusUpdate = async () => {
    if (!selectedReportUuid || !selectedReport) {
      return;
    }

    try {
      await updateReportMutation.mutateAsync({
        reportUuid: selectedReportUuid,
        data: {
          status: statusDraft,
          resolutionNote: isTerminalReportStatus(statusDraft)
            ? normalizeNote(resolutionNoteDraft) || null
            : null,
        },
      });
      toast.success(t("admin:reportsPage.updateSuccess"));
    } catch (error) {
      toast.error(parseApiError(error) || t("admin:reportsPage.updateError"));
    }
  };

  return (
    <>
      <AdminPageShell
        title={t("admin:reportsPage.title")}
        description={t("admin:reportsPage.description")}
        badges={
          <>
            <span className="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700 dark:bg-amber-950/30 dark:text-amber-300">
              {t("admin:reports")}
            </span>
            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
              {t("admin:reportsPage.moderationInbox")}
            </span>
          </>
        }
        toolbar={
          <AdminToolbar>
            <div className="grid flex-1 gap-3 md:grid-cols-2 lg:grid-cols-5">
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value as ReportStatus | "")}
                className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">{t("admin:allStatuses")}</option>
                {REPORT_STATUS_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {t(`reporting:${reportStatusTranslationKey(option)}`)}
                  </option>
                ))}
              </select>

              <select
                value={targetType}
                onChange={(event) => setTargetType(event.target.value as ReportTargetType | "")}
                className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">{t("admin:reportsPage.allTargetTypes")}</option>
                {REPORT_TARGET_TYPE_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {t(`reporting:${reportTargetTypeTranslationKey(option)}`)}
                  </option>
                ))}
              </select>

              <select
                value={reasonCode}
                onChange={(event) => setReasonCode(event.target.value as ReportReasonCode | "")}
                className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">{t("admin:reportsPage.allReasons")}</option>
                {REPORT_REASON_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {t(`reporting:${reportReasonTranslationKey(option)}`)}
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
                  aria-label={t("admin:reportsPage.rowsPerPage")}
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
        <div className="grid gap-4 xl:grid-cols-3">
          <SummaryMetricCard
            title={t("admin:reportsPage.summaryOpen")}
            value={summaryQuery.isLoading ? "—" : summaryQuery.data?.openCount ?? 0}
            helper={t("admin:reportsPage.summaryOpenHelper")}
            toneClassName="bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300"
          />
          <SummaryMetricCard
            title={t("admin:reportsPage.summaryInReview")}
            value={summaryQuery.isLoading ? "—" : summaryQuery.data?.inReviewCount ?? 0}
            helper={t("admin:reportsPage.summaryInReviewHelper")}
            toneClassName="bg-indigo-100 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300"
          />
          <SummaryMetricCard
            title={t("admin:reportsPage.summaryStale")}
            value={summaryQuery.isLoading ? "—" : summaryQuery.data?.staleOpenCount ?? 0}
            helper={t("admin:reportsPage.summaryStaleHelper", { hours: staleThresholdHours })}
            toneClassName="bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300"
          />
        </div>

        <AdminSurface
          title={t("admin:reportsPage.queueTitle")}
          description={t("admin:reportsPage.matchedReports", { count: totalReports })}
          contentClassName="space-y-0"
        >
          {reportsQuery.isLoading ? (
            <div className="flex items-center justify-center py-16">
              <Spinner />
            </div>
          ) : reportsQuery.isError || !data ? (
            <EmptyState
              icon={<ShieldAlert className="size-12" />}
              title={t("admin:reportsPage.loadErrorTitle")}
              description={t("admin:reportsPage.loadErrorDescription")}
              action={
                <Button variant="outline" onClick={() => reportsQuery.refetch()}>
                  {t("common:tryAgain")}
                </Button>
              }
            />
          ) : reports.length === 0 ? (
            <EmptyState
              icon={<Flag className="size-12" />}
              title={t("admin:reportsPage.emptyTitle")}
              description={
                hasFilters
                  ? t("admin:reportsPage.emptyFilteredDescription")
                  : t("admin:reportsPage.emptyDescription")
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
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">
                        {t("admin:reportsPage.target")}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">
                        {t("admin:reportsPage.reporter")}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">
                        {t("admin:reportsPage.reason")}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">
                        {t("admin:reportsPage.status")}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">
                        {t("admin:reportsPage.createdDate")}
                      </th>
                      <th className="px-4 py-3 text-right font-semibold text-slate-600 dark:text-slate-300">
                        {t("common:actions")}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                    {reports.map((report) => {
                      const isSelected = report.uuid === selectedReportUuid;
                      const stale = isStaleOpenReport(report.createdAt, staleThresholdHours, report.status);

                      return (
                        <tr
                          key={report.uuid}
                          className={isSelected ? "bg-indigo-50/70 dark:bg-indigo-950/20" : "hover:bg-slate-50 dark:hover:bg-slate-900/50"}
                        >
                          <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                            <div className="space-y-1">
                              <div className="flex flex-wrap items-center gap-2">
                                <Badge variant="secondary">
                                  {t(`reporting:${reportTargetTypeTranslationKey(report.targetType)}`)}
                                </Badge>
                                {stale ? (
                                  <Badge variant="danger">{t("admin:reportsPage.staleBadge")}</Badge>
                                ) : null}
                                <span className="font-medium text-slate-900 dark:text-slate-100">
                                  {report.targetSummary.title}
                                </span>
                              </div>
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                {report.targetSummary.subtitle}
                              </div>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                            <div className="font-medium">{report.reporter.username}</div>
                            <div className="text-xs text-slate-500 dark:text-slate-400">{report.reporter.uuid}</div>
                            <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                              {report.assignedModerator
                                ? t("admin:reportsPage.assignedTo", { username: report.assignedModerator.username })
                                : t("admin:reportsPage.unassignedShort")}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                            {t(`reporting:${reportReasonTranslationKey(report.reasonCode)}`)}
                          </td>
                          <td className="px-4 py-3">
                            <Badge variant={reportStatusBadgeVariant(report.status)}>
                              {t(`reporting:${reportStatusTranslationKey(report.status)}`)}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                            <div>{formatDateTime(report.createdAt)}</div>
                            <div className="mt-1 inline-flex items-center gap-1 text-xs">
                              <Clock3 className="size-3" />
                              {t("admin:reportsPage.ageValue", { value: reportAgeLabel(report.createdAt) })}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Button
                              variant={isSelected ? "secondary" : "outline"}
                              size="sm"
                              onClick={() => setSelectedReportUuid(report.uuid)}
                            >
                              {t("admin:openDetail")}
                            </Button>
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
                      {totalReports === 0
                        ? t("admin:reportsPage.noReportsMatched")
                        : t("admin:showingRange", { start: rangeStart, end: rangeEnd, total: totalReports })}
                    </span>
                    {reportsQuery.isFetching ? (
                      <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                        {t("admin:refreshing")}
                      </span>
                    ) : null}
                  </>
                }
              />
            </>
          )}
        </AdminSurface>

        <AdminSurface
          title={t("admin:reportsPage.detailsTitle")}
          description={t("admin:reportsPage.detailsDescription")}
        >
          {!selectedReportUuid ? (
            <EmptyState
              icon={<SearchCheck className="size-12" />}
              title={t("admin:reportsPage.noSelectionTitle")}
              description={t("admin:reportsPage.noSelectionDescription")}
            />
          ) : detailQuery.isLoading ? (
            <div className="flex items-center justify-center py-16">
              <Spinner />
            </div>
          ) : detailQuery.isError || !selectedReport ? (
            <EmptyState
              icon={<ShieldAlert className="size-12" />}
              title={t("admin:reportsPage.detailLoadErrorTitle")}
              description={t("admin:reportsPage.detailLoadErrorDescription")}
              action={
                <Button variant="outline" onClick={() => detailQuery.refetch()}>
                  {t("common:tryAgain")}
                </Button>
              }
            />
          ) : (
            <div className="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_360px]">
              <div className="space-y-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900/60">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.reportedTarget")}
                    </p>
                    <p className="mt-2 text-base font-semibold text-slate-900 dark:text-slate-100">
                      {selectedReport.targetSummary.title}
                    </p>
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                      {selectedReport.targetSummary.subtitle}
                    </p>
                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <Badge variant="secondary">
                        {t(`reporting:${reportTargetTypeTranslationKey(selectedReport.targetType)}`)}
                      </Badge>
                      <span className="font-mono text-xs text-slate-500 dark:text-slate-400">
                        {selectedReport.targetUuid}
                      </span>
                    </div>
                    {targetLink ? (
                      <div className="mt-4">
                        <Link to={targetLink}>
                          <Button variant="outline" size="sm">
                            <ExternalLink className="size-4" />
                            {selectedReport.targetType === "ITEM"
                              ? t("admin:reportsPage.openListingDetail")
                              : t("admin:reportsPage.openTargetProfile")}
                          </Button>
                        </Link>
                      </div>
                    ) : null}
                  </div>

                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900/60">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.reporterInfo")}
                    </p>
                    <p className="mt-2 text-base font-semibold text-slate-900 dark:text-slate-100">
                      {selectedReport.reporter.username}
                    </p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{selectedReport.reporter.uuid}</p>
                    <div className="mt-3 text-sm text-slate-600 dark:text-slate-300">
                      {selectedReport.assignedModerator ? (
                        <>
                          <span className="font-medium">{t("reporting:assignedModerator")}:</span>{" "}
                          {selectedReport.assignedModerator.username}
                        </>
                      ) : (
                        t("admin:reportsPage.unassigned")
                      )}
                    </div>
                    <div className="mt-4">
                      <Link to={reporterProfilePath ?? routePaths.profile}>
                        <Button variant="outline" size="sm">
                          <ExternalLink className="size-4" />
                          {t("admin:reportsPage.openReporterProfile")}
                        </Button>
                      </Link>
                    </div>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.reason")}
                    </p>
                    <p className="mt-1 text-sm text-slate-900 dark:text-slate-100">
                      {t(`reporting:${reportReasonTranslationKey(selectedReport.reasonCode)}`)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.status")}
                    </p>
                    <div className="mt-1">
                      <Badge variant={reportStatusBadgeVariant(selectedReport.status)}>
                        {t(`reporting:${reportStatusTranslationKey(selectedReport.status)}`)}
                      </Badge>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.age")}
                    </p>
                    <p className="mt-1 text-sm text-slate-900 dark:text-slate-100">
                      {t("admin:reportsPage.ageValue", { value: reportAgeLabel(selectedReport.createdAt) })}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:created")}
                    </p>
                    <p className="mt-1 text-sm text-slate-900 dark:text-slate-100">
                      {formatDateTime(selectedReport.createdAt)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:updated")}
                    </p>
                    <p className="mt-1 text-sm text-slate-900 dark:text-slate-100">
                      {formatDateTime(selectedReport.updatedAt)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("admin:reportsPage.resolvedAt")}
                    </p>
                    <p className="mt-1 text-sm text-slate-900 dark:text-slate-100">
                      {formatDateTime(selectedReport.resolvedAt)}
                    </p>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-950/40">
                  <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                    {t("reporting:details")}
                  </p>
                  <p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-700 dark:text-slate-300">
                    {selectedReport.details?.trim() || t("admin:reportsPage.noDetails")}
                  </p>
                </div>

                {selectedReport.targetSummary.preview ? (
                  <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-950/40">
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      {t("reporting:preview")}
                    </p>
                    <p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-700 dark:text-slate-300">
                      {selectedReport.targetSummary.preview}
                    </p>
                  </div>
                ) : null}
              </div>

              <div className="space-y-6">
                {selectedReport.targetType === "ITEM" ? (
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-900/60">
                    <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                      {t("admin:reportsPage.listingModerationTitle")}
                    </h3>
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                      {t("admin:reportsPage.listingModerationDescription")}
                    </p>

                    {listingQuery.isLoading ? (
                      <div className="flex items-center justify-center py-8">
                        <Spinner />
                      </div>
                    ) : listingQuery.isError || !selectedListing ? (
                      <div className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-5 text-sm text-slate-600 dark:border-slate-700 dark:text-slate-300">
                        {t("admin:reportsPage.listingStatusUnavailable")}
                      </div>
                    ) : (
                      <div className="mt-4 space-y-4">
                        <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-950/40">
                          <div className="flex flex-wrap items-center gap-2">
                            <Badge variant={listingStatusBadgeVariant(selectedListing)}>{selectedListing.status}</Badge>
                            <span className="text-sm font-medium text-slate-900 dark:text-slate-100">
                              {selectedListing.title}
                            </span>
                          </div>
                          <div className="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
                            <p>
                              <span className="font-medium">{t("admin:reportsPage.listingOwner")}:</span>{" "}
                              {selectedListing.ownerUsername}
                            </p>
                            <p>
                              <span className="font-medium">{t("admin:reportsPage.listingRemovedAt")}:</span>{" "}
                              {formatDateTime(selectedListing.removedAt)}
                            </p>
                          </div>
                        </div>

                        <div className="flex flex-wrap gap-3">
                          <Link to={routePaths.admin.listingDetail(selectedListing.uuid)}>
                            <Button variant="outline" size="sm">
                              <ExternalLink className="size-4" />
                              {t("admin:reportsPage.openListingDetail")}
                            </Button>
                          </Link>
                          {selectedListing.status === "REMOVED" ? (
                            <Button size="sm" onClick={() => setRestoreOpen(true)}>
                              {t("admin:listingDetail.restoreListing")}
                            </Button>
                          ) : (
                            <Button variant="danger" size="sm" onClick={() => setRemoveOpen(true)}>
                              {t("admin:listingDetail.removeListing")}
                            </Button>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                ) : null}

                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-900/60">
                  <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                    {t("admin:reportsPage.statusUpdateTitle")}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                    {t("admin:reportsPage.statusUpdateDescription")}
                  </p>

                  <div className="mt-5 space-y-4">
                    <label className="block">
                      <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                        {t("admin:reportsPage.status")}
                      </span>
                      <select
                        value={statusDraft}
                        onChange={(event) => setStatusDraft(event.target.value as ReportStatus)}
                        disabled={updateReportMutation.isPending}
                        className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                      >
                        {currentStatusOptions.map((option) => (
                          <option key={option} value={option}>
                            {t(`reporting:${reportStatusTranslationKey(option)}`)}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="block">
                      <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                        {t("reporting:resolutionNote")}
                        {canEditResolutionNote ? (
                          <span className="ml-1 text-rose-600 dark:text-rose-300">*</span>
                        ) : (
                          <span className="ml-1">{t("common:optionalParenthesized")}</span>
                        )}
                      </span>
                      <textarea
                        value={resolutionNoteDraft}
                        onChange={(event) => setResolutionNoteDraft(event.target.value)}
                        disabled={updateReportMutation.isPending || !canEditResolutionNote}
                        rows={6}
                        maxLength={2000}
                        className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:bg-slate-100 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:disabled:bg-slate-900"
                        placeholder={
                          canEditResolutionNote
                            ? t("admin:reportsPage.resolutionNotePlaceholder")
                            : t("admin:reportsPage.resolutionNoteLockedPlaceholder")
                        }
                      />
                      <div className="mt-2 flex items-center justify-between gap-3 text-xs text-slate-500 dark:text-slate-400">
                        <span>
                          {canEditResolutionNote
                            ? t("admin:reportsPage.resolutionNoteRequiredHelper")
                            : t("admin:reportsPage.resolutionNoteHelper")}
                        </span>
                        <span>{resolutionNoteDraft.length}/2000</span>
                      </div>
                    </label>

                    <Button
                      type="button"
                      onClick={saveStatusUpdate}
                      isLoading={updateReportMutation.isPending}
                      disabled={!canSaveStatusUpdate}
                      className="w-full"
                    >
                      {t("admin:saveChanges")}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </AdminSurface>
      </AdminPageShell>

      {selectedListing ? (
        <AdminListingModerationDialog
          isOpen={removeOpen}
          mode="remove"
          listingTitle={selectedListing.title}
          isSubmitting={removeListingMutation.isPending}
          onClose={() => setRemoveOpen(false)}
          onSubmit={(payload) => {
            removeListingMutation.mutate(
              { listingUuid: selectedListing.uuid, data: payload },
              {
                onSuccess: () => {
                  toast.success(t("admin:reportsPage.listingRemoveSuccess"));
                  setRemoveOpen(false);
                },
                onError: (error) => {
                  toast.error(parseApiError(error) || t("admin:reportsPage.listingRemoveError"));
                },
              },
            );
          }}
        />
      ) : null}

      {selectedListing ? (
        <AdminListingModerationDialog
          isOpen={restoreOpen}
          mode="restore"
          listingTitle={selectedListing.title}
          isSubmitting={restoreListingMutation.isPending}
          onClose={() => setRestoreOpen(false)}
          onSubmit={(payload) => {
            restoreListingMutation.mutate(
              { listingUuid: selectedListing.uuid, data: payload },
              {
                onSuccess: () => {
                  toast.success(t("admin:reportsPage.listingRestoreSuccess"));
                  setRestoreOpen(false);
                },
                onError: (error) => {
                  toast.error(parseApiError(error) || t("admin:reportsPage.listingRestoreError"));
                },
              },
            );
          }}
        />
      ) : null}
    </>
  );
}

