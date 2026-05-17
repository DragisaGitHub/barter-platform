import { useState } from "react";
import { MessageSquareWarning, ThumbsDown, ThumbsUp } from "lucide-react";
import type { TradeOfferResponse, TradeOfferSummaryResponse } from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { TradeReviewDialog } from "./TradeReviewDialog";
import { useTranslation } from "react-i18next";

interface TradeReviewSectionProps {
  offer: TradeOfferResponse | TradeOfferSummaryResponse;
  currentUserUuid: string;
  compact?: boolean;
}

function getCounterpartyUsername(offer: TradeOfferResponse | TradeOfferSummaryResponse, currentUserUuid: string) {
  return offer.sender.uuid === currentUserUuid ? offer.receiver.username : offer.sender.username;
}

function ReviewStatusLine({ offer }: { offer: TradeOfferResponse | TradeOfferSummaryResponse }) {
  const { t } = useTranslation("trade");

  if (offer.currentUserHasReviewed) {
    return <span>{t("reviews.youReviewed")}</span>;
  }

  if (offer.canCurrentUserReview) {
    return <span>{t("reviews.waitingForYourReview")}</span>;
  }

  return <span>{t("reviews.unavailable")}</span>;
}

export function TradeReviewSection({ offer, currentUserUuid, compact = false }: TradeReviewSectionProps) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const { t } = useTranslation("trade");
  const counterpartyUsername = getCounterpartyUsername(offer, currentUserUuid);
  const completed = offer.status === "COMPLETED";

  if (!completed) {
    return null;
  }

  return (
    <div className={compact ? undefined : "mb-6"}>
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2 text-slate-700">
            {offer.currentUserHasReviewed ? (
              <ThumbsUp className="size-4 text-emerald-600" />
            ) : offer.canCurrentUserReview ? (
              <MessageSquareWarning className="size-4 text-amber-600" />
            ) : (
              <ThumbsDown className="size-4 text-slate-400" />
            )}
            <span className="font-medium"><ReviewStatusLine offer={offer} /></span>
            {offer.counterpartyHasReviewed ? (
              <span className="rounded-full bg-white px-2 py-0.5 text-xs font-medium text-slate-600 ring-1 ring-slate-200">
                {t("reviews.counterpartyReviewed")}
              </span>
            ) : null}
          </div>
          {offer.canCurrentUserReview ? (
            <Button size="sm" onClick={() => setDialogOpen(true)}>
              {t("reviews.reviewTrade")}
            </Button>
          ) : null}
        </div>
      </div>

      <TradeReviewDialog
        tradeOfferUuid={offer.uuid}
        counterpartyUsername={counterpartyUsername}
        isOpen={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />
    </div>
  );
}

