import type { NotificationResponse } from "@/api/generated/types.ts";

type TranslationFunction = (key: string, options?: Record<string, unknown>) => string;

type RenderedNotificationText = {
  title: string;
  message: string | null;
};

type NotificationMetadata = Record<string, unknown>;

function isMetadataRecord(metadata: unknown): metadata is NotificationMetadata {
  return typeof metadata === "object" && metadata !== null && !Array.isArray(metadata);
}

function hasMetadata(metadata: unknown): metadata is NotificationMetadata {
  return isMetadataRecord(metadata) && Object.keys(metadata).length > 0;
}

function getString(metadata: NotificationMetadata, key: string): string | undefined {
  const value = metadata[key];
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : undefined;
}

function getNumber(metadata: NotificationMetadata, key: string): number | undefined {
  const value = metadata[key];
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  return undefined;
}

function fallback(notification: NotificationResponse): RenderedNotificationText {
  return {
    title: notification.title,
    message: notification.message ?? null,
  };
}

export function renderNotificationText(
  notification: NotificationResponse,
  t: TranslationFunction,
): RenderedNotificationText {
  if (!hasMetadata(notification.metadata)) {
    return fallback(notification);
  }

  const metadata = notification.metadata;
  const actorUsername = getString(metadata, "actorUsername");
  const counterpartyUsername = getString(metadata, "counterpartyUsername");
  const itemTitle = getString(metadata, "itemTitle");
  const offeredItemTitle = getString(metadata, "offeredItemTitle");
  const offeredItemCount = getNumber(metadata, "offeredItemCount");
  const listingTitle = getString(metadata, "listingTitle");
  const userMessage = getString(metadata, "userMessage");

  switch (notification.type) {
    case "TRADE_OFFER_RECEIVED": {
      const title = actorUsername
        ? t("notifications:render.TRADE_OFFER_RECEIVED.title", { actorUsername })
        : t("notifications:render.TRADE_OFFER_RECEIVED.titleFallback");

      let message = t("notifications:render.TRADE_OFFER_RECEIVED.messageFallback");
      if (actorUsername && itemTitle && offeredItemTitle && offeredItemCount === 1) {
        message = t("notifications:render.TRADE_OFFER_RECEIVED.messageWithSingleItem", {
          actorUsername,
          itemTitle,
          offeredItemTitle,
        });
      } else if (actorUsername && itemTitle && typeof offeredItemCount === "number" && offeredItemCount > 1) {
        message = t("notifications:render.TRADE_OFFER_RECEIVED.messageWithMultipleItems", {
          actorUsername,
          itemTitle,
          count: offeredItemCount,
        });
      } else if (actorUsername && itemTitle) {
        message = t("notifications:render.TRADE_OFFER_RECEIVED.message", {
          actorUsername,
          itemTitle,
        });
      }

      return { title, message };
    }
    case "TRADE_OFFER_ACCEPTED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_OFFER_ACCEPTED.title", { actorUsername })
          : t("notifications:render.TRADE_OFFER_ACCEPTED.titleFallback"),
        message: actorUsername && itemTitle
          ? t("notifications:render.TRADE_OFFER_ACCEPTED.message", { actorUsername, itemTitle })
          : t("notifications:render.TRADE_OFFER_ACCEPTED.messageFallback", { actorUsername }),
      };
    case "TRADE_OFFER_COMPLETION_CONFIRMED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_OFFER_COMPLETION_CONFIRMED.title", { actorUsername })
          : t("notifications:render.TRADE_OFFER_COMPLETION_CONFIRMED.titleFallback"),
        message: actorUsername && itemTitle
          ? t("notifications:render.TRADE_OFFER_COMPLETION_CONFIRMED.message", { actorUsername, itemTitle })
          : t("notifications:render.TRADE_OFFER_COMPLETION_CONFIRMED.messageFallback", { actorUsername }),
      };
    case "TRADE_OFFER_COMPLETED":
      return {
        title: counterpartyUsername
          ? t("notifications:render.TRADE_OFFER_COMPLETED.title", { counterpartyUsername })
          : t("notifications:render.TRADE_OFFER_COMPLETED.titleFallback"),
        message: counterpartyUsername && itemTitle
          ? t("notifications:render.TRADE_OFFER_COMPLETED.message", { counterpartyUsername, itemTitle })
          : t("notifications:render.TRADE_OFFER_COMPLETED.messageFallback", { counterpartyUsername }),
      };
    case "TRADE_OFFER_REJECTED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_OFFER_REJECTED.title", { actorUsername })
          : t("notifications:render.TRADE_OFFER_REJECTED.titleFallback"),
        message: actorUsername
          ? t("notifications:render.TRADE_OFFER_REJECTED.message", { actorUsername })
          : t("notifications:render.TRADE_OFFER_REJECTED.messageFallback"),
      };
    case "TRADE_OFFER_CANCELLED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_OFFER_CANCELLED.title", { actorUsername })
          : t("notifications:render.TRADE_OFFER_CANCELLED.titleFallback"),
        message: actorUsername
          ? t("notifications:render.TRADE_OFFER_CANCELLED.message", { actorUsername })
          : t("notifications:render.TRADE_OFFER_CANCELLED.messageFallback"),
      };
    case "TRADE_MESSAGE_RECEIVED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_MESSAGE_RECEIVED.title", { actorUsername })
          : t("notifications:render.TRADE_MESSAGE_RECEIVED.titleFallback"),
        message: actorUsername && itemTitle
          ? t("notifications:render.TRADE_MESSAGE_RECEIVED.messageWithItem", { actorUsername, itemTitle })
          : actorUsername
            ? t("notifications:render.TRADE_MESSAGE_RECEIVED.message", { actorUsername })
            : t("notifications:render.TRADE_MESSAGE_RECEIVED.messageFallback"),
      };
    case "TRADE_REVIEW_RECEIVED":
      return {
        title: actorUsername
          ? t("notifications:render.TRADE_REVIEW_RECEIVED.title", { actorUsername })
          : t("notifications:render.TRADE_REVIEW_RECEIVED.titleFallback"),
        message: actorUsername && itemTitle
          ? t("notifications:render.TRADE_REVIEW_RECEIVED.messageWithItem", { actorUsername, itemTitle })
          : actorUsername
            ? t("notifications:render.TRADE_REVIEW_RECEIVED.message", { actorUsername })
            : t("notifications:render.TRADE_REVIEW_RECEIVED.messageFallback"),
      };
    case "LISTING_REMOVED":
      return {
        title: t("notifications:render.LISTING_REMOVED.title"),
        message: userMessage
          ? userMessage
          : listingTitle
            ? t("notifications:render.LISTING_REMOVED.message", { listingTitle })
            : t("notifications:render.LISTING_REMOVED.messageFallback"),
      };
    case "LISTING_RESTORED":
      return {
        title: t("notifications:render.LISTING_RESTORED.title"),
        message: userMessage
          ? userMessage
          : listingTitle
            ? t("notifications:render.LISTING_RESTORED.message", { listingTitle })
            : t("notifications:render.LISTING_RESTORED.messageFallback"),
      };
    default:
      return fallback(notification);
  }
}

