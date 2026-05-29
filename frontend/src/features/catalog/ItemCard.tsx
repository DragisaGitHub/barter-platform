import { Link } from "react-router-dom";
import { MapPin, Package } from "lucide-react";
import type { ItemSummaryResponse } from "@/api/generated/types.ts";
import { Card } from "../../components/ui/Card";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { routePaths } from "@/routes/routePaths.ts";
import { useTranslation } from "react-i18next";

interface ItemCardProps {
  item: ItemSummaryResponse;
  linkPrefix?: string;
}

export function ItemCard({ item, linkPrefix = "/marketplace/items" }: ItemCardProps) {
  const { t } = useTranslation("catalog");
  const approximateLocation = formatApproximateExchangeLocation(item);
  return (
    <Link to={`${linkPrefix}/${item.uuid}`} className="block group">
      <Card className="h-full transition-shadow hover:shadow-md">
        {/* Image area */}
        <div className="aspect-[4/3] rounded-lg bg-slate-100 dark:bg-slate-700 mb-4 overflow-hidden flex items-center justify-center">
          {item.primaryImageUrl ? (
            <img
              src={item.primaryImageUrl}
              alt={item.title}
              loading="lazy"
              className="w-full h-full object-cover"
            />
          ) : (
            <Package className="size-12 text-slate-300 dark:text-slate-500" />
          )}
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
          {item.ownerUuid ? (
            <Link
              to={routePaths.publicProfile(item.ownerUuid)}
              onClick={(e) => e.stopPropagation()}
              className="hover:text-indigo-600 dark:hover:text-indigo-400 hover:underline"
            >
              {t("itemCard.byOwner", { username: item.ownerUsername })}
            </Link>
          ) : (
            <span>{t("itemCard.byOwner", { username: item.ownerUsername })}</span>
          )}
        </div>

        {approximateLocation ? (
          <div className="mt-2 flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
            <MapPin className="size-3.5 shrink-0" />
            <span className="truncate">{approximateLocation}</span>
          </div>
        ) : null}
      </Card>
    </Link>
  );
}

function formatApproximateExchangeLocation(item: ItemSummaryResponse) {
  const locality = [item.exchangeArea, item.exchangeCity].filter(Boolean).join(", ");
  return [locality, item.exchangeLocation].filter(Boolean).join(" · ");
}

