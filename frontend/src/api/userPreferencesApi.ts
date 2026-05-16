import { apiClient } from "./axios";
import type {
  UpdateUserPreferencesRequest,
  UserPreferencesResponse,
} from "./generated/types";

export async function getCurrentUserPreferences(): Promise<UserPreferencesResponse> {
  const response = await apiClient.get<UserPreferencesResponse>("/users/me/preferences");
  return response.data;
}

export async function updateCurrentUserPreferences(
  payload: UpdateUserPreferencesRequest,
): Promise<UserPreferencesResponse> {
  const response = await apiClient.patch<UserPreferencesResponse>(
    "/users/me/preferences",
    payload,
  );
  return response.data;
}

