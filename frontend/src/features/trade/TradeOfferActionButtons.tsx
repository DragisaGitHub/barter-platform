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
import { useTranslation } from "react-i18next";

interface TradeOfferActionButtonsProps {
  offerUuid: string;
  canAccept: boolean;
  canReject: boolean;
  canCancel: boolean;
  mode?: TradeOfferMode;
}

type ConfirmAction = "accept" | "reject" | "cancel" | null;

function getAcceptDescriptionKey(mode?: TradeOfferMode): string {
  switch (mode) {
    case "GIFT":
      return "actions.acceptGiftDescription";
    case "NEGOTIABLE":
      return "actions.acceptNegotiationDescription";
    case "ITEM_EXCHANGE":
    default:
      return "actions.acceptDescription";
  }
}

export function TradeOfferActionButtons({
  offerUuid,
  canAccept,
  canReject,
  canCancel,
  mode,
}: TradeOfferActionButtonsProps) {
  const { t } = useTranslation(["trade", "common"]);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);

  const acceptMutation = useAcceptTradeOffer();
  const rejectMutation = useRejectTradeOffer();
  const cancelMutation = useCancelTradeOffer();

  const isPending =
    acceptMutation.isPending || rejectMutation.isPending || cancelMutation.isPending;

  const confirmConfig = {
    accept: {
      title: t("trade:actions.acceptTitle"),
      description: t(`trade:${getAcceptDescriptionKey(mode)}`),
      buttonLabel: t("trade:actions.accept"),
      buttonVariant: "primary" as const,
    },
    reject: {
      title: t("trade:actions.rejectTitle"),
      description: t("trade:actions.rejectDescription"),
      buttonLabel: t("trade:actions.reject"),
      buttonVariant: "danger" as const,
    },
    cancel: {
      title: t("trade:actions.cancelTitle"),
      description: t("trade:actions.cancelDescription"),
      buttonLabel: t("trade:actions.cancelOffer"),
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
      accept: t("trade:actions.acceptSuccess"),
      reject: t("trade:actions.rejectSuccess"),
      cancel: t("trade:actions.cancelSuccess"),
    };

    mutations[confirmAction].mutate(offerUuid, {
      onSuccess: () => {
        toast.success(successMessages[confirmAction!]);
        setConfirmAction(null);
      },
      onError: () => {
        toast.error(t("trade:actions.error", { action: t(`trade:actions.${confirmAction}`) }));
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
            {t("trade:actions.acceptTrade")}
          </Button>
        )}
        {canReject && (
          <Button variant="danger" size="sm" onClick={() => setConfirmAction("reject")}>
            <X className="size-4" />
            {t("trade:actions.reject")}
          </Button>
        )}
        {canCancel && (
          <Button variant="outline" size="sm" onClick={() => setConfirmAction("cancel")}>
            <Ban className="size-4" />
            {t("trade:actions.cancelOffer")}
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
            {t("common:goBack")}
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

