import type { UserStatus } from "../../api/generated/types";
import { Badge } from "./Badge";

interface StatusBadgeProps {
  status: UserStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config: Record<
    UserStatus,
    { label: string; variant: "success" | "warning" | "danger" | "default" }
  > = {
    ACTIVE: { label: "Active", variant: "success" },
    PENDING_VERIFICATION: { label: "Pending", variant: "warning" },
    SUSPENDED: { label: "Suspended", variant: "warning" },
    BANNED: { label: "Banned", variant: "danger" },
    DELETED: { label: "Deleted", variant: "default" },
  };

  const { label, variant } = config[status];

  return <Badge variant={variant}>{label}</Badge>;
}
