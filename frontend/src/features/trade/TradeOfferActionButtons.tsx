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

interface TradeOfferActionButtonsProps {
  offerUuid: string;
  canAccept: boolean;
  canReject: boolean;
  canCancel: boolean;
}

type ConfirmAction = "accept" | "reject" | "cancel" | null;

const confirmConfig = {
  accept: {
    title: "Accept Trade Offer",
    description:
      "Are you sure you want to accept this trade offer? Both items will be archived and other competing offers will be rejected.",
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

export function TradeOfferActionButtons({
  offerUuid,
  canAccept,
  canReject,
  canCancel,
}: TradeOfferActionButtonsProps) {
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);

  const acceptMutation = useAcceptTradeOffer();
  const rejectMutation = useRejectTradeOffer();
  const cancelMutation = useCancelTradeOffer();

  const isPending =
    acceptMutation.isPending || rejectMutation.isPending || cancelMutation.isPending;

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
            Accept
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
            Cancel
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

