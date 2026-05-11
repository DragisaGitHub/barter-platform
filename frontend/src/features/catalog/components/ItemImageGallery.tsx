import { useState } from "react";
import { Star, Trash2, Package } from "lucide-react";
import type { ItemImageResponse } from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { cn } from "@/utils";
import { useDeleteItemImage, useSetPrimaryItemImage } from "../useItemImages";

interface ItemImageGalleryProps {
  itemUuid: string;
  images: ItemImageResponse[];
  /** When true, hide edit controls (use on public detail page) */
  readOnly?: boolean;
}

interface ImageTileProps {
  image: ItemImageResponse;
  readOnly: boolean;
  onSetPrimary: () => void;
  onDelete: () => void;
  isSettingPrimary: boolean;
  isDeleting: boolean;
}

function ImageTile({
  image,
  readOnly,
  onSetPrimary,
  onDelete,
  isSettingPrimary,
  isDeleting,
}: ImageTileProps) {
  const [imgError, setImgError] = useState(false);

  return (
    <div className="relative group rounded-lg overflow-hidden aspect-square bg-slate-100 dark:bg-slate-700">
      {imgError ? (
        <div className="w-full h-full flex items-center justify-center">
          <Package className="size-8 text-slate-300 dark:text-slate-500" />
        </div>
      ) : (
        <img
          src={image.url}
          alt={image.originalFilename}
          loading="lazy"
          className="w-full h-full object-cover"
          onError={() => setImgError(true)}
        />
      )}

      {/* Primary badge */}
      {image.isPrimary && (
        <div className="absolute top-1.5 left-1.5 bg-indigo-600 text-white text-xs font-semibold px-2 py-0.5 rounded-full flex items-center gap-1">
          <Star className="size-3" />
          Primary
        </div>
      )}

      {/* Edit overlay */}
      {!readOnly && (
        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-end justify-between p-2 gap-1">
          {!image.isPrimary && (
            <button
              className="flex items-center gap-1 text-xs bg-white/90 dark:bg-slate-800/90 text-slate-800 dark:text-slate-100 rounded px-2 py-1 hover:bg-white transition-colors disabled:opacity-50"
              onClick={onSetPrimary}
              disabled={isSettingPrimary || isDeleting}
              title="Set as primary"
            >
              <Star className="size-3" />
              Primary
            </button>
          )}
          <button
            className={cn(
              "flex items-center gap-1 text-xs bg-red-600 text-white rounded px-2 py-1 hover:bg-red-700 transition-colors disabled:opacity-50",
              image.isPrimary ? "ml-auto" : ""
            )}
            onClick={onDelete}
            disabled={isSettingPrimary || isDeleting}
            title="Delete image"
          >
            <Trash2 className="size-3" />
            Delete
          </button>
        </div>
      )}

      {(isSettingPrimary || isDeleting) && (
        <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
          <div className="size-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
        </div>
      )}
    </div>
  );
}

export function ItemImageGallery({ itemUuid, images, readOnly = false }: ItemImageGalleryProps) {
  const deleteMutation = useDeleteItemImage(itemUuid);
  const setPrimaryMutation = useSetPrimaryItemImage(itemUuid);
  const [confirmDeleteUuid, setConfirmDeleteUuid] = useState<string | null>(null);

  if (images.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center text-slate-500 dark:text-slate-400 border-2 border-dashed border-slate-200 dark:border-slate-700 rounded-lg">
        <Package className="size-10 mb-2 text-slate-300 dark:text-slate-600" />
        <p className="text-sm">No images yet</p>
        {!readOnly && (
          <p className="text-xs mt-0.5">Upload images using the uploader above</p>
        )}
      </div>
    );
  }

  function requestDelete(imageUuid: string) {
    setConfirmDeleteUuid(imageUuid);
  }

  function confirmDelete() {
    if (confirmDeleteUuid) {
      deleteMutation.mutate(confirmDeleteUuid);
      setConfirmDeleteUuid(null);
    }
  }

  // Sort: primary first, then by sortOrder
  const sorted = [...images].sort((a, b) => {
    if (a.isPrimary) return -1;
    if (b.isPrimary) return 1;
    return a.sortOrder - b.sortOrder;
  });

  return (
    <>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {sorted.map((image) => (
          <ImageTile
            key={image.uuid}
            image={image}
            readOnly={readOnly}
            onSetPrimary={() => setPrimaryMutation.mutate(image.uuid)}
            onDelete={() => requestDelete(image.uuid)}
            isSettingPrimary={setPrimaryMutation.isPending && setPrimaryMutation.variables === image.uuid}
            isDeleting={deleteMutation.isPending && deleteMutation.variables === confirmDeleteUuid && confirmDeleteUuid === image.uuid}
          />
        ))}
      </div>

      {/* Confirm delete dialog */}
      {confirmDeleteUuid && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            onClick={() => setConfirmDeleteUuid(null)}
          />
          <div className="relative bg-white dark:bg-slate-800 rounded-xl shadow-xl p-6 m-4 max-w-sm w-full">
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-2">
              Delete image?
            </h3>
            <p className="text-sm text-slate-600 dark:text-slate-400 mb-4">
              This action cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setConfirmDeleteUuid(null)}>
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={confirmDelete}
                isLoading={deleteMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

