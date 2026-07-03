import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  Bookmark,
  Clock,
  Heart,
  LogIn,
  MapPin,
  Package,
  Search,
  Sparkles,
  User,
  UserPlus,
  X,
} from "lucide-react";
import type { SearchItemsParams, SchemaFieldFilterValues } from "@/api/catalogApi";
import type {
  CategoryResponse,
  ItemSummaryResponse,
  SavedSearchCriteria,
} from "@/api/generated/types";
import { buildPathWithQuery, routePaths } from "@/routes/routePaths";
import { parseApiError } from "@/utils";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Spinner } from "../../components/ui/Spinner";
import {
  useCategories,
  useFavoriteItem,
  useFavoriteItems,
  usePopularCategories,
  useSearchItems,
  useUnfavoriteItem,
} from "./useCatalog";
import { CategoryFiltersPanel } from "./CategoryFiltersPanel";
import { SavedSearchesPanel } from "./SavedSearchesPanel";
import { RecommendationsSection } from "./RecommendationsSection";
import { MarketplaceUserMenu } from "./MarketplaceUserMenu";
import { useCreateSavedSearch } from "./useSavedSearches";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { markMarketplaceVisited } from "@/features/onboarding/onboardingState";

const pageShellClassName = "marketplace-panel";

type SidebarCategoryEntry = CategoryResponse & {
  activeItemCount?: number;
};

export function MarketplacePage() {
  const { user, isAuthenticated } = useAuth();
  const { t } = useTranslation(["catalog", "common", "navigation"]);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryUuidFromUrl = searchParams.get("categoryUuid") ?? undefined;
  const [params, setParams] = useState<SearchItemsParams>(() => ({
    page: 0,
    size: 20,
    sort: searchParams.get("sort") ?? "createdAt,desc",
    q: searchParams.get("q")?.trim() || undefined,
    categoryUuid: categoryUuidFromUrl,
    tagUuids: searchParams.getAll("tagUuids").filter(Boolean),
    condition: (searchParams.get("condition") as SearchItemsParams["condition"]) ?? undefined,
    location: searchParams.get("location")?.trim() || undefined,
  }));
  const [searchInput, setSearchInput] = useState(searchParams.get("q") ?? "");
  const [locationInput, setLocationInput] = useState(searchParams.get("location") ?? "");
  const [loadedItems, setLoadedItems] = useState<ItemSummaryResponse[]>([]);
  const [favoriteOverrides, setFavoriteOverrides] = useState<Record<string, boolean>>({});
  const [pendingFavoriteUuid, setPendingFavoriteUuid] = useState<string | null>(null);
  const [isSaveSearchOpen, setIsSaveSearchOpen] = useState(false);
  const [savedSearchName, setSavedSearchName] = useState("");
  const resetResults = () => setLoadedItems([]);

  const { data, isLoading, isFetching, isError } = useSearchItems(params);
  const { data: categories } = useCategories();
  const { data: popularCategoriesData, isLoading: isPopularCategoriesLoading } = usePopularCategories({ limit: 10 });
  const favoriteListParams = useMemo(
    () => ({ page: 0, size: 100, sort: "createdAt,desc" }),
    []
  );
  const { data: favoriteItemsData } = useFavoriteItems(favoriteListParams, isAuthenticated);
  const favoriteItemMutation = useFavoriteItem();
  const unfavoriteItemMutation = useUnfavoriteItem();
  const createSavedSearchMutation = useCreateSavedSearch();

  const orderedCategories = useMemo(
    () =>
      categories
        ? [...categories].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name))
        : [],
    [categories]
  );

  const popularCategories = useMemo(() => popularCategoriesData ?? [], [popularCategoriesData]);

  const featuredPopularCategories = useMemo(
    () => popularCategories.slice(0, 6),
    [popularCategories]
  );

  useEffect(() => {
    if (!data) {
      return;
    }

    setLoadedItems((previousItems) =>
      data.page === 0
        ? data.content
        : dedupeItemsByUuid([...previousItems, ...data.content])
    );
  }, [data]);

  useEffect(() => {
    if (!isAuthenticated) {
      setFavoriteOverrides({});
      setPendingFavoriteUuid(null);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      markMarketplaceVisited(user?.uuid);
    }
  }, [isAuthenticated, user?.uuid]);

  useEffect(() => {
    if (categoryUuidFromUrl === (params.categoryUuid ?? undefined)) {
      return;
    }

    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      categoryUuid: categoryUuidFromUrl,
      fieldFilters: undefined,
    }));
  }, [categoryUuidFromUrl, params.categoryUuid]);


  const filteredItems = useMemo(() => {
    if (!user) {
      return loadedItems;
    }

    return loadedItems.filter((item) => item.ownerUuid !== user.uuid);
  }, [loadedItems, user]);

  const favoriteItemUuids = useMemo(() => {
    const uuids = new Set(favoriteItemsData?.content.map((item) => item.uuid) ?? []);

    Object.entries(favoriteOverrides).forEach(([itemUuid, isFavorite]) => {
      if (isFavorite) {
        uuids.add(itemUuid);
      } else {
        uuids.delete(itemUuid);
      }
    });

    return uuids;
  }, [favoriteItemsData?.content, favoriteOverrides]);

  const selectedCategory = useMemo(
    () => orderedCategories.find((category) => category.uuid === params.categoryUuid),
    [orderedCategories, params.categoryUuid]
  );

  const sidebarCategories = useMemo<SidebarCategoryEntry[]>(() => {
    const topPopularCategories = popularCategories.slice(0, 10).map((category) => ({
      uuid: category.uuid,
      name: category.name,
      slug: category.slug,
      description: category.description,
      sortOrder: category.sortOrder,
      activeItemCount: category.activeItemCount,
    }));

    if (!selectedCategory || topPopularCategories.some((category) => category.uuid === selectedCategory.uuid)) {
      return topPopularCategories;
    }

    return [{ ...selectedCategory }, ...topPopularCategories].slice(0, 10);
  }, [popularCategories, selectedCategory]);

  const resultsTitle = params.q
    ? t("catalog:marketplace.results.searchResults", { query: params.q })
    : selectedCategory
      ? t("catalog:marketplace.results.categoryItems", { category: selectedCategory.name })
      : t("catalog:featuredItems");

  const resultsMeta = t("catalog:marketplace.results.liveListings", { count: filteredItems.length });

  const canSaveCurrentSearch = hasSearchCriteria(params);


  const handleSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextQuery = searchInput.trim();
    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      q: nextQuery || undefined,
    }));
    updateMarketplaceSearchParams({ ...params, page: 0, q: nextQuery || undefined }, setSearchParams);
  };

  const clearSearch = () => {
    setSearchInput("");
    resetResults();
    setParams((previous) => {
      const nextParams = { ...previous, page: 0, q: undefined };
      updateMarketplaceSearchParams(nextParams, setSearchParams);
      return nextParams;
    });
  };

  const selectCategory = (categoryUuid: string) => {
    const nextCategoryUuid = categoryUuid || undefined;

    if ((params.categoryUuid ?? undefined) === nextCategoryUuid) {
      return;
    }

    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      categoryUuid: nextCategoryUuid,
      fieldFilters: undefined,
    }));

    if (categoryUuidFromUrl !== nextCategoryUuid) {
      setSearchParams((current) => {
        const next = new URLSearchParams(current);

        if (nextCategoryUuid) {
          next.set("categoryUuid", nextCategoryUuid);
        } else {
          next.delete("categoryUuid");
        }

        return next;
      });
    }
  };

  const handleApplyFieldFilters = (fieldFilters: SchemaFieldFilterValues) => {
    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      fieldFilters: Object.keys(fieldFilters).length > 0 ? fieldFilters : undefined,
    }));
  };


  const handleLocationSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextLocation = locationInput.trim();
    resetResults();
    setParams((previous) => {
      const nextParams = { ...previous, page: 0, location: nextLocation || undefined };
      updateMarketplaceSearchParams(nextParams, setSearchParams);
      return nextParams;
    });
  };

  const clearLocation = () => {
    setLocationInput("");
    resetResults();
    setParams((previous) => {
      const nextParams = { ...previous, page: 0, location: undefined };
      updateMarketplaceSearchParams(nextParams, setSearchParams);
      return nextParams;
    });
  };

  const openSaveSearch = () => {
    if (!isAuthenticated) {
      navigate(buildPathWithQuery(routePaths.login, { redirect: routePaths.marketplace }));
      return;
    }

    setSavedSearchName(defaultSavedSearchName(params, selectedCategory?.name));
    setIsSaveSearchOpen(true);
  };

  const handleSaveSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    createSavedSearchMutation.mutate(
      {
        name: savedSearchName,
        criteria: buildSavedSearchCriteria(params),
      },
      {
        onSuccess: () => {
          setIsSaveSearchOpen(false);
          toast.success(t("catalog:savedSearches.saveSuccess"));
        },
        onError: (error) => toast.error(parseApiError(error)),
      }
    );
  };

  const applySavedSearch = (criteria: SavedSearchCriteria) => {
    const nextParams: SearchItemsParams = {
      page: 0,
      size: 20,
      sort: criteria.sort ?? "createdAt,desc",
      q: criteria.q || undefined,
      categoryUuid: criteria.categoryUuid,
      tagUuids: criteria.tagUuids && criteria.tagUuids.length > 0 ? criteria.tagUuids : undefined,
      condition: criteria.condition,
      location: criteria.location || undefined,
    };

    resetResults();
    setSearchInput(criteria.q ?? "");
    setLocationInput(criteria.location ?? "");
    setParams(nextParams);
    updateMarketplaceSearchParams(nextParams, setSearchParams);
    document.getElementById("marketplace-results")?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const loadMore = () => {
    if (!data || data.last || isFetching) {
      return;
    }

    setParams((previous) => ({ ...previous, page: (previous.page ?? 0) + 1 }));
  };

  const scrollToSection = (sectionId: string) => {
    document.getElementById(sectionId)?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  const handleToggleFavorite = (itemUuid: string) => {
    if (!isAuthenticated) {
      navigate(
        buildPathWithQuery(routePaths.login, {
          redirect: routePaths.marketplace,
        })
      );
      return;
    }

    const isCurrentlyFavorite = favoriteItemUuids.has(itemUuid);
    const mutation = isCurrentlyFavorite ? unfavoriteItemMutation : favoriteItemMutation;

    setPendingFavoriteUuid(itemUuid);
    mutation.mutate(itemUuid, {
      onSuccess: () => {
        setFavoriteOverrides((previous) => ({
          ...previous,
          [itemUuid]: !isCurrentlyFavorite,
        }));
      },
      onError: (error) => {
        toast.error(parseApiError(error));
      },
      onSettled: () => {
        setPendingFavoriteUuid((current) => (current === itemUuid ? null : current));
      },
    });
  };

  return (
    <div className="marketplace-page min-h-screen text-slate-900">
      <header className="marketplace-nav sticky top-0 z-40 backdrop-blur-sm">
        <div className="mx-auto flex max-w-400 flex-col gap-4 px-4 py-4 sm:px-6 xl:flex-row xl:items-center xl:gap-8">
          <div className="flex items-center justify-between gap-4 xl:shrink-0">
            <Link
              to={routePaths.marketplace}
              className="flex items-center gap-2"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-violet-500">
                <span className="text-base font-semibold text-white">⇄</span>
              </div>
              <span className="text-xl font-semibold text-slate-900">{t("common:appName")}</span>
            </Link>

            {isAuthenticated ? <MarketplaceUserMenu username={user?.username} compact align="start" className="xl:hidden" /> : null}
          </div>

          <form onSubmit={handleSearch} className="flex-1 xl:max-w-2xl">
            <div className="relative">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder={t("catalog:searchPlaceholder")}
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Escape" && searchInput) {
                    event.preventDefault();
                    clearSearch();
                  }
                }}
                className="marketplace-search-input h-11 w-full pl-12 pr-12 text-sm text-slate-900 outline-none transition focus:bg-white"
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
            <button type="submit" className="sr-only">
              {t("common:search")}
            </button>
          </form>

          <div className="flex flex-wrap items-center gap-4 text-sm text-slate-700 xl:ml-auto xl:shrink-0 xl:justify-end xl:gap-6">
            <div className="flex flex-wrap items-center gap-4 xl:gap-6">
              <button
                type="button"
                onClick={() => scrollToSection("popular-categories")}
                className="transition hover:text-violet-600"
              >
                {t("catalog:categories")}
              </button>
              <button
                type="button"
                onClick={() => scrollToSection("how-it-works")}
                className="transition hover:text-violet-600"
              >
                {t("catalog:howItWorks")}
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-2">
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
        </div>
      </header>

      <div className="mx-auto flex max-w-400 flex-col gap-6 px-4 py-6 sm:px-6 xl:flex-row xl:items-start">
        <aside className="hidden w-64 shrink-0 space-y-4 xl:sticky xl:top-22.25 xl:block">
          <div className={`${pageShellClassName} p-4`}>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-medium text-slate-900">{t("catalog:categories")}</h2>
              <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-500">
                {orderedCategories.length}
              </span>
            </div>

            <p className="mb-3 text-xs leading-5 text-slate-500">
              {t("catalog:marketplace.categoriesHelper")}
            </p>

            <div className="space-y-1.5">
              <button
                type="button"
                onClick={() => selectCategory("")}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                  !params.categoryUuid
                    ? "bg-violet-50 text-violet-700"
                    : "text-slate-700 hover:bg-slate-100"
                }`}
              >
                <span className="min-w-0 flex-1 truncate text-sm font-medium">{t("catalog:allCategories")}</span>
              </button>

              {sidebarCategories.map((category) => {
                const isSelected = params.categoryUuid === category.uuid;

                return (
                  <button
                    key={category.uuid}
                    type="button"
                    onClick={() => selectCategory(category.uuid)}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                      isSelected
                        ? "bg-violet-50 text-violet-700"
                        : "text-slate-700 hover:bg-slate-100"
                    }`}
                  >
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium">{category.name}</span>
                      {typeof category.activeItemCount === "number" ? (
                        <span className="text-xs text-slate-500">
                          {t("catalog:marketplace.activeListings", { count: category.activeItemCount })}
                        </span>
                      ) : null}
                    </span>
                  </button>
                );
              })}
            </div>

            <Link
              to={routePaths.marketplaceCategories}
              className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:border-violet-200 hover:text-violet-600"
            >
              {t("catalog:showAllCategories")}
              <ArrowRight className="size-4" />
            </Link>
          </div>

          {params.categoryUuid ? (
            <CategoryFiltersPanel
              categoryUuid={params.categoryUuid}
              values={params.fieldFilters ?? {}}
              onApply={handleApplyFieldFilters}
              className={`${pageShellClassName} p-4`}
            />
          ) : null}


          <div className={`${pageShellClassName} p-4`}>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-medium text-slate-900">{t("catalog:marketplace.location.title")}</h2>
              {params.location ? (
                <button
                  type="button"
                  onClick={clearLocation}
                  className="text-xs font-medium text-violet-600 transition hover:text-violet-800"
                >
                  {t("catalog:clear")}
                </button>
              ) : null}
            </div>
            <p className="mb-3 text-xs leading-5 text-slate-500">
              {t("catalog:marketplace.location.helper")}
            </p>
            <form onSubmit={handleLocationSubmit} className="space-y-2">
              <div className="relative">
                <MapPin className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="search"
                  value={locationInput}
                  onChange={(event) => setLocationInput(event.target.value)}
                  placeholder={t("catalog:marketplace.location.placeholder")}
                  className="h-10 w-full rounded-lg border border-slate-200 bg-white pl-9 pr-3 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
                />
              </div>
              <Button
                type="submit"
                variant="outline"
                className="h-9 w-full rounded-lg border-slate-200 bg-white text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700"
              >
                {t("catalog:marketplace.location.apply")}
              </Button>
            </form>
          </div>

          {isAuthenticated ? (
            <div className={`${pageShellClassName} p-4`}>
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-base font-medium text-slate-900">{t("catalog:savedSearches.sidebarTitle")}</h2>
                <Link
                  to={routePaths.savedSearches}
                  className="text-xs font-medium text-violet-600 transition hover:text-violet-800"
                >
                  {t("catalog:savedSearches.viewAll")}
                </Link>
              </div>
              <SavedSearchesPanel compact onApply={applySavedSearch} />
            </div>
          ) : null}

          {!isAuthenticated ? (
            <div className="rounded-lg border border-violet-200 bg-violet-50/70 p-4">
              <div className="mb-3 flex size-10 items-center justify-center rounded-lg bg-violet-500 text-white">
                <Sparkles className="size-4.5" />
              </div>
              <h3 className="text-base font-medium text-slate-900">{t("catalog:joinCommunity")}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {t("catalog:joinCommunityBody")}
              </p>
              <Link
                to={routePaths.register}
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-violet-500 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-violet-600"
              >
                {t("catalog:signUpFree")}
                <ArrowRight className="size-4" />
              </Link>
            </div>
          ) : null}

          <div id="how-it-works" className={`${pageShellClassName} p-4`}>
            <h3 className="text-base font-medium text-slate-900">{t("catalog:howItWorks")}</h3>
            <div className="mt-3 space-y-3">
              {[
                t("catalog:howItWorksStep1"),
                t("catalog:howItWorksStep2"),
                t("catalog:howItWorksStep3"),
              ].map((step, index) => (
                <div key={step} className="flex gap-3">
                  <div className="flex size-6 shrink-0 items-center justify-center rounded-full bg-violet-50 text-sm font-medium text-violet-600">
                    {index + 1}
                  </div>
                  <p className="text-sm leading-6 text-slate-600">{step}</p>
                </div>
              ))}
            </div>
          </div>
        </aside>

        <main className="min-w-0 flex-1 space-y-6">
          <section id="popular-categories" className={`${pageShellClassName} p-4`}>
            <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div className="max-w-2xl">
                <h2 className="text-lg font-medium text-slate-900">{t("catalog:popularCategories")}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  {t("catalog:marketplace.popularCategoriesDescription")}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                {selectedCategory ? (
                  <button
                    type="button"
                    onClick={() => selectCategory("")}
                    className="inline-flex items-center gap-2 rounded-lg bg-violet-50 px-3 py-2 text-xs font-medium text-violet-600 transition hover:bg-violet-100"
                  >
                    {t("catalog:clearFilter")}
                  </button>
                ) : null}
                <Link
                  to={routePaths.marketplaceCategories}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-violet-200 hover:text-violet-600"
                >
                  {t("catalog:viewAllCategories")}
                  <ArrowRight className="size-4" />
                </Link>
              </div>
            </div>

            {isPopularCategoriesLoading ? (
              <div className="mt-5 flex justify-center py-8">
                <Spinner />
              </div>
            ) : null}

            {!isPopularCategoriesLoading && featuredPopularCategories.length === 0 ? (
              <div className="mt-5 rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
                {t("catalog:marketplace.noPopularCategories")}
              </div>
            ) : null}

            {!isPopularCategoriesLoading && featuredPopularCategories.length > 0 ? (
              <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {featuredPopularCategories.map((category) => {
                  const isSelected = params.categoryUuid === category.uuid;

                  return (
                    <button
                      key={category.uuid}
                      type="button"
                      onClick={() => selectCategory(category.uuid)}
                      className={`group flex items-start justify-between gap-3 rounded-xl border px-4 py-3 text-left transition-colors ${
                        isSelected
                          ? "border-violet-200 bg-violet-50 text-violet-700"
                          : "border-slate-200 bg-white text-slate-700 hover:border-violet-200 hover:bg-slate-50"
                      }`}
                    >
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-slate-900 transition group-hover:text-violet-700">
                          {category.name}
                        </div>
                        <div className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
                          {category.description?.trim() || t("catalog:marketplace.defaultCategoryDescription")}
                        </div>
                      </div>

                      <div className="shrink-0 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                        {t("catalog:marketplace.activeCount", { count: category.activeItemCount })}
                      </div>
                    </button>
                  );
                })}
              </div>
            ) : null}
          </section>

          <RecommendationsSection
            size={5}
            className={`${pageShellClassName} p-4`}
            titleClassName="text-lg font-medium text-slate-900"
            gridClassName="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5"
            showViewAllLink={false}
          />

          <section id="marketplace-results" className={`${pageShellClassName} p-4`}>
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-lg font-medium text-slate-900">{resultsTitle}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  {resultsMeta}
                  {params.categoryUuid && selectedCategory ? t("catalog:marketplace.results.inCategory", { category: selectedCategory.name }) : t("catalog:marketplace.results.fromCommunity")}
                  {params.q ? t("catalog:marketplace.results.matchingQuery", { query: params.q }) : ""}
                  {params.location ? t("catalog:marketplace.results.nearLocation", { location: params.location }) : ""}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={openSaveSearch}
                  disabled={!canSaveCurrentSearch}
                  className="h-10 rounded-lg border-slate-200 bg-white px-3 text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Bookmark className="mr-2 size-4" />
                  {t("catalog:savedSearches.saveCurrent")}
                </Button>
                <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
                  {data?.last ? t("catalog:latestListingsLoaded") : t("catalog:freshListings")}
                </div>
              </div>
            </div>

            {isLoading && filteredItems.length === 0 ? (
              <div className="flex justify-center py-20">
                <Spinner size="lg" />
              </div>
            ) : null}

            {isError ? (
              <EmptyState
                title={t("catalog:failedToLoadItems")}
                description={t("catalog:failedToLoadItemsBody")}
                action={
                  <Button variant="outline" onClick={() => window.location.reload()}>
                    {t("common:tryAgain")}
                  </Button>
                }
              />
            ) : null}

            {!isError && data && filteredItems.length === 0 ? (
              <EmptyState
                icon={<Package className="size-16" />}
                title={t("catalog:noTradeableItems")}
                description={t("catalog:noTradeableItemsBody")}
              />
            ) : null}

            {!isError && filteredItems.length > 0 ? (
              <>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
                  {filteredItems.map((item) => (
                    <MarketplaceItemCard
                      key={item.uuid}
                      item={item}
                      isFavorite={favoriteItemUuids.has(item.uuid)}
                      isFavoritePending={pendingFavoriteUuid === item.uuid}
                      onToggleFavorite={handleToggleFavorite}
                    />
                  ))}
                </div>

                {!data?.last ? (
                  <div className="mt-5 flex justify-center">
                    <Button
                      variant="outline"
                      onClick={loadMore}
                      isLoading={isFetching && (params.page ?? 0) > 0}
                      className="h-10 rounded-lg border-slate-200 bg-white px-6 text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700"
                    >
                      {t("catalog:loadMoreItems")}
                    </Button>
                  </div>
                ) : null}
              </>
            ) : null}
          </section>
        </main>
      </div>

      <Modal
        isOpen={isSaveSearchOpen}
        onClose={() => setIsSaveSearchOpen(false)}
        title={t("catalog:savedSearches.saveDialogTitle")}
      >
        <form onSubmit={handleSaveSearch} className="space-y-4">
          <div>
            <label htmlFor="saved-search-name" className="text-sm font-medium text-slate-700">
              {t("catalog:savedSearches.nameLabel")}
            </label>
            <input
              id="saved-search-name"
              value={savedSearchName}
              onChange={(event) => setSavedSearchName(event.target.value)}
              maxLength={120}
              className="mt-2 h-10 w-full rounded-lg border border-slate-200 px-3 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
              placeholder={t("catalog:savedSearches.namePlaceholder")}
              required
            />
          </div>
          <p className="rounded-lg bg-slate-50 px-3 py-2 text-xs leading-5 text-slate-500">
            {formatSavedSearchCriteriaPreview(buildSavedSearchCriteria(params), t)}
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setIsSaveSearchOpen(false)}>
              {t("common:cancel")}
            </Button>
            <Button type="submit" isLoading={createSavedSearchMutation.isPending}>
              {t("catalog:savedSearches.save")}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function MarketplaceItemCard({
  item,
  isFavorite,
  isFavoritePending,
  onToggleFavorite,
}: {
  item: ItemSummaryResponse;
  isFavorite: boolean;
  isFavoritePending: boolean;
  onToggleFavorite: (itemUuid: string) => void;
}) {
  const { t, i18n } = useTranslation("catalog");
  const approximateLocation = formatApproximateExchangeLocation(item);
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
        onClick={() => onToggleFavorite(item.uuid)}
        disabled={isFavoritePending}
        className={`absolute right-2.5 top-2.5 z-10 inline-flex size-9 items-center justify-center rounded-full border border-white/80 bg-white/95 shadow-sm backdrop-blur transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-60 ${
          isFavorite ? "text-rose-500" : "text-slate-500 hover:text-rose-500"
        }`}
        aria-label={isFavorite ? t("removeFromFavorites") : t("addToFavorites")}
      >
        <Heart className={`size-4.5 ${isFavorite ? "fill-current" : ""}`} />
      </button>

      <Link
        to={routePaths.marketplaceItem(item.uuid)}
        className="marketplace-item-card flex h-full flex-col overflow-hidden transition-colors duration-200"
      >
        <div className="relative aspect-4/3 overflow-hidden bg-slate-100">
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

          <h3 className="line-clamp-2 min-h-10 text-sm font-medium leading-5 text-slate-900 transition-colors group-hover:text-violet-600">
            {item.title}
          </h3>

          {approximateLocation ? (
            <div className="mt-2 flex items-center gap-1.5 text-xs text-slate-500">
              <MapPin className="size-3.5 shrink-0 text-slate-400" />
              <span className="truncate">{approximateLocation}</span>
            </div>
          ) : null}

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

function dedupeItemsByUuid(items: ItemSummaryResponse[]) {
  return Array.from(new Map(items.map((item) => [item.uuid, item])).values());
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

function hasSearchCriteria(params: SearchItemsParams) {
  return Boolean(params.q || params.categoryUuid || params.tagUuids?.length || params.condition || params.location);
}

function buildSavedSearchCriteria(params: SearchItemsParams): SavedSearchCriteria {
  return {
    q: params.q,
    categoryUuid: params.categoryUuid,
    tagUuids: params.tagUuids,
    condition: params.condition,
    location: params.location,
    sort: params.sort,
  };
}

function updateMarketplaceSearchParams(
  params: SearchItemsParams,
  setSearchParams: ReturnType<typeof useSearchParams>[1]
) {
  setSearchParams(() => {
    const next = new URLSearchParams();
    if (params.q) next.set("q", params.q);
    if (params.categoryUuid) next.set("categoryUuid", params.categoryUuid);
    params.tagUuids?.forEach((tagUuid) => next.append("tagUuids", tagUuid));
    if (params.condition) next.set("condition", params.condition);
    if (params.location) next.set("location", params.location);
    if (params.sort && params.sort !== "createdAt,desc") next.set("sort", params.sort);
    return next;
  });
}

function defaultSavedSearchName(params: SearchItemsParams, categoryName?: string) {
  if (params.q) {
    return params.q;
  }
  if (categoryName) {
    return categoryName;
  }
  if (params.location) {
    return params.location;
  }
  return "Marketplace search";
}

function formatSavedSearchCriteriaPreview(
  criteria: SavedSearchCriteria,
  t: (key: string, options?: Record<string, unknown>) => string
) {
  const parts: string[] = [];
  if (criteria.q) parts.push(t("catalog:savedSearches.criteria.query", { query: criteria.q }));
  if (criteria.categoryUuid) parts.push(t("catalog:savedSearches.criteria.category"));
  if (criteria.tagUuids?.length) parts.push(t("catalog:savedSearches.criteria.tags", { count: criteria.tagUuids.length }));
  if (criteria.condition) parts.push(t("catalog:savedSearches.criteria.condition", { condition: criteria.condition }));
  if (criteria.location) parts.push(t("catalog:savedSearches.criteria.location", { location: criteria.location }));
  return parts.length ? parts.join(" · ") : t("catalog:savedSearches.criteria.catalogFilters");
}

function formatApproximateExchangeLocation(item: ItemSummaryResponse) {
  const locality = [item.exchangeArea, item.exchangeCity].filter(Boolean).join(", ");
  return [locality, item.exchangeLocation].filter(Boolean).join(" · ");
}

