import { useParams, Link } from "react-router-dom";
import { ArrowLeft, ArrowRightLeft } from "lucide-react";
import { useTradeOffer } from "./useTradeOffers";
import { TradeOfferStatusBadge } from "./TradeOfferStatusBadge";
import { TradeOfferActionButtons } from "./TradeOfferActionButtons";
import { ItemStatusBadge, ItemConditionBadge } from "../catalog/ItemBadges";
import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { useAuth } from "../../auth/AuthContext";

export function TradeOfferDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const { user } = useAuth();
  const { data: offer, isLoading, isError } = useTradeOffer(uuid ?? "");

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !offer) {
    return (
      <div className="max-w-3xl mx-auto">
        <EmptyState
          title="Trade offer not found"
          description="This offer may have been removed or you don't have access."
          action={
            <Link to="/offers/incoming">
              <Button variant="outline">Back to Offers</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const isSender = offer.sender.uuid === user?.uuid;
  const isReceiver = offer.receiver.uuid === user?.uuid;
  const isPending = offer.status === "PENDING";

  return (
    <div className="max-w-4xl mx-auto">
      <Link
        to={isSender ? "/offers/sent" : "/offers/incoming"}
        className="inline-flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 mb-6"
      >
        <ArrowLeft className="size-4" />
        Back to {isSender ? "Sent" : "Incoming"} Offers
      </Link>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Trade Offer</h1>
        <TradeOfferStatusBadge status={offer.status} />
      </div>

      {/* Trade details */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* Sender item */}
        <Card>
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
            {isSender ? "You offer" : `${offer.sender.username} offers`}
          </p>
          <Link
            to={`/marketplace/items/${offer.senderItem.uuid}`}
            className="block group"
          >
            <h3 className="font-semibold text-lg text-slate-900 dark:text-slate-100 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 mb-2">
              {offer.senderItem.title}
            </h3>
          </Link>
          <div className="flex flex-wrap gap-2 mb-2">
            <ItemStatusBadge status={offer.senderItem.status} />
            <ItemConditionBadge condition={offer.senderItem.condition} />
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {offer.senderItem.categoryName}
          </p>
        </Card>

        {/* Receiver item */}
        <Card>
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
            {isReceiver ? "Your item" : `${offer.receiver.username}'s item`}
          </p>
          <Link
            to={`/marketplace/items/${offer.receiverItem.uuid}`}
            className="block group"
          >
            <h3 className="font-semibold text-lg text-slate-900 dark:text-slate-100 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 mb-2">
              {offer.receiverItem.title}
            </h3>
          </Link>
          <div className="flex flex-wrap gap-2 mb-2">
            <ItemStatusBadge status={offer.receiverItem.status} />
            <ItemConditionBadge condition={offer.receiverItem.condition} />
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {offer.receiverItem.categoryName}
          </p>
        </Card>
      </div>

      {/* Arrow between cards on mobile */}
      <div className="flex justify-center -mt-3 mb-3 md:hidden">
        <ArrowRightLeft className="size-5 text-slate-400" />
      </div>

      {/* Metadata */}
      <Card className="mb-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              Sender
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">{offer.sender.username}</p>
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              Receiver
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">{offer.receiver.username}</p>
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              Created
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">
              {new Date(offer.createdAt).toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              Responded
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">
              {offer.respondedAt ? new Date(offer.respondedAt).toLocaleString() : "—"}
            </p>
          </div>
        </div>

        {offer.message && (
          <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700">
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              Message
            </p>
            <p className="text-sm text-slate-700 dark:text-slate-300 italic">"{offer.message}"</p>
          </div>
        )}
      </Card>

      {/* Actions */}
      <TradeOfferActionButtons
        offerUuid={offer.uuid}
        canAccept={isReceiver && isPending}
        canReject={isReceiver && isPending}
        canCancel={isSender && isPending}
      />
    </div>
  );
}

