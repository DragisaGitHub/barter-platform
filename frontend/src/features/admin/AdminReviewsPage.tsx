import { useEffect, useMemo, useState } from "react";
import { MessageSquareWarning, Search, ShieldAlert } from "lucide-react";
import type { TradeReviewNegativeReason, TradeReviewRating } from "@/api/generated/types.ts";
import { Pagination } from "@/components/data/Pagination";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { formatNegativeReason } from "@/features/trade/TradeReviewDialog";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useAdminReviews } from "./useAdminReviews";

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

const RATING_OPTIONS: { value: TradeReviewRating | ""; label: string }[] = [
  { value: "", label: "All ratings" },
  { value: "NEGATIVE", label: "Negative" },
  { value: "POSITIVE", label: "Positive" },
];

const NEGATIVE_REASON_OPTIONS: { value: TradeReviewNegativeReason | ""; label: string }[] = [
  { value: "", label: "All reasons" },
  { value: "NO_SHOW", label: "No-show" },
  { value: "ITEM_NOT_AS_DESCRIBED", label: "Item not as described" },
  { value: "DAMAGED_OR_UNSAFE_ITEM", label: "Damaged or unsafe item" },
  { value: "RUDE_OR_ABUSIVE_BEHAVIOR", label: "Rude or abusive behavior" },
  { value: "SPAM_OR_SCAM_BEHAVIOR", label: "Spam or scam behavior" },
  { value: "OTHER", label: "Other" },
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
  const className =
    rating === "NEGATIVE"
      ? "bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300"
      : "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300";

  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${className}`}>{rating}</span>;
}

export function AdminReviewsPage() {
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
      title="Trade reviews queue"
      description="Review completed-trade feedback for governance visibility. Negative reviews appear by default in this queue, while backend filtering remains explicit."
      badges={
        <>
          <span className="inline-flex items-center rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">
            Reviews
          </span>
          <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
            Governance queue
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
                <option key={option.label} value={option.value}>
                  {option.label}
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
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={reviewerUserInput}
                onChange={(event) => setReviewerUserInput(event.target.value)}
                placeholder="Reviewer username/email"
                className="pl-9"
              />
            </div>

            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={reviewedUserInput}
                onChange={(event) => setReviewedUserInput(event.target.value)}
                placeholder="Reviewed username/email"
                className="pl-9"
              />
            </div>
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
              <span>Rows</span>
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
              Reset filters
            </Button>
          </div>
        </AdminToolbar>
      }
    >
      <AdminSurface
        title="Completed-trade reviews"
        description={`${totalReviews} review${totalReviews === 1 ? "" : "s"} matched the current governance filters.`}
        contentClassName="space-y-0"
      >
        {reviewsQuery.isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : reviewsQuery.isError || !data ? (
          <EmptyState
            icon={<ShieldAlert className="size-12" />}
            title="Unable to load reviews"
            description="The server could not apply the current review filters. Clear filters or try again."
            action={
              <Button variant="outline" onClick={() => reviewsQuery.refetch()}>
                Retry
              </Button>
            }
          />
        ) : reviews.length === 0 ? (
          <EmptyState
            icon={<MessageSquareWarning className="size-12" />}
            title="No reviews found"
            description={
              hasFilters
                ? "Try broadening the review filters or clearing the current search terms."
                : "Completed-trade reviews will appear here once participants submit them."
            }
            action={hasFilters ? <Button variant="outline" onClick={resetFilters}>Clear filters</Button> : undefined}
          />
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 dark:bg-slate-900/60">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Rating</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Reason</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Reviewer</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Reviewed user</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Trade UUID</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Comment</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                  {reviews.map((review) => (
                    <tr key={review.uuid} className="hover:bg-slate-50 dark:hover:bg-slate-900/50">
                      <td className="px-4 py-3"><RatingBadge rating={review.rating} /></td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">{formatNegativeReason(review.negativeReason)}</td>
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
                    {totalReviews === 0 ? "No reviews matched" : `Showing ${rangeStart}–${rangeEnd} of ${totalReviews}`}
                  </span>
                  {reviewsQuery.isFetching && (
                    <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                      Refreshing…
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

