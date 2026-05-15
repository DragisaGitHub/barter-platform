import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Search, ShieldAlert } from "lucide-react";
import type { ItemStatus } from "@/api/generated/types.ts";
import { useCategories } from "@/features/catalog/useCatalog.ts";
import { ItemStatusBadge } from "@/features/catalog/ItemBadges.tsx";
import { Pagination } from "@/components/data/Pagination";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { routePaths } from "@/routes/routePaths.ts";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useAdminListings } from "./useAdminListings";

const STATUS_OPTIONS: { value: ItemStatus | ""; label: string }[] = [
  { value: "", label: "All statuses" },
  { value: "DRAFT", label: "Draft" },
  { value: "ACTIVE", label: "Active" },
  { value: "RESERVED", label: "Reserved" },
  { value: "ARCHIVED", label: "Archived" },
  { value: "REMOVED", label: "Removed" },
];

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

export function AdminListingsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [sort] = useState("createdAt,desc");
  const [searchInput, setSearchInput] = useState("");
  const [ownerInput, setOwnerInput] = useState("");
  const [query, setQuery] = useState("");
  const [ownerQuery, setOwnerQuery] = useState("");
  const [status, setStatus] = useState<ItemStatus | "">("");
  const [categoryUuid, setCategoryUuid] = useState("");

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setPage(0);
      setQuery(searchInput.trim());
      setOwnerQuery(ownerInput.trim());
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [ownerInput, searchInput]);

  const params = useMemo(
    () => ({
      page,
      size: pageSize,
      sort,
      q: query || undefined,
      ownerQuery: ownerQuery || undefined,
      categoryUuid: categoryUuid || undefined,
      status: status || undefined,
    }),
    [categoryUuid, ownerQuery, page, pageSize, query, sort, status]
  );

  const listingsQuery = useAdminListings(params);
  const categoriesQuery = useCategories();

  const data = listingsQuery.data;
  const listings = data?.content ?? [];
  const hasFilters = !!query || !!ownerQuery || !!categoryUuid || !!status;
  const totalListings = data?.totalElements ?? 0;
  const rangeStart = totalListings === 0 ? 0 : page * pageSize + 1;
  const rangeEnd = totalListings === 0 ? 0 : rangeStart + listings.length - 1;

  return (
    <AdminPageShell
      title="Listing moderation"
      description="Review listings across all lifecycle states, inspect moderation history, and perform focused governance actions without leaving the admin workspace."
      badges={
        <>
          <span className="inline-flex items-center rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700 dark:bg-red-950/30 dark:text-red-300">
            Admin listings
          </span>
          <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
            Governance foundation
          </span>
        </>
      }
      toolbar={
        <AdminToolbar>
          <div className="grid flex-1 gap-3 lg:grid-cols-4">
            <div className="relative lg:col-span-2">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Search listing title"
                className="pl-9"
              />
            </div>
            <Input
              value={ownerInput}
              onChange={(event) => setOwnerInput(event.target.value)}
              placeholder="Owner username or email"
            />
            <select
              value={status}
              onChange={(event) => {
                setPage(0);
                setStatus(event.target.value as ItemStatus | "");
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <select
              value={categoryUuid}
              onChange={(event) => {
                setPage(0);
                setCategoryUuid(event.target.value);
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              <option value="">All categories</option>
              {(categoriesQuery.data ?? []).map((category) => (
                <option key={category.uuid} value={category.uuid}>
                  {category.name}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300">
              <span>Rows</span>
              <select
                value={pageSize}
                onChange={(event) => {
                  setPage(0);
                  setPageSize(Number(event.target.value));
                }}
                className="bg-transparent text-sm font-medium text-slate-900 outline-none dark:text-slate-100"
                aria-label="Listings per page"
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
            <Button
              variant="outline"
              onClick={() => {
                setSearchInput("");
                setOwnerInput("");
                setQuery("");
                setOwnerQuery("");
                setStatus("");
                setCategoryUuid("");
                setPage(0);
              }}
              disabled={!hasFilters}
            >
              Reset filters
            </Button>
          </div>
        </AdminToolbar>
      }
    >
      <AdminSurface
        title="All listings"
        description={`${data?.totalElements ?? 0} listing${(data?.totalElements ?? 0) === 1 ? "" : "s"} matched the current moderation filters.`}
        contentClassName="space-y-0"
      >
        {listingsQuery.isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : listingsQuery.isError || !data ? (
          <EmptyState
            icon={<ShieldAlert className="size-12" />}
            title="Unable to load listings"
            description="Unable to load listings. The server could not apply the current filters. Clear filters or try again."
            action={
              <Button variant="outline" onClick={() => listingsQuery.refetch()}>
                Retry
              </Button>
            }
          />
        ) : listings.length === 0 ? (
          <EmptyState
            icon={<ShieldAlert className="size-12" />}
            title="No listings found"
            description={
              hasFilters
                ? "Try broadening the moderation filters or clearing the current search terms."
                : "Listings will appear here once users start publishing inventory."
            }
            action={
              hasFilters ? (
                <Button
                  variant="outline"
                  onClick={() => {
                    setSearchInput("");
                    setOwnerInput("");
                    setQuery("");
                    setOwnerQuery("");
                    setStatus("");
                    setCategoryUuid("");
                    setPage(0);
                  }}
                >
                  Clear filters
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 dark:bg-slate-900/60">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Listing</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Owner</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Category</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Status</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Created</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600 dark:text-slate-300">Removed</th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-600 dark:text-slate-300">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                  {listings.map((listing) => (
                    <tr key={listing.uuid} className="hover:bg-slate-50 dark:hover:bg-slate-900/50">
                      <td className="px-4 py-3">
                        <div>
                          <Link
                            to={routePaths.admin.listingDetail(listing.uuid)}
                            className="font-semibold text-slate-900 hover:text-indigo-600 dark:text-slate-100 dark:hover:text-indigo-400"
                          >
                            {listing.title}
                          </Link>
                          <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                            {listing.condition.replace(/_/g, " ")}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">{listing.ownerUsername}</td>
                      <td className="px-4 py-3 text-slate-700 dark:text-slate-300">{listing.categoryName}</td>
                      <td className="px-4 py-3">
                        <ItemStatusBadge status={listing.status} />
                      </td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{formatDateTime(listing.createdAt)}</td>
                      <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{formatDateTime(listing.removedAt)}</td>
                      <td className="px-4 py-3 text-right">
                        <Link to={routePaths.admin.listingDetail(listing.uuid)}>
                          <Button variant="outline" size="sm">
                            Open detail
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              currentPage={data.page ?? 0}
              totalPages={Math.max(data.totalPages ?? 0, 1)}
              onPageChange={setPage}
              statusContent={
                <>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    {totalListings === 0 ? "No listings matched" : `Showing ${rangeStart}–${rangeEnd} of ${totalListings}`}
                  </span>
                  {listingsQuery.isFetching && (
                    <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                      Refreshing…
                    </span>
                  )}
                </>
              }
            />
          </>
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}

