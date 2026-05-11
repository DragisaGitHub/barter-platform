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
}

function StatItem({ icon, label, value }: StatItemProps) {
  return (
    <div className="flex items-center gap-3">
      <div className="shrink-0">{icon}</div>
      <div>
        <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
        <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">{value}</p>
      </div>
    </div>
  );
}

export function TrustSummary({
  activeItemCount,
  completedTradeCount,
  cancelledTradeCount,
  averageRating,
}: TrustSummaryProps) {
  return (
    <Card>
      <h2 className="text-sm font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-4">
        Trust &amp; Activity
      </h2>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatItem
          icon={<CheckCircle className="size-5 text-emerald-500" />}
          label="Completed"
          value={String(completedTradeCount)}
        />
        <StatItem
          icon={<XCircle className="size-5 text-red-400" />}
          label="Cancelled"
          value={String(cancelledTradeCount)}
        />
        <StatItem
          icon={<Package className="size-5 text-indigo-500" />}
          label="Active Items"
          value={String(activeItemCount)}
        />
        <StatItem
          icon={<Star className="size-5 text-amber-400" />}
          label="Rating"
          value={averageRating != null ? averageRating.toFixed(1) : "Coming soon"}
        />
      </div>
    </Card>
  );
}

