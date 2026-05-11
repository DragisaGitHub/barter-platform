import { apiClient } from "./axios";
import type {
  NotificationPagedResponse,
  NotificationUnreadCountResponse,
  MessageResponse,
} from "./generated/types";

// ─── Query parameter types ──────────────────────────────────────────────────

export interface ListNotificationsParams {
  page?: number;
  size?: number;
  sort?: string;
}

// ─── API functions ──────────────────────────────────────────────────────────

export async function listNotifications(
  params: ListNotificationsParams = {},
): Promise<NotificationPagedResponse> {
  const response = await apiClient.get<NotificationPagedResponse>("/notifications", { params });
  return response.data;
}

export async function getUnreadNotificationCount(): Promise<NotificationUnreadCountResponse> {
  const response = await apiClient.get<NotificationUnreadCountResponse>("/notifications/unread-count");
  return response.data;
}

export async function markNotificationAsRead(notificationUuid: string): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>(`/notifications/${notificationUuid}/read`);
  return response.data;
}

export async function markAllNotificationsAsRead(): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>("/notifications/read-all");
  return response.data;
}

