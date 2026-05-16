import { CheckCircle2, Clock3 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { toast } from "sonner";
import { useConfirmTradeOfferCompletion } from "./useTradeOffers";
import type { TradeOfferStatus } from "@/api/generated/types.ts";

interface CompletionOfferLike {
  uuid: string;
  status: TradeOfferStatus;
  sender: { uuid: string; username: string };
  receiver: { uuid: string; username: string };
  senderCompletedAt?: string | null;
  receiverCompletedAt?: string | null;
  completedAt?: string | null;
  currentUserCompletionConfirmed?: boolean;
  canConfirmCompletion?: boolean;
}

interface TradeOfferCompletionActionsProps {
  offer: CompletionOfferLike;
  currentUserUuid: string;
  compact?: boolean;
}

function formatDateTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : null;
}

export function TradeOfferCompletionActions({
  offer,
  currentUserUuid,
  compact = false,
}: TradeOfferCompletionActionsProps) {
  if (offer.status !== "ACCEPTED" && offer.status !== "COMPLETED") {
    return null;
  }

  const confirmMutation = useConfirmTradeOfferCompletion();
  const isSender = offer.sender.uuid === currentUserUuid;
  const senderLabel = isSender ? "You" : offer.sender.username;
  const receiverLabel = isSender ? offer.receiver.username : "You";
  const currentUserCompletionConfirmed = Boolean(offer.currentUserCompletionConfirmed);
  const canConfirmCompletion = Boolean(offer.canConfirmCompletion);

  const handleConfirm = () => {
    confirmMutation.mutate(offer.uuid, {
      onSuccess: (updatedOffer) => {
        if (updatedOffer.status === "COMPLETED") {
          toast.success("Trade marked as completed.");
          return;
        }
        toast.success("Your completion confirmation was recorded.");
      },
      onError: () => {
        toast.error("Failed to confirm trade completion.");
      },
    });
  };

  return (
    <div
      className={compact
        ? "rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/60"
        : "rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60"}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            {offer.status === "COMPLETED" ? (
              <CheckCircle2 className="size-4 text-emerald-500" />
            ) : (
              <Clock3 className="size-4 text-amber-500" />
            )}
            <p className="font-medium text-slate-900 dark:text-slate-100">
              {offer.status === "COMPLETED" ? "Trade completed" : "Awaiting completion"}
            </p>
          </div>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {offer.status === "COMPLETED"
              ? `Both participants confirmed the exchange${offer.completedAt ? ` on ${formatDateTime(offer.completedAt)}` : ""}.`
              : currentUserCompletionConfirmed
                ? "You confirmed completion. Waiting for the other participant to confirm."
                : "The trade is agreed and the items are archived. Confirm once the exchange is complete on your side."}
          </p>
        </div>

        {offer.status === "COMPLETED" ? (
          <Badge variant="success">Completed</Badge>
        ) : currentUserCompletionConfirmed ? (
          <Badge variant="secondary">Waiting for the other participant</Badge>
        ) : null}
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {senderLabel}
          </p>
          <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">
            {offer.senderCompletedAt ? `Confirmed ${formatDateTime(offer.senderCompletedAt)}` : "Not confirmed yet"}
          </p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {receiverLabel}
          </p>
          <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">
            {offer.receiverCompletedAt ? `Confirmed ${formatDateTime(offer.receiverCompletedAt)}` : "Not confirmed yet"}
          </p>
        </div>
      </div>

      {canConfirmCompletion && offer.status === "ACCEPTED" && (
        <div className="mt-3 flex justify-start">
          <Button onClick={handleConfirm} isLoading={confirmMutation.isPending} size={compact ? "sm" : "md"}>
            Confirm completion
          </Button>
        </div>
      )}
    </div>
  );
}

