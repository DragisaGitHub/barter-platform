import { AlertTriangle, MessageSquare, ShieldCheck } from "lucide-react";
import type { ItemDetailResponse } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

interface OwnerModerationPanelProps {
  item: ItemDetailResponse;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

export function OwnerModerationPanel({ item }: OwnerModerationPanelProps) {
  const { t } = useTranslation("catalog");

  if (item.status !== "REMOVED" || !item.moderationSummary) {
    return null;
  }

  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-900 dark:border-red-900/60 dark:bg-red-950/20 dark:text-red-100">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 flex size-9 items-center justify-center rounded-full bg-red-100 text-red-600 dark:bg-red-950/60 dark:text-red-300">
          <AlertTriangle className="size-4" />
        </div>
        <div className="min-w-0 flex-1 space-y-2">
          <div>
            <p className="font-semibold">{t("moderation.removedTitle")}</p>
            <p className="mt-1 text-red-800/90 dark:text-red-100/85">
              {t("moderation.removedDescription")}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-red-700 dark:text-red-200">
            <span className="inline-flex items-center gap-1 rounded-full bg-white px-2.5 py-1 dark:bg-red-950/60">
              <ShieldCheck className="size-3.5" />
              {item.moderationSummary.reasonCode
                ? t(`moderation.reasons.${item.moderationSummary.reasonCode}`)
                : t("moderation.reasonUnavailable")}
            </span>
            <span>{formatDateTime(item.moderationSummary.actionAt)}</span>
          </div>

          {item.moderationSummary.userMessage ? (
            <div className="rounded-lg border border-red-200 bg-white px-3 py-2.5 dark:border-red-900/60 dark:bg-red-950/50">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.08em] text-red-700 dark:text-red-200">
                <MessageSquare className="size-3.5" />
                {t("moderation.moderatorMessage")}
              </div>
              <p className="mt-1 whitespace-pre-line text-sm text-red-900 dark:text-red-100">
                {item.moderationSummary.userMessage}
              </p>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

