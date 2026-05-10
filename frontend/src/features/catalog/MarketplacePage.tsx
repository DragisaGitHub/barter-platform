import { useState, type ChangeEvent } from "react";
import { Search, SlidersHorizontal } from "lucide-react";
import { useSearchItems, useCategories } from "./useCatalog";
import { ItemCard } from "./ItemCard";
import { ItemGridSkeleton } from "./ItemCardSkeleton";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import type { ItemCondition } from "@/api/generated/types.ts";
import type { SearchItemsParams } from "@/api/catalogApi.ts";

const CONDITIONS: { value: ItemCondition; label: string }[] = [
  { value: "NEW", label: "New" },
  { value: "LIKE_NEW", label: "Like New" },
  { value: "GOOD", label: "Good" },
  { value: "USED", label: "Used" },
  { value: "FOR_PARTS", label: "For Parts" },
];

export function MarketplacePage() {
  const [params, setParams] = useState<SearchItemsParams>({
    page: 0,
    size: 12,
    sort: "createdAt,desc",
  });
  const [searchInput, setSearchInput] = useState("");
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading, isError } = useSearchItems(params);
  const { data: categories } = useCategories();

  const handleSearch = () => {
    setParams((prev) => ({ ...prev, page: 0, q: searchInput || undefined }));
  };

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSearch();
  };

  const handleCategoryChange = (e: ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setParams((prev) => ({ ...prev, page: 0, categoryUuid: val || undefined }));
  };

  const handleConditionChange = (e: ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as ItemCondition | "";
    setParams((prev) => ({
      ...prev,
      page: 0,
      condition: val || undefined,
    }));
  };

  const handlePageChange = (page: number) => {
    setParams((prev) => ({ ...prev, page }));
  };

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Marketplace</h1>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowFilters((f) => !f)}
        >
          <SlidersHorizontal className="size-4" />
          Filters
        </Button>
      </div>

      {/* Search bar */}
      <div className="flex gap-2 mb-4">
        <div className="flex-1">
          <Input
            placeholder="Search items..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={handleSearchKeyDown}
          />
        </div>
        <Button onClick={handleSearch}>
          <Search className="size-4" />
          Search
        </Button>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="flex flex-wrap gap-4 mb-6 p-4 rounded-xl border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
          <div className="min-w-[180px]">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
              Category
            </label>
            <select
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              value={params.categoryUuid ?? ""}
              onChange={handleCategoryChange}
            >
              <option value="">All Categories</option>
              {categories?.map((cat) => (
                <option key={cat.uuid} value={cat.uuid}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          <div className="min-w-[180px]">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
              Condition
            </label>
            <select
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              value={params.condition ?? ""}
              onChange={handleConditionChange}
            >
              <option value="">Any Condition</option>
              {CONDITIONS.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {/* Results */}
      {isLoading && <ItemGridSkeleton />}

      {isError && (
        <EmptyState
          title="Failed to load items"
          description="Something went wrong while loading the marketplace. Please try again."
          action={
            <Button variant="outline" onClick={() => window.location.reload()}>
              Retry
            </Button>
          }
        />
      )}

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<Search className="size-16" />}
          title="No items found"
          description="Try adjusting your search or filter criteria."
        />
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {data.content.map((item) => (
              <ItemCard key={item.uuid} item={item} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}

