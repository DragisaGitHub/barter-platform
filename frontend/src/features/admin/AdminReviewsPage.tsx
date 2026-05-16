import { useEffect, useMemo, useState } from "react";
import { MessageSquareWarning, Search, ShieldAlert } from "lucide-react";
import type { TradeReviewNegativeReason, TradeReviewRating } from "@/api/generated/types.ts";
import { Pagination } from "@/components/data/Pagination";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { negativeReasonTranslationKey } from "@/features/trade/TradeReviewDialog";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useAdminReviews } from "./useAdminReviews";
import { useTranslation } from "react-i18next";

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

const RATING_OPTIONS: { value: TradeReviewRating | ""; labelKey: string }[] = [
  { value: "", labelKey: "reviewsPage.allRatings" },
  { value: "NEGATIVE", labelKey: "trade:reviews.negative" },
  { value: "POSITIVE", labelKey: "trade:reviews.positive" },
];

const NEGATIVE_REASON_OPTIONS: { value: TradeReviewNegativeReason | ""; labelKey: string }[] = [
  { value: "", labelKey: "reviewsPage.allReasons" },
  { value: "NO_SHOW", labelKey: "trade:reviews.reasons.noShow" },
  { value: "ITEM_NOT_AS_DESCRIBED", labelKey: "trade:reviews.reasons.itemNotAsDescribed" },
  { value: "DAMAGED_OR_UNSAFE_ITEM", labelKey: "trade:reviews.reasons.damagedOrUnsafeItem" },
  { value: "RUDE_OR_ABUSIVE_BEHAVIOR", labelKey: "trade:reviews.reasons.rudeOrAbusiveBehavior" },
  { value: "SPAM_OR_SCAM_BEHAVIOR", labelKey: "trade:reviews.reasons.spamOrScamBehavior" },
  { value: "OTHER", labelKey: "trade:reviews.reasons.other" },
];

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString();
}

function previewComment(comment?: string | null) {
  if (!comment?.trim()) {
    return "—";
  }
  const trimmed = comment.trim();
  return trimmed.length > 120 ? `${trimmed.slice(0, 117)}…` : trimmed;
}

function RatingBadge({ rating }: { rating: TradeReviewRating }) {
  const { t } = useTranslation("trade");
  const className =
    rating === "NEGATIVE"
      ? "bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300"
      : "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300";

  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${className}`}>{rating === "NEGATIVE" ? t("reviews.negative") : t("reviews.positive")}</span>;
}

export function AdminReviewsPage() {
  const { t } = useTranslation(["admin", "trade", "common"]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [sort] = useState("createdAt,desc");
  const [rating, setRating] = useState<TradeReviewRating | "">("NEGATIVE");
  const [negativeReason, setNegativeReason] = useState<TradeReviewNegativeReason | "">("");
  const [reviewedUserInput, setReviewedUserInput] = useState("");
  const [reviewerUserInput, setReviewerUserInput] = useState("");
  const [reviewedUserQuery, setReviewedUserQuery] = useState("");
  const [reviewerUserQuery, setReviewerUserQuery] = useState("");

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setPage(0);
      setReviewedUserQuery(reviewedUserInput.trim());
      setReviewerUserQuery(reviewerUserInput.trim());
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [reviewedUserInput, reviewerUserInput]);

  const params = useMemo(
    () => ({
      page,
      size: pageSize,
      sort,
      rating: rating || undefined,
      negativeReason: negativeReason || undefined,
      reviewedUserQuery: reviewedUserQuery || undefined,
      reviewerUserQuery: reviewerUserQuery || undefined,
    }),
    [negativeReason, page, pageSize, rating, reviewedUserQuery, reviewerUserQuery, sort]
  );

  const reviewsQuery = useAdminReviews(params);
  const data = reviewsQuery.data;
  const reviews = data?.content ?? [];
  const totalReviews = data?.totalElements ?? 0;
  const rangeStart = totalReviews === 0 ? 0 : page * pageSize + 1;
  const rangeEnd = totalReviews === 0 ? 0 : rangeStart + reviews.length - 1;
  const hasFilters = !!rating || !!negativeReason || !!reviewedUserQuery || !!reviewerUserQuery;

  const resetFilters = () => {
    setRating("NEGATIVE");
    setNegativeReason("");
    setReviewedUserInput("");
    setReviewerUserInput("");
    setReviewedUserQuery("");
    setReviewerUserQuery("");
    setPage(0);
  };

  return (
    <AdminPageShell
      title={t("admin:reviewsPage.title")}
      description={t("admin:reviewsPage.description")}
      badges={
        <>
          <span className="inline-flex items-center rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">
            {t("admin:reviews")}
          </span>
          <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
            {t("admin:reviewsPage.governanceQueue")}
          </span>
        </>
      }
      toolbar={
        <AdminToolbar>
          <div className="grid flex-1 gap-3 lg:grid-cols-4">
            <select
              value={rating}
              onChange={(event) => {
                setPage(0);
                setRating(event.target.value as TradeReviewRating | "");
                if (event.target.value === "POSITIVE") {
                  setNegativeReason("");
                }
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {RATING_OPTIONS.map((option) => (
                <option key={option.labelKey} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>

            <select
              value={negativeReason}
              disabled={rating === "POSITIVE"}
              onChange={(event) => {
                setPage(0);
                setNegativeReason(event.target.value as TradeReviewNegativeReason | "");
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 disabled:opacity-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {NEGATIVE_REASON_OPTIONS.map((option) => (
                <option key={option.labelKey} value={option.value}>
                  {t(option.labelKey)}
                </option>
              ))}
            </select>

            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={reviewerUserInput}
                onChange={(event) => setReviewerUserInput(event.target.value)}
                placeholder={t("admin:reviewsPage.reviewerPlaceholder")}
                className="pl-9"
              />
            </div>

            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={reviewedUserInput}
                onChange={(event) => setReviewedUserInput(event.target.value)}
                placeholder={t("admin:reviewsPage.reviewedPlaceholder")}
                className="pl-9"
              />
            </div>
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
                aria-label="Reviews per page"
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
        title={t("admin:reviewsPage.completedTradeReviews")}
        description={t("admin:reviewsPage.matchedReviews", { count: totalReviews })}
        contentClassName="space-y-0"
      >
        {reviewsQuery.isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : reviewsQuery.isError || !data ? (
          <EmptyState
            icon={<ShieldAlert className="size-12" />}
            title={t("admin:reviewsPage.loadErrorTitle")}
            description={t("admin:reviewsPage.loadErrorDescription")}
            action={
              <Button variant="outline" onClick={() => reviewsQuery.refetch()}>
                {t("common:tryAgain")}
              </Button>
            }
          />
        ) : reviews.length === 0 ? (
          <EmptyState
            icon={<MessageSquareWarning className="size-12" />}
            title={t("admin:reviewsPage.emptyTitle")}
            description={
              hasFilters
                ? t("admin:reviewsPage.emptyFilteredDescription")
                : t("admin:reviewsPage.emptyDescription")
            }
            action={hasFilters ? <Button variant="outline" onClick={resetFilters}>{t("admin:clearFilters")}</Button> : undefined}
          />
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 dark:bg-slate-900/60">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.rating")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.reason")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.reviewer")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.reviewedUser")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.tradeUuid")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:reviewsPage.comment")}</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">{t("admin:created")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                  {reviews.map((review) => (
                    <tr key={review.uuid} className="hover:bg-slate-50 dark:hover:bg-slate-900/50">
                      <td className="px-4 py-3"><RatingBadge rating={review.rating} /></td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">{review.negativeReason ? t(`trade:${negativeReasonTranslationKey(review.negativeReason)}`) : "—"}</td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                        <div className="font-medium">{review.reviewerUsername}</div>
                        <div className="text-xs text-slate-500">{review.reviewerUserUuid}</div>
                      </td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                        <div className="font-medium">{review.reviewedUsername}</div>
                        <div className="text-xs text-slate-500">{review.reviewedUserUuid}</div>
                      </td>
                      <td className="px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-400">{review.tradeOfferUuid}</td>
                      <td className="max-w-xs px-4 py-3 text-slate-600 dark:text-slate-300">{previewComment(review.comment)}</td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{formatDateTime(review.createdAt)}</td>
                    </tr>
                  ))}
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
                    {totalReviews === 0 ? t("admin:reviewsPage.noReviewsMatched") : t("admin:showingRange", { start: rangeStart, end: rangeEnd, total: totalReviews })}
                  </span>
                  {reviewsQuery.isFetching && (
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

