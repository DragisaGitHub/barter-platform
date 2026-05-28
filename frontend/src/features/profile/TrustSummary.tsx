import { CheckCircle, MessageSquareText, Package, ShieldCheck, ThumbsDown, ThumbsUp, XCircle } from "lucide-react";
import { Card } from "../../components/ui/Card";
import React from "react";
import type { PublicProfileReviewSnippetResponse, ReputationSummaryResponse } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

interface TrustSummaryProps {
  activeItemCount: number;
  completedTradeCount: number;
  cancelledTradeCount: number;
  averageRating?: number | null;
  reputationSummary: ReputationSummaryResponse;
  recentReviews: PublicProfileReviewSnippetResponse[];
}

interface StatItemProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  description: string;
  accentClassName: string;
}

function StatItem({ icon, label, value, description, accentClassName }: StatItemProps) {
  return (
    <Card className="h-full rounded-2xl border-slate-200/80 p-5 shadow-sm transition-colors dark:border-slate-700/80">
      <div className={`flex size-11 items-center justify-center rounded-2xl ${accentClassName}`}>
        {icon}
      </div>
      <div>
        <p className="mt-4 text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
        <p className="mt-1 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          {value}
        </p>
        <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{description}</p>
      </div>
    </Card>
  );
}

export function TrustSummary({
  activeItemCount,
  completedTradeCount,
  cancelledTradeCount,
  reputationSummary,
  recentReviews,
}: TrustSummaryProps) {
  const { t } = useTranslation("profile");
  const isNewTrader = completedTradeCount === 0 && reputationSummary.totalReviewCount === 0;
  const stats = [
    {
      icon: <Package className="size-5 text-violet-600 dark:text-violet-300" />,
      label: t("trust.activeListings"),
      value: String(activeItemCount),
      description:
        activeItemCount === 1
          ? t("trust.activeListingsDescription_one")
          : t("trust.activeListingsDescription_other"),
      accentClassName: "bg-violet-100 text-violet-600 dark:bg-violet-950/30 dark:text-violet-300",
    },
    {
      icon: <CheckCircle className="size-5 text-emerald-600 dark:text-emerald-300" />,
      label: t("trust.completedTrades"),
      value: String(completedTradeCount),
      description:
        completedTradeCount === 1
          ? t("trust.completedTradesDescription_one")
          : t("trust.completedTradesDescription_other"),
      accentClassName: "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/30 dark:text-emerald-300",
    },
    {
      icon: <XCircle className="size-5 text-rose-600 dark:text-rose-300" />,
      label: t("trust.cancelledTrades"),
      value: String(cancelledTradeCount),
      description:
        cancelledTradeCount === 1
          ? t("trust.cancelledTradesDescription_one")
          : t("trust.cancelledTradesDescription_other"),
      accentClassName: "bg-rose-100 text-rose-600 dark:bg-rose-950/30 dark:text-rose-300",
    },
    {
      icon: <ThumbsUp className="size-5 text-emerald-600 dark:text-emerald-300" />,
      label: t("trust.positiveReviews"),
      value: String(reputationSummary.positiveReviewCount),
      description: t("trust.positiveReviewsDescription"),
      accentClassName: "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/30 dark:text-emerald-300",
    },
    {
      icon: <ThumbsDown className="size-5 text-rose-600 dark:text-rose-300" />,
      label: t("trust.negativeReviews"),
      value: String(reputationSummary.negativeReviewCount),
      description: t("trust.negativeReviewsDescription"),
      accentClassName: "bg-rose-100 text-rose-600 dark:bg-rose-950/30 dark:text-rose-300",
    },
  ] as const;

  return (
    <section>
      <div className="mb-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.16em] text-violet-700 dark:text-violet-300">
          {t("trust.title")}
        </h2>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          {t("trust.description")}
        </p>
      </div>

      {isNewTrader ? (
        <Card className="mb-4 rounded-2xl border-sky-200/80 bg-sky-50/80 p-5 shadow-sm dark:border-sky-900/50 dark:bg-sky-950/20">
          <div className="flex gap-4">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-white text-sky-600 shadow-sm dark:bg-slate-900 dark:text-sky-300">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                {t("trust.newTraderTitle")}
              </p>
              <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
                {t("trust.newTraderDescription")}
              </p>
            </div>
          </div>
        </Card>
      ) : null}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {stats.map((stat) => (
          <StatItem key={stat.label} {...stat} />
        ))}
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="rounded-2xl border-slate-200/80 p-5 shadow-sm dark:border-slate-700/80">
          <div className="flex items-start gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-violet-100 text-violet-600 dark:bg-violet-950/30 dark:text-violet-300">
              <MessageSquareText className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                {t("trust.recentReviewsTitle")}
              </h3>
              <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
                {t("trust.recentReviewsDescription")}
              </p>
            </div>
          </div>

          {recentReviews.length > 0 ? (
            <div className="mt-5 space-y-3">
              {recentReviews.map((review) => (
                <article
                  key={review.uuid}
                  className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 dark:border-slate-700/80 dark:bg-slate-800/40"
                >
                  <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
                    <span className="rounded-full bg-white px-2.5 py-1 text-slate-700 ring-1 ring-slate-200/80 dark:bg-slate-900 dark:text-slate-200 dark:ring-slate-700/80">
                      {t(`trust.rating.${review.rating}`)}
                    </span>
                    <span>
                      {t("trust.reviewedByOn", {
                        username: review.reviewerUsername,
                        date: formatReviewDate(review.createdAt),
                      })}
                    </span>
                  </div>
                  <p className="mt-3 text-sm leading-6 text-slate-700 dark:text-slate-200">
                    “{review.commentSnippet}”
                  </p>
                </article>
              ))}
            </div>
          ) : (
            <p className="mt-5 rounded-2xl border border-dashed border-slate-300/80 p-4 text-sm leading-6 text-slate-600 dark:border-slate-700 dark:text-slate-300">
              {t("trust.recentReviewsEmpty")}
            </p>
          )}
        </Card>

        <Card className="rounded-2xl border-amber-200/80 bg-amber-50/80 p-5 shadow-sm dark:border-amber-900/50 dark:bg-amber-950/20">
          <div className="flex gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-white text-amber-700 shadow-sm dark:bg-slate-900 dark:text-amber-300">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                {t("trust.safetyNoticeTitle")}
              </h3>
              <p className="mt-2 text-sm leading-6 text-slate-700 dark:text-slate-200">
                {t("trust.safetyNoticeDescription")}
              </p>
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
}

function formatReviewDate(iso: string): string {
  const date = new Date(iso);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

