import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTag,
  listAdminTags,
  updateAdminTag,
  type ListAdminTagsParams,
} from "@/api/adminTagsApi.ts";
import type {
  AdminTagPagedResponse,
  AdminTagResponse,
  CreateTagRequest,
  UpdateTagRequest,
} from "@/api/generated/types.ts";
import { catalogKeys } from "@/features/catalog/useCatalog.ts";

export const adminTagKeys = {
  all: ["admin", "tags"] as const,
  list: (params: ListAdminTagsParams) => ["admin", "tags", "list", params] as const,
  detail: (tagUuid: string) => ["admin", "tags", "detail", tagUuid] as const,
};

export function useAdminTags(params: ListAdminTagsParams = {}, enabled = true) {
  return useQuery<AdminTagPagedResponse>({
    queryKey: adminTagKeys.list(params),
    queryFn: () => listAdminTags(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminTag(tagUuid: string, enabled = true) {
  return useQuery<AdminTagResponse>({
    queryKey: adminTagKeys.detail(tagUuid),
    queryFn: () => getAdminTag(tagUuid),
    enabled: enabled && !!tagUuid,
  });
}

export function useCreateAdminTag() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTagRequest) => createAdminTag(data),
    onSuccess: (tag) => {
      queryClient.invalidateQueries({ queryKey: adminTagKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.tags });
      queryClient.setQueryData(adminTagKeys.detail(tag.uuid), tag);
    },
  });
}

export function useUpdateAdminTag() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ tagUuid, data }: { tagUuid: string; data: UpdateTagRequest }) => updateAdminTag(tagUuid, data),
    onSuccess: (tag) => {
      queryClient.setQueryData(adminTagKeys.detail(tag.uuid), tag);
      queryClient.invalidateQueries({ queryKey: adminTagKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.tags });
    },
  });
}

export function useDeleteAdminTag() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (tagUuid: string) => deleteAdminTag(tagUuid),
    onSuccess: (_, tagUuid) => {
      queryClient.removeQueries({ queryKey: adminTagKeys.detail(tagUuid) });
      queryClient.invalidateQueries({ queryKey: adminTagKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.tags });
    },
  });
}

