import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { ArrowLeft, ArrowRightLeft, Package } from "lucide-react";
import { useItemDetail } from "./useCatalog";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { SendOfferModal } from "../trade/SendOfferModal";
import { useAuth } from "../../auth/AuthContext";
import type { ItemImageResponse } from "@/api/generated/types.ts";
import { cn } from "@/utils";

function ImageSection({ images }: { images: ItemImageResponse[] }) {
  const [selectedIdx, setSelectedIdx] = useState(0);
  const primary = images.find((img) => img.isPrimary) ?? images[0];
  const sorted = [...images].sort((a, b) => {
    if (a.isPrimary) return -1;
    if (b.isPrimary) return 1;
    return a.sortOrder - b.sortOrder;
  });
  const displayed = sorted[selectedIdx] ?? primary;

  if (images.length === 0) {
    return (
      <div className="aspect-square rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
        <Package className="size-24 text-slate-300 dark:text-slate-500" />
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Main image */}
      <div className="aspect-square rounded-xl bg-slate-100 dark:bg-slate-700 overflow-hidden">
        {displayed ? (
          <img
            src={displayed.url}
            alt={displayed.originalFilename}
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Package className="size-24 text-slate-300 dark:text-slate-500" />
          </div>
        )}
      </div>

      {/* Thumbnails if more than 1 image */}
      {sorted.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {sorted.map((img, idx) => (
            <button
              key={img.uuid}
              onClick={() => setSelectedIdx(idx)}
              className={cn(
                "shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition-colors",
                idx === selectedIdx
                  ? "border-indigo-500"
                  : "border-transparent hover:border-slate-300 dark:hover:border-slate-600"
              )}
            >
              <img
                src={img.url}
                alt={img.originalFilename}
                loading="lazy"
                className="w-full h-full object-cover"
              />
              {img.isPrimary && (
                <span className="sr-only">Primary image</span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function ItemDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const { data: item, isLoading, isError } = useItemDetail(uuid ?? "");
  const { user, isAuthenticated } = useAuth();
  const [showOfferModal, setShowOfferModal] = useState(false);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="max-w-3xl mx-auto">
        <EmptyState
          title="Item not found"
          description="This item may have been removed or does not exist."
          action={
            <Link to="/marketplace">
              <Button variant="outline">Back to Marketplace</Button>
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <Link
        to="/marketplace"
        className="inline-flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 mb-6"
      >
        <ArrowLeft className="size-4" />
        Back to Marketplace
      </Link>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Image gallery */}
        <div>
          <ImageSection images={item.images ?? []} />
        </div>

        {/* Details */}
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">
            {item.title}
          </h1>

          <div className="flex flex-wrap gap-2 mb-4">
            <ItemStatusBadge status={item.status} />
            <ItemConditionBadge condition={item.condition} />
          </div>

          <div className="space-y-3 mb-6">
            <div>
              <span className="text-sm font-medium text-slate-500 dark:text-slate-400">
                Category
              </span>
              <p className="text-slate-900 dark:text-slate-100">{item.category.name}</p>
            </div>

            <div>
              <span className="text-sm font-medium text-slate-500 dark:text-slate-400">
                Owner
              </span>
              <p className="text-slate-900 dark:text-slate-100">{item.ownerUsername}</p>
            </div>

            {item.tags.length > 0 && (
              <div>
                <span className="text-sm font-medium text-slate-500 dark:text-slate-400">
                  Tags
                </span>
                <div className="flex flex-wrap gap-1 mt-1">
                  {item.tags.map((tag) => (
                    <Badge key={tag.uuid} variant="default">
                      {tag.name}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            <div>
              <span className="text-sm font-medium text-slate-500 dark:text-slate-400">
                Listed on
              </span>
              <p className="text-slate-900 dark:text-slate-100">
                {new Date(item.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>

          {item.description && (
            <div>
              <h2 className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">
                Description
              </h2>
              <p className="text-slate-700 dark:text-slate-300 whitespace-pre-line">
                {item.description}
              </p>
            </div>
          )}

          {/* Propose Trade button - only for authenticated users viewing someone else's ACTIVE item */}
          {isAuthenticated &&
            item.status === "ACTIVE" &&
            user?.username !== item.ownerUsername && (
              <div className="mt-6">
                <Button onClick={() => setShowOfferModal(true)}>
                  <ArrowRightLeft className="size-4" />
                  Propose Trade
                </Button>
              </div>
            )}
        </div>
      </div>

      {item && (
        <SendOfferModal
          isOpen={showOfferModal}
          onClose={() => setShowOfferModal(false)}
          receiverItem={item}
        />
      )}
    </div>
  );
}

