import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Inbox, CheckCircle, XCircle, Ban, CheckCheck, ShieldAlert, ShieldCheck, Clock3, BadgeCheck, MessageCircle, MessageSquareHeart } from "lucide-react";
import { useNotifications, useUnreadNotificationCount, useMarkNotificationAsRead, useMarkAllNotificationsAsRead } from "./useNotifications";
import { formatNotificationTime, getNotificationColor, getNotificationTargetPath } from "./notificationHelpers";
import { renderNotificationText } from "./renderNotificationText";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Pagination } from "@/components/data/Pagination";
import { cn } from "@/utils";
import type { NotificationResponse, NotificationType } from "@/api/generated/types.ts";
import { useTranslation } from "react-i18next";

function NotificationIcon({ type }: { type: NotificationType }) {
  const colorClass = getNotificationColor(type);
  const iconClass = cn("size-5", colorClass);

  switch (type) {
    case "TRADE_OFFER_RECEIVED":
      return <Inbox className={iconClass} />;
    case "TRADE_OFFER_ACCEPTED":
      return <CheckCircle className={iconClass} />;
    case "TRADE_OFFER_COMPLETION_CONFIRMED":
      return <Clock3 className={iconClass} />;
    case "TRADE_OFFER_COMPLETED":
      return <BadgeCheck className={iconClass} />;
    case "TRADE_MESSAGE_RECEIVED":
      return <MessageCircle className={iconClass} />;
    case "TRADE_REVIEW_RECEIVED":
      return <MessageSquareHeart className={iconClass} />;
    case "TRADE_OFFER_REJECTED":
      return <XCircle className={iconClass} />;
    case "TRADE_OFFER_CANCELLED":
      return <Ban className={iconClass} />;
    case "LISTING_REMOVED":
      return <ShieldAlert className={iconClass} />;
    case "LISTING_RESTORED":
      return <ShieldCheck className={iconClass} />;
    default:
      return <Bell className={iconClass} />;
  }
}

export function NotificationsPage() {
  const { t, i18n } = useTranslation("notifications");
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  const { data, isLoading } = useNotifications({ page, size: 20, sort: "createdAt,desc" });
  const { data: unreadData } = useUnreadNotificationCount();
  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();

  const notifications = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const unreadCount = unreadData?.count ?? 0;
  const hasUnread = unreadCount > 0;

  const handleNotificationClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      void markAsRead.mutateAsync(notification.uuid).catch(() => undefined);
    }

    const targetPath = getNotificationTargetPath(notification);
    if (targetPath) {
      navigate(targetPath);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-800 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
            <Bell className="size-6" />
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t("notifications")}</h1>
              <Badge
                variant={hasUnread ? "primary" : "default"}
                className="px-2 py-1 text-[11px] font-semibold"
              >
                {hasUnread
                  ? t("unreadCount", { count: unreadCount })
                  : t("allCaughtUp")}
              </Badge>
            </div>

            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              {t("pageDescription")}
            </p>

            <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <span>{t("newestFirst")}</span>
              {hasUnread && (
                <>
                  <span className="hidden sm:inline text-slate-300 dark:text-slate-600">•</span>
                  <span>{t("updatesNeedAttention", { count: unreadCount })}</span>
                </>
              )}
            </div>
          </div>
        </div>

        {hasUnread && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllAsRead.mutate()}
            isLoading={markAllAsRead.isPending}
            className="self-start sm:self-auto"
          >
            <CheckCheck className="size-4" />
            {t("markAllRead")}
          </Button>
        )}
      </div>

      {/* Content */}
      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <Spinner />
          </div>
        ) : notifications.length === 0 ? (
          <EmptyState
            icon={<Bell className="size-12" />}
            title={t("noNotifications")}
              description={t("noNotificationsPageBody")}
            className="py-16"
          />
        ) : (
          <>
            <div className="border-b border-slate-200 bg-slate-50/80 px-4 py-3 text-xs font-medium uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:bg-slate-900/40 dark:text-slate-400 sm:px-6">
              {t("recentActivity")}
            </div>

            <div className="divide-y divide-slate-200 dark:divide-slate-700">
              {notifications.map((notification) => {
                const renderedNotification = renderNotificationText(notification, t);

                return (
                  <button
                    key={notification.uuid}
                    onClick={() => handleNotificationClick(notification)}
                    className={cn(
                      "flex w-full items-start gap-4 border-l-2 px-4 py-4 text-left transition-colors focus:outline-none focus-visible:bg-slate-50 dark:focus-visible:bg-slate-700/60 sm:px-6",
                      notification.isRead
                        ? "border-l-transparent bg-white hover:bg-slate-50 dark:bg-slate-800 dark:hover:bg-slate-700/50"
                        : "border-l-indigo-500 bg-indigo-50/80 hover:bg-indigo-50 dark:bg-indigo-950/25 dark:hover:bg-indigo-950/35"
                    )}
                  >
                    <div
                      className={cn(
                        "mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl border",
                        notification.isRead
                          ? "border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-700/60"
                          : "border-indigo-100 bg-white shadow-sm dark:border-indigo-900/60 dark:bg-slate-800"
                      )}
                    >
                      <NotificationIcon type={notification.type} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p
                          className={cn(
                            "text-sm",
                            notification.isRead
                              ? "text-slate-700 dark:text-slate-300"
                              : "font-semibold text-slate-900 dark:text-slate-100"
                          )}
                        >
                          {renderedNotification.title}
                        </p>

                        {!notification.isRead && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-600/10 px-2 py-0.5 text-[11px] font-semibold text-indigo-700 dark:text-indigo-300">
                            <span className="size-1.5 rounded-full bg-indigo-500" />
                            {t("unread")}
                          </span>
                        )}
                      </div>

                      {renderedNotification.message && (
                        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                          {renderedNotification.message}
                        </p>
                      )}

                      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-400 dark:text-slate-500">
                        <span>{formatNotificationTime(notification.createdAt, i18n.language)}</span>
                        <span className="text-slate-300 dark:text-slate-600">•</span>
                        <span>{t("openDetails")}</span>
                      </div>
                    </div>
                    {!notification.isRead && (
                      <div className="shrink-0 mt-2">
                        <span className="block size-2.5 rounded-full bg-indigo-500 shadow-[0_0_0_4px] shadow-indigo-500/15" />
                      </div>
                    )}
                  </button>
                );
              })}
            </div>

            {totalPages > 1 && (
              <Pagination
                currentPage={page}
                totalPages={totalPages}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}

