import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Package, Archive } from "lucide-react";
import { useMyItems, useArchiveItem } from "./useCatalog";
import { ItemCard } from "./ItemCard";
import { ItemGridSkeleton } from "./ItemCardSkeleton";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import type { ItemStatus } from "@/api/generated/types.ts";
import type { MyItemsParams } from "@/api/catalogApi.ts";
import { toast } from "sonner";

const STATUS_OPTIONS: { value: ItemStatus | ""; label: string }[] = [
  { value: "", label: "All Statuses" },
  { value: "DRAFT", label: "Draft" },
  { value: "ACTIVE", label: "Active" },
  { value: "RESERVED", label: "Reserved" },
  { value: "ARCHIVED", label: "Archived" },
];

export function MyItemsPage() {
  const [params, setParams] = useState<MyItemsParams>({
    page: 0,
    size: 12,
    sort: "createdAt,desc",
  });
  const [archiveUuid, setArchiveUuid] = useState<string | null>(null);
  const [archiveReason, setArchiveReason] = useState("");

  const { data, isLoading, isError } = useMyItems(params);
  const archiveMutation = useArchiveItem();

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

      {/* Status filter */}
      <div className="mb-6">
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

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<Package className="size-16" />}
          title="No items yet"
          description="Create your first listing to start trading."
          action={
            <Link to="/my-items/new">
              <Button>
                <Plus className="size-4" />
                Create Item
              </Button>
            </Link>
          }
        />
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {data.content.map((item) => (
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

          {data.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={(page) => setParams((prev) => ({ ...prev, page }))}
              />
            </div>
          )}
        </>
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

