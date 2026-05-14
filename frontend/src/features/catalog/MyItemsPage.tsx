import { useState, useEffect, useMemo, type ChangeEvent } from "react";
import { Link } from "react-router-dom";
import {
  Plus,
  Package,
  Archive,
  LayoutGrid,
  List,
  Search,
  Edit3,
  RefreshCw,
  Layers3,
  BadgeCheck,
  FolderArchive,
  Filter,
} from "lucide-react";
import { useMyItems, useArchiveItem, useCategories } from "./useCatalog";
import { ItemCard } from "./ItemCard";
import { ItemGridSkeleton } from "./ItemCardSkeleton";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import { Card } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { cn } from "@/utils";
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

function SummaryCard({
  title,
  value,
  helper,
  icon: Icon,
  iconClassName,
  isLoading,
}: {
  title: string;
  value?: number;
  helper: string;
  icon: typeof Package;
  iconClassName: string;
  isLoading: boolean;
}) {
  return (
    <Card className="rounded-2xl border-slate-200/90 p-4 shadow-sm dark:border-slate-700/90">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
            {title}
          </p>
          <div className="mt-3 min-h-9 text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">
            {isLoading ? <span className="text-sm font-medium text-slate-400 dark:text-slate-500">Loading…</span> : value ?? 0}
          </div>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{helper}</p>
        </div>
        <div className={cn("flex size-11 items-center justify-center rounded-2xl", iconClassName)}>
          <Icon className="size-5" />
        </div>
      </div>
    </Card>
  );
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
  const { data: totalData, isLoading: totalLoading } = useMyItems({ page: 0, size: 1, sort: "createdAt,desc" });
  const { data: activeData, isLoading: activeLoading } = useMyItems({
    page: 0,
    size: 1,
    sort: "createdAt,desc",
    status: "ACTIVE",
  });
  const { data: draftData, isLoading: draftLoading } = useMyItems({
    page: 0,
    size: 1,
    sort: "createdAt,desc",
    status: "DRAFT",
  });
  const { data: archivedData, isLoading: archivedLoading } = useMyItems({
    page: 0,
    size: 1,
    sort: "createdAt,desc",
    status: "ARCHIVED",
  });
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
  const selectedStatusLabel = STATUS_OPTIONS.find((option) => option.value === (params.status ?? ""))?.label ?? "All Statuses";
  const pageStart = data ? data.page * data.size + 1 : 0;
  const pageEnd = data ? data.page * data.size + filteredItems.length : 0;

  const handleStatusChange = (e: ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as ItemStatus | "";
    setParams((prev) => ({
      ...prev,
      page: 0,
      status: val || undefined,
    }));
  };

  const resetFilters = () => {
    setSearchInput("");
    setConditionFilter("");
    setCategoryFilter("");
    setParams((prev) => ({ ...prev, page: 0, status: undefined }));
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
    <div className="mx-auto max-w-7xl">
      <section className="mb-6 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="bg-gradient-to-r from-indigo-500/10 via-slate-100 to-emerald-500/10 px-5 py-6 dark:from-indigo-500/10 dark:via-slate-800 dark:to-emerald-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">
            <div className="max-w-2xl">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="primary">Seller dashboard</Badge>
                <Badge variant="default">Inventory management</Badge>
              </div>
              <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 dark:text-white sm:text-4xl">
                My Items
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">
                Manage your listings, keep statuses current, and move quickly between drafts, active items, and archived inventory.
              </p>
              <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-slate-500 dark:text-slate-400">
                <span className="inline-flex items-center gap-2 rounded-full bg-white/80 px-3 py-1.5 shadow-sm dark:bg-slate-900/50">
                  <Layers3 className="size-4" />
                  {totalLoading ? "Loading inventory…" : `${totalData?.totalElements ?? 0} total listings`}
                </span>
                <span className="inline-flex items-center gap-2 rounded-full bg-white/80 px-3 py-1.5 shadow-sm dark:bg-slate-900/50">
                  <Filter className="size-4" />
                  {selectedStatusLabel}
                </span>
              </div>
            </div>

            <div className="flex shrink-0 flex-col items-stretch gap-3 sm:flex-row xl:w-60 xl:flex-col">
              <Link to="/my-items/new" className="w-full">
                <Button className="w-full justify-center">
                  <Plus className="size-4" />
                  New Item
                </Button>
              </Link>
              <p className="text-xs leading-5 text-slate-500 dark:text-slate-400 xl:text-right">
                Create a detailed listing to attract better trade offers and present your inventory professionally.
              </p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              title="Total Listings"
              value={totalData?.totalElements}
              helper="All listings in your seller inventory"
              icon={Package}
              iconClassName="bg-slate-100 text-slate-700 dark:bg-slate-700/70 dark:text-slate-200"
              isLoading={totalLoading}
            />
            <SummaryCard
              title="Active"
              value={activeData?.totalElements}
              helper="Currently visible in the marketplace"
              icon={BadgeCheck}
              iconClassName="bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300"
              isLoading={activeLoading}
            />
            <SummaryCard
              title="Drafts"
              value={draftData?.totalElements}
              helper="Still being prepared before publishing"
              icon={Edit3}
              iconClassName="bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300"
              isLoading={draftLoading}
            />
            <SummaryCard
              title="Archived"
              value={archivedData?.totalElements}
              helper="No longer shown in public browsing"
              icon={FolderArchive}
              iconClassName="bg-violet-100 text-violet-700 dark:bg-violet-950/30 dark:text-violet-300"
              isLoading={archivedLoading}
            />
          </div>
        </div>
      </section>

      <section className="mb-6 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800 sm:p-5">
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Filter and browse your inventory</h2>
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                Search listings, narrow by status or condition, and switch views without breaking your workflow.
              </p>
            </div>

            <div className="inline-flex items-center rounded-2xl border border-slate-200 bg-slate-50 p-1 dark:border-slate-700 dark:bg-slate-900/50">
              <button
                onClick={() => setViewMode("grid")}
                title="Grid view"
                aria-pressed={viewMode === "grid"}
                className={cn(
                  "inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-colors",
                  viewMode === "grid"
                    ? "bg-white text-indigo-600 shadow-sm dark:bg-slate-800 dark:text-indigo-300"
                    : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
                )}
              >
                <LayoutGrid className="size-4" />
                Grid
              </button>
              <button
                onClick={() => setViewMode("table")}
                title="Table view"
                aria-pressed={viewMode === "table"}
                className={cn(
                  "inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-colors",
                  viewMode === "table"
                    ? "bg-white text-indigo-600 shadow-sm dark:bg-slate-800 dark:text-indigo-300"
                    : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
                )}
              >
                <List className="size-4" />
                Table
              </button>
            </div>
          </div>

          <div className="grid gap-3 xl:grid-cols-[minmax(0,1.6fr)_repeat(3,minmax(0,0.8fr))]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search listings by title or category"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full rounded-2xl border border-slate-300 bg-white py-2.5 pl-10 pr-4 text-sm text-slate-900 placeholder:text-slate-400 transition-colors focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-900/60 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-indigo-400 dark:focus:ring-indigo-400/20"
              />
            </div>

            <select
              className="rounded-2xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 shadow-sm outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-900/60 dark:text-slate-100"
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
              className="rounded-2xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 shadow-sm outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-900/60 dark:text-slate-100"
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
              className="rounded-2xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 shadow-sm outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-900/60 dark:text-slate-100"
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
          </div>

          <div className="flex flex-col gap-3 rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-3 dark:border-slate-700 dark:bg-slate-900/40 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
              <span className="font-medium text-slate-900 dark:text-slate-100">
                {data ? `${filteredItems.length} item${filteredItems.length === 1 ? "" : "s"}` : "Loading items…"}
              </span>
              {data && filteredItems.length > 0 && (
                <span>
                  showing {pageStart}-{pageEnd} on this page
                </span>
              )}
              {hasActiveFilters && <Badge variant="secondary">Filters applied</Badge>}
            </div>

            {hasActiveFilters && (
              <Button variant="ghost" size="sm" onClick={resetFilters} className="self-start rounded-xl px-3 sm:self-auto">
                <RefreshCw className="size-4" />
                Reset filters
              </Button>
            )}
          </div>
        </div>
      </section>

      {isLoading && <ItemGridSkeleton />}

      {isError && (
        <EmptyState
          title="Failed to load your items"
          description="Something went wrong while loading your seller inventory. Please try again."
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
              ? "Try broadening your filters or search terms to surface more listings in your seller inventory."
              : "Start your seller catalog with a detailed listing so other traders can quickly understand what you offer."
          }
          action={
            hasActiveFilters ? (
              <Button variant="outline" onClick={resetFilters}>
                <RefreshCw className="size-4" />
                Reset filters
              </Button>
            ) : (
              <Link to="/my-items/new">
                <Button>
                  <Plus className="size-4" />
                  Create your first listing
                </Button>
              </Link>
            )
          }
        />
      )}

      {data && filteredItems.length > 0 && viewMode === "grid" && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {filteredItems.map((item) => (
            <div key={item.uuid} className="group relative">
              <div className="absolute inset-x-3 top-3 z-10 flex items-start justify-between gap-2">
                <div className="rounded-full bg-white/95 px-2 py-1 shadow-sm backdrop-blur dark:bg-slate-900/90">
                  <ItemStatusBadge status={item.status} />
                </div>
                <div className="flex items-center gap-2 rounded-full bg-white/95 p-1 shadow-sm opacity-100 transition-all sm:opacity-0 sm:translate-y-1 sm:group-hover:translate-y-0 sm:group-hover:opacity-100 dark:bg-slate-900/90">
                  <Link to={`/my-items/${item.uuid}/edit`}>
                    <Button variant="outline" size="sm" className="rounded-full border-0 bg-transparent px-2.5 shadow-none">
                      <Edit3 className="size-3.5" />
                      <span className="sr-only sm:not-sr-only sm:inline">Edit</span>
                    </Button>
                  </Link>
                  {item.status !== "ARCHIVED" && item.status !== "REMOVED" && (
                    <Button
                      variant="outline"
                      size="sm"
                      className="rounded-full border-0 bg-transparent px-2.5 shadow-none"
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        setArchiveUuid(item.uuid);
                      }}
                      aria-label={`Archive ${item.title}`}
                    >
                      <Archive className="size-3.5" />
                    </Button>
                  )}
                </div>
              </div>

              <ItemCard item={item} linkPrefix="/my-items" />
            </div>
          ))}
        </div>
      )}

      {data && filteredItems.length > 0 && viewMode === "table" && (
        <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/80 dark:border-slate-700 dark:bg-slate-900/40">
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Title
                  </th>
                  <th className="px-4 py-3.5 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Category
                  </th>
                  <th className="px-4 py-3.5 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Condition
                  </th>
                  <th className="px-4 py-3.5 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Status
                  </th>
                  <th className="px-4 py-3.5 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Created
                  </th>
                  <th className="px-5 py-3.5 text-right text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
                {filteredItems.map((item) => (
                  <tr key={item.uuid} className="transition-colors hover:bg-slate-50 dark:hover:bg-slate-900/30">
                    <td className="px-5 py-4">
                      <Link
                        to={`/my-items/${item.uuid}`}
                        className="block font-medium text-slate-900 transition-colors hover:text-indigo-600 dark:text-slate-100 dark:hover:text-indigo-400"
                      >
                        <span className="block max-w-xs truncate">{item.title}</span>
                        <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">
                          Seller listing
                        </span>
                      </Link>
                    </td>
                    <td className="px-4 py-4 text-slate-600 dark:text-slate-400">{item.categoryName}</td>
                    <td className="px-4 py-4">
                      <ItemConditionBadge condition={item.condition} />
                    </td>
                    <td className="px-4 py-4">
                      <ItemStatusBadge status={item.status} />
                    </td>
                    <td className="px-4 py-4 text-slate-600 dark:text-slate-400">
                      {new Date(item.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link to={`/my-items/${item.uuid}/edit`}>
                          <Button variant="outline" size="sm" className="rounded-xl">
                            <Edit3 className="size-3.5" />
                            Edit
                          </Button>
                        </Link>
                        {item.status !== "ARCHIVED" && item.status !== "REMOVED" && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="rounded-xl"
                            onClick={() => setArchiveUuid(item.uuid)}
                            aria-label={`Archive ${item.title}`}
                          >
                            <Archive className="size-3.5" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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

      <Modal
        isOpen={!!archiveUuid}
        onClose={() => {
          setArchiveUuid(null);
          setArchiveReason("");
        }}
        title="Archive Item"
        size="sm"
      >
        <p className="mb-4 text-sm text-slate-600 dark:text-slate-400">
          Are you sure you want to archive this item? It will no longer appear in the public marketplace.
        </p>
        <div className="mb-4">
          <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">
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
          <Button variant="danger" isLoading={archiveMutation.isPending} onClick={handleArchive}>
            Archive
          </Button>
        </div>
      </Modal>
    </div>
  );
}

