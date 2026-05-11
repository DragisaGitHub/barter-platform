import { formatDistanceToNow } from "date-fns";
import type { NotificationResponse, NotificationType } from "@/api/generated/types.ts";

/**
 * Format notification createdAt to a relative time string (e.g. "5 minutes ago").
 */
export function formatNotificationTime(createdAt: string): string {
  return formatDistanceToNow(new Date(createdAt), { addSuffix: true });
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
    case "TRADE_OFFER_REJECTED":
      return "x-circle";
    case "TRADE_OFFER_CANCELLED":
      return "ban";
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
    case "TRADE_OFFER_REJECTED":
      return "text-red-500 dark:text-red-400";
    case "TRADE_OFFER_CANCELLED":
      return "text-amber-500 dark:text-amber-400";
    default:
      return "text-slate-500 dark:text-slate-400";
  }
}

/**
 * Determine the target path for navigation when a notification is clicked.
 * Returns null if no specific target is available.
 */
export function getNotificationTargetPath(notification: NotificationResponse): string | null {
  if (notification.referenceType === "TRADE_OFFER" && notification.referenceUuid) {
    return `/offers/${notification.referenceUuid}`;
  }
  return null;
}

