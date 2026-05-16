import { useEffect, useMemo, useState } from "react";
import type {
  AdminRemoveListingRequest,
  AdminRestoreListingRequest,
  ListingModerationReasonCode,
} from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { useTranslation } from "react-i18next";

const REASON_OPTIONS: { value: ListingModerationReasonCode; labelKey: string }[] = [
  { value: "POLICY_VIOLATION", labelKey: "moderation.reasons.POLICY_VIOLATION" },
  { value: "PROHIBITED_ITEM", labelKey: "moderation.reasons.PROHIBITED_ITEM" },
  { value: "MISLEADING_CONTENT", labelKey: "moderation.reasons.MISLEADING_CONTENT" },
  { value: "DUPLICATE_LISTING", labelKey: "moderation.reasons.DUPLICATE_LISTING" },
  { value: "SPAM", labelKey: "moderation.reasons.SPAM" },
  { value: "SAFETY_CONCERN", labelKey: "moderation.reasons.SAFETY_CONCERN" },
  { value: "OWNER_REQUEST", labelKey: "moderation.reasons.OWNER_REQUEST" },
  { value: "OTHER", labelKey: "moderation.reasons.OTHER" },
];

type ModerationMode = "remove" | "restore";

type ModerationPayload = AdminRemoveListingRequest | AdminRestoreListingRequest;

interface AdminListingModerationDialogProps {
  isOpen: boolean;
  mode: ModerationMode;
  listingTitle: string;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (payload: ModerationPayload) => void;
}

export function AdminListingModerationDialog({
  isOpen,
  mode,
  listingTitle,
  isSubmitting = false,
  onClose,
  onSubmit,
}: AdminListingModerationDialogProps) {
  const { t } = useTranslation(["admin", "common"]);
  const [reasonCode, setReasonCode] = useState<ListingModerationReasonCode>("POLICY_VIOLATION");
  const [userMessage, setUserMessage] = useState("");
  const [internalNote, setInternalNote] = useState("");

  useEffect(() => {
    if (!isOpen) {
      setReasonCode(mode === "remove" ? "POLICY_VIOLATION" : "OTHER");
      setUserMessage("");
      setInternalNote("");
    }
  }, [isOpen, mode]);

  const dialogTitle = mode === "remove" ? t("admin:listingDetail.removeListing") : t("admin:listingDetail.restoreListing");
  const submitLabel = dialogTitle;
  const submitVariant = mode === "remove" ? "danger" : "primary";
  const description = useMemo(
    () =>
      mode === "remove"
        ? t("admin:moderationDialog.removeDescription", { title: listingTitle })
        : t("admin:moderationDialog.restoreDescription", { title: listingTitle }),
    [listingTitle, mode, t]
  );

  const handleSubmit = () => {
    onSubmit({
      reasonCode,
      userMessage: userMessage.trim() || undefined,
      internalNote: internalNote.trim() || undefined,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={dialogTitle} size="md">
      <div className="space-y-4">
        <p className="text-sm leading-6 text-slate-600 dark:text-slate-300">{description}</p>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">{t("admin:moderationDialog.reason")}</label>
          <select
            value={reasonCode}
            onChange={(event) => setReasonCode(event.target.value as ListingModerationReasonCode)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {REASON_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {t(`admin:${option.labelKey}`)}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">{t("admin:moderationDialog.userMessage")}</label>
          <textarea
            rows={4}
            value={userMessage}
            onChange={(event) => setUserMessage(event.target.value)}
            placeholder={t("admin:moderationDialog.userMessagePlaceholder")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">{t("admin:moderationDialog.internalNote")}</label>
          <textarea
            rows={4}
            value={internalNote}
            onChange={(event) => setInternalNote(event.target.value)}
            placeholder={t("admin:moderationDialog.internalNotePlaceholder")}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </div>

        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            {t("common:cancel")}
          </Button>
          <Button variant={submitVariant} onClick={handleSubmit} isLoading={isSubmitting}>
            {submitLabel}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

