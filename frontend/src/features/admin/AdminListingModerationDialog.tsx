import { useEffect, useMemo, useState } from "react";
import type {
  AdminRemoveListingRequest,
  AdminRestoreListingRequest,
  ListingModerationReasonCode,
} from "@/api/generated/types.ts";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

const REASON_OPTIONS: { value: ListingModerationReasonCode; label: string }[] = [
  { value: "POLICY_VIOLATION", label: "Policy violation" },
  { value: "PROHIBITED_ITEM", label: "Prohibited item" },
  { value: "MISLEADING_CONTENT", label: "Misleading content" },
  { value: "DUPLICATE_LISTING", label: "Duplicate listing" },
  { value: "SPAM", label: "Spam" },
  { value: "SAFETY_CONCERN", label: "Safety concern" },
  { value: "OWNER_REQUEST", label: "Owner request" },
  { value: "OTHER", label: "Other" },
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

  const dialogTitle = mode === "remove" ? "Remove listing" : "Restore listing";
  const submitLabel = mode === "remove" ? "Remove listing" : "Restore listing";
  const submitVariant = mode === "remove" ? "danger" : "primary";
  const description = useMemo(
    () =>
      mode === "remove"
        ? `Remove \"${listingTitle}\" from the public marketplace and invalidate pending offers that reference it.`
        : `Restore \"${listingTitle}\" so it can become active again in the marketplace.`,
    [listingTitle, mode]
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
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Reason</label>
          <select
            value={reasonCode}
            onChange={(event) => setReasonCode(event.target.value as ListingModerationReasonCode)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {REASON_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">User message</label>
          <textarea
            rows={4}
            value={userMessage}
            onChange={(event) => setUserMessage(event.target.value)}
            placeholder="Optional owner-facing explanation"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Internal note</label>
          <textarea
            rows={4}
            value={internalNote}
            onChange={(event) => setInternalNote(event.target.value)}
            placeholder="Optional internal moderation note"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </div>

        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button variant={submitVariant} onClick={handleSubmit} isLoading={isSubmitting}>
            {submitLabel}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

