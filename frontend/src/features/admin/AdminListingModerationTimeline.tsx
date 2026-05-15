import { ShieldAlert, ShieldCheck, UserRound } from "lucide-react";
import type { ListingModerationActionResponse } from "@/api/generated/types.ts";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { cn } from "@/utils";

interface AdminListingModerationTimelineProps {
  actions: ListingModerationActionResponse[];
  isLoading?: boolean;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

function formatReason(reasonCode: string) {
  return reasonCode
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function AdminListingModerationTimeline({
  actions,
  isLoading = false,
}: AdminListingModerationTimelineProps) {
  if (isLoading) {
    return <div className="text-sm text-slate-500 dark:text-slate-400">Loading moderation history…</div>;
  }

  if (actions.length === 0) {
    return <div className="text-sm text-slate-500 dark:text-slate-400">No moderation actions recorded for this listing.</div>;
  }

  return (
    <div className="space-y-4">
      {actions.map((action) => {
        const isRemove = action.actionType === "REMOVE";

        return (
          <Card
            key={action.uuid}
            className={cn(
              "border-l-4 p-4",
              isRemove
                ? "border-l-red-500 bg-red-50/40 dark:bg-red-950/10"
                : "border-l-emerald-500 bg-emerald-50/40 dark:bg-emerald-950/10"
            )}
          >
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <div
                    className={cn(
                      "flex size-9 items-center justify-center rounded-full",
                      isRemove
                        ? "bg-red-100 text-red-600 dark:bg-red-950/40 dark:text-red-300"
                        : "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-300"
                    )}
                  >
                    {isRemove ? <ShieldAlert className="size-4" /> : <ShieldCheck className="size-4" />}
                  </div>
                  <Badge variant={isRemove ? "danger" : "success"}>
                    {isRemove ? "Removed" : "Restored"}
                  </Badge>
                  <Badge variant="secondary">{formatReason(action.reasonCode)}</Badge>
                  <Badge variant="default">{action.sourceType}</Badge>
                </div>

                <div className="text-sm text-slate-600 dark:text-slate-300">
                  <div className="flex items-center gap-2">
                    <UserRound className="size-4 text-slate-400" />
                    <span>
                      {action.performedByUsername ?? "System"}
                      {action.performedByUserUuid ? " • moderator action" : ""}
                    </span>
                  </div>
                </div>

                {action.userMessage ? (
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      Owner message
                    </p>
                    <p className="mt-1 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">
                      {action.userMessage}
                    </p>
                  </div>
                ) : null}

                {action.internalNote ? (
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">
                      Internal note
                    </p>
                    <p className="mt-1 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">
                      {action.internalNote}
                    </p>
                  </div>
                ) : null}
              </div>

              <div className="text-xs text-slate-500 dark:text-slate-400">{formatDateTime(action.createdAt)}</div>
            </div>
          </Card>
        );
      })}
    </div>
  );
}

