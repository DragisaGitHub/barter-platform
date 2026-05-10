import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  listCategories,
  listTags,
  searchItems,
  getItemByUuid,
  listMyItems,
  createItem,
  updateItem,
  archiveItem,
  type SearchItemsParams,
  type MyItemsParams,
} from "@/api/catalogApi.ts";
import type {
  CategoryResponse,
  TagResponse,
  ItemPagedResponse,
  ItemDetailResponse,
  CreateItemRequest,
  UpdateItemRequest,
  ArchiveItemRequest,
} from "@/api/generated/types.ts";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const catalogKeys = {
  categories: ["catalog", "categories"] as const,
  tags: ["catalog", "tags"] as const,
  items: ["catalog", "items"] as const,
  itemSearch: (params: SearchItemsParams) => ["catalog", "items", "search", params] as const,
  itemDetail: (uuid: string) => ["catalog", "items", uuid] as const,
  myItems: (params: MyItemsParams) => ["catalog", "items", "mine", params] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function useCategories() {
  return useQuery<CategoryResponse[]>({
    queryKey: catalogKeys.categories,
    queryFn: listCategories,
    staleTime: 5 * 60 * 1000, // categories rarely change
  });
}

export function useTags() {
  return useQuery<TagResponse[]>({
    queryKey: catalogKeys.tags,
    queryFn: listTags,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSearchItems(params: SearchItemsParams = {}) {
  return useQuery<ItemPagedResponse>({
    queryKey: catalogKeys.itemSearch(params),
    queryFn: () => searchItems(params),
  });
}

export function useItemDetail(uuid: string) {
  return useQuery<ItemDetailResponse>({
    queryKey: catalogKeys.itemDetail(uuid),
    queryFn: () => getItemByUuid(uuid),
    enabled: !!uuid,
  });
}

export function useMyItems(params: MyItemsParams = {}) {
  return useQuery<ItemPagedResponse>({
    queryKey: catalogKeys.myItems(params),
    queryFn: () => listMyItems(params),
  });
}

// ─── Mutation hooks ─────────────────────────────────────────────────────────

export function useCreateItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateItemRequest) => createItem(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
  });
}

export function useUpdateItem(uuid: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateItemRequest) => updateItem(uuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: catalogKeys.itemDetail(uuid) });
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
  });
}

export function useArchiveItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ uuid, data }: { uuid: string; data?: ArchiveItemRequest }) =>
      archiveItem(uuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
  });
}

