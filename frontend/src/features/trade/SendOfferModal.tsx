import { useState } from "react";
import { ArrowRightLeft, Package } from "lucide-react";
import { Modal } from "../../components/ui/Modal";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { useMyItems } from "@/features/catalog/useCatalog.ts";
import { useCreateTradeOffer } from "./useTradeOffers";
import type { ItemDetailResponse } from "@/api/generated/types.ts";
import { toast } from "sonner";

interface SendOfferModalProps {
  isOpen: boolean;
  onClose: () => void;
  receiverItem: ItemDetailResponse;
}

export function SendOfferModal({ isOpen, onClose, receiverItem }: SendOfferModalProps) {
  const [selectedItemUuid, setSelectedItemUuid] = useState<string | null>(null);
  const [message, setMessage] = useState("");

  const { data, isLoading } = useMyItems({ size: 100, status: "ACTIVE" });
  const createMutation = useCreateTradeOffer();

  const activeItems = data?.content ?? [];

  const handleSubmit = () => {
    if (!selectedItemUuid) return;

    createMutation.mutate(
      {
        senderItemUuid: selectedItemUuid,
        receiverItemUuid: receiverItem.uuid,
        message: message || undefined,
      },
      {
        onSuccess: () => {
          toast.success("Trade offer sent!");
          setSelectedItemUuid(null);
          setMessage("");
          onClose();
        },
        onError: (error: any) => {
          const msg = error?.response?.data?.message ?? "Failed to send trade offer.";
          toast.error(msg);
        },
      },
    );
  };

  const handleClose = () => {
    setSelectedItemUuid(null);
    setMessage("");
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Send Trade Offer" size="lg">
      <div className="mb-4">
        <p className="text-sm text-slate-600 dark:text-slate-400">
          You want: <strong className="text-slate-900 dark:text-white">{receiverItem.title}</strong>{" "}
          by <strong>{receiverItem.ownerUsername}</strong>
        </p>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
          Select one of your active items to offer
        </label>

        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {!isLoading && activeItems.length === 0 && (
          <EmptyState
            icon={<Package className="size-12" />}
            title="No active items"
            description="You need at least one active item to send a trade offer."
          />
        )}

        {!isLoading && activeItems.length > 0 && (
          <div className="max-h-60 overflow-y-auto space-y-2 border border-slate-200 dark:border-slate-700 rounded-lg p-2">
            {activeItems.map((item) => (
              <button
                key={item.uuid}
                type="button"
                onClick={() => setSelectedItemUuid(item.uuid)}
                className={`w-full text-left p-3 rounded-lg border transition-colors ${
                  selectedItemUuid === item.uuid
                    ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20"
                    : "border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/50"
                }`}
              >
                <p className="font-medium text-sm text-slate-900 dark:text-slate-100">
                  {item.title}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {item.categoryName} • {item.condition}
                </p>
              </button>
            ))}
          </div>
        )}
      </div>

      {selectedItemUuid && (
        <div className="flex items-center justify-center gap-3 mb-4 text-sm text-slate-600 dark:text-slate-400">
          <span className="font-medium text-slate-900 dark:text-white">
            {activeItems.find((i) => i.uuid === selectedItemUuid)?.title}
          </span>
          <ArrowRightLeft className="size-4" />
          <span className="font-medium text-slate-900 dark:text-white">
            {receiverItem.title}
          </span>
        </div>
      )}

      <div className="mb-4">
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
          Message (optional)
        </label>
        <textarea
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          rows={3}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Add a message to your trade offer..."
        />
      </div>

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={handleClose}>
          Cancel
        </Button>
        <Button
          disabled={!selectedItemUuid}
          isLoading={createMutation.isPending}
          onClick={handleSubmit}
        >
          <ArrowRightLeft className="size-4" />
          Send Offer
        </Button>
      </div>
    </Modal>
  );
}

