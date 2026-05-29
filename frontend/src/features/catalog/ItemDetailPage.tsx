import { useMemo, useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft,
  ArrowRightLeft,
  CalendarDays,
  MapPin,
  ZoomIn,
  Package,
  ShieldCheck,
  Tag,
  User,
} from "lucide-react";
import { useItemDetail } from "./useCatalog";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { OwnerModerationPanel } from "./OwnerModerationPanel";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { ImageLightbox, type LightboxImage } from "../../components/ui/ImageLightbox";
import { SendOfferModal } from "../trade/SendOfferModal";
import { ReportTrigger } from "@/features/reports/ReportTrigger";
import { useAuth } from "../../auth/AuthContext";
import { routePaths } from "@/routes/routePaths.ts";
import type { ItemImageResponse } from "@/api/generated/types.ts";
import { cn } from "@/utils";
import { useTranslation } from "react-i18next";

function ImageSection({ images }: { images: ItemImageResponse[] }) {
  const { t } = useTranslation("catalog");
  const [selectedIdx, setSelectedIdx] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const sorted = useMemo(
    () =>
      [...images].sort((a, b) => {
        if (a.isPrimary) return -1;
        if (b.isPrimary) return 1;
        return a.sortOrder - b.sortOrder;
      }),
    [images]
  );
  const primary = sorted[0];
  const displayed = sorted[selectedIdx] ?? primary;
  const lightboxImages: LightboxImage[] = useMemo(
    () =>
      sorted.map((image, index) => ({
        id: image.uuid,
        src: image.url,
        alt: image.originalFilename || t("images.itemPhotoAlt", { number: index + 1 }),
      })),
    [sorted, t]
  );

  if (images.length === 0) {
    return (
      <div className="marketplace-panel overflow-hidden p-0">
        <div className="aspect-5/4 bg-slate-100 flex flex-col items-center justify-center gap-2.5">
          <div className="flex size-14 items-center justify-center rounded-full bg-white border border-slate-200">
            <Package className="size-7 text-slate-300" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-slate-700">{t("itemDetail.noPhotos")}</p>
            <p className="mt-1 text-xs text-slate-500">{t("itemDetail.noPhotosDescription")}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="marketplace-panel p-3">
      <button
        type="button"
        onClick={() => setIsLightboxOpen(true)}
        className="group relative block aspect-5/4 w-full overflow-hidden rounded-lg border border-slate-200 bg-slate-100 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-violet-500 focus-visible:ring-offset-2 dark:border-slate-700 dark:bg-slate-900"
        aria-label={t("images.openViewer")}
      >
        {displayed ? (
          <>
            <img
              src={displayed.url}
              alt={displayed.originalFilename || t("images.itemPhotoAlt", { number: selectedIdx + 1 })}
              loading="lazy"
              className="h-full w-full object-contain"
            />
            <span className="pointer-events-none absolute bottom-3 right-3 inline-flex items-center gap-1.5 rounded-full bg-slate-950/70 px-3 py-1.5 text-xs font-medium text-white opacity-0 shadow-lg transition group-hover:opacity-100 group-focus-visible:opacity-100">
              <ZoomIn className="size-3.5" aria-hidden="true" />
              {t("images.openViewer")}
            </span>
          </>
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Package className="size-16 text-slate-300" />
          </div>
        )}
      </button>

      {sorted.length > 1 && (
        <div className="mt-2.5 grid grid-cols-5 gap-1.5 sm:grid-cols-6">
          {sorted.map((img, idx) => (
            <button
              key={img.uuid}
              type="button"
              onClick={() => setSelectedIdx(idx)}
              aria-label={t("images.selectImage", { number: idx + 1 })}
              aria-pressed={idx === selectedIdx}
              className={cn(
                "relative aspect-square overflow-hidden rounded-lg border bg-slate-100 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-violet-500 focus-visible:ring-offset-2 dark:bg-slate-900",
                idx === selectedIdx
                  ? "border-violet-400 ring-1 ring-violet-200 dark:border-violet-400 dark:ring-violet-500/40"
                  : "border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-500"
              )}
            >
              <img
                src={img.url}
                alt={img.originalFilename || t("images.itemPhotoAlt", { number: idx + 1 })}
                loading="lazy"
                className="w-full h-full object-contain"
              />
              {img.isPrimary ? <span className="sr-only">{t("images.primaryImage")}</span> : null}
            </button>
          ))}
        </div>
      )}

      <ImageLightbox
        images={lightboxImages}
        isOpen={isLightboxOpen}
        selectedIndex={selectedIdx}
        onOpenChange={setIsLightboxOpen}
        onSelectedIndexChange={setSelectedIdx}
        labels={{
          title: t("images.viewerTitle"),
          description: t("images.viewerDescription"),
          close: t("images.closeViewer"),
          previous: t("images.previousImage"),
          next: t("images.nextImage"),
          counter: (current, total) => t("images.imageCounter", { current, total }),
          thumbnail: (index) => t("images.selectImage", { number: index }),
        }}
      />
    </div>
  );
}

export function ItemDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const { data: item, isLoading, isError } = useItemDetail(uuid ?? "");
  const { user, isAuthenticated } = useAuth();
  const [showOfferModal, setShowOfferModal] = useState(false);
  const { t, i18n } = useTranslation(["catalog", "common"]);

  if (isLoading) {
    return (
      <div className="marketplace-page min-h-screen px-4 py-12 sm:px-6">
        <div className="mx-auto flex max-w-400 items-center justify-center py-24">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="marketplace-page min-h-screen px-4 py-12 sm:px-6">
        <div className="mx-auto max-w-3xl">
          <EmptyState
            title={t("catalog:itemNotFound")}
            description={t("catalog:itemDetail.notFoundDescription")}
            action={
              <Link to={routePaths.marketplace}>
                <Button variant="outline">{t("common:backToMarketplace")}</Button>
              </Link>
            }
          />
        </div>
      </div>
    );
  }

  const isOwner = !!user && user.uuid === item.ownerUuid;
  const ownerItemPath = routePaths.myItemDetail(item.uuid);
  const canProposeTrade = isAuthenticated && item.status === "ACTIVE" && !isOwner;
  const showGuestTradeCta = !isAuthenticated && item.status === "ACTIVE";
  const loginRedirectUrl = `${routePaths.login}?redirect=${encodeURIComponent(
    routePaths.marketplaceItem(item.uuid)
  )}`;
  const registerRedirectUrl = `${routePaths.register}?redirect=${encodeURIComponent(
    routePaths.marketplaceItem(item.uuid)
  )}`;
  const descriptionPreview = item.description?.trim();
  const approximateLocation = formatApproximateExchangeLocation(item);
  const listingMode = item.listingMode ?? "SINGLE";
  const isMultiItem = listingMode !== "SINGLE";
  const listedDate = new Date(item.createdAt).toLocaleDateString(i18n.language === "sr" ? "sr-Latn-RS" : "en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  return (
    <div className="marketplace-page min-h-screen px-4 py-6 sm:px-6">
      <div className="mx-auto max-w-370">
        <Link
          to={routePaths.marketplace}
          className="mb-3 inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition hover:text-violet-600"
        >
          <ArrowLeft className="size-4" />
          {t("common:backToMarketplace")}
        </Link>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,0.9fr)_290px]">
          <div className="min-w-0">
            <ImageSection images={item.images ?? []} />
          </div>

          <section className="marketplace-panel p-4">
            {isOwner ? <OwnerModerationPanel item={item} /> : null}

            <div className="flex flex-wrap gap-1.5">
              <ItemStatusBadge status={item.status} />
              <ItemConditionBadge condition={item.condition} />
              {isMultiItem ? (
                <Badge variant="warning" className="rounded-full">
                  {t(`catalog:listingMode.badge.${listingMode}`)}
                </Badge>
              ) : null}
              <span className="marketplace-soft-badge inline-flex items-center bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-700">
                {item.category.name}
              </span>
            </div>

            <h1 className="mt-3 text-xl font-semibold leading-tight text-slate-900 sm:text-[26px]">
              {item.title}
            </h1>

            <div className="mt-4 grid gap-2.5 sm:grid-cols-2">
              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5">
                <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
                  <CalendarDays className="size-3.5" />
                  {t("catalog:itemDetail.listedOn")}
                </div>
                <p className="mt-1 text-sm font-medium text-slate-900">{listedDate}</p>
              </div>

              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5">
                <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
                  <Tag className="size-3.5" />
                  {t("catalog:fields.category")}
                </div>
                <p className="mt-1 text-sm font-medium text-slate-900">{item.category.name}</p>
              </div>

              {approximateLocation ? (
                <div className="rounded-lg border border-emerald-100 bg-emerald-50 px-3.5 py-2.5 sm:col-span-2">
                  <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-emerald-700">
                    <MapPin className="size-3.5" />
                    {t("catalog:itemDetail.exchangeLocation")}
                  </div>
                  <p className="mt-1 text-sm font-medium text-slate-900">{approximateLocation}</p>
                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    {t("catalog:itemDetail.exchangeLocationPrivacy")}
                  </p>
                </div>
              ) : null}
            </div>

            {item.tags.length > 0 ? (
              <div className="mt-4">
                <h2 className="text-sm font-medium text-slate-500">{t("catalog:tags")}</h2>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {item.tags.map((tag) => (
                    <Badge
                      key={tag.uuid}
                      variant="secondary"
                      className="rounded-md border border-slate-200 bg-white px-2 py-0.5 text-[11px] font-medium text-slate-600 shadow-none"
                    >
                      {tag.name}
                    </Badge>
                  ))}
                </div>
              </div>
            ) : null}

            <div className="mt-4 border-t border-slate-200 pt-4">
              <h2 className="text-sm font-medium text-slate-500">{t("catalog:itemDetail.listingPreview")}</h2>
              <p className="mt-1.5 whitespace-pre-line text-sm leading-6 text-slate-700">
                {descriptionPreview || t("catalog:itemDetail.noDescription")}
              </p>
            </div>

            {isMultiItem ? (
              <div className="mt-4 rounded-lg border border-amber-100 bg-amber-50 px-3.5 py-3 text-sm text-amber-800">
                <p className="font-medium">{t(`catalog:listingMode.label.${listingMode}`)}</p>
                <p className="mt-1 leading-5">
                  {listingMode === "BUNDLE"
                    ? t("catalog:itemDetail.bundleExplanation")
                    : t("catalog:itemDetail.pickAnyExplanation")}
                </p>
              </div>
            ) : null}
          </section>

          <aside className="space-y-3">
            <section className="marketplace-panel p-4">
              <div className="flex items-start gap-2.5">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-violet-50 text-violet-600">
                  <User className="size-4.5" />
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">{t("catalog:itemDetail.seller")}</p>
                  {item.ownerUuid ? (
                    <Link
                      to={routePaths.publicProfile(item.ownerUuid)}
                      className="mt-0.5 block truncate text-[15px] font-semibold text-slate-900 transition hover:text-violet-600"
                    >
                      {item.ownerUsername}
                    </Link>
                  ) : (
                    <p className="mt-0.5 truncate text-[15px] font-semibold text-slate-900">{item.ownerUsername}</p>
                  )}
                  <p className="mt-0.5 text-sm text-slate-500">{t("catalog:itemDetail.memberTrader")}</p>
                </div>
              </div>

              <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-600">
                <div className="flex items-center gap-2 font-medium text-slate-700">
                  <ShieldCheck className="size-4 text-violet-600" />
                  {t("catalog:itemDetail.tradeSafely")}
                </div>
                <p className="mt-1 leading-5 text-slate-500">
                  {t("catalog:itemDetail.tradeSafelyDescription")}
                </p>
              </div>

              {!isOwner ? (
                <ReportTrigger
                  targetType="ITEM"
                  targetUuid={item.uuid}
                  contextLabel={item.title}
                  className="mt-3 w-full rounded-lg border-slate-200"
                />
              ) : null}

              {showGuestTradeCta ? (
                <div className="mt-4 space-y-2.5">
                  <Link to={registerRedirectUrl} className="block">
                    <Button className="h-10 w-full rounded-lg bg-violet-500 text-white hover:bg-violet-600">
                      <ArrowRightLeft className="size-4" />
                      {t("catalog:itemDetail.createAccountToTrade")}
                    </Button>
                  </Link>
                  <Link
                    to={loginRedirectUrl}
                    className="inline-flex items-center justify-center text-sm font-medium text-violet-600 transition hover:text-violet-700"
                  >
                    {t("catalog:itemDetail.alreadyHaveAccount")}
                  </Link>
                </div>
              ) : null}

              {canProposeTrade ? (
                <div className="mt-4 space-y-2.5">
                  <Button
                    onClick={() => setShowOfferModal(true)}
                    className="h-10 w-full rounded-lg bg-violet-500 text-white hover:bg-violet-600"
                  >
                    <ArrowRightLeft className="size-4" />
                    {t("catalog:itemDetail.proposeTrade")}
                  </Button>
                  <p className="text-sm leading-5 text-slate-500">
                    {t("catalog:itemDetail.proposeTradeDescription")}
                  </p>
                </div>
              ) : null}

              {isOwner ? (
                <div className="mt-4 space-y-2.5">
                  <div className="rounded-lg border border-violet-200 bg-violet-50 px-3.5 py-2.5 text-sm leading-5 text-violet-700">
                    {t("catalog:itemDetail.ownerNotice")}
                  </div>
                  <Link to={ownerItemPath} className="block">
                    <Button variant="outline" className="h-10 w-full rounded-lg border-slate-200">
                      {t("catalog:itemDetail.openOwnerDetail")}
                    </Button>
                  </Link>
                  <Link to={routePaths.myItemsEdit(item.uuid)} className="block">
                    <Button variant="outline" className="h-10 w-full rounded-lg border-slate-200">
                      {t("catalog:itemDetail.editListing")}
                    </Button>
                  </Link>
                </div>
              ) : null}
            </section>
          </aside>
        </div>

        <section className="marketplace-panel mt-4 p-4">
          <h2 className="text-lg font-medium text-slate-900">{t("catalog:fields.description")}</h2>
          <div className="mt-2.5 border-t border-slate-200 pt-3">
            <p className="whitespace-pre-line text-sm leading-6 text-slate-700">
              {descriptionPreview || t("catalog:itemDetail.noDescription")}
            </p>
          </div>
        </section>

        {isMultiItem ? (
          <section className="marketplace-panel mt-4 p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-medium text-slate-900">{t("catalog:itemDetail.entriesTitle")}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  {listingMode === "BUNDLE"
                    ? t("catalog:itemDetail.bundleExplanation")
                    : t("catalog:itemDetail.pickAnyExplanation")}
                </p>
              </div>
              <Badge variant="warning">
                {t("catalog:itemDetail.entryCount", { count: item.entries?.length ?? 0 })}
              </Badge>
            </div>

            <div className="mt-4 grid gap-3 md:grid-cols-2">
              {(item.entries ?? []).map((entry, index) => (
                <div key={entry.uuid} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
                        {t("catalog:itemDetail.entryNumber", { number: index + 1 })}
                      </p>
                      <h3 className="mt-1 font-semibold text-slate-900">{entry.title}</h3>
                    </div>
                    {entry.quantity ? (
                      <span className="rounded-full bg-white px-2 py-0.5 text-xs font-medium text-slate-600 ring-1 ring-slate-200">
                        {t("catalog:itemDetail.entryQuantity", { quantity: entry.quantity })}
                      </span>
                    ) : null}
                  </div>
                  {entry.description ? (
                    <p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{entry.description}</p>
                  ) : null}
                </div>
              ))}
            </div>
          </section>
        ) : null}
      </div>

      {canProposeTrade && (
        <SendOfferModal
          isOpen={showOfferModal}
          onClose={() => setShowOfferModal(false)}
          receiverItem={item}
        />
      )}
    </div>
  );
}

function formatApproximateExchangeLocation(item: {
  exchangeArea?: string | null;
  exchangeCity?: string | null;
  exchangeLocation?: string | null;
}) {
  const locality = [item.exchangeArea, item.exchangeCity].filter(Boolean).join(", ");
  return [locality, item.exchangeLocation].filter(Boolean).join(" · ");
}

