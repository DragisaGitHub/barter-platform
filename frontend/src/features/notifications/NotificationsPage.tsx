import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Inbox, CheckCircle, XCircle, Ban, CheckCheck } from "lucide-react";
import { useNotifications, useUnreadNotificationCount, useMarkNotificationAsRead, useMarkAllNotificationsAsRead } from "./useNotifications";
import { formatNotificationTime, getNotificationColor, getNotificationTargetPath } from "./notificationHelpers";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Pagination } from "@/components/data/Pagination";
import { cn } from "@/utils";
import type { NotificationResponse, NotificationType } from "@/api/generated/types.ts";

function NotificationIcon({ type }: { type: NotificationType }) {
  const colorClass = getNotificationColor(type);
  const iconClass = cn("size-5", colorClass);

  switch (type) {
    case "TRADE_OFFER_RECEIVED":
      return <Inbox className={iconClass} />;
    case "TRADE_OFFER_ACCEPTED":
      return <CheckCircle className={iconClass} />;
    case "TRADE_OFFER_REJECTED":
      return <XCircle className={iconClass} />;
    case "TRADE_OFFER_CANCELLED":
      return <Ban className={iconClass} />;
    default:
      return <Bell className={iconClass} />;
  }
}

export function NotificationsPage() {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  const { data, isLoading } = useNotifications({ page, size: 20, sort: "createdAt,desc" });
  const { data: unreadData } = useUnreadNotificationCount();
  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();

  const notifications = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const unreadCount = unreadData?.count ?? 0;

  const handleNotificationClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      markAsRead.mutate(notification.uuid);
    }
    const targetPath = getNotificationTargetPath(notification);
    if (targetPath) {
      navigate(targetPath);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Notifications</h1>
          {unreadCount > 0 && (
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              {unreadCount} unread notification{unreadCount !== 1 ? "s" : ""}
            </p>
          )}
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllAsRead.mutate()}
            isLoading={markAllAsRead.isPending}
          >
            <CheckCheck className="size-4" />
            Mark all as read
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
            title="No notifications"
            description="You're all caught up! Notifications about your trade offers will appear here."
          />
        ) : (
          <>
            <div className="divide-y divide-slate-200 dark:divide-slate-700">
              {notifications.map((notification) => (
                <button
                  key={notification.uuid}
                  onClick={() => handleNotificationClick(notification)}
                  className={cn(
                    "w-full flex items-start gap-4 px-6 py-4 text-left transition-colors hover:bg-slate-50 dark:hover:bg-slate-700/50",
                    !notification.isRead && "bg-indigo-50/50 dark:bg-indigo-950/20"
                  )}
                >
                  <div className="shrink-0 mt-0.5">
                    <NotificationIcon type={notification.type} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p
                      className={cn(
                        "text-sm",
                        notification.isRead
                          ? "text-slate-600 dark:text-slate-400"
                          : "font-semibold text-slate-900 dark:text-slate-100"
                      )}
                    >
                      {notification.title}
                    </p>
                    {notification.message && (
                      <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                        {notification.message}
                      </p>
                    )}
                    <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
                      {formatNotificationTime(notification.createdAt)}
                    </p>
                  </div>
                  {!notification.isRead && (
                    <div className="shrink-0 mt-2">
                      <span className="block size-2.5 rounded-full bg-indigo-500" />
                    </div>
                  )}
                </button>
              ))}
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

