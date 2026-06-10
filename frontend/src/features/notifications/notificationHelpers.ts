import { formatDistanceToNow } from "date-fns";
import { enUS, srLatn } from "date-fns/locale";
import type { NotificationResponse, NotificationType } from "@/api/generated/types.ts";
import { routePaths } from "@/routes/routePaths.ts";

/**
 * Format notification createdAt to a relative time string (e.g. "5 minutes ago").
 */
export function formatNotificationTime(createdAt: string, language: string = "sr"): string {
  return formatDistanceToNow(new Date(createdAt), {
    addSuffix: true,
    locale: language === "en" ? enUS : srLatn,
  });
}

/**
 * Get lucide-react icon name per notification type (returns string key for lookup).
 */
export function getNotificationIconName(type: NotificationType): string {
  switch (type) {
    case "TRADE_OFFER_RECEIVED":
      return "inbox";
    case "TRADE_OFFER_ACCEPTED":
      return "check-circle";
    case "TRADE_OFFER_COMPLETION_CONFIRMED":
      return "clock-3";
    case "TRADE_OFFER_COMPLETED":
      return "badge-check";
    case "TRADE_REVIEW_RECEIVED":
      return "message-square-heart";
    case "TRADE_MESSAGE_RECEIVED":
      return "message-circle";
    case "TRADE_OFFER_REJECTED":
      return "x-circle";
    case "TRADE_OFFER_CANCELLED":
      return "ban";
    case "LISTING_REMOVED":
      return "shield-alert";
    case "LISTING_RESTORED":
      return "shield-check";
    default:
      return "bell";
  }
}

/**
 * Get tailwind color class per notification type.
 */
export function getNotificationColor(type: NotificationType): string {
  switch (type) {
    case "TRADE_OFFER_RECEIVED":
      return "text-indigo-500 dark:text-indigo-400";
    case "TRADE_OFFER_ACCEPTED":
      return "text-emerald-500 dark:text-emerald-400";
    case "TRADE_OFFER_COMPLETION_CONFIRMED":
      return "text-amber-500 dark:text-amber-400";
    case "TRADE_OFFER_COMPLETED":
      return "text-emerald-500 dark:text-emerald-400";
    case "TRADE_REVIEW_RECEIVED":
      return "text-violet-500 dark:text-violet-400";
    case "TRADE_MESSAGE_RECEIVED":
      return "text-sky-500 dark:text-sky-400";
    case "TRADE_OFFER_REJECTED":
      return "text-red-500 dark:text-red-400";
    case "TRADE_OFFER_CANCELLED":
      return "text-amber-500 dark:text-amber-400";
    case "LISTING_REMOVED":
      return "text-red-500 dark:text-red-400";
    case "LISTING_RESTORED":
      return "text-emerald-500 dark:text-emerald-400";
    default:
      return "text-slate-500 dark:text-slate-400";
  }
}

/**
 * Determine the target path for navigation when a notification is clicked.
 * Returns null if no specific target is available.
 */
export function getNotificationTargetPath(notification: NotificationResponse): string | null {
  const referenceUuid = notification.referenceUuid?.trim();
  const normalizedReferenceType = notification.referenceType?.trim().toUpperCase();

  if (!referenceUuid) {
    return null;
  }

  if (
    normalizedReferenceType === "TRADE_OFFER"
    || notification.type.startsWith("TRADE_OFFER")
    || notification.type === "TRADE_REVIEW_RECEIVED"
  ) {
    return `${routePaths.offers}/${referenceUuid}`;
  }

  if (normalizedReferenceType === "ITEM" || notification.type.startsWith("LISTING_")) {
    return routePaths.myItemDetail(referenceUuid);
  }

  return null;
}

