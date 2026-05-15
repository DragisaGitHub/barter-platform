import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Image as ImageIcon, ShieldAlert, ShieldCheck, UserRound } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { ItemConditionBadge, ItemStatusBadge } from "@/features/catalog/ItemBadges.tsx";
import { routePaths } from "@/routes/routePaths.ts";
import { AdminPageShell, AdminSurface } from "./components/AdminPageShell";
import { AdminListingModerationDialog } from "./AdminListingModerationDialog";
import { AdminListingModerationTimeline } from "./AdminListingModerationTimeline";
import {
  useAdminListing,
  useListingModerationActions,
  useRemoveAdminListing,
  useRestoreAdminListing,
} from "./useAdminListings";

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

function formatReasonCode(reasonCode?: string) {
  if (!reasonCode) {
    return "Reason unavailable";
  }

  return reasonCode.replace(/_/g, " ");
}

export function AdminListingDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const [removeOpen, setRemoveOpen] = useState(false);
  const [restoreOpen, setRestoreOpen] = useState(false);

  const listingQuery = useAdminListing(uuid ?? "", !!uuid);
  const actionsQuery = useListingModerationActions(uuid ?? "", !!uuid);
  const removeMutation = useRemoveAdminListing();
  const restoreMutation = useRestoreAdminListing();

  const listing = listingQuery.data;

  if (listingQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!listing || listingQuery.isError) {
    return (
      <EmptyState
        icon={<ShieldAlert className="size-12" />}
        title="Listing not found"
        description="The requested listing could not be loaded or may no longer exist."
        action={
          <Link to={routePaths.admin.listings}>
            <Button variant="outline">Back to listings</Button>
          </Link>
        }
      />
    );
  }

  const isRemoved = listing.status === "REMOVED";

  return (
    <>
      <AdminPageShell
        title={listing.title}
        description="Review the full listing record, owner context, visibility status, uploaded images, and chronological moderation actions."
        actions={
          <div className="flex flex-wrap items-center gap-3">
            <Link to={routePaths.admin.listings}>
              <Button variant="outline">
                <ArrowLeft className="size-4" />
                Back to listings
              </Button>
            </Link>
            {isRemoved ? (
              <Button onClick={() => setRestoreOpen(true)}>
                <ShieldCheck className="size-4" />
                Restore listing
              </Button>
            ) : (
              <Button variant="danger" onClick={() => setRemoveOpen(true)}>
                <ShieldAlert className="size-4" />
                Remove listing
              </Button>
            )}
          </div>
        }
        badges={
          <>
            <ItemStatusBadge status={listing.status} />
            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
              Admin detail
            </span>
          </>
        }
        contentClassName="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(340px,0.85fr)]"
      >
        <div className="space-y-6">
          <AdminSurface title="Listing overview" description="Core listing data and current marketplace lifecycle state.">
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Title
                </p>
                <p className="mt-1 text-base font-semibold text-slate-900 dark:text-slate-100">{listing.title}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Category
                </p>
                <p className="mt-1 text-base text-slate-900 dark:text-slate-100">{listing.category.name}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Status
                </p>
                <div className="mt-1 flex items-center gap-2">
                  <ItemStatusBadge status={listing.status} />
                  <ItemConditionBadge condition={listing.condition} />
                </div>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Owner
                </p>
                <div className="mt-1 flex items-center gap-2 text-slate-900 dark:text-slate-100">
                  <UserRound className="size-4 text-slate-400" />
                  <span>{listing.ownerUsername}</span>
                </div>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Created
                </p>
                <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{formatDateTime(listing.createdAt)}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Updated
                </p>
                <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{formatDateTime(listing.updatedAt)}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Archived at
                </p>
                <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{formatDateTime(listing.archivedAt)}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                  Removed at
                </p>
                <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{formatDateTime(listing.removedAt)}</p>
              </div>
            </div>

            <div className="mt-5">
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                Description
              </p>
              <p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-700 dark:text-slate-300">
                {listing.description?.trim() || "No description provided."}
              </p>
            </div>

            <div className="mt-5">
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">Tags</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {listing.tags.length > 0 ? (
                  listing.tags.map((tag) => (
                    <span
                      key={tag.uuid}
                      className="inline-flex rounded-full border border-slate-200 px-3 py-1 text-xs font-medium text-slate-700 dark:border-slate-700 dark:text-slate-300"
                    >
                      {tag.name}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-slate-500 dark:text-slate-400">No tags assigned.</span>
                )}
              </div>
            </div>
          </AdminSurface>

          <AdminSurface title="Listing images" description="Uploaded media that currently represents this listing.">
            {listing.images.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 px-6 py-12 text-center dark:border-slate-700">
                <ImageIcon className="size-10 text-slate-300 dark:text-slate-600" />
                <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No images uploaded for this listing.</p>
              </div>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {listing.images.map((image) => (
                  <div key={image.uuid} className="overflow-hidden rounded-2xl border border-slate-200 dark:border-slate-700">
                    <img src={image.url} alt={image.originalFilename} className="aspect-square w-full object-cover" />
                    <div className="space-y-1 px-3 py-3 text-xs text-slate-500 dark:text-slate-400">
                      <div className="font-medium text-slate-700 dark:text-slate-200">{image.originalFilename}</div>
                      <div>{image.isPrimary ? "Primary image" : `Sort order ${image.sortOrder}`}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </AdminSurface>
        </div>

        <div className="space-y-6">
          <AdminSurface title="Owner-facing moderation summary" description="What the listing owner currently sees when this listing is removed or restored.">
            {listing.moderationSummary ? (
              <div className="space-y-2 rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900/60">
                <div className="flex flex-wrap items-center gap-2">
                  <ItemStatusBadge status={listing.status} />
                  <span className="inline-flex rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                    {formatReasonCode(listing.moderationSummary.reasonCode)}
                  </span>
                </div>
                <p className="text-sm text-slate-700 dark:text-slate-300">
                  {listing.moderationSummary.userMessage || "No owner-facing message was included for the latest moderation action."}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {formatDateTime(listing.moderationSummary.actionAt)} • {listing.moderationSummary.actionType}
                </p>
              </div>
            ) : (
              <p className="text-sm text-slate-500 dark:text-slate-400">No owner-facing moderation summary is available yet.</p>
            )}
          </AdminSurface>

          <AdminSurface title="Moderation timeline" description="Chronological record of remove and restore actions for this listing.">
            <AdminListingModerationTimeline actions={actionsQuery.data ?? []} isLoading={actionsQuery.isLoading} />
          </AdminSurface>
        </div>
      </AdminPageShell>

      <AdminListingModerationDialog
        isOpen={removeOpen}
        mode="remove"
        listingTitle={listing.title}
        isSubmitting={removeMutation.isPending}
        onClose={() => setRemoveOpen(false)}
        onSubmit={(payload) => {
          removeMutation.mutate(
            { listingUuid: listing.uuid, data: payload },
            {
              onSuccess: () => {
                toast.success("Listing removed and pending offers invalidated.");
                setRemoveOpen(false);
              },
              onError: () => toast.error("Failed to remove listing."),
            }
          );
        }}
      />

      <AdminListingModerationDialog
        isOpen={restoreOpen}
        mode="restore"
        listingTitle={listing.title}
        isSubmitting={restoreMutation.isPending}
        onClose={() => setRestoreOpen(false)}
        onSubmit={(payload) => {
          restoreMutation.mutate(
            { listingUuid: listing.uuid, data: payload },
            {
              onSuccess: () => {
                toast.success("Listing restored successfully.");
                setRestoreOpen(false);
              },
              onError: () => toast.error("Failed to restore listing."),
            }
          );
        }}
      />
    </>
  );
}

