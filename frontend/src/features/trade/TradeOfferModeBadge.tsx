import type { TradeOfferMode } from "@/api/generated/types.ts";
import { Badge } from "../../components/ui/Badge";
import { ArrowRightLeft, Gift, MessageSquare } from "lucide-react";

const modeConfig: Record<
  TradeOfferMode,
  { label: string; variant: "primary" | "success" | "warning" | "default" | "secondary" | "danger"; icon: React.ReactNode }
> = {
  ITEM_EXCHANGE: { label: "Item Exchange", variant: "primary", icon: <ArrowRightLeft className="size-3" /> },
  GIFT: { label: "Gift", variant: "success", icon: <Gift className="size-3" /> },
  NEGOTIABLE: { label: "Negotiable", variant: "warning", icon: <MessageSquare className="size-3" /> },
};

export function TradeOfferModeBadge({ mode }: { mode: TradeOfferMode }) {
  const { label, variant, icon } = modeConfig[mode];
  return (
    <Badge variant={variant}>
      <span className="inline-flex items-center gap-1">
        {icon}
        {label}
      </span>
    </Badge>
  );
}

