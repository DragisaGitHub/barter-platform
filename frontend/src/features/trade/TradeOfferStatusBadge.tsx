import type { TradeOfferStatus } from "@/api/generated/types.ts";
import { Badge } from "../../components/ui/Badge";
import { useTranslation } from "react-i18next";

const statusConfig: Record<
  TradeOfferStatus,
  { key: string; variant: "success" | "warning" | "danger" | "default" | "primary" | "secondary" }
> = {
  PENDING: { key: "status.pending", variant: "warning" },
  ACCEPTED: { key: "status.awaitingCompletion", variant: "primary" },
  COMPLETED: { key: "status.completed", variant: "success" },
  REJECTED: { key: "status.rejected", variant: "danger" },
  CANCELLED: { key: "status.cancelled", variant: "default" },
  EXPIRED: { key: "status.expired", variant: "secondary" },
  INVALIDATED: { key: "status.invalidated", variant: "secondary" },
};

export function TradeOfferStatusBadge({ status }: { status: TradeOfferStatus }) {
  const { t } = useTranslation("trade");
  const { key, variant } = statusConfig[status];
  return <Badge variant={variant}>{t(key)}</Badge>;
}

