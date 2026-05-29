import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createSavedSearch,
  deleteSavedSearch,
  listSavedSearches,
  type SavedSearchesParams,
} from "@/api/savedSearchesApi";
import type {
  CreateSavedSearchRequest,
  SavedSearchPagedResponse,
  SavedSearchResponse,
} from "@/api/generated/types";

export const savedSearchKeys = {
  all: ["saved-searches"] as const,
  list: (params: SavedSearchesParams) => ["saved-searches", "list", params] as const,
};

export function useSavedSearches(params: SavedSearchesParams = {}, enabled = true) {
  return useQuery<SavedSearchPagedResponse>({
    queryKey: savedSearchKeys.list(params),
    queryFn: () => listSavedSearches(params),
    enabled,
  });
}

export function useCreateSavedSearch() {
  const queryClient = useQueryClient();
  return useMutation<SavedSearchResponse, Error, CreateSavedSearchRequest>({
    mutationFn: createSavedSearch,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: savedSearchKeys.all });
    },
  });
}

export function useDeleteSavedSearch() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: deleteSavedSearch,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: savedSearchKeys.all });
    },
  });
}

