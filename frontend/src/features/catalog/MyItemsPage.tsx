import { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import { Plus, Package, Archive, LayoutGrid, List, Search } from "lucide-react";
import { useMyItems, useArchiveItem, useCategories } from "./useCatalog";
import { ItemCard } from "./ItemCard";
import { ItemGridSkeleton } from "./ItemCardSkeleton";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import type { ItemStatus, ItemCondition } from "@/api/generated/types.ts";
import type { MyItemsParams } from "@/api/catalogApi.ts";
import { toast } from "sonner";

const STATUS_OPTIONS: { value: ItemStatus | ""; label: string }[] = [
  { value: "", label: "All Statuses" },
  { value: "DRAFT", label: "Draft" },
  { value: "ACTIVE", label: "Active" },
  { value: "RESERVED", label: "Reserved" },
  { value: "ARCHIVED", label: "Archived" },
];

const CONDITION_OPTIONS: { value: ItemCondition | ""; label: string }[] = [
  { value: "", label: "Any Condition" },
  { value: "NEW", label: "New" },
  { value: "LIKE_NEW", label: "Like New" },
  { value: "GOOD", label: "Good" },
  { value: "USED", label: "Used" },
  { value: "FOR_PARTS", label: "For Parts" },
];

type ViewMode = "grid" | "table";

const VIEW_MODE_KEY = "myItems_viewMode";

function getStoredViewMode(): ViewMode {
  try {
    const stored = localStorage.getItem(VIEW_MODE_KEY);
    if (stored === "grid" || stored === "table") return stored;
  } catch {}
  return "grid";
}

export function MyItemsPage() {
  const [params, setParams] = useState<MyItemsParams>({
    page: 0,
    size: 12,
    sort: "createdAt,desc",
  });
  const [archiveUuid, setArchiveUuid] = useState<string | null>(null);
  const [archiveReason, setArchiveReason] = useState("");
  const [viewMode, setViewMode] = useState<ViewMode>(getStoredViewMode);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [conditionFilter, setConditionFilter] = useState<ItemCondition | "">("");
  const [categoryFilter, setCategoryFilter] = useState("");

  const { data, isLoading, isError } = useMyItems(params);
  const { data: categories } = useCategories();
  const archiveMutation = useArchiveItem();

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Persist view mode
  useEffect(() => {
    try {
      localStorage.setItem(VIEW_MODE_KEY, viewMode);
    } catch {}
  }, [viewMode]);

  // Client-side filtering for search, condition, and category
  const filteredItems = useMemo(() => {
    if (!data) return [];
    return data.content.filter((item) => {
      if (debouncedSearch) {
        const q = debouncedSearch.toLowerCase();
        if (!item.title.toLowerCase().includes(q) && !item.categoryName.toLowerCase().includes(q)) {
          return false;
        }
      }
      if (conditionFilter && item.condition !== conditionFilter) return false;
      if (categoryFilter && item.categoryUuid !== categoryFilter) return false;
      return true;
    });
  }, [data, debouncedSearch, conditionFilter, categoryFilter]);

  const hasActiveFilters = !!debouncedSearch || !!conditionFilter || !!categoryFilter || !!params.status;

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as ItemStatus | "";
    setParams((prev) => ({
      ...prev,
      page: 0,
      status: val || undefined,
    }));
  };

  const handleArchive = () => {
    if (!archiveUuid) return;
    archiveMutation.mutate(
      { uuid: archiveUuid, data: archiveReason ? { reason: archiveReason } : undefined },
      {
        onSuccess: () => {
          toast.success("Item archived successfully");
          setArchiveUuid(null);
          setArchiveReason("");
        },
        onError: () => {
          toast.error("Failed to archive item");
        },
      }
    );
  };

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">My Items</h1>
        <Link to="/my-items/new">
          <Button>
            <Plus className="size-4" />
            New Item
          </Button>
        </Link>
      </div>

      {/* Search bar */}
      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search your items..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white pl-10 pr-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
          />
        </div>
      </div>

      {/* Filters row */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <select
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          value={params.status ?? ""}
          onChange={handleStatusChange}
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <select
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          value={conditionFilter}
          onChange={(e) => setConditionFilter(e.target.value as ItemCondition | "")}
        >
          {CONDITION_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <select
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
        >
          <option value="">All Categories</option>
          {categories?.map((cat) => (
            <option key={cat.uuid} value={cat.uuid}>
              {cat.name}
            </option>
          ))}
        </select>

        {/* Spacer */}
        <div className="flex-1" />

        {/* View toggle */}
        <div className="flex items-center rounded-lg border border-slate-300 dark:border-slate-600 overflow-hidden">
          <button
            onClick={() => setViewMode("grid")}
            className={`p-2 transition-colors ${
              viewMode === "grid"
                ? "bg-indigo-50 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-400"
                : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            }`}
            title="Grid view"
          >
            <LayoutGrid className="size-4" />
          </button>
          <button
            onClick={() => setViewMode("table")}
            className={`p-2 transition-colors border-l border-slate-300 dark:border-slate-600 ${
              viewMode === "table"
                ? "bg-indigo-50 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-400"
                : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            }`}
            title="Table view"
          >
            <List className="size-4" />
          </button>
        </div>
      </div>

      {isLoading && <ItemGridSkeleton />}

      {isError && (
        <EmptyState
          title="Failed to load your items"
          description="Something went wrong. Please try again."
          action={
            <Button variant="outline" onClick={() => window.location.reload()}>
              Retry
            </Button>
          }
        />
      )}

      {data && filteredItems.length === 0 && (
        <EmptyState
          icon={<Package className="size-16" />}
          title={hasActiveFilters ? "No items match your filters" : "No items yet"}
          description={
            hasActiveFilters
              ? "Try adjusting your search or filter criteria to find items."
              : "Create your first listing to start trading."
          }
          action={
            hasActiveFilters ? (
              <Button
                variant="outline"
                onClick={() => {
                  setSearchInput("");
                  setConditionFilter("");
                  setCategoryFilter("");
                  setParams((prev) => ({ ...prev, page: 0, status: undefined }));
                }}
              >
                Clear Filters
              </Button>
            ) : (
              <Link to="/my-items/new">
                <Button>
                  <Plus className="size-4" />
                  Create Item
                </Button>
              </Link>
            )
          }
        />
      )}

      {data && filteredItems.length > 0 && viewMode === "grid" && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredItems.map((item) => (
              <div key={item.uuid} className="relative group">
                <ItemCard item={item} linkPrefix="/my-items" />
                <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                  <Link to={`/my-items/${item.uuid}/edit`}>
                    <Button variant="outline" size="sm">
                      Edit
                    </Button>
                  </Link>
                  {item.status !== "ARCHIVED" && item.status !== "REMOVED" && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        setArchiveUuid(item.uuid);
                      }}
                    >
                      <Archive className="size-3" />
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {data && filteredItems.length > 0 && viewMode === "table" && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-700">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-700">
                <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Title</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Category</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Condition</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Status</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Created</th>
                <th className="text-right px-4 py-3 font-medium text-slate-600 dark:text-slate-300">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
              {filteredItems.map((item) => (
                <tr
                  key={item.uuid}
                  className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors"
                >
                  <td className="px-4 py-3">
                    <Link
                      to={`/my-items/${item.uuid}`}
                      className="font-medium text-slate-900 dark:text-slate-100 hover:text-indigo-600 dark:hover:text-indigo-400"
                    >
                      {item.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{item.categoryName}</td>
                  <td className="px-4 py-3"><ItemConditionBadge condition={item.condition} /></td>
                  <td className="px-4 py-3"><ItemStatusBadge status={item.status} /></td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-400">
                    {new Date(item.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Link to={`/my-items/${item.uuid}/edit`}>
                        <Button variant="outline" size="sm">Edit</Button>
                      </Link>
                      {item.status !== "ARCHIVED" && item.status !== "REMOVED" && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setArchiveUuid(item.uuid)}
                        >
                          <Archive className="size-3" />
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-6">
          <Pagination
            currentPage={data.page}
            totalPages={data.totalPages}
            onPageChange={(page) => setParams((prev) => ({ ...prev, page }))}
          />
        </div>
      )}

      {/* Archive confirmation modal */}
      <Modal
        isOpen={!!archiveUuid}
        onClose={() => {
          setArchiveUuid(null);
          setArchiveReason("");
        }}
        title="Archive Item"
        size="sm"
      >
        <p className="text-sm text-slate-600 dark:text-slate-400 mb-4">
          Are you sure you want to archive this item? It will no longer appear in the public marketplace.
        </p>
        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
            Reason (optional)
          </label>
          <textarea
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            rows={3}
            value={archiveReason}
            onChange={(e) => setArchiveReason(e.target.value)}
            placeholder="Why are you archiving this item?"
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button
            variant="outline"
            onClick={() => {
              setArchiveUuid(null);
              setArchiveReason("");
            }}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            isLoading={archiveMutation.isPending}
            onClick={handleArchive}
          >
            Archive
          </Button>
        </div>
      </Modal>
    </div>
  );
}

