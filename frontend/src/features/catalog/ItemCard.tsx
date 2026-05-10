import { Link } from "react-router-dom";
import { Package } from "lucide-react";
import type { ItemSummaryResponse } from "@/api/generated/types.ts";
import { Card } from "../../components/ui/Card";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";

interface ItemCardProps {
  item: ItemSummaryResponse;
  linkPrefix?: string;
}

export function ItemCard({ item, linkPrefix = "/marketplace/items" }: ItemCardProps) {
  return (
    <Link to={`${linkPrefix}/${item.uuid}`} className="block group">
      <Card className="h-full transition-shadow hover:shadow-md">
        {/* Placeholder image area */}
        <div className="aspect-[4/3] rounded-lg bg-slate-100 dark:bg-slate-700 mb-4 flex items-center justify-center">
          <Package className="size-12 text-slate-300 dark:text-slate-500" />
        </div>

        <h3 className="font-semibold text-slate-900 dark:text-slate-100 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 line-clamp-2 mb-2">
          {item.title}
        </h3>

        <div className="flex flex-wrap items-center gap-2 mb-3">
          <ItemStatusBadge status={item.status} />
          <ItemConditionBadge condition={item.condition} />
        </div>

        <div className="flex items-center justify-between text-sm text-slate-500 dark:text-slate-400">
          <span>{item.categoryName}</span>
          <span>by {item.ownerUsername}</span>
        </div>
      </Card>
    </Link>
  );
}

