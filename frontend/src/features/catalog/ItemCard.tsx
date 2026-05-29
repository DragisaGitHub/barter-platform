import { Link } from "react-router-dom";
import { MapPin, Package } from "lucide-react";
import type { ItemSummaryResponse } from "@/api/generated/types.ts";
import { Card } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
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
  const listingMode = item.listingMode ?? "SINGLE";
  const isMultiItem = listingMode !== "SINGLE";
  return (
    <Link to={`${linkPrefix}/${item.uuid}`} className="block group">
      <Card className="h-full transition-shadow hover:shadow-md">
        {/* Image area */}
        <div className="aspect-4/3 rounded-lg bg-slate-100 dark:bg-slate-700 mb-4 overflow-hidden flex items-center justify-center">
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
          {isMultiItem ? (
            <Badge variant="secondary" className="rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
              {t(`listingMode.badge.${listingMode}`)}
            </Badge>
          ) : null}
        </div>

        {isMultiItem ? (
          <div className="mb-3 rounded-lg border border-amber-100 bg-amber-50/70 px-3 py-2 text-xs text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/20 dark:text-amber-200">
            <p className="font-medium">
              {listingMode === "BUNDLE"
                ? t("itemCard.bundleEntryCount", { count: item.entryCount ?? 0 })
                : t("itemCard.pickAnyEntryCount", { count: item.entryCount ?? 0 })}
            </p>
            {item.previewEntries?.length ? (
              <p className="mt-1 truncate text-amber-700/80 dark:text-amber-200/80">
                {item.previewEntries.map((entry) => entry.title).join(" · ")}
              </p>
            ) : null}
          </div>
        ) : null}

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

