import { useState } from "react";
import { MessageSquareWarning, ThumbsDown, ThumbsUp } from "lucide-react";
import type { TradeOfferResponse, TradeOfferSummaryResponse } from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { TradeReviewDialog, formatNegativeReason } from "./TradeReviewDialog";

interface TradeReviewSectionProps {
  offer: TradeOfferResponse | TradeOfferSummaryResponse;
  currentUserUuid: string;
  compact?: boolean;
}

function getCounterpartyUsername(offer: TradeOfferResponse | TradeOfferSummaryResponse, currentUserUuid: string) {
  return offer.sender.uuid === currentUserUuid ? offer.receiver.username : offer.sender.username;
}

function ReviewStatusLine({ offer }: { offer: TradeOfferResponse | TradeOfferSummaryResponse }) {
  if (offer.currentUserHasReviewed) {
    return <span>You reviewed this trade</span>;
  }

  if (offer.canCurrentUserReview) {
    return <span>Waiting for your review</span>;
  }

  return <span>Review unavailable</span>;
}

export function TradeReviewSection({ offer, currentUserUuid, compact = false }: TradeReviewSectionProps) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const counterpartyUsername = getCounterpartyUsername(offer, currentUserUuid);
  const completed = offer.status === "COMPLETED";

  if (!completed) {
    return null;
  }

  if (compact) {
    return (
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm dark:border-slate-700 dark:bg-slate-800/60">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-slate-700 dark:text-slate-200">
            {offer.currentUserHasReviewed ? (
              <ThumbsUp className="size-4 text-emerald-500" />
            ) : offer.canCurrentUserReview ? (
              <MessageSquareWarning className="size-4 text-amber-500" />
            ) : (
              <ThumbsDown className="size-4 text-slate-400" />
            )}
            <ReviewStatusLine offer={offer} />
            {offer.counterpartyHasReviewed ? <span className="text-slate-400">• Counterparty has reviewed</span> : null}
          </div>
          {offer.canCurrentUserReview ? (
            <Button size="sm" onClick={() => setDialogOpen(true)}>
              Review trade
            </Button>
          ) : null}
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

  const detailOffer = offer as TradeOfferResponse;

  return (
    <Card className="mb-6 border-indigo-100 bg-indigo-50/40 dark:border-indigo-900/50 dark:bg-indigo-950/10">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="font-semibold text-slate-900 dark:text-slate-100">Trade review</h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
            Completed trades can be reviewed once by each participant.
          </p>
          <div className="mt-3 flex flex-wrap gap-2 text-xs font-medium">
            <span className="rounded-full bg-white px-3 py-1 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900 dark:text-slate-200 dark:ring-slate-700">
              <ReviewStatusLine offer={offer} />
            </span>
            {offer.counterpartyHasReviewed ? (
              <span className="rounded-full bg-white px-3 py-1 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900 dark:text-slate-200 dark:ring-slate-700">
                Counterparty has reviewed
              </span>
            ) : null}
          </div>
        </div>
        {offer.canCurrentUserReview ? (
          <Button onClick={() => setDialogOpen(true)}>Review {counterpartyUsername}</Button>
        ) : null}
      </div>

      {detailOffer.currentUserReview ? (
        <div className="mt-4 rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/60">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Your review</p>
          <p className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
            {detailOffer.currentUserReview.rating === "POSITIVE" ? "Positive" : "Negative"}
            {detailOffer.currentUserReview.negativeReason ? ` • ${formatNegativeReason(detailOffer.currentUserReview.negativeReason)}` : ""}
          </p>
          {detailOffer.currentUserReview.comment ? (
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{detailOffer.currentUserReview.comment}</p>
          ) : null}
        </div>
      ) : null}

      <TradeReviewDialog
        tradeOfferUuid={offer.uuid}
        counterpartyUsername={counterpartyUsername}
        isOpen={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />
    </Card>
  );
}

