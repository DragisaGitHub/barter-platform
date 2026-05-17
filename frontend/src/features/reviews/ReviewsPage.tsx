import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2, MessageSquareWarning, Star, ThumbsDown, ThumbsUp } from "lucide-react";
import { useTranslation } from "react-i18next";
import type {
  ReviewDirection,
  TradeReviewNegativeReason,
  TradeReviewRating,
  UserTradeReviewSummaryResponse,
} from "@/api/generated/types.ts";
import { Pagination } from "@/components/data/Pagination";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { negativeReasonTranslationKey } from "@/features/trade/TradeReviewDialog";
import { routePaths } from "@/routes/routePaths";
import { cn } from "@/utils";
import { useReviews } from "./useReviews";

const PAGE_SIZE = 10;
const DEFAULT_SORT = "createdAt,desc";

const DIRECTION_TABS: { value: ReviewDirection; labelKey: string; descriptionKey: string }[] = [
  { value: "RECEIVED", labelKey: "reviewsPage.tabs.received", descriptionKey: "reviewsPage.tabs.receivedDescription" },
  { value: "GIVEN", labelKey: "reviewsPage.tabs.given", descriptionKey: "reviewsPage.tabs.givenDescription" },
];

const RATING_FILTERS: { value: TradeReviewRating | undefined; labelKey: string }[] = [
  { value: undefined, labelKey: "reviewsPage.filters.all" },
  { value: "POSITIVE", labelKey: "reviewsPage.filters.positive" },
  { value: "NEGATIVE", labelKey: "reviewsPage.filters.negative" },
];

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function RatingBadge({ rating }: { rating: TradeReviewRating }) {
  const { t } = useTranslation("trade");
  const positive = rating === "POSITIVE";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ring-1",
        positive
          ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
          : "bg-rose-50 text-rose-700 ring-rose-200",
      )}
    >
      {positive ? <ThumbsUp className="size-3.5" /> : <ThumbsDown className="size-3.5" />}
      {positive ? t("reviews.positive") : t("reviews.negative")}
    </span>
  );
}

function reasonLabel(reason: TradeReviewNegativeReason, t: (key: string) => string) {
  return t(negativeReasonTranslationKey(reason));
}

function ReviewCard({ review, direction }: { review: UserTradeReviewSummaryResponse; direction: ReviewDirection }) {
  const { t } = useTranslation("trade");
  const counterparty = direction === "RECEIVED" ? review.reviewerUsername : review.reviewedUsername;

  return (
    <article className="rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm transition-colors hover:border-slate-300">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <RatingBadge rating={review.rating} />
            {review.completedTrade ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
                <CheckCircle2 className="size-3.5 text-emerald-600" />
                {t("reviewsPage.completedTrade")}
              </span>
            ) : null}
          </div>
          <h2 className="mt-2 text-sm font-semibold text-slate-950">
            {direction === "RECEIVED"
              ? t("reviewsPage.reviewFrom", { username: counterparty })
              : t("reviewsPage.reviewFor", { username: counterparty })}
          </h2>
          <p className="mt-0.5 text-xs text-slate-500">
            {review.relatedItemTitle
              ? t("reviewsPage.relatedItem", { title: review.relatedItemTitle })
              : t("reviewsPage.relatedTrade")}
          </p>
        </div>

        <div className="flex shrink-0 items-center gap-2 text-xs text-slate-500">
          <span>{formatDate(review.createdAt)}</span>
          <Link to={routePaths.offerDetail(review.tradeOfferUuid)} className="font-medium text-indigo-700 hover:text-indigo-800">
            {t("reviewsPage.openTrade")}
          </Link>
        </div>
      </div>

      {review.negativeReason ? (
        <p className="mt-2 inline-flex rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700">
          {t("reviewsPage.negativeReason", { reason: reasonLabel(review.negativeReason, t) })}
        </p>
      ) : null}

      {review.comment ? (
        <p className="mt-2 border-l-2 border-slate-200 pl-3 text-sm leading-6 text-slate-700">{review.comment}</p>
      ) : (
        <p className="mt-2 text-sm text-slate-500">{t("reviewsPage.noComment")}</p>
      )}
    </article>
  );
}

export function ReviewsPage() {
  const { t } = useTranslation(["trade", "common"]);
  const [direction, setDirection] = useState<ReviewDirection>("RECEIVED");
  const [rating, setRating] = useState<TradeReviewRating | undefined>();
  const [page, setPage] = useState(0);

  const params = useMemo(
    () => ({
      direction,
      page,
      size: PAGE_SIZE,
      sort: DEFAULT_SORT,
      rating,
    }),
    [direction, page, rating],
  );

  const reviewsQuery = useReviews(params);
  const data = reviewsQuery.data;
  const reviews = data?.content ?? [];
  const totalReviews = data?.totalElements ?? 0;
  const rangeStart = totalReviews === 0 ? 0 : page * PAGE_SIZE + 1;
  const rangeEnd = totalReviews === 0 ? 0 : rangeStart + reviews.length - 1;
  const activeTab = DIRECTION_TABS.find((tab) => tab.value === direction) ?? DIRECTION_TABS[0];

  const changeDirection = (nextDirection: ReviewDirection) => {
    setDirection(nextDirection);
    setPage(0);
  };

  const changeRating = (nextRating: TradeReviewRating | undefined) => {
    setRating(nextRating);
    setPage(0);
  };

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-indigo-700">{t("trade:reviewsPage.eyebrow")}</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950">{t("trade:reviewsPage.title")}</h1>
          <p className="mt-1 text-sm text-slate-600">{t(activeTab.descriptionKey)}</p>
        </div>
        <span className="inline-flex w-fit items-center gap-1 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
          <Star className="size-3.5 text-amber-500" />
          {t("trade:reviewsPage.total", { count: totalReviews })}
        </span>
      </div>

      <Card className="p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="inline-flex w-full rounded-lg bg-slate-100 p-1 sm:w-fit">
            {DIRECTION_TABS.map((tab) => (
              <button
                key={tab.value}
                type="button"
                onClick={() => changeDirection(tab.value)}
                className={cn(
                  "flex-1 rounded-md px-3 py-1.5 text-sm font-semibold transition-colors sm:flex-none",
                  direction === tab.value ? "bg-white text-slate-950 shadow-sm" : "text-slate-600 hover:text-slate-900",
                )}
              >
                {t(tab.labelKey)}
              </button>
            ))}
          </div>

          <div className="flex flex-wrap gap-2">
            {RATING_FILTERS.map((filter) => (
              <button
                key={filter.labelKey}
                type="button"
                onClick={() => changeRating(filter.value)}
                className={cn(
                  "rounded-full border px-3 py-1 text-xs font-semibold transition-colors",
                  rating === filter.value
                    ? "border-indigo-600 bg-indigo-50 text-indigo-700"
                    : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:text-slate-900",
                )}
              >
                {t(filter.labelKey)}
              </button>
            ))}
          </div>
        </div>
      </Card>

      <Card className="p-0">
        {reviewsQuery.isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : reviewsQuery.isError || !data ? (
          <div className="p-6">
            <EmptyState
              icon={<MessageSquareWarning className="size-12" />}
              title={t("trade:reviewsPage.loadErrorTitle")}
              description={t("trade:reviewsPage.loadErrorDescription")}
              action={
                <Button variant="outline" onClick={() => reviewsQuery.refetch()}>
                  {t("common:tryAgain")}
                </Button>
              }
            />
          </div>
        ) : reviews.length === 0 ? (
          <div className="p-6">
            <EmptyState
              icon={<Star className="size-12" />}
              title={t(rating ? "trade:reviewsPage.emptyFilteredTitle" : "trade:reviewsPage.emptyTitle")}
              description={t(
                rating
                  ? "trade:reviewsPage.emptyFilteredDescription"
                  : direction === "RECEIVED"
                    ? "trade:reviewsPage.emptyReceivedDescription"
                    : "trade:reviewsPage.emptyGivenDescription",
              )}
              action={
                rating ? (
                  <Button variant="outline" onClick={() => changeRating(undefined)}>
                    {t("trade:reviewsPage.clearRatingFilter")}
                  </Button>
                ) : undefined
              }
            />
          </div>
        ) : (
          <>
            <div className="space-y-3 p-4">
              {reviews.map((review) => (
                <ReviewCard key={review.uuid} review={review} direction={direction} />
              ))}
            </div>

            <Pagination
              currentPage={data.page ?? 0}
              totalPages={Math.max(data.totalPages ?? 0, 1)}
              onPageChange={setPage}
              statusContent={
                <>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600">
                    {totalReviews === 0
                      ? t("trade:reviewsPage.noReviewsMatched")
                      : t("trade:reviewsPage.showingRange", { start: rangeStart, end: rangeEnd, total: totalReviews })}
                  </span>
                  {reviewsQuery.isFetching ? (
                    <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700">
                      {t("trade:reviewsPage.refreshing")}
                    </span>
                  ) : null}
                </>
              }
            />
          </>
        )}
      </Card>
    </div>
  );
}

