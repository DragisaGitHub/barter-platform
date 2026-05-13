import { useQuery, useMutation, useQueryClient, type QueryKey } from "@tanstack/react-query";
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
  lists: ["notifications", "list"] as const,
  list: (params: ListNotificationsParams) => ["notifications", "list", params] as const,
  unreadCount: ["notifications", "unread-count"] as const,
};

type NotificationCacheSnapshot = {
  previousLists: [QueryKey, NotificationPagedResponse | undefined][];
  previousUnreadCount?: NotificationUnreadCountResponse;
};

function markNotificationReadInPage(
  page: NotificationPagedResponse | undefined,
  notificationUuid: string,
) {
  if (!page) {
    return { page, didChange: false };
  }

  let didChange = false;
  const readAt = new Date().toISOString();
  const content = page.content.map((notification) => {
    if (notification.uuid !== notificationUuid || notification.isRead) {
      return notification;
    }

    didChange = true;
    return {
      ...notification,
      isRead: true,
      readAt: notification.readAt ?? readAt,
    };
  });

  return {
    page: didChange ? { ...page, content } : page,
    didChange,
  };
}

function markAllNotificationsReadInPage(page: NotificationPagedResponse | undefined) {
  if (!page || page.content.every((notification) => notification.isRead)) {
    return page;
  }

  const readAt = new Date().toISOString();

  return {
    ...page,
    content: page.content.map((notification) => ({
      ...notification,
      isRead: true,
      readAt: notification.readAt ?? readAt,
    })),
  };
}

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
    onMutate: async (notificationUuid): Promise<NotificationCacheSnapshot> => {
      await Promise.all([
        queryClient.cancelQueries({ queryKey: notificationKeys.lists }),
        queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount }),
      ]);

      const previousLists = queryClient.getQueriesData<NotificationPagedResponse>({
        queryKey: notificationKeys.lists,
      });
      const previousUnreadCount = queryClient.getQueryData<NotificationUnreadCountResponse>(
        notificationKeys.unreadCount,
      );

      let shouldDecreaseUnreadCount = false;

      queryClient.setQueriesData<NotificationPagedResponse>(
        { queryKey: notificationKeys.lists },
        (currentPage) => {
          const { page, didChange } = markNotificationReadInPage(currentPage, notificationUuid);

          if (didChange) {
            shouldDecreaseUnreadCount = true;
          }

          return page;
        },
      );

      if (shouldDecreaseUnreadCount && previousUnreadCount) {
        queryClient.setQueryData<NotificationUnreadCountResponse>(notificationKeys.unreadCount, {
          ...previousUnreadCount,
          count: Math.max(0, previousUnreadCount.count - 1),
        });
      }

      return { previousLists, previousUnreadCount };
    },
    onError: (error, _notificationUuid, context) => {
      context?.previousLists.forEach(([queryKey, page]) => {
        queryClient.setQueryData(queryKey, page);
      });

      if (context?.previousUnreadCount) {
        queryClient.setQueryData(notificationKeys.unreadCount, context.previousUnreadCount);
      }

      toast.error(parseApiError(error));
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useMarkAllNotificationsAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onMutate: async (): Promise<NotificationCacheSnapshot> => {
      await Promise.all([
        queryClient.cancelQueries({ queryKey: notificationKeys.lists }),
        queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount }),
      ]);

      const previousLists = queryClient.getQueriesData<NotificationPagedResponse>({
        queryKey: notificationKeys.lists,
      });
      const previousUnreadCount = queryClient.getQueryData<NotificationUnreadCountResponse>(
        notificationKeys.unreadCount,
      );

      queryClient.setQueriesData<NotificationPagedResponse>(
        { queryKey: notificationKeys.lists },
        (currentPage) => markAllNotificationsReadInPage(currentPage),
      );

      if (previousUnreadCount) {
        queryClient.setQueryData<NotificationUnreadCountResponse>(notificationKeys.unreadCount, {
          ...previousUnreadCount,
          count: 0,
        });
      }

      return { previousLists, previousUnreadCount };
    },
    onError: (error, _variables, context) => {
      context?.previousLists.forEach(([queryKey, page]) => {
        queryClient.setQueryData(queryKey, page);
      });

      if (context?.previousUnreadCount) {
        queryClient.setQueryData(notificationKeys.unreadCount, context.previousUnreadCount);
      }

      toast.error(parseApiError(error));
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

