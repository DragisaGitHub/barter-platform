import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { ArrowRightLeft, Gift, MessageSquare } from "lucide-react";
import type { TradeOfferSummaryResponse, TradeOfferStatus, TradeOfferMode } from "@/api/generated/types.ts";
import { Card } from "../../components/ui/Card";
import { TradeOfferStatusBadge } from "./TradeOfferStatusBadge";
import { TradeOfferModeBadge } from "./TradeOfferModeBadge";
import { TradeOfferActionButtons } from "./TradeOfferActionButtons";
import { TradeOfferCompletionActions } from "./TradeOfferCompletionActions";
import { TradeReviewSection } from "./TradeReviewSection";
import { useTranslation } from "react-i18next";

interface TradeOfferCardProps {
  offer: TradeOfferSummaryResponse;
  currentUserUuid: string;
}

const STATUS_EXPLANATION: Record<TradeOfferStatus, string> = {
  PENDING: "card.statusExplanation.pending",
  ACCEPTED: "card.statusExplanation.accepted",
  COMPLETED: "card.statusExplanation.completed",
  REJECTED: "card.statusExplanation.rejected",
  CANCELLED: "card.statusExplanation.cancelled",
  EXPIRED: "card.statusExplanation.expired",
  INVALIDATED: "card.statusExplanation.invalidated",
};

function buildSummaryLine(
  offer: TradeOfferSummaryResponse,
  isSender: boolean,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const receiverItemTitle = offer.receiverItem.title;
  const senderName = offer.sender.username;
  const receiverName = offer.receiver.username;
  const offeredCount = offer.offeredItems.length;

  switch (offer.mode) {
    case "GIFT":
      return isSender
        ? t("card.summary.giftSender", { receiverName, receiverItemTitle })
        : t("card.summary.giftReceiver", { senderName, receiverItemTitle });
    case "NEGOTIABLE":
      return isSender
        ? t("card.summary.negotiableSender", { receiverName, receiverItemTitle })
        : t("card.summary.negotiableReceiver", { senderName, receiverItemTitle });
    case "ITEM_EXCHANGE":
    default:
      if (offeredCount === 1) {
        const offeredTitle = offer.offeredItems[0].title;
        return isSender
          ? t("card.summary.exchangeSingleSender", { offeredTitle, receiverName, receiverItemTitle })
          : t("card.summary.exchangeSingleReceiver", { senderName, offeredTitle, receiverItemTitle });
      }
      return isSender
        ? t("card.summary.exchangeMultipleSender", { count: offeredCount, receiverName, receiverItemTitle })
        : t("card.summary.exchangeMultipleReceiver", { senderName, count: offeredCount, receiverItemTitle });
  }
}

const MODE_ICON: Record<TradeOfferMode, ReactNode> = {
  ITEM_EXCHANGE: <ArrowRightLeft className="size-5 text-slate-400 shrink-0" />,
  GIFT: <Gift className="size-5 text-emerald-500 shrink-0" />,
  NEGOTIABLE: <MessageSquare className="size-5 text-amber-500 shrink-0" />,
};

export function TradeOfferCard({ offer, currentUserUuid }: TradeOfferCardProps) {
  const { t } = useTranslation("trade");
  const isSender = offer.sender.uuid === currentUserUuid;
  const isReceiver = offer.receiver.uuid === currentUserUuid;
  const isPending = offer.status === "PENDING";

  const summaryLine = buildSummaryLine(offer, isSender, t);

  return (
    <Card className="transition-shadow hover:shadow-md">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <TradeOfferStatusBadge status={offer.status} />
          <TradeOfferModeBadge mode={offer.mode} />
        </div>
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {new Date(offer.createdAt).toLocaleDateString()}
        </span>
      </div>

      {/* Summary line */}
      <p className="text-sm text-slate-700 dark:text-slate-300 mb-4">
        {summaryLine}
      </p>

      <Link to={`/offers/${offer.uuid}`} className="block group">
        {/* Items being traded */}
        <div className="flex items-center gap-3 mb-4">
          {/* Offered items (sender side) */}
          <div className="flex-1 min-w-0 p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50">
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wider">
              {offer.mode === "GIFT" ? t("card.noItemsOffered") : t("card.offered", { count: offer.offeredItems.length })}
            </p>
            {offer.offeredItems.length === 0 ? (
              <p className="text-xs text-slate-400 dark:text-slate-500 italic">
                {offer.mode === "GIFT" ? t("card.giftRequest") : t("card.none")}
              </p>
            ) : offer.offeredItems.length === 1 ? (
              <>
                <p className="font-medium text-slate-900 dark:text-slate-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
                  {offer.offeredItems[0].title}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  {isSender ? t("yourItem") : t("fromUser", { username: offer.sender.username })}
                </p>
              </>
            ) : (
              <div className="space-y-0.5">
                {offer.offeredItems.slice(0, 2).map((item) => (
                  <p key={item.uuid} className="text-sm text-slate-900 dark:text-slate-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
                    {item.title}
                  </p>
                ))}
                {offer.offeredItems.length > 2 && (
                  <p className="text-xs text-slate-500">{t("card.moreItems", { count: offer.offeredItems.length - 2 })}</p>
                )}
              </div>
            )}
          </div>

          {MODE_ICON[offer.mode]}

          {/* Requested item (receiver) */}
          <div className="flex-1 min-w-0 p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50">
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wider">
              {t("card.requestedItem")}
            </p>
            <p className="font-medium text-slate-900 dark:text-slate-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
              {offer.receiverItem.title}
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {isReceiver ? t("yourItem") : t("toUser", { username: offer.receiver.username })}
            </p>
          </div>
        </div>
      </Link>

      {offer.message && (
        <p className="text-sm text-slate-600 dark:text-slate-400 mb-3 italic line-clamp-2">
          "{offer.message}"
        </p>
      )}

      {/* Status explanation */}
      <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
        {t(STATUS_EXPLANATION[offer.status])}
      </p>

      <div className="mb-3">
        <TradeOfferCompletionActions
          offer={offer}
          currentUserUuid={currentUserUuid}
          compact
        />
      </div>

      <div className="mb-3">
        <TradeReviewSection offer={offer} currentUserUuid={currentUserUuid} compact />
      </div>

      {offer.respondedAt && (
        <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
          {t("card.responded", { date: new Date(offer.respondedAt).toLocaleDateString() })}
        </p>
      )}

      {/* Actions for pending incoming offers */}
      <TradeOfferActionButtons
        offerUuid={offer.uuid}
        canAccept={isReceiver && isPending}
        canReject={isReceiver && isPending}
        canCancel={isSender && isPending}
        mode={offer.mode}
      />
    </Card>
  );
}
