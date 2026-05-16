import type { ItemStatus, ItemCondition } from "../../api/generated/types";
import { Badge } from "../../components/ui/Badge";
import { useTranslation } from "react-i18next";

const statusConfig: Record<ItemStatus, { key: string; variant: "success" | "warning" | "danger" | "default" | "primary" | "secondary" }> = {
  ACTIVE: { key: "status.active", variant: "success" },
  DRAFT: { key: "status.draft", variant: "default" },
  RESERVED: { key: "status.reserved", variant: "warning" },
  ARCHIVED: { key: "status.archived", variant: "secondary" },
  REMOVED: { key: "status.removed", variant: "danger" },
};

const conditionConfig: Record<ItemCondition, { key: string; variant: "success" | "primary" | "warning" | "default" | "secondary" }> = {
  NEW: { key: "condition.new", variant: "success" },
  LIKE_NEW: { key: "condition.likeNew", variant: "primary" },
  GOOD: { key: "condition.good", variant: "warning" },
  USED: { key: "condition.used", variant: "default" },
  FOR_PARTS: { key: "condition.forParts", variant: "secondary" },
};

export function ItemStatusBadge({ status }: { status: ItemStatus }) {
  const { t } = useTranslation("catalog");
  const { key, variant } = statusConfig[status];
  return <Badge variant={variant}>{t(key)}</Badge>;
}

export function ItemConditionBadge({ condition }: { condition: ItemCondition }) {
  const { t } = useTranslation("catalog");
  const { key, variant } = conditionConfig[condition];
  return <Badge variant={variant}>{t(key)}</Badge>;
}

