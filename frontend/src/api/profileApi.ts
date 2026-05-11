import { apiClient } from "./axios";
import type { PublicProfileResponse, ItemPagedResponse } from "./generated/types";

// ─── Query parameter types ──────────────────────────────────────────────────

export interface PublicProfileItemsParams {
  page?: number;
  size?: number;
  sort?: string;
}

// ─── API functions ──────────────────────────────────────────────────────────

export async function getPublicProfile(userUuid: string): Promise<PublicProfileResponse> {
  const response = await apiClient.get<PublicProfileResponse>(`/profiles/${userUuid}`);
  return response.data;
}

export async function getPublicProfileItems(
  userUuid: string,
  params: PublicProfileItemsParams = {}
): Promise<ItemPagedResponse> {
  const response = await apiClient.get<ItemPagedResponse>(`/profiles/${userUuid}/items`, { params });
  return response.data;
}

