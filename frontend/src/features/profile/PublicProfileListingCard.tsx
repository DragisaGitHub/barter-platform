import { Link } from "react-router-dom";
import { ArrowUpRight, Clock3, Package, Tag } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import type { ItemSummaryResponse } from "@/api/generated/types.ts";
import { routePaths } from "@/routes/routePaths.ts";
import { Card } from "../../components/ui/Card";
import { ItemConditionBadge, ItemStatusBadge } from "../catalog/ItemBadges";

interface PublicProfileListingCardProps {
  item: ItemSummaryResponse;
}

export function PublicProfileListingCard({ item }: PublicProfileListingCardProps) {
  const listedLabel = formatListedLabel(item.createdAt);

  return (
    <Link to={routePaths.marketplaceItem(item.uuid)} className="group block h-full">
      <Card className="flex h-full flex-col overflow-hidden border-slate-200/80 p-0 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-violet-200 hover:shadow-md dark:border-slate-700/80 dark:hover:border-violet-700/70">
        <div className="relative aspect-[4/3] overflow-hidden bg-slate-100 dark:bg-slate-800/80">
          {item.primaryImageUrl ? (
            <img
              src={item.primaryImageUrl}
              alt={item.title}
              loading="lazy"
              className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-violet-50 via-white to-slate-100 text-violet-400 dark:from-violet-950/30 dark:via-slate-900 dark:to-slate-800 dark:text-violet-300">
              <Package className="size-10" />
            </div>
          )}
        </div>

        <div className="flex flex-1 flex-col p-4">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <ItemStatusBadge status={item.status} />
            <ItemConditionBadge condition={item.condition} />
          </div>

          <h3 className="line-clamp-2 min-h-[3rem] text-sm font-semibold leading-6 text-slate-900 transition-colors group-hover:text-violet-700 dark:text-slate-100 dark:group-hover:text-violet-300">
            {item.title}
          </h3>

          <div className="mt-4 space-y-3 text-sm text-slate-600 dark:text-slate-300">
            <div className="flex items-center gap-2">
              <Tag className="size-4 shrink-0 text-violet-500 dark:text-violet-300" />
              <span className="truncate">{item.categoryName}</span>
            </div>
            <div className="flex items-center gap-2">
              <Clock3 className="size-4 shrink-0 text-slate-400" />
              <span>{listedLabel}</span>
            </div>
          </div>

          <div className="mt-auto pt-4">
            <span className="inline-flex items-center gap-1 text-sm font-medium text-violet-700 transition-colors group-hover:text-violet-800 dark:text-violet-300 dark:group-hover:text-violet-200">
              View listing
              <ArrowUpRight className="size-4" />
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

function formatListedLabel(createdAt: string) {
  try {
    return `Listed ${formatDistanceToNow(new Date(createdAt), { addSuffix: true })}`;
  } catch {
    return "Recently listed";
  }
}

