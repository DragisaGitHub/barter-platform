import type {
  ReportReasonCode,
  ReportStatus,
  ReportTargetType,
} from "@/api/generated/types";

type BadgeVariant = "default" | "primary" | "success" | "warning" | "danger" | "secondary";

export const REPORT_REASON_OPTIONS: ReportReasonCode[] = [
  "PROHIBITED_ITEM",
  "SPAM_SCAM",
  "HARASSMENT",
  "MISLEADING_LISTING",
  "UNSAFE_EXCHANGE",
  "NO_SHOW",
  "OTHER",
];

export const REPORT_STATUS_OPTIONS: ReportStatus[] = [
  "OPEN",
  "IN_REVIEW",
  "RESOLVED",
  "DISMISSED",
];

export const REPORT_TARGET_TYPE_OPTIONS: ReportTargetType[] = [
  "ITEM",
  "USER",
  "MESSAGE",
  "TRADE_OFFER",
  "REVIEW",
];

const REPORT_REASON_TRANSLATION_KEYS: Record<ReportReasonCode, string> = {
  PROHIBITED_ITEM: "reasons.prohibitedItem",
  SPAM_SCAM: "reasons.spamScam",
  HARASSMENT: "reasons.harassment",
  MISLEADING_LISTING: "reasons.misleadingListing",
  UNSAFE_EXCHANGE: "reasons.unsafeExchange",
  NO_SHOW: "reasons.noShow",
  OTHER: "reasons.other",
};

const REPORT_STATUS_TRANSLATION_KEYS: Record<ReportStatus, string> = {
  OPEN: "statuses.open",
  IN_REVIEW: "statuses.inReview",
  RESOLVED: "statuses.resolved",
  DISMISSED: "statuses.dismissed",
};

const REPORT_TARGET_TYPE_TRANSLATION_KEYS: Record<ReportTargetType, string> = {
  ITEM: "targetTypes.item",
  USER: "targetTypes.user",
  MESSAGE: "targetTypes.message",
  TRADE_OFFER: "targetTypes.tradeOffer",
  REVIEW: "targetTypes.review",
};

export function reportReasonTranslationKey(reasonCode: ReportReasonCode): string {
  return REPORT_REASON_TRANSLATION_KEYS[reasonCode];
}

export function reportStatusTranslationKey(status: ReportStatus): string {
  return REPORT_STATUS_TRANSLATION_KEYS[status];
}

export function reportTargetTypeTranslationKey(targetType: ReportTargetType): string {
  return REPORT_TARGET_TYPE_TRANSLATION_KEYS[targetType];
}

export function reportStatusBadgeVariant(status: ReportStatus): BadgeVariant {
  switch (status) {
    case "OPEN":
      return "warning";
    case "IN_REVIEW":
      return "primary";
    case "RESOLVED":
      return "success";
    case "DISMISSED":
      return "default";
    default:
      return "default";
  }
}

