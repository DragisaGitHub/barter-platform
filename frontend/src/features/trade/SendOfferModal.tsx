import React, { useState, useMemo, useEffect } from "react";
import { ArrowRightLeft, Gift, MessageSquare, Package, Search, X } from "lucide-react";
import { Modal } from "../../components/ui/Modal";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { useMyItems } from "@/features/catalog/useCatalog.ts";
import { useCreateTradeOffer } from "./useTradeOffers";
import type { ItemDetailResponse, TradeOfferMode } from "@/api/generated/types.ts";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";

interface SendOfferModalProps {
  isOpen: boolean;
  onClose: () => void;
  receiverItem: ItemDetailResponse;
}

const MODE_OPTIONS: { value: TradeOfferMode; labelKey: string; icon: React.ReactNode; descriptionKey: string }[] = [
  {
    value: "ITEM_EXCHANGE",
    labelKey: "mode.itemExchange",
    icon: <ArrowRightLeft className="size-5" />,
    descriptionKey: "sendOffer.modeDescription.itemExchange",
  },
  {
    value: "GIFT",
    labelKey: "mode.gift",
    icon: <Gift className="size-5" />,
    descriptionKey: "sendOffer.modeDescription.gift",
  },
  {
    value: "NEGOTIABLE",
    labelKey: "mode.negotiable",
    icon: <MessageSquare className="size-5" />,
    descriptionKey: "sendOffer.modeDescription.negotiable",
  },
];

export function SendOfferModal({ isOpen, onClose, receiverItem }: SendOfferModalProps) {
  const { t } = useTranslation(["trade", "common", "catalog"]);
  const [mode, setMode] = useState<TradeOfferMode>("ITEM_EXCHANGE");
  const [selectedItemUuids, setSelectedItemUuids] = useState<string[]>([]);
  const [message, setMessage] = useState("");
  const [itemSearch, setItemSearch] = useState("");

  const { data, isLoading } = useMyItems({ size: 100, status: "ACTIVE" });
  const createMutation = useCreateTradeOffer();

  const activeItems = data?.content ?? [];

  // Filter items by search
  const filteredItems = useMemo(() => {
    if (!itemSearch.trim()) return activeItems;
    const q = itemSearch.toLowerCase();
    return activeItems.filter(
      (item) =>
        item.title.toLowerCase().includes(q) ||
        item.categoryName?.toLowerCase().includes(q),
    );
  }, [activeItems, itemSearch]);

  // Clear selected items when switching to GIFT
  useEffect(() => {
    if (mode === "GIFT") {
      setSelectedItemUuids([]);
    }
  }, [mode]);

  const toggleItem = (uuid: string) => {
    setSelectedItemUuids((prev) =>
      prev.includes(uuid) ? prev.filter((u) => u !== uuid) : [...prev, uuid],
    );
  };

  const removeItem = (uuid: string) => {
    setSelectedItemUuids((prev) => prev.filter((u) => u !== uuid));
  };

  // Validation
  const messageRequired = mode === "GIFT" || mode === "NEGOTIABLE";
  const itemsRequired = mode === "ITEM_EXCHANGE";
  const itemsForbidden = mode === "GIFT";

  const isFormValid = useMemo(() => {
    if (itemsRequired && selectedItemUuids.length === 0) return false;
    return !(messageRequired && !message.trim());

  }, [mode, selectedItemUuids, message, itemsRequired, messageRequired]);

  const handleSubmit = () => {
    if (!isFormValid) return;

    createMutation.mutate(
      {
        receiverItemUuid: receiverItem.uuid,
        senderItemUuids: selectedItemUuids.length > 0 ? selectedItemUuids : undefined,
        mode,
        message: message.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast.success(t("trade:sendOffer.toast.success"));
          resetAndClose();
        },
        onError: (error: any) => {
          const msg = error?.response?.data?.message ?? t("trade:sendOffer.toast.error");
          toast.error(msg);
        },
      },
    );
  };

  const resetAndClose = () => {
    setMode("ITEM_EXCHANGE");
    setSelectedItemUuids([]);
    setMessage("");
    setItemSearch("");
    onClose();
  };

  const selectedItemDetails = activeItems.filter((i) => selectedItemUuids.includes(i.uuid));

  return (
    <Modal isOpen={isOpen} onClose={resetAndClose} title={t("trade:sendOffer.title")} size="lg">
      {/* ── You Want (requested item) ───────────────────────── */}
      <div className="mb-5 p-3 rounded-lg bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-800">
        <p className="text-xs font-medium text-indigo-600 dark:text-indigo-400 uppercase tracking-wider mb-1">
          {t("trade:sendOffer.youWant")}
        </p>
        <p className="font-semibold text-slate-900 dark:text-white">{receiverItem.title}</p>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          {t("trade:byUser", { username: receiverItem.ownerUsername })} • {receiverItem.category?.name}
        </p>
        {receiverItem.listingMode && receiverItem.listingMode !== "SINGLE" ? (
          <p className="mt-1 text-xs font-medium text-indigo-700 dark:text-indigo-300">
            {t(`catalog:listingMode.badge.${receiverItem.listingMode}`)} · {t("catalog:itemDetail.entryCount", { count: receiverItem.entries?.length ?? 0 })}
          </p>
        ) : null}
      </div>

      {/* ── Trade Mode Selector ─────────────────────────────── */}
      <div className="mb-5">
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
          {t("trade:sendOffer.tradeMode")}
        </label>
        <div className="grid grid-cols-3 gap-2">
          {MODE_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setMode(opt.value)}
              className={`flex flex-col items-center gap-1.5 p-3 rounded-lg border text-center transition-colors ${
                mode === opt.value
                  ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-300"
                  : "border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/50 text-slate-600 dark:text-slate-400"
              }`}
            >
              {opt.icon}
              <span className="text-xs font-medium">{t(`trade:${opt.labelKey}`)}</span>
            </button>
          ))}
        </div>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
          {t(`trade:${MODE_OPTIONS.find((o) => o.value === mode)?.descriptionKey}`)}
        </p>
      </div>

      {/* ── Item Selector (not for GIFT) ────────────────────── */}
      {!itemsForbidden && (
        <div className="mb-5">
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
            {t("trade:sendOffer.youOffer")}
            {itemsRequired && <span className="text-red-500 ml-1">*</span>}
            {!itemsRequired && <span className="text-slate-400 ml-1">{t("common:optionalParenthesized")}</span>}
          </label>

          {/* Selected items chips */}
          {selectedItemDetails.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-3">
              {selectedItemDetails.map((item) => (
                <span
                  key={item.uuid}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300"
                >
                  {item.title}
                  <button
                    type="button"
                    onClick={() => removeItem(item.uuid)}
                    className="hover:text-red-600 dark:hover:text-red-400"
                  >
                    <X className="size-3" />
                  </button>
                </span>
              ))}
            </div>
          )}

          {selectedItemUuids.length === 0 && (
            <p className="text-xs text-slate-400 dark:text-slate-500 mb-3 italic">
              {t("trade:sendOffer.noOfferedItemsSelected")}
            </p>
          )}

          {/* Search box */}
          <div className="relative mb-2">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-slate-400" />
            <input
              type="text"
              className="w-full pl-9 pr-3 py-2 rounded-lg border border-slate-300 bg-white text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              placeholder={t("trade:sendOffer.searchYourItems")}
              value={itemSearch}
              onChange={(e) => setItemSearch(e.target.value)}
            />
          </div>

          {isLoading && (
            <div className="flex justify-center py-6">
              <Spinner />
            </div>
          )}

          {!isLoading && activeItems.length === 0 && (
            <EmptyState
              icon={<Package className="size-10" />}
              title={t("trade:sendOffer.noActiveItems")}
              description={t("trade:sendOffer.noActiveItemsDescription")}
            />
          )}

          {!isLoading && activeItems.length > 0 && (
            <div className="max-h-48 overflow-y-auto space-y-1.5 border border-slate-200 dark:border-slate-700 rounded-lg p-2">
              {filteredItems.map((item) => {
                const isSelected = selectedItemUuids.includes(item.uuid);
                return (
                  <button
                    key={item.uuid}
                    type="button"
                    onClick={() => toggleItem(item.uuid)}
                    className={`w-full text-left p-2.5 rounded-lg border transition-colors flex items-center gap-3 ${
                      isSelected
                        ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20"
                        : "border-transparent hover:bg-slate-50 dark:hover:bg-slate-700/50"
                    }`}
                  >
                    <div
                      className={`size-5 rounded border-2 flex items-center justify-center shrink-0 transition-colors ${
                        isSelected
                          ? "border-indigo-500 bg-indigo-500 text-white"
                          : "border-slate-300 dark:border-slate-600"
                      }`}
                    >
                      {isSelected && (
                        <svg className="size-3" viewBox="0 0 12 12" fill="none">
                          <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-sm text-slate-900 dark:text-slate-100 truncate">
                        {item.title}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                         {item.categoryName} • {t(`catalog:condition.${item.condition === "LIKE_NEW" ? "likeNew" : item.condition === "FOR_PARTS" ? "forParts" : item.condition.toLowerCase()}`)}
                      </p>
                      {item.listingMode && item.listingMode !== "SINGLE" ? (
                        <p className="mt-0.5 text-xs font-medium text-amber-700 dark:text-amber-300">
                          {t(`catalog:listingMode.badge.${item.listingMode}`)} · {item.listingMode === "BUNDLE"
                            ? t("catalog:itemCard.bundleEntryCount", { count: item.entryCount ?? 0 })
                            : t("catalog:itemCard.pickAnyEntryCount", { count: item.entryCount ?? 0 })}
                        </p>
                      ) : null}
                    </div>
                  </button>
                );
              })}
              {filteredItems.length === 0 && (
                <p className="text-xs text-slate-400 text-center py-4">{t("trade:sendOffer.noItemsMatchSearch")}</p>
              )}
            </div>
          )}
        </div>
      )}

      {/* ── GIFT mode: explain no items ─────────────────────── */}
      {itemsForbidden && (
        <div className="mb-5 p-3 rounded-lg bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800">
          <p className="text-sm text-emerald-700 dark:text-emerald-300">
            <Gift className="size-4 inline mr-1 -mt-0.5" />
            {t("trade:sendOffer.giftModeExplanation")}
          </p>
        </div>
      )}

      {/* ── Message ─────────────────────────────────────────── */}
      <div className="mb-5">
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
          {t("trade:message")}
          {messageRequired ? (
            <span className="text-red-500 ml-1">*</span>
          ) : (
            <span className="text-slate-400 ml-1">{t("common:optionalParenthesized")}</span>
          )}
        </label>
        <textarea
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          rows={3}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder={
            mode === "GIFT"
              ? t("trade:sendOffer.placeholders.gift")
              : mode === "NEGOTIABLE"
                ? t("trade:sendOffer.placeholders.negotiable")
                : t("trade:sendOffer.placeholders.itemExchange")
          }
          maxLength={1000}
        />
        {messageRequired && !message.trim() && (
          <p className="text-xs text-red-500 mt-1">{t("trade:sendOffer.messageRequired", { mode: mode === "GIFT" ? t("trade:mode.gift") : t("trade:mode.negotiable") })}</p>
        )}
      </div>

      {/* ── Footer ──────────────────────────────────────────── */}
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={resetAndClose}>
          {t("common:cancel")}
        </Button>
        <Button
          disabled={!isFormValid}
          isLoading={createMutation.isPending}
          onClick={handleSubmit}
        >
          {mode === "ITEM_EXCHANGE" && <ArrowRightLeft className="size-4" />}
          {mode === "GIFT" && <Gift className="size-4" />}
          {mode === "NEGOTIABLE" && <MessageSquare className="size-4" />}
          {t("trade:sendOffer.submit")}
        </Button>
      </div>
    </Modal>
  );
}
