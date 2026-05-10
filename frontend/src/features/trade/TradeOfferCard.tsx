import { Link } from "react-router-dom";
import { ArrowRightLeft } from "lucide-react";
import type { TradeOfferSummaryResponse } from "@/api/generated/types.ts";
import { Card } from "../../components/ui/Card";
import { TradeOfferStatusBadge } from "./TradeOfferStatusBadge";
import { TradeOfferActionButtons } from "./TradeOfferActionButtons";

interface TradeOfferCardProps {
  offer: TradeOfferSummaryResponse;
  currentUserUuid: string;
}

export function TradeOfferCard({ offer, currentUserUuid }: TradeOfferCardProps) {
  const isSender = offer.sender.uuid === currentUserUuid;
  const isReceiver = offer.receiver.uuid === currentUserUuid;
  const isPending = offer.status === "PENDING";

  return (
    <Card className="transition-shadow hover:shadow-md">
      <div className="flex items-start justify-between mb-4">
        <TradeOfferStatusBadge status={offer.status} />
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {new Date(offer.createdAt).toLocaleDateString()}
        </span>
      </div>

      <Link to={`/offers/${offer.uuid}`} className="block group">
        {/* Items being traded */}
        <div className="flex items-center gap-3 mb-4">
          {/* Sender item */}
          <div className="flex-1 min-w-0">
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-1">
              {isSender ? "You offer" : `${offer.sender.username} offers`}
            </p>
            <p className="font-medium text-slate-900 dark:text-slate-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
              {offer.senderItem.title}
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {offer.senderItem.categoryName}
            </p>
          </div>

          <ArrowRightLeft className="size-5 text-slate-400 shrink-0" />

          {/* Receiver item */}
          <div className="flex-1 min-w-0">
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-1">
              {isReceiver ? "Your item" : `${offer.receiver.username}'s item`}
            </p>
            <p className="font-medium text-slate-900 dark:text-slate-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
              {offer.receiverItem.title}
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {offer.receiverItem.categoryName}
            </p>
          </div>
        </div>
      </Link>

      {offer.message && (
        <p className="text-sm text-slate-600 dark:text-slate-400 mb-4 italic line-clamp-2">
          "{offer.message}"
        </p>
      )}

      {offer.respondedAt && (
        <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
          Responded: {new Date(offer.respondedAt).toLocaleDateString()}
        </p>
      )}

      <TradeOfferActionButtons
        offerUuid={offer.uuid}
        canAccept={isReceiver && isPending}
        canReject={isReceiver && isPending}
        canCancel={isSender && isPending}
      />
    </Card>
  );
}

