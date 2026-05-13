import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Clock,
  Heart,
  LogIn,
  Package,
  Search,
  Sparkles,
  User,
  UserPlus,
} from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import type { SearchItemsParams } from "@/api/catalogApi";
import type {
  CategoryResponse,
  ItemSummaryResponse,
} from "@/api/generated/types";
import { buildPathWithQuery, routePaths } from "@/routes/routePaths";
import { parseApiError } from "@/utils";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Spinner } from "../../components/ui/Spinner";
import { getCategoryIcon } from "./categoryIcons";
import {
  useCategories,
  useFavoriteItem,
  useFavoriteItems,
  useSearchItems,
  useUnfavoriteItem,
} from "./useCatalog";
import { toast } from "sonner";

const pageShellClassName = "marketplace-panel";

export function MarketplacePage() {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useState<SearchItemsParams>({
    page: 0,
    size: 20,
    sort: "createdAt,desc",
  });
  const [searchInput, setSearchInput] = useState("");
  const [loadedItems, setLoadedItems] = useState<ItemSummaryResponse[]>([]);
  const [favoriteOverrides, setFavoriteOverrides] = useState<Record<string, boolean>>({});
  const [pendingFavoriteUuid, setPendingFavoriteUuid] = useState<string | null>(null);

  const { data, isLoading, isFetching, isError } = useSearchItems(params);
  const { data: categories } = useCategories();
  const favoriteListParams = useMemo(
    () => ({ page: 0, size: 200, sort: "createdAt,desc" }),
    []
  );
  const { data: favoriteItemsData } = useFavoriteItems(favoriteListParams, isAuthenticated);
  const favoriteItemMutation = useFavoriteItem();
  const unfavoriteItemMutation = useUnfavoriteItem();

  const orderedCategories = useMemo(
    () => (categories ? [...categories].sort((a, b) => a.sortOrder - b.sortOrder) : []),
    [categories]
  );

  const allCategories = useMemo<CategoryResponse[]>(
    () => [
      {
        uuid: "",
        name: "All Categories",
        slug: "all",
        description: null,
        sortOrder: -1,
      },
      ...orderedCategories,
    ],
    [orderedCategories]
  );

  const featuredCategories = useMemo(() => orderedCategories.slice(0, 6), [orderedCategories]);

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

  const filteredItems = useMemo(() => {
    const activeItems = loadedItems.filter((item) => item.status === "ACTIVE");

    if (!user) {
      return activeItems;
    }

    return activeItems.filter((item) => item.ownerUuid !== user.uuid);
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

  const resultsTitle = params.q
    ? `Search results for “${params.q}”`
    : selectedCategory
      ? `${selectedCategory.name} items`
      : "Featured items";

  const resultsMeta = filteredItems.length === 1 ? "1 live listing" : `${filteredItems.length} live listings`;

  const resetResults = () => setLoadedItems([]);

  const handleSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextQuery = searchInput.trim();
    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      q: nextQuery || undefined,
    }));
  };

  const selectCategory = (categoryUuid: string) => {
    resetResults();
    setParams((previous) => ({
      ...previous,
      page: 0,
      categoryUuid: categoryUuid || undefined,
    }));
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
        <div className="mx-auto flex max-w-[1600px] flex-col gap-4 px-4 py-4 sm:px-6 xl:flex-row xl:items-center xl:gap-8">
          <div className="flex items-center justify-between gap-4 xl:shrink-0">
            <Link
              to={routePaths.marketplace}
              className="flex items-center gap-2"
            >
              <div className="flex size-8 items-center justify-center rounded-lg bg-violet-500">
                <span className="text-base font-semibold text-white">⇄</span>
              </div>
              <span className="text-xl font-semibold text-slate-900">Barter Platform</span>
            </Link>

            {isAuthenticated ? (
              <div className="rounded-lg border border-violet-200 bg-violet-50 px-3 py-1.5 text-xs font-medium text-violet-700 xl:hidden">
                {user?.username}
              </div>
            ) : null}
          </div>

          <form onSubmit={handleSearch} className="flex-1 xl:max-w-2xl">
            <div className="relative">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="Search for items to trade..."
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                className="marketplace-search-input h-11 w-full pl-12 pr-4 text-sm text-slate-900 outline-none transition focus:bg-white"
              />
            </div>
            <button type="submit" className="sr-only">
              Search
            </button>
          </form>

          <div className="flex flex-wrap items-center gap-4 text-sm text-slate-700 xl:ml-auto xl:shrink-0 xl:justify-end xl:gap-6">
            <div className="flex flex-wrap items-center gap-4 xl:gap-6">
              <button
                type="button"
                onClick={() => scrollToSection("browse-by-category")}
                className="transition hover:text-violet-600"
              >
                Categories
              </button>
              <button
                type="button"
                onClick={() => scrollToSection("how-it-works")}
                className="transition hover:text-violet-600"
              >
                How it works
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {isAuthenticated ? (
                <>
                  <div className="hidden rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-medium text-violet-700 xl:block">
                    Welcome, {user?.username}
                  </div>
                  <Link
                    to={routePaths.dashboard}
                    className="inline-flex h-10 items-center justify-center rounded-lg border border-slate-200 bg-white px-4 text-sm font-medium text-slate-700 transition hover:border-violet-200 hover:text-violet-600"
                  >
                    Dashboard
                  </Link>
                </>
              ) : (
                <>
                  <Link
                    to={routePaths.login}
                    className="inline-flex h-10 items-center justify-center gap-2 px-4 text-sm font-medium text-slate-700 transition hover:text-violet-600"
                  >
                    <LogIn className="size-4" />
                    Login
                  </Link>
                  <Link
                    to={routePaths.register}
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-violet-500 px-5 text-sm font-medium text-white transition hover:bg-violet-600"
                  >
                    <UserPlus className="size-4" />
                    Sign up
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-[1600px] flex-col gap-6 px-4 py-6 sm:px-6 xl:flex-row xl:items-start">
        <aside className="hidden w-64 shrink-0 space-y-4 xl:sticky xl:top-[89px] xl:block">
          <div className={`${pageShellClassName} p-4`}>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-medium text-slate-900">Categories</h2>
              <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-500">
                {orderedCategories.length}
              </span>
            </div>

            <div className="space-y-1">
              {allCategories.map((category) => {
                const Icon = getCategoryIcon(category.slug);
                const isSelected = (params.categoryUuid ?? "") === category.uuid;

                return (
                  <button
                    key={category.uuid || "all"}
                    type="button"
                    onClick={() => selectCategory(category.uuid)}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                      isSelected
                        ? "bg-violet-50 text-violet-700"
                        : "text-slate-700 hover:bg-slate-100"
                    }`}
                  >
                    <Icon className={`size-4.5 shrink-0 ${isSelected ? "text-violet-600" : "text-slate-500"}`} />
                    <span className="min-w-0 flex-1 truncate text-sm font-medium">{category.name}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {!isAuthenticated ? (
            <div className="rounded-lg border border-violet-200 bg-violet-50/70 p-4">
              <div className="mb-3 flex size-10 items-center justify-center rounded-lg bg-violet-500 text-white">
                <Sparkles className="size-4.5" />
              </div>
              <h3 className="text-base font-medium text-slate-900">Join our community</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Create an account to list your own items, make offers, and trade with the community.
              </p>
              <Link
                to={routePaths.register}
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-violet-500 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-violet-600"
              >
                Sign up free
                <ArrowRight className="size-4" />
              </Link>
            </div>
          ) : null}

          <div id="how-it-works" className={`${pageShellClassName} p-4`}>
            <h3 className="text-base font-medium text-slate-900">How it works</h3>
            <div className="mt-3 space-y-3">
              {[
                "List items you want to trade",
                "Browse and search community listings",
                "Connect directly and arrange the exchange",
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
          <section id="browse-by-category" className={`${pageShellClassName} p-4`}>
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-lg font-medium text-slate-900">Browse by category</h2>
                <p className="text-sm text-slate-500">Explore the most active sections of the marketplace.</p>
              </div>
              {selectedCategory ? (
                <button
                  type="button"
                  onClick={() => selectCategory("")}
                  className="inline-flex items-center gap-2 self-start rounded-lg bg-violet-50 px-3 py-2 text-xs font-medium text-violet-600 transition hover:bg-violet-100 sm:self-auto"
                >
                  Clear filter
                </button>
              ) : null}
            </div>

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-6">
              {featuredCategories.map((category) => {
                const Icon = getCategoryIcon(category.slug);
                const isSelected = params.categoryUuid === category.uuid;

                return (
                  <button
                    key={category.uuid}
                    type="button"
                    onClick={() => selectCategory(category.uuid)}
                    className={`group flex min-h-[108px] flex-col items-center justify-center gap-2 rounded-lg border px-3 py-3 text-center transition-colors ${
                      isSelected
                        ? "border-violet-200 bg-violet-50 text-violet-700"
                        : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    <div
                      className={`flex size-10 items-center justify-center rounded-full transition-colors ${
                        isSelected
                          ? "bg-violet-100 text-violet-600"
                          : "bg-violet-50 text-violet-600 group-hover:bg-violet-100"
                      }`}
                    >
                      <Icon className="size-5" />
                    </div>
                    <span className="text-xs font-medium leading-4 text-slate-700 group-hover:text-violet-700">
                      {category.name}
                    </span>
                  </button>
                );
              })}
            </div>
          </section>

          <section className={`${pageShellClassName} p-4`}>
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-lg font-medium text-slate-900">{resultsTitle}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  {resultsMeta}
                  {params.categoryUuid && selectedCategory ? ` in ${selectedCategory.name}` : " from the community"}
                  {params.q ? ` matching “${params.q}”` : ""}
                </p>
              </div>

              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
                {data?.last ? "Latest listings loaded" : "Fresh listings updated regularly"}
              </div>
            </div>

            {isLoading && filteredItems.length === 0 ? (
              <div className="flex justify-center py-20">
                <Spinner size="lg" />
              </div>
            ) : null}

            {isError ? (
              <EmptyState
                title="Failed to load items"
                description="Something went wrong while loading the marketplace. Please try again."
                action={
                  <Button variant="outline" onClick={() => window.location.reload()}>
                    Retry
                  </Button>
                }
              />
            ) : null}

            {!isError && data && filteredItems.length === 0 ? (
              <EmptyState
                icon={<Package className="size-16" />}
                title="No tradeable items found"
                description="There are no active items from other users right now. Try another search or choose a different category."
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
                      Load more items
                    </Button>
                  </div>
                ) : null}
              </>
            ) : null}
          </section>
        </main>
      </div>
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
  const timeAgo = useMemo(() => {
    try {
      return formatDistanceToNow(new Date(item.createdAt), { addSuffix: true });
    } catch {
      return "recently";
    }
  }, [item.createdAt]);

  return (
    <div className="group relative">
      <button
        type="button"
        onClick={() => onToggleFavorite(item.uuid)}
        disabled={isFavoritePending}
        className={`absolute right-2.5 top-2.5 z-10 inline-flex size-9 items-center justify-center rounded-full border border-white/80 bg-white/95 shadow-sm backdrop-blur transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-60 ${
          isFavorite ? "text-rose-500" : "text-slate-500 hover:text-rose-500"
        }`}
        aria-label={isFavorite ? "Remove from favorites" : "Add to favorites"}
      >
        <Heart className={`size-4.5 ${isFavorite ? "fill-current" : ""}`} />
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
            {item.status}
          </div>
        </div>

        <div className="flex flex-1 flex-col p-3">
          <div className="mb-2 flex flex-wrap items-center gap-1.5">
            <span className="marketplace-soft-badge inline-flex max-w-full items-center bg-violet-50 px-2 py-0.5 text-[10px] font-medium text-violet-700">
              <span className="truncate">{item.categoryName}</span>
            </span>
            <span className="marketplace-soft-badge inline-flex items-center bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">
              {formatEnumLabel(item.condition)}
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
                <span>{timeAgo}</span>
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

function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

