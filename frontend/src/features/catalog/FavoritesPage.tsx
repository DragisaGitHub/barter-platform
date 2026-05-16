import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Clock, Heart, Package, User } from "lucide-react";
import type { FavoriteItemsParams } from "@/api/catalogApi.ts";
import type { ItemSummaryResponse } from "@/api/generated/types.ts";
import { routePaths } from "@/routes/routePaths.ts";
import { parseApiError } from "@/utils";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { ItemGridSkeleton } from "./ItemCardSkeleton";
import { useFavoriteItems, useUnfavoriteItem } from "./useCatalog";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";

export function FavoritesPage() {
  const { t } = useTranslation(["catalog", "common"]);
  const [params, setParams] = useState<FavoriteItemsParams>({
    page: 0,
    size: 20,
    sort: "createdAt,desc",
  });
  const [pendingItemUuid, setPendingItemUuid] = useState<string | null>(null);

  const { data, isLoading, isError } = useFavoriteItems(params);
  const unfavoriteMutation = useUnfavoriteItem();

  const totalLabel = useMemo(() => {
    const total = data?.totalElements ?? 0;
    return t("catalog:favorites.savedItems", { count: total });
  }, [data?.totalElements, t]);

  const handleUnfavorite = (itemUuid: string) => {
    setPendingItemUuid(itemUuid);
    unfavoriteMutation.mutate(itemUuid, {
      onError: (error) => {
        toast.error(parseApiError(error));
      },
      onSettled: () => {
        setPendingItemUuid((current) => (current === itemUuid ? null : current));
      },
    });
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <section className="marketplace-panel p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-rose-200 bg-rose-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-rose-700">
              {t("catalog:favorites.badge")}
            </div>
            <h1 className="mt-3 text-3xl font-bold text-slate-900">{t("catalog:favorites.title")}</h1>
            <p className="mt-2 text-sm text-slate-600">
              {t("catalog:favorites.subtitle")}
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            {totalLabel}
          </div>
        </div>
      </section>

      {isLoading ? <ItemGridSkeleton count={8} /> : null}

      {isError ? (
        <EmptyState
          title={t("catalog:favorites.errorTitle")}
          description={t("catalog:favorites.errorDescription")}
          action={
            <Button variant="outline" onClick={() => window.location.reload()}>
              {t("common:tryAgain")}
            </Button>
          }
        />
      ) : null}

      {!isLoading && !isError && data?.content.length === 0 ? (
        <section className="marketplace-panel p-4 sm:p-6">
          <EmptyState
            icon={<Heart className="size-16" />}
            title={t("catalog:favorites.emptyTitle")}
            description={t("catalog:favorites.emptyDescription")}
            action={
              <Link to={routePaths.marketplace}>
                <Button>{t("catalog:favorites.browseMarketplace")}</Button>
              </Link>
            }
          />
        </section>
      ) : null}

      {!isLoading && !isError && (data?.content.length ?? 0) > 0 ? (
        <>
          <section className="marketplace-panel p-4 sm:p-5">
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-lg font-medium text-slate-900">{t("catalog:favorites.savedListings")}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  {t("catalog:favorites.savedListingsDescription")}
                </p>
              </div>
              <Link to={routePaths.marketplace}>
                <Button variant="outline" className="border-slate-200 bg-white text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700">
                  {t("catalog:favorites.continueBrowsing")}
                </Button>
              </Link>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {data?.content.map((item) => (
                <FavoriteMarketplaceItemCard
                  key={item.uuid}
                  item={item}
                  isPending={pendingItemUuid === item.uuid}
                  onUnfavorite={handleUnfavorite}
                />
              ))}
            </div>
          </section>

          {data && data.totalPages > 1 ? (
            <div className="flex justify-center">
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={(page) => setParams((previous) => ({ ...previous, page }))}
              />
            </div>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

function FavoriteMarketplaceItemCard({
  item,
  isPending,
  onUnfavorite,
}: {
  item: ItemSummaryResponse;
  isPending: boolean;
  onUnfavorite: (itemUuid: string) => void;
}) {
  const { t, i18n } = useTranslation("catalog");
  const createdLabel = useMemo(() => {
    try {
      return new Date(item.createdAt).toLocaleDateString(i18n.language === "sr" ? "sr-Latn-RS" : "en-US");
    } catch {
      return t("recently");
    }
  }, [i18n.language, item.createdAt, t]);

  return (
    <div className="group relative">
      <button
        type="button"
        onClick={() => onUnfavorite(item.uuid)}
        disabled={isPending}
        className="absolute right-2.5 top-2.5 z-10 inline-flex size-9 items-center justify-center rounded-full border border-white/80 bg-white/95 text-rose-500 shadow-sm backdrop-blur transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
        aria-label={t("removeFromFavorites")}
      >
        <Heart className="size-4.5 fill-current" />
      </button>

      <Link
        to={routePaths.marketplaceItem(item.uuid)}
        className="marketplace-item-card flex h-full flex-col overflow-hidden transition-colors duration-200"
      >
        <div className="relative aspect-[4/3] overflow-hidden bg-slate-100">
          {item.primaryImageUrl ? (
            <img
              src={item.primaryImageUrl}
              alt={item.title}
              loading="lazy"
              className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-slate-100">
              <Package className="size-10 text-slate-300" />
            </div>
          )}

          <div className="marketplace-soft-badge absolute left-2.5 top-2.5 inline-flex items-center bg-white/95 px-2 py-0.5 text-[10px] font-medium uppercase tracking-[0.08em] text-emerald-700">
            {t(`status.${item.status.toLowerCase()}`)}
          </div>
        </div>

        <div className="flex flex-1 flex-col p-3">
          <div className="mb-2 flex flex-wrap items-center gap-1.5">
            <span className="marketplace-soft-badge inline-flex max-w-full items-center bg-violet-50 px-2 py-0.5 text-[10px] font-medium text-violet-700">
              <span className="truncate">{item.categoryName}</span>
            </span>
            <span className="marketplace-soft-badge inline-flex items-center bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">
              {t(conditionTranslationKey(item.condition))}
            </span>
          </div>

          <h3 className="line-clamp-2 min-h-[2.5rem] text-sm font-medium leading-5 text-slate-900 transition-colors group-hover:text-violet-600">
            {item.title}
          </h3>

          <div className="mt-auto space-y-2 pt-2">
            <div className="h-px w-full bg-slate-100" />
            <div className="flex items-center justify-between gap-3 text-xs text-slate-500">
              <div className="flex min-w-0 items-center gap-1.5">
                <User className="size-3.5 shrink-0 text-slate-400" />
                <span className="truncate">{item.ownerUsername}</span>
              </div>
              <div className="flex shrink-0 items-center gap-1.5">
                <Clock className="size-3.5 text-slate-400" />
                <span>{createdLabel}</span>
              </div>
            </div>
          </div>
        </div>
      </Link>
    </div>
  );
}

function conditionTranslationKey(value: string) {
  const keys: Record<string, string> = {
    NEW: "condition.new",
    LIKE_NEW: "condition.likeNew",
    GOOD: "condition.good",
    USED: "condition.used",
    FOR_PARTS: "condition.forParts",
  };
  return keys[value] ?? value;
}

