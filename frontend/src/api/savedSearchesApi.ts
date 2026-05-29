import { apiClient } from "./axios";
import type {
  CreateSavedSearchRequest,
  SavedSearchPagedResponse,
  SavedSearchResponse,
} from "./generated/types";

export interface SavedSearchesParams {
  page?: number;
  size?: number;
  sort?: string;
}

export async function createSavedSearch(
  data: CreateSavedSearchRequest
): Promise<SavedSearchResponse> {
  const response = await apiClient.post<SavedSearchResponse>("/saved-searches", data);
  return response.data;
}

export async function listSavedSearches(
  params: SavedSearchesParams = {}
): Promise<SavedSearchPagedResponse> {
  const response = await apiClient.get<SavedSearchPagedResponse>("/saved-searches", { params });
  return response.data;
}

export async function deleteSavedSearch(uuid: string): Promise<void> {
  await apiClient.delete(`/saved-searches/${uuid}`);
}

