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
        {/* Image placeholder */}
        <div className="aspect-square rounded-xl bg-slate-100 dark:bg-slate-700 flex items-center justify-center">
          <Package className="size-24 text-slate-300 dark:text-slate-500" />
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

