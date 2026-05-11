import { useState } from "react";
import { Check, X, Ban } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Modal } from "../../components/ui/Modal";
import {
  useAcceptTradeOffer,
  useRejectTradeOffer,
  useCancelTradeOffer,
} from "./useTradeOffers";
import { toast } from "sonner";
import type { TradeOfferMode } from "@/api/generated/types.ts";

interface TradeOfferActionButtonsProps {
  offerUuid: string;
  canAccept: boolean;
  canReject: boolean;
  canCancel: boolean;
  mode?: TradeOfferMode;
}

type ConfirmAction = "accept" | "reject" | "cancel" | null;

function getAcceptDescription(mode?: TradeOfferMode): string {
  switch (mode) {
    case "GIFT":
      return "Are you sure you want to accept this gift request? Your requested item will be archived.";
    case "NEGOTIABLE":
      return "Are you sure you want to accept this negotiation? All involved items will be archived and competing offers will be rejected.";
    case "ITEM_EXCHANGE":
    default:
      return "Are you sure you want to accept this trade offer? All involved items will be archived and competing offers will be rejected.";
  }
}

export function TradeOfferActionButtons({
  offerUuid,
  canAccept,
  canReject,
  canCancel,
  mode,
}: TradeOfferActionButtonsProps) {
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);

  const acceptMutation = useAcceptTradeOffer();
  const rejectMutation = useRejectTradeOffer();
  const cancelMutation = useCancelTradeOffer();

  const isPending =
    acceptMutation.isPending || rejectMutation.isPending || cancelMutation.isPending;

  const confirmConfig = {
    accept: {
      title: "Accept Trade Offer",
      description: getAcceptDescription(mode),
      buttonLabel: "Accept",
      buttonVariant: "primary" as const,
    },
    reject: {
      title: "Reject Trade Offer",
      description: "Are you sure you want to reject this trade offer?",
      buttonLabel: "Reject",
      buttonVariant: "danger" as const,
    },
    cancel: {
      title: "Cancel Trade Offer",
      description: "Are you sure you want to cancel this trade offer?",
      buttonLabel: "Cancel Offer",
      buttonVariant: "danger" as const,
    },
  };

  const handleConfirm = () => {
    if (!confirmAction) return;

    const mutations = {
      accept: acceptMutation,
      reject: rejectMutation,
      cancel: cancelMutation,
    };

    const successMessages = {
      accept: "Trade offer accepted!",
      reject: "Trade offer rejected.",
      cancel: "Trade offer cancelled.",
    };

    mutations[confirmAction].mutate(offerUuid, {
      onSuccess: () => {
        toast.success(successMessages[confirmAction!]);
        setConfirmAction(null);
      },
      onError: () => {
        toast.error(`Failed to ${confirmAction} trade offer.`);
        setConfirmAction(null);
      },
    });
  };

  if (!canAccept && !canReject && !canCancel) return null;

  const config = confirmAction ? confirmConfig[confirmAction] : null;

  return (
    <>
      <div className="flex gap-2">
        {canAccept && (
          <Button size="sm" onClick={() => setConfirmAction("accept")}>
            <Check className="size-4" />
            Accept Trade
          </Button>
        )}
        {canReject && (
          <Button variant="danger" size="sm" onClick={() => setConfirmAction("reject")}>
            <X className="size-4" />
            Reject
          </Button>
        )}
        {canCancel && (
          <Button variant="outline" size="sm" onClick={() => setConfirmAction("cancel")}>
            <Ban className="size-4" />
            Cancel Offer
          </Button>
        )}
      </div>

      <Modal
        isOpen={!!confirmAction}
        onClose={() => setConfirmAction(null)}
        title={config?.title ?? ""}
        size="sm"
      >
        <p className="text-sm text-slate-600 dark:text-slate-400 mb-6">
          {config?.description}
        </p>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => setConfirmAction(null)}>
            Go Back
          </Button>
          <Button
            variant={config?.buttonVariant ?? "primary"}
            isLoading={isPending}
            onClick={handleConfirm}
          >
            {config?.buttonLabel}
          </Button>
        </div>
      </Modal>
    </>
  );
}

