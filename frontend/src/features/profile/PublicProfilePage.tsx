import { useEffect, useState, type ReactNode } from "react";
import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  Package,
  ShieldCheck,
  ThumbsUp,
  RefreshCw,
  CheckCircle2,
  XCircle,
} from "lucide-react";
import { usePublicProfile, usePublicProfileItems } from "./useProfile";
import { TrustSummary } from "./TrustSummary";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { routePaths } from "@/routes/routePaths.ts";
import { useAuth } from "../../auth/AuthContext";
import { Card } from "../../components/ui/Card";
import { Pagination } from "../../components/data/Pagination";
import { Skeleton } from "../../components/ui/Skeleton";
import { PublicProfileListingCard } from "./PublicProfileListingCard";
import { PublicProfilePageSkeleton } from "./PublicProfilePageSkeleton";
import { ReportTrigger } from "@/features/reports/ReportTrigger";
import { useTranslation } from "react-i18next";

function formatJoinDate(iso?: string | null): string | null {
  if (!iso) {
    return null;
  }

  const date = new Date(iso);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return date.toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function getInitials(username: string): string {
  const parts = username
    .split(/[\s._-]+/)
    .map((part) => part.trim())
    .filter(Boolean);

  if (parts.length === 0) {
    return username.slice(0, 2).toUpperCase();
  }

  return parts
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
}

export function PublicProfilePage() {
  const { t } = useTranslation(["profile", "common"]);
  const { uuid } = useParams<{ uuid: string }>();
  const { user } = useAuth();
  const [itemsPageIndex, setItemsPageIndex] = useState(0);
  const {
    data: profile,
    isLoading: profileLoading,
    isError: profileError,
    refetch: refetchProfile,
  } = usePublicProfile(uuid ?? "");
  const {
    data: itemsPage,
    isLoading: itemsLoading,
    isError: itemsError,
    refetch: refetchItems,
  } = usePublicProfileItems(uuid ?? "", {
    page: itemsPageIndex,
    size: 12,
    sort: "createdAt,desc",
  });

  useEffect(() => {
    setItemsPageIndex(0);
  }, [uuid]);

  if (profileLoading) {
    return <PublicProfilePageSkeleton />;
  }

  if (profileError || !profile) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <Link
          to={routePaths.marketplace}
          className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-violet-700 transition hover:text-violet-800 dark:text-violet-300 dark:hover:text-violet-200"
        >
          <ArrowLeft className="size-4" />
          {t("common:backToMarketplace")}
        </Link>
        <EmptyState
          icon={<ShieldCheck className="size-12" />}
          title={t("profile:public.notFoundTitle")}
          description={t("profile:public.notFoundDescription")}
          action={
            <div className="flex flex-wrap justify-center gap-3">
              <Button variant="outline" onClick={() => void refetchProfile()}>
                <RefreshCw className="size-4" />
                {t("common:tryAgain")}
              </Button>
              <Link to={routePaths.marketplace}>
                <Button>{t("common:backToMarketplace")}</Button>
              </Link>
            </div>
          }
        />
      </div>
    );
  }

  const items = itemsPage?.content ?? [];
  const joinedDate = formatJoinDate(profile.joinedAt);
  const isOwnProfile = user?.uuid === profile.uuid;
  const listingsTotal = itemsPage?.totalElements ?? items.length;
  const positivePercentage = profile.reputationSummary.positivePercentage;

  const handleItemsPageChange = (page: number) => {
    setItemsPageIndex(page);
    document.getElementById("public-profile-listings")?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Link
          to={routePaths.marketplace}
          className="inline-flex items-center gap-2 text-sm font-medium text-violet-700 transition hover:text-violet-800 dark:text-violet-300 dark:hover:text-violet-200"
        >
          <ArrowLeft className="size-4" />
          {t("common:backToMarketplace")}
        </Link>

        {isOwnProfile ? (
          <Link to={routePaths.myItems}>
            <Button variant="outline">{t("profile:public.manageMyItems")}</Button>
          </Link>
        ) : profile ? (
          <ReportTrigger
            targetType="USER"
            targetUuid={profile.uuid}
            contextLabel={profile.username}
            variant="outline"
          />
        ) : null}
      </div>

      <Card className="overflow-hidden border-violet-100/80 bg-gradient-to-br from-violet-50 via-white to-white p-0 shadow-sm dark:border-violet-900/40 dark:from-violet-950/20 dark:via-slate-900 dark:to-slate-900">
        <div className="relative">
          <div className="absolute inset-x-0 top-0 h-28 bg-gradient-to-r from-violet-500/12 via-indigo-500/6 to-transparent" />

          <div className="relative flex flex-col gap-6 p-6 lg:flex-row lg:items-center lg:justify-between lg:p-8">
            <div className="flex min-w-0 flex-1 flex-col gap-5 sm:flex-row sm:items-center">
              <div className="flex size-20 shrink-0 items-center justify-center rounded-full bg-white text-2xl font-bold tracking-wide text-violet-700 shadow-sm ring-1 ring-violet-100 dark:bg-slate-900 dark:text-violet-300 dark:ring-violet-900/60">
                {getInitials(profile.username)}
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="truncate text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
                    {profile.username}
                  </h1>
                  <span className="inline-flex items-center rounded-full border border-violet-200/80 bg-white/80 px-3 py-1 text-xs font-medium uppercase tracking-[0.12em] text-violet-700 shadow-sm dark:border-violet-800/60 dark:bg-slate-900/80 dark:text-violet-300">
                    {t("profile:public.badge")}
                  </span>
                </div>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">
                  {t("profile:public.subtitle")}
                </p>

                <div className="mt-4 flex flex-wrap gap-2">
                  {joinedDate ? (
                    <ProfilePill icon={<CalendarDays className="size-4" />} label={t("profile:public.joined", { date: joinedDate })} />
                  ) : null}
                  <ProfilePill
                    icon={<Package className="size-4" />}
                    label={t("profile:public.activeListings", { count: profile.activeItemCount })}
                  />
                  <ProfilePill
                    icon={<CheckCircle2 className="size-4" />}
                    label={t("profile:public.completedTrades", { count: profile.completedTradeCount })}
                  />
                  <ProfilePill
                    icon={<XCircle className="size-4" />}
                    label={t("profile:public.cancelledTrades", { count: profile.cancelledTradeCount })}
                  />
                  {profile.reputationSummary.totalReviewCount > 0 ? (
                    <ProfilePill
                      icon={<ThumbsUp className="size-4" />}
                      label={
                        positivePercentage == null
                          ? t("profile:public.reviews", { count: profile.reputationSummary.totalReviewCount })
                          : t("profile:public.positiveReviewsPercent", { percent: positivePercentage.toFixed(0) })
                      }
                    />
                  ) : null}
                </div>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:w-[360px] lg:shrink-0">
              <div className="rounded-2xl border border-white/80 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700/70 dark:bg-slate-900/80">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-violet-600 dark:text-violet-300">
                  {t("profile:public.marketplaceViewTitle")}
                </p>
                <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
                  {t("profile:public.marketplaceViewDescription")}
                </p>
              </div>

              <div className="rounded-2xl border border-violet-200/70 bg-violet-50/80 p-4 shadow-sm dark:border-violet-900/50 dark:bg-violet-950/20">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-violet-700 dark:text-violet-300">
                  {t("profile:public.liveInventory")}
                </p>
                <p className="mt-2 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-50">
                  {profile.activeItemCount}
                </p>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                  {profile.activeItemCount === 1
                    ? t("profile:public.listingAvailable")
                    : t("profile:public.listingsAvailable")}
                </p>
              </div>
            </div>
          </div>
        </div>
      </Card>

      <div className="mt-6">
        <TrustSummary
          activeItemCount={profile.activeItemCount}
          completedTradeCount={profile.completedTradeCount}
          cancelledTradeCount={profile.cancelledTradeCount}
          averageRating={profile.averageRating}
          reputationSummary={profile.reputationSummary}
        />
      </div>

      <section id="public-profile-listings" className="mt-8">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
              {t("profile:public.activeListingsTitle")}
            </h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
              {t("profile:public.activeListingsDescription")}
            </p>
          </div>

          <div className="inline-flex items-center rounded-full border border-violet-200/80 bg-violet-50 px-3 py-1.5 text-sm font-medium text-violet-700 dark:border-violet-900/60 dark:bg-violet-950/20 dark:text-violet-300">
            {t("profile:public.liveListings", { count: listingsTotal })}
          </div>
        </div>

        <Card className="overflow-hidden border-slate-200/80 p-5 shadow-sm dark:border-slate-700/80 sm:p-6">
          {itemsLoading ? (
            <ListingsSkeletonGrid />
          ) : itemsError && items.length === 0 ? (
            <EmptyState
              icon={<Package className="size-10" />}
              title={t("profile:public.listingsErrorTitle")}
              description={t("profile:public.listingsErrorDescription")}
              action={
                <Button variant="outline" onClick={() => void refetchItems()}>
                  <RefreshCw className="size-4" />
                  {t("common:tryAgain")}
                </Button>
              }
            />
          ) : items.length === 0 ? (
            <EmptyState
              icon={<Package className="size-10" />}
              title={t("profile:public.noActiveListings")}
              description={t("profile:public.noActiveListingsDescription")}
            />
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {items.map((item) => (
                  <PublicProfileListingCard key={item.uuid} item={item} />
                ))}
              </div>

              {itemsPage && itemsPage.totalPages > 1 ? (
                <div className="mt-6 rounded-xl border border-slate-200/80 dark:border-slate-700/80">
                  <Pagination
                    currentPage={itemsPage.page}
                    totalPages={itemsPage.totalPages}
                    onPageChange={handleItemsPageChange}
                  />
                </div>
              ) : null}
            </>
          )}
        </Card>
      </section>
    </div>
  );
}

function ProfilePill({ icon, label }: { icon: ReactNode; label: string }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/90 px-3 py-1.5 text-sm text-slate-700 shadow-sm dark:border-slate-700/80 dark:bg-slate-900/80 dark:text-slate-200">
      <span className="text-violet-600 dark:text-violet-300">{icon}</span>
      <span>{label}</span>
    </div>
  );
}

function ListingsSkeletonGrid() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: 8 }).map((_, index) => (
        <Card
          key={index}
          className="overflow-hidden border-slate-200/80 p-0 shadow-sm dark:border-slate-700/80"
        >
          <Skeleton className="aspect-[4/3] w-full rounded-none" />
          <div className="space-y-3 p-4">
            <div className="flex gap-2">
              <Skeleton className="h-5 w-16 rounded-full" />
              <Skeleton className="h-5 w-20 rounded-full" />
            </div>
            <Skeleton className="h-5 w-full" />
            <Skeleton className="h-5 w-3/4" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-4 w-24" />
          </div>
        </Card>
      ))}
    </div>
  );
}

