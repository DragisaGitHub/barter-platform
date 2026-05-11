import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Inbox, CheckCircle, XCircle, Ban, CheckCheck } from "lucide-react";
import { useNotifications, useUnreadNotificationCount, useMarkNotificationAsRead, useMarkAllNotificationsAsRead } from "./useNotifications";
import { formatNotificationTime, getNotificationColor, getNotificationTargetPath } from "./notificationHelpers";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/utils";
import type { NotificationResponse, NotificationType } from "@/api/generated/types.ts";
import { routePaths } from "@/routes/routePaths.ts";

function NotificationIcon({ type, className }: { type: NotificationType; className?: string }) {
  const colorClass = getNotificationColor(type);
  const iconClass = cn("size-5", colorClass, className);

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

function NotificationRow({
  notification,
  onClickNotification,
}: {
  notification: NotificationResponse;
  onClickNotification: (notification: NotificationResponse) => void;
}) {
  return (
    <button
      onClick={() => onClickNotification(notification)}
      className={cn(
        "w-full flex items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-slate-50 dark:hover:bg-slate-700/50",
        !notification.isRead && "bg-indigo-50/50 dark:bg-indigo-950/20"
      )}
    >
      <div className="shrink-0 mt-0.5">
        <NotificationIcon type={notification.type} />
      </div>
      <div className="flex-1 min-w-0">
        <p
          className={cn(
            "text-sm truncate",
            notification.isRead
              ? "text-slate-600 dark:text-slate-400"
              : "font-semibold text-slate-900 dark:text-slate-100"
          )}
        >
          {notification.title}
        </p>
        {notification.message && (
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 truncate">
            {notification.message}
          </p>
        )}
        <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
          {formatNotificationTime(notification.createdAt)}
        </p>
      </div>
      {!notification.isRead && (
        <div className="shrink-0 mt-2">
          <span className="block size-2 rounded-full bg-indigo-500" />
        </div>
      )}
    </button>
  );
}

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const { data: unreadData } = useUnreadNotificationCount();
  const { data: notificationsData, isLoading } = useNotifications({ page: 0, size: 5, sort: "createdAt,desc" });
  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();

  const unreadCount = unreadData?.count ?? 0;
  const notifications = notificationsData?.content ?? [];

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const handleNotificationClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      markAsRead.mutate(notification.uuid);
    }
    const targetPath = getNotificationTargetPath(notification);
    if (targetPath) {
      navigate(targetPath);
    }
    setIsOpen(false);
  };

  const handleMarkAllRead = () => {
    markAllAsRead.mutate();
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
        aria-label="Notifications"
      >
        <Bell className="size-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[10px] font-bold leading-none">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl shadow-lg z-50 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200 dark:border-slate-700">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              Notifications
            </h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                disabled={markAllAsRead.isPending}
                className="flex items-center gap-1 text-xs text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors disabled:opacity-50"
              >
                <CheckCheck className="size-3.5" />
                Mark all read
              </button>
            )}
          </div>

          {/* Content */}
          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-700/50">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Spinner size="sm" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-center">
                <Bell className="size-8 text-slate-300 dark:text-slate-600 mb-2" />
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  No notifications yet
                </p>
              </div>
            ) : (
              notifications.map((notification) => (
                <NotificationRow
                  key={notification.uuid}
                  notification={notification}
                  onClickNotification={handleNotificationClick}
                />
              ))
            )}
          </div>

          {/* Footer */}
          <div className="border-t border-slate-200 dark:border-slate-700 px-4 py-2.5">
            <button
              onClick={() => {
                navigate(routePaths.notifications);
                setIsOpen(false);
              }}
              className="w-full text-center text-sm text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 font-medium transition-colors"
            >
              View all notifications
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

