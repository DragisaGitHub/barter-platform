import { CheckCircle, Package, Percent, ThumbsDown, ThumbsUp, XCircle } from "lucide-react";
import { Card } from "../../components/ui/Card";
import React from "react";
import type { ReputationSummaryResponse } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

interface TrustSummaryProps {
  activeItemCount: number;
  completedTradeCount: number;
  cancelledTradeCount: number;
  averageRating?: number | null;
  reputationSummary: ReputationSummaryResponse;
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
}: TrustSummaryProps) {
  const { t } = useTranslation("profile");
  const positivePercentage = reputationSummary.positivePercentage;
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
    {
      icon: <Percent className="size-5 text-sky-600 dark:text-sky-300" />,
      label: t("trust.positivePercentage"),
      value: positivePercentage == null ? "—" : `${positivePercentage.toFixed(0)}%`,
      description: t("trust.totalReviews", { count: reputationSummary.totalReviewCount }),
      accentClassName: "bg-sky-100 text-sky-600 dark:bg-sky-950/30 dark:text-sky-300",
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

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {stats.map((stat) => (
          <StatItem key={stat.label} {...stat} />
        ))}
      </div>
    </section>
  );
}

