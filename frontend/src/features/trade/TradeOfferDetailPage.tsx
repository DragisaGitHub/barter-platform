import { useParams, Link } from "react-router-dom";
import { ArrowLeft, ArrowRightLeft, Check, Clock, X, Ban, AlertTriangle, Gift, MessageSquare } from "lucide-react";
import { useTradeOffer } from "./useTradeOffers";
import { TradeOfferStatusBadge } from "./TradeOfferStatusBadge";
import { TradeOfferModeBadge } from "./TradeOfferModeBadge";
import { TradeOfferActionButtons } from "./TradeOfferActionButtons";
import { ItemStatusBadge, ItemConditionBadge } from "../catalog/ItemBadges";
import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { useAuth } from "../../auth/AuthContext";
import type { TradeOfferStatus, TradeOfferItemSummary } from "@/api/generated/types.ts";
import {TradeOfferMessagesPanel} from "@/features/trade/TradeOfferMessagesPanel.tsx";
import React from "react";

const STATUS_DETAIL: Record<TradeOfferStatus, { label: string; description: string; icon: React.ReactNode }> = {
  PENDING: {
    label: "Pending",
    description: "Waiting for the receiver to respond to this trade offer.",
    icon: <Clock className="size-4 text-amber-500" />,
  },
  ACCEPTED: {
    label: "Accepted",
    description: "Trade accepted. All involved items are now archived.",
    icon: <Check className="size-4 text-green-500" />,
  },
  REJECTED: {
    label: "Rejected",
    description: "This trade offer was rejected by the receiver.",
    icon: <X className="size-4 text-red-500" />,
  },
  CANCELLED: {
    label: "Cancelled",
    description: "This trade offer was cancelled by the sender.",
    icon: <Ban className="size-4 text-slate-500" />,
  },
  EXPIRED: {
    label: "Expired",
    description: "This trade offer has expired without a response.",
    icon: <AlertTriangle className="size-4 text-slate-400" />,
  },
  INVALIDATED: {
    label: "Invalidated",
    description: "This trade offer was automatically invalidated because a referenced listing was removed from the marketplace.",
    icon: <Ban className="size-4 text-slate-500" />,
  },
};

function ItemCard({ item}: { item: TradeOfferItemSummary; label: string; sublabel: string }) {
  return (
    <div className="p-4 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
      <Link
        to={`/marketplace/items/${item.uuid}`}
        className="block group"
      >
        <h3 className="font-semibold text-lg text-slate-900 dark:text-slate-100 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 mb-2">
          {item.title}
        </h3>
      </Link>
      <div className="flex flex-wrap gap-2 mb-2">
        <ItemStatusBadge status={item.status} />
        <ItemConditionBadge condition={item.condition} />
      </div>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {item.categoryName}
      </p>
    </div>
  );
}

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
  const statusInfo = STATUS_DETAIL[offer.status];

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
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Trade Offer Details</h1>
        <div className="flex items-center gap-2">
          <TradeOfferModeBadge mode={offer.mode} />
          <TradeOfferStatusBadge status={offer.status} />
        </div>
      </div>

      {/* Status summary */}
      <Card className="mb-6">
        <div className="flex items-start gap-3">
          {statusInfo.icon}
          <div>
            <p className="font-medium text-slate-900 dark:text-slate-100">{statusInfo.label}</p>
            <p className="text-sm text-slate-600 dark:text-slate-400">{statusInfo.description}</p>
          </div>
        </div>
      </Card>

      {/* Message (prominent for GIFT/NEGOTIABLE) */}
      {offer.message && (
        <Card className={`mb-6 ${
          offer.mode === "GIFT" || offer.mode === "NEGOTIABLE"
            ? "border-amber-200 dark:border-amber-800 bg-amber-50/50 dark:bg-amber-900/10"
            : ""
        }`}>
          <div className="flex items-start gap-2">
            {offer.mode === "NEGOTIABLE" && <MessageSquare className="size-4 text-amber-500 mt-0.5 shrink-0" />}
            {offer.mode === "GIFT" && <Gift className="size-4 text-emerald-500 mt-0.5 shrink-0" />}
            <div>
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                {offer.mode === "NEGOTIABLE" ? "Negotiation Terms" : offer.mode === "GIFT" ? "Gift Message" : "Message"}
              </p>
              <p className="text-sm text-slate-700 dark:text-slate-300">"{offer.message}"</p>
            </div>
          </div>
        </Card>
      )}

      {/* Trade sides */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* Requested item (receiver side) */}
        <Card>
          <p className="text-xs font-medium text-indigo-600 dark:text-indigo-400 uppercase tracking-wider mb-1">
            {isReceiver ? "Your Requested Item" : "Requested Item"}
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
            From: {isReceiver ? "You" : offer.receiver.username}
          </p>
          <ItemCard
            item={offer.receiverItem}
            label="Requested Item"
            sublabel={isReceiver ? "Your item" : `From ${offer.receiver.username}`}
          />
        </Card>

        {/* Offered items (sender side) */}
        <Card>
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
            {offer.offeredItems.length === 0
              ? "No Items Offered"
              : isSender
                ? `Your Offered Item${offer.offeredItems.length > 1 ? "s" : ""}`
                : `Offered Item${offer.offeredItems.length > 1 ? "s" : ""}`}
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
            From: {isSender ? "You" : offer.sender.username}
          </p>

          {offer.offeredItems.length === 0 ? (
            <div className="p-4 rounded-lg border border-dashed border-slate-300 dark:border-slate-600 text-center">
              <p className="text-sm text-slate-400 dark:text-slate-500 italic">
                {offer.mode === "GIFT" ? "Gift request — no items offered" : "No items offered"}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {offer.offeredItems.map((item) => (
                <ItemCard
                  key={item.uuid}
                  item={item}
                  label="Offered Item"
                  sublabel={isSender ? "Your item" : `From ${offer.sender.username}`}
                />
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* Arrow between cards on mobile */}
      <div className="flex justify-center -mt-3 mb-3 md:hidden">
        {offer.mode === "ITEM_EXCHANGE" && <ArrowRightLeft className="size-5 text-slate-400" />}
        {offer.mode === "GIFT" && <Gift className="size-5 text-emerald-500" />}
        {offer.mode === "NEGOTIABLE" && <MessageSquare className="size-5 text-amber-500" />}
      </div>

      {/* Metadata */}
      <Card className="mb-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              From
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">
              {isSender ? "You" : offer.sender.username}
            </p>
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              To
            </p>
            <p className="text-sm text-slate-900 dark:text-slate-100">
              {isReceiver ? "You" : offer.receiver.username}
            </p>
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
      </Card>

      {/* Actions */}
      {(isReceiver && isPending) || (isSender && isPending) ? (
        <Card>
          <p className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-3">
            {isReceiver ? "Respond to this trade offer:" : "You can cancel this offer:"}
          </p>
          <TradeOfferActionButtons
            offerUuid={offer.uuid}
            canAccept={isReceiver && isPending}
            canReject={isReceiver && isPending}
            canCancel={isSender && isPending}
            mode={offer.mode}
          />
        </Card>
      ) : null}
      <TradeOfferMessagesPanel
          tradeOfferUuid={offer.uuid}
          status={offer.status}
      />
    </div>
  );
}

