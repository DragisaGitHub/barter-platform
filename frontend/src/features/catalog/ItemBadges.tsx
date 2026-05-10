import type { ItemStatus, ItemCondition } from "../../api/generated/types";
import { Badge } from "../../components/ui/Badge";

const statusConfig: Record<ItemStatus, { label: string; variant: "success" | "warning" | "danger" | "default" | "primary" | "secondary" }> = {
  ACTIVE: { label: "Active", variant: "success" },
  DRAFT: { label: "Draft", variant: "default" },
  RESERVED: { label: "Reserved", variant: "warning" },
  ARCHIVED: { label: "Archived", variant: "secondary" },
  REMOVED: { label: "Removed", variant: "danger" },
};

const conditionConfig: Record<ItemCondition, { label: string; variant: "success" | "primary" | "warning" | "default" | "secondary" }> = {
  NEW: { label: "New", variant: "success" },
  LIKE_NEW: { label: "Like New", variant: "primary" },
  GOOD: { label: "Good", variant: "warning" },
  USED: { label: "Used", variant: "default" },
  FOR_PARTS: { label: "For Parts", variant: "secondary" },
};

export function ItemStatusBadge({ status }: { status: ItemStatus }) {
  const { label, variant } = statusConfig[status];
  return <Badge variant={variant}>{label}</Badge>;
}

export function ItemConditionBadge({ condition }: { condition: ItemCondition }) {
  const { label, variant } = conditionConfig[condition];
  return <Badge variant={variant}>{label}</Badge>;
}

