import { useMemo, useState } from "react";
import { AlertTriangle, ThumbsDown, ThumbsUp } from "lucide-react";
import type {
  CreateTradeReviewRequest,
  TradeReviewNegativeReason,
  TradeReviewRating,
} from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { parseApiError } from "@/utils/parseApiError.ts";
import { useCreateTradeReview } from "./useTradeOffers";
import { useTranslation } from "react-i18next";

interface TradeReviewDialogProps {
  tradeOfferUuid: string;
  counterpartyUsername: string;
  isOpen: boolean;
  onClose: () => void;
}

const NEGATIVE_REASON_OPTIONS: { value: TradeReviewNegativeReason; labelKey: string }[] = [
  { value: "NO_SHOW", labelKey: "reviews.reasons.noShow" },
  { value: "ITEM_NOT_AS_DESCRIBED", labelKey: "reviews.reasons.itemNotAsDescribed" },
  { value: "DAMAGED_OR_UNSAFE_ITEM", labelKey: "reviews.reasons.damagedOrUnsafeItem" },
  { value: "RUDE_OR_ABUSIVE_BEHAVIOR", labelKey: "reviews.reasons.rudeOrAbusiveBehavior" },
  { value: "SPAM_OR_SCAM_BEHAVIOR", labelKey: "reviews.reasons.spamOrScamBehavior" },
  { value: "OTHER", labelKey: "reviews.reasons.other" },
];

export function negativeReasonTranslationKey(reason?: TradeReviewNegativeReason | null) {
  if (!reason) {
    return "";
  }

  return NEGATIVE_REASON_OPTIONS.find((option) => option.value === reason)?.labelKey ?? "";
}

export function TradeReviewDialog({ tradeOfferUuid, counterpartyUsername, isOpen, onClose }: TradeReviewDialogProps) {
  const { t } = useTranslation(["trade", "common"]);
  const [rating, setRating] = useState<TradeReviewRating>("POSITIVE");
  const [negativeReason, setNegativeReason] = useState<TradeReviewNegativeReason | "">("");
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const createReview = useCreateTradeReview();

  const validationError = useMemo(() => {
    if (rating === "NEGATIVE" && !negativeReason) {
      return t("trade:reviews.validation.chooseReason");
    }
    if (rating === "NEGATIVE" && negativeReason === "OTHER" && !comment.trim()) {
      return t("trade:reviews.validation.commentForOther");
    }
    return null;
  }, [comment, negativeReason, rating, t]);

  const resetAndClose = () => {
    if (createReview.isPending) {
      return;
    }
    setRating("POSITIVE");
    setNegativeReason("");
    setComment("");
    setError(null);
    onClose();
  };

  const handleSubmit = async () => {
    setError(null);
    if (validationError) {
      setError(validationError);
      return;
    }

    const payload: CreateTradeReviewRequest = {
      rating,
      negativeReason: rating === "NEGATIVE" ? (negativeReason as TradeReviewNegativeReason) : null,
      comment: comment.trim() ? comment.trim() : null,
    };

    try {
      await createReview.mutateAsync({ tradeOfferUuid, data: payload });
      resetAndClose();
    } catch (err) {
      const apiError = parseApiError(err);
      const isGenericUnexpectedError = apiError.toLowerCase().includes("unexpected")
        || apiError.toLowerCase().includes("internal error");
      setError(isGenericUnexpectedError ? t("trade:reviews.submitError") : apiError);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={resetAndClose} title={t("trade:reviews.dialogTitle", { username: counterpartyUsername })} size="md">
      <div className="space-y-5">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          {t("trade:reviews.finalNotice")}
        </p>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => {
              setRating("POSITIVE");
              setNegativeReason("");
              setError(null);
            }}
            className={`rounded-xl border p-4 text-left transition ${
              rating === "POSITIVE"
                ? "border-emerald-300 bg-emerald-50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100"
                : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            }`}
          >
            <ThumbsUp className="mb-2 size-5" />
            <div className="font-semibold">{t("trade:reviews.positive")}</div>
            <div className="mt-1 text-xs opacity-80">{t("trade:reviews.positiveDescription")}</div>
          </button>
          <button
            type="button"
            onClick={() => {
              setRating("NEGATIVE");
              setError(null);
            }}
            className={`rounded-xl border p-4 text-left transition ${
              rating === "NEGATIVE"
                ? "border-rose-300 bg-rose-50 text-rose-900 dark:border-rose-800 dark:bg-rose-950/30 dark:text-rose-100"
                : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            }`}
          >
            <ThumbsDown className="mb-2 size-5" />
            <div className="font-semibold">{t("trade:reviews.negative")}</div>
            <div className="mt-1 text-xs opacity-80">{t("trade:reviews.negativeDescription")}</div>
          </button>
        </div>

        {rating === "NEGATIVE" ? (
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">{t("trade:reviews.reason")}</span>
            <select
              value={negativeReason}
              onChange={(event) => setNegativeReason(event.target.value as TradeReviewNegativeReason | "")}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              <option value="">{t("trade:reviews.selectReason")}</option>
              {NEGATIVE_REASON_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {t(`trade:${option.labelKey}`)}
                </option>
              ))}
            </select>
          </label>
        ) : null}

        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t("trade:reviews.comment")} {rating === "NEGATIVE" && negativeReason === "OTHER" ? t("common:requiredParenthesized") : t("common:optionalParenthesized")}
          </span>
          <textarea
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            rows={4}
            maxLength={2000}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            placeholder={
              rating === "POSITIVE"
                ? t("trade:reviews.positivePlaceholder")
                : t("trade:reviews.negativePlaceholder")
            }
          />
        </label>

        {error ? (
          <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/20 dark:text-red-300">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" />
            <span>{error}</span>
          </div>
        ) : null}

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={resetAndClose} disabled={createReview.isPending}>
            {t("common:cancel")}
          </Button>
          <Button type="button" onClick={handleSubmit} isLoading={createReview.isPending}>
            {t("trade:reviews.submit")}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

