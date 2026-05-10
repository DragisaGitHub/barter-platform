import type { TradeOfferStatus } from "@/api/generated/types.ts";
import { Badge } from "../../components/ui/Badge";

const statusConfig: Record<
  TradeOfferStatus,
  { label: string; variant: "success" | "warning" | "danger" | "default" | "primary" | "secondary" }
> = {
  PENDING: { label: "Pending", variant: "warning" },
  ACCEPTED: { label: "Accepted", variant: "success" },
  REJECTED: { label: "Rejected", variant: "danger" },
  CANCELLED: { label: "Cancelled", variant: "default" },
  EXPIRED: { label: "Expired", variant: "secondary" },
};

export function TradeOfferStatusBadge({ status }: { status: TradeOfferStatus }) {
  const { label, variant } = statusConfig[status];
  return <Badge variant={variant}>{label}</Badge>;
}

