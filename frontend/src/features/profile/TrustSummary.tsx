import { CheckCircle, XCircle, Package, Star } from "lucide-react";
import { Card } from "../../components/ui/Card";
import React from "react";

interface TrustSummaryProps {
  activeItemCount: number;
  completedTradeCount: number;
  cancelledTradeCount: number;
  averageRating?: number | null;
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
  averageRating,
}: TrustSummaryProps) {
  const stats = [
    {
      icon: <Package className="size-5 text-violet-600 dark:text-violet-300" />,
      label: "Active listings",
      value: String(activeItemCount),
      description:
        activeItemCount === 1
          ? "Listing currently available for browsing."
          : "Listings currently available for browsing.",
      accentClassName: "bg-violet-100 text-violet-600 dark:bg-violet-950/30 dark:text-violet-300",
    },
    {
      icon: <CheckCircle className="size-5 text-emerald-600 dark:text-emerald-300" />,
      label: "Completed trades",
      value: String(completedTradeCount),
      description:
        completedTradeCount === 1
          ? "Successful trade recorded on this profile."
          : "Successful trades recorded on this profile.",
      accentClassName: "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/30 dark:text-emerald-300",
    },
    {
      icon: <XCircle className="size-5 text-rose-600 dark:text-rose-300" />,
      label: "Cancelled trades",
      value: String(cancelledTradeCount),
      description:
        cancelledTradeCount === 1
          ? "Cancelled trade associated with this profile."
          : "Cancelled trades associated with this profile.",
      accentClassName: "bg-rose-100 text-rose-600 dark:bg-rose-950/30 dark:text-rose-300",
    },
    {
      icon: <Star className="size-5 text-amber-500 dark:text-amber-300" />,
      label: "Average rating",
      value: averageRating != null ? averageRating.toFixed(1) : "Not rated",
      description:
        averageRating != null
          ? "Average rating currently available on this public profile."
          : "No rating is available for this profile yet.",
      accentClassName: "bg-amber-100 text-amber-600 dark:bg-amber-950/30 dark:text-amber-300",
    },
  ] as const;

  return (
    <section>
      <div className="mb-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.16em] text-violet-700 dark:text-violet-300">
          Trust &amp; activity
        </h2>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          A quick marketplace summary based on the public profile activity available right now.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((stat) => (
          <StatItem key={stat.label} {...stat} />
        ))}
      </div>
    </section>
  );
}

