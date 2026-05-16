import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Inbox, CheckCircle, XCircle, Ban, CheckCheck, ShieldAlert, ShieldCheck, Clock3, BadgeCheck, MessageSquareHeart } from "lucide-react";
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
    case "TRADE_OFFER_COMPLETION_CONFIRMED":
      return <Clock3 className={iconClass} />;
    case "TRADE_OFFER_COMPLETED":
      return <BadgeCheck className={iconClass} />;
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
        "flex w-full items-start gap-3 border-l-2 px-4 py-3.5 text-left transition-colors focus:outline-none focus-visible:bg-slate-50 dark:focus-visible:bg-slate-700/60",
        notification.isRead
          ? "border-l-transparent bg-white hover:bg-slate-50 dark:bg-slate-800 dark:hover:bg-slate-700/60"
          : "border-l-indigo-500 bg-indigo-50/80 hover:bg-indigo-50 dark:bg-indigo-950/30 dark:hover:bg-indigo-950/40"
      )}
    >
      <div
        className={cn(
          "mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-full border",
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
              "min-w-0 flex-1 text-sm",
              notification.isRead
                ? "text-slate-700 dark:text-slate-300"
                : "font-semibold text-slate-900 dark:text-slate-50"
            )}
          >
            {notification.title}
          </p>
          {!notification.isRead && (
            <span className="inline-flex items-center gap-1 rounded-full bg-indigo-600/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-indigo-700 dark:text-indigo-300">
              <span className="size-1.5 rounded-full bg-indigo-500" />
              Unread
            </span>
          )}
        </div>
        {notification.message && (
          <p className="mt-1 line-clamp-2 text-xs text-slate-500 dark:text-slate-400">
            {notification.message}
          </p>
        )}
        <p className="mt-2 text-xs text-slate-400 dark:text-slate-500">
          {formatNotificationTime(notification.createdAt)}
        </p>
      </div>
      {!notification.isRead && (
        <div className="mt-2 shrink-0">
          <span className="block size-2.5 rounded-full bg-indigo-500 shadow-[0_0_0_4px] shadow-indigo-500/15" />
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
    setIsOpen(false);

    if (!notification.isRead) {
      void markAsRead.mutateAsync(notification.uuid).catch(() => undefined);
    }

    const targetPath = getNotificationTargetPath(notification);
    if (targetPath) {
      navigate(targetPath);
    }
  };

  const handleMarkAllRead = () => {
    markAllAsRead.mutate();
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className={cn(
          "relative rounded-xl p-2.5 text-slate-600 transition-colors hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700",
          isOpen && "bg-slate-100 dark:bg-slate-700",
          unreadCount > 0 && "text-slate-900 dark:text-slate-100"
        )}
        aria-label={
          unreadCount > 0
            ? `${unreadCount} unread notification${unreadCount === 1 ? "" : "s"}`
            : "Notifications"
        }
      >
        <Bell className="size-5" />
        {unreadCount > 0 && (
          <>
            <span className="absolute right-2 top-2 size-2 rounded-full bg-indigo-500 ring-4 ring-white dark:ring-slate-800" />
            <span className="absolute -right-1 -top-1 flex min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1.5 py-0.5 text-[10px] font-bold leading-none text-white shadow-sm ring-2 ring-white dark:ring-slate-800">
            {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          </>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-3 w-[calc(100vw-2rem)] max-w-sm overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-950/10 dark:border-slate-700 dark:bg-slate-800 sm:w-[26rem]">
          {/* Header */}
          <div className="border-b border-slate-200 px-4 py-4 dark:border-slate-700">
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-300">
                  <Bell className="size-5" />
                </div>
                <div className="min-w-0">
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                    Notifications
                  </h3>
                  <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                    {unreadCount > 0
                      ? `${unreadCount} unread update${unreadCount === 1 ? "" : "s"}`
                      : "You’re all caught up"}
                  </p>
                </div>
              </div>

              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllRead}
                  disabled={markAllAsRead.isPending}
                  className="inline-flex shrink-0 items-center gap-1 rounded-full border border-indigo-200 bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-700 transition-colors hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-indigo-900/60 dark:bg-indigo-950/30 dark:text-indigo-300 dark:hover:bg-indigo-950/50"
                >
                  <CheckCheck className="size-3.5" />
                  Mark all read
                </button>
              )}
            </div>
          </div>

          {/* Content */}
          <div className="max-h-[26rem] overflow-y-auto divide-y divide-slate-100 dark:divide-slate-700/60">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Spinner size="sm" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center px-6 py-10 text-center">
                <div className="mb-3 flex size-14 items-center justify-center rounded-full bg-slate-100 text-slate-400 dark:bg-slate-700/60 dark:text-slate-500">
                  <Bell className="size-7" />
                </div>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  No notifications yet
                </p>
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  Trade offer and completion updates will show up here as soon as something changes.
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
          <div className="border-t border-slate-200 px-4 py-3 dark:border-slate-700">
            <button
              onClick={() => {
                setIsOpen(false);
                navigate(routePaths.notifications);
              }}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900/40 dark:text-slate-200 dark:hover:bg-slate-700/70"
            >
              View all notifications
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

