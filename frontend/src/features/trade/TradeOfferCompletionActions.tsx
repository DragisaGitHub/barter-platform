import { CheckCircle2, Clock3 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { toast } from "sonner";
import { useConfirmTradeOfferCompletion } from "./useTradeOffers";
import type { TradeOfferStatus } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

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
  const { t } = useTranslation("trade");
  const confirmMutation = useConfirmTradeOfferCompletion();

  if (offer.status !== "ACCEPTED" && offer.status !== "COMPLETED") {
    return null;
  }

  const isSender = offer.sender.uuid === currentUserUuid;
  const senderLabel = isSender ? t("you") : offer.sender.username;
  const receiverLabel = isSender ? offer.receiver.username : t("you");
  const currentUserCompletionConfirmed = Boolean(offer.currentUserCompletionConfirmed);
  const canConfirmCompletion = Boolean(offer.canConfirmCompletion);

  const handleConfirm = () => {
    confirmMutation.mutate(offer.uuid, {
      onSuccess: (updatedOffer) => {
        if (updatedOffer.status === "COMPLETED") {
          toast.success(t("completion.completedToast"));
          return;
        }
        toast.success(t("completion.confirmedToast"));
      },
      onError: () => {
        toast.error(t("completion.errorToast"));
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
              {offer.status === "COMPLETED" ? t("completion.tradeCompleted") : t("status.awaitingCompletion")}
            </p>
          </div>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {offer.status === "COMPLETED"
              ? t("completion.bothConfirmed", { date: offer.completedAt ? t("completion.onDate", { date: formatDateTime(offer.completedAt) }) : "" })
              : currentUserCompletionConfirmed
                ? t("completion.waitingForOther")
                : t("completion.confirmInstructions")}
          </p>
        </div>

        {offer.status === "COMPLETED" ? (
          <Badge variant="success">{t("status.completed")}</Badge>
        ) : currentUserCompletionConfirmed ? (
          <Badge variant="secondary">{t("completion.waitingBadge")}</Badge>
        ) : null}
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {senderLabel}
          </p>
          <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">
            {offer.senderCompletedAt ? t("completion.confirmedAt", { date: formatDateTime(offer.senderCompletedAt) }) : t("completion.notConfirmedYet")}
          </p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            {receiverLabel}
          </p>
          <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">
            {offer.receiverCompletedAt ? t("completion.confirmedAt", { date: formatDateTime(offer.receiverCompletedAt) }) : t("completion.notConfirmedYet")}
          </p>
        </div>
      </div>

      {canConfirmCompletion && offer.status === "ACCEPTED" && (
        <div className="mt-3 flex justify-start">
          <Button onClick={handleConfirm} isLoading={confirmMutation.isPending} size={compact ? "sm" : "md"}>
            {t("completion.confirmButton")}
          </Button>
        </div>
      )}
    </div>
  );
}

