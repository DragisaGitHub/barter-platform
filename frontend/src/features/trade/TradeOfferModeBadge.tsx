import type { TradeOfferMode } from "@/api/generated/types.ts";
import { Badge } from "../../components/ui/Badge";
import { ArrowRightLeft, Gift, MessageSquare } from "lucide-react";
import { useTranslation } from "react-i18next";

const modeConfig: Record<
  TradeOfferMode,
  { key: string; variant: "primary" | "success" | "warning" | "default" | "secondary" | "danger"; icon: React.ReactNode }
> = {
  ITEM_EXCHANGE: { key: "mode.itemExchange", variant: "primary", icon: <ArrowRightLeft className="size-3" /> },
  GIFT: { key: "mode.gift", variant: "success", icon: <Gift className="size-3" /> },
  NEGOTIABLE: { key: "mode.negotiable", variant: "warning", icon: <MessageSquare className="size-3" /> },
};

export function TradeOfferModeBadge({ mode }: { mode: TradeOfferMode }) {
  const { t } = useTranslation("trade");
  const { key, variant, icon } = modeConfig[mode];
  return (
    <Badge variant={variant}>
      <span className="inline-flex items-center gap-1">
        {icon}
        {t(key)}
      </span>
    </Badge>
  );
}

