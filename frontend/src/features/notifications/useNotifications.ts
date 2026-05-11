import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  listNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  type ListNotificationsParams,
} from "@/api/notificationsApi.ts";
import type {
  NotificationPagedResponse,
  NotificationUnreadCountResponse,
} from "@/api/generated/types.ts";
import { toast } from "sonner";
import { parseApiError } from "@/utils";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const notificationKeys = {
  all: ["notifications"] as const,
  list: (params: ListNotificationsParams) => ["notifications", "list", params] as const,
  unreadCount: ["notifications", "unread-count"] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function useNotifications(params: ListNotificationsParams = {}) {
  return useQuery<NotificationPagedResponse>({
    queryKey: notificationKeys.list(params),
    queryFn: () => listNotifications(params),
    refetchInterval: 30_000,
  });
}

export function useUnreadNotificationCount() {
  return useQuery<NotificationUnreadCountResponse>({
    queryKey: notificationKeys.unreadCount,
    queryFn: () => getUnreadNotificationCount(),
    refetchInterval: 15_000,
    staleTime: 10_000,
  });
}

// ─── Mutation hooks ─────────────────────────────────────────────────────────

export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationUuid: string) => markNotificationAsRead(notificationUuid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
    onError: (error) => {
      toast.error(parseApiError(error));
    },
  });
}

export function useMarkAllNotificationsAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onSuccess: () => {
      toast.success("All notifications marked as read");
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
    onError: (error) => {
      toast.error(parseApiError(error));
    },
  });
}

