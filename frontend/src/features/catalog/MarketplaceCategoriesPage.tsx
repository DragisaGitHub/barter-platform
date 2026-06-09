import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, ArrowRight, LogIn, Search, Sparkles, UserPlus, X } from "lucide-react";
import type { CategoryResponse } from "@/api/generated/types";
import { routePaths, buildPathWithQuery } from "@/routes/routePaths";
import { useAuth } from "@/auth/AuthContext";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { useCategories, usePopularCategories } from "./useCatalog";
import { MarketplaceUserMenu } from "./MarketplaceUserMenu";
import { useTranslation } from "react-i18next";

const pageShellClassName = "marketplace-panel";

export function MarketplaceCategoriesPage() {
  const { user, isAuthenticated } = useAuth();
  const { t } = useTranslation(["catalog", "common"]);
  const [searchInput, setSearchInput] = useState("");
  const { data: categories, isLoading, isError } = useCategories();
  const { data: popularCategories } = usePopularCategories({ limit: 20 });

  const orderedCategories = useMemo(
    () => (categories ? [...categories].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)) : []),
    [categories]
  );

  const popularCategoryCounts = useMemo(
    () =>
      new Map(
        (popularCategories ?? []).map((category) => [category.uuid, category.activeItemCount ?? 0])
      ),
    [popularCategories]
  );

  const filteredCategories = useMemo(() => {
    const query = searchInput.trim().toLowerCase();

    if (!query) {
      return orderedCategories;
    }

    return orderedCategories.filter((category) => {
      const haystack = [category.name, category.description ?? ""].join(" ").toLowerCase();
      return haystack.includes(query);
    });
  }, [orderedCategories, searchInput]);

  const visibleCategoriesLabel = t("catalog:categoriesPage.matchingCategories", { count: filteredCategories.length });

  const clearSearch = () => {
    setSearchInput("");
  };

  return (
    <div className="marketplace-page min-h-screen text-slate-900">
      <header className="marketplace-nav sticky top-0 z-40 backdrop-blur-sm">
        <div className="mx-auto flex max-w-[1600px] flex-col gap-4 px-4 py-4 sm:px-6 xl:flex-row xl:items-center xl:gap-8">
          <div className="flex items-center justify-between gap-4 xl:shrink-0">
            <Link to={routePaths.marketplace} className="flex items-center gap-2">
              <div className="flex size-8 items-center justify-center rounded-lg bg-violet-500">
                <span className="text-base font-semibold text-white">⇄</span>
              </div>
              <span className="text-xl font-semibold text-slate-900">{t("common:appName")}</span>
            </Link>

            {isAuthenticated ? <MarketplaceUserMenu username={user?.username} compact align="start" className="xl:hidden" /> : null}
          </div>

          <div className="flex flex-1 flex-col gap-2 xl:max-w-3xl">
            <div className="flex flex-wrap items-center gap-2 text-sm text-slate-500">
              <Link
                to={routePaths.marketplace}
                className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-slate-600 transition hover:border-violet-200 hover:text-violet-600"
              >
                <ArrowLeft className="size-4" />
                {t("common:backToMarketplace")}
              </Link>
              <span className="rounded-full bg-violet-50 px-3 py-1.5 font-medium text-violet-700">
                {t("catalog:allCategories")}
              </span>
            </div>
            <div>
              <h1 className="text-2xl font-semibold text-slate-900 sm:text-3xl">{t("catalog:categoriesPage.title")}</h1>
              <p className="mt-1 text-sm text-slate-500 sm:text-base">
                {t("catalog:categoriesPage.subtitle")}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2 xl:ml-auto xl:justify-end">
            {isAuthenticated ? (
              <MarketplaceUserMenu username={user?.username} className="hidden xl:inline-flex" />
            ) : (
              <>
                <Link
                  to={routePaths.login}
                  className="inline-flex h-10 items-center justify-center gap-2 px-4 text-sm font-medium text-slate-700 transition hover:text-violet-600"
                >
                  <LogIn className="size-4" />
                  {t("common:login")}
                </Link>
                <Link
                  to={routePaths.register}
                  className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-violet-500 px-5 text-sm font-medium text-white transition hover:bg-violet-600"
                >
                  <UserPlus className="size-4" />
                  {t("common:signUp")}
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto flex max-w-[1600px] flex-col gap-6 px-4 py-6 sm:px-6">
        <section className={`${pageShellClassName} p-5 sm:p-6`}>
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-violet-50 px-3 py-1 text-xs font-medium uppercase tracking-[0.12em] text-violet-700">
                <Sparkles className="size-3.5" />
                {t("catalog:categoriesPage.badge")}
              </div>
              <h2 className="text-xl font-semibold text-slate-900 sm:text-2xl">{t("catalog:categoriesPage.discoveryTitle")}</h2>
              <p className="mt-2 text-sm leading-6 text-slate-500 sm:text-base">
                {t("catalog:categoriesPage.discoveryDescription")}
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              <div className="font-medium text-slate-900">{t("catalog:categoriesPage.totalCategories", { count: orderedCategories.length })}</div>
              <div className="mt-1">{visibleCategoriesLabel} matching your search</div>
            </div>
          </div>

          <div className="mt-6 relative max-w-2xl">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Escape" && searchInput) {
                  event.preventDefault();
                  clearSearch();
                }
              }}
              placeholder={t("catalog:categoriesPage.searchPlaceholder")}
              className="marketplace-search-input h-12 w-full pl-12 pr-12 text-sm text-slate-900 outline-none transition focus:bg-white"
            />

            {searchInput ? (
              <button
                type="button"
                onClick={clearSearch}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-violet-500/30"
                aria-label={t("common:clearSearch")}
                title={t("common:clearSearch")}
              >
                <X className="size-4" />
              </button>
            ) : null}
          </div>
        </section>

        <section className={`${pageShellClassName} p-5 sm:p-6`}>
          <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-lg font-medium text-slate-900">{t("catalog:categoriesPage.directoryTitle")}</h2>
              <p className="text-sm text-slate-500">{t("catalog:categoriesPage.directoryDescription")}</p>
            </div>
            <Link
              to={routePaths.marketplace}
              className="inline-flex items-center gap-2 self-start text-sm font-medium text-violet-600 transition hover:text-violet-700 sm:self-auto"
            >
              {t("catalog:categoriesPage.viewMarketplace")}
              <ArrowRight className="size-4" />
            </Link>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-16">
              <Spinner size="lg" />
            </div>
          ) : null}

          {isError ? (
            <EmptyState
              title={t("catalog:categoriesPage.errorTitle")}
              description={t("catalog:categoriesPage.errorDescription")}
            />
          ) : null}

          {!isLoading && !isError && filteredCategories.length === 0 ? (
            <EmptyState
              icon={<Search className="size-16" />}
              title={t("catalog:categoriesPage.emptyTitle")}
              description={t("catalog:categoriesPage.emptyDescription")}
              action={
                <button
                  type="button"
                  onClick={() => setSearchInput("")}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-violet-200 hover:text-violet-600"
                >
                  {t("catalog:categoriesPage.clearSearch")}
                </button>
              }
            />
          ) : null}

          {!isLoading && !isError && filteredCategories.length > 0 ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
              {filteredCategories.map((category) => (
                <MarketplaceCategoryCard
                  key={category.uuid}
                  category={category}
                  activeItemCount={popularCategoryCounts.get(category.uuid)}
                />
              ))}
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}

function MarketplaceCategoryCard({
  category,
  activeItemCount,
}: {
  category: CategoryResponse;
  activeItemCount?: number;
}) {
  const { t } = useTranslation("catalog");
  return (
    <Link
      to={buildPathWithQuery(routePaths.marketplace, { categoryUuid: category.uuid })}
      className="group flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-violet-200 hover:shadow-sm"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900 transition group-hover:text-violet-700">{category.name}</h3>
          <p className="mt-1 text-xs font-medium uppercase tracking-[0.12em] text-slate-400">/{category.slug}</p>
        </div>
        {typeof activeItemCount === "number" ? (
          <span className="rounded-full bg-violet-50 px-2.5 py-1 text-xs font-medium text-violet-700">
            {t("categoriesPage.popularNow")}
          </span>
        ) : null}
      </div>

      <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-600">
        {category.description?.trim() || t("categoriesPage.defaultCardDescription")}
      </p>

      <div className="mt-auto flex items-center justify-between gap-3 pt-6 text-sm text-slate-500">
        <span>
          {typeof activeItemCount === "number"
            ? t("marketplace.activeListings", { count: activeItemCount })
            : t("categoriesPage.exploreCategory")}
        </span>
        <span className="inline-flex items-center gap-1 font-medium text-violet-600 transition group-hover:translate-x-0.5">
          {t("categoriesPage.browse")}
          <ArrowRight className="size-4" />
        </span>
      </div>
    </Link>
  );
}

