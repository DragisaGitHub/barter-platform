import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createAdminCategory,
  deleteAdminCategory,
  getAdminCategory,
  listAdminCategories,
  updateAdminCategory,
  type ListAdminCategoriesParams,
} from "@/api/adminCategoriesApi.ts";
import type {
  AdminCategoryPagedResponse,
  AdminCategoryResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from "@/api/generated/types.ts";
import { catalogKeys } from "@/features/catalog/useCatalog.ts";

export const adminCategoryKeys = {
  all: ["admin", "categories"] as const,
  lists: () => ["admin", "categories", "list"] as const,
  list: (params: ListAdminCategoriesParams) => ["admin", "categories", "list", params] as const,
  details: () => ["admin", "categories", "detail"] as const,
  detail: (categoryUuid: string) => ["admin", "categories", "detail", categoryUuid] as const,
};

export function useAdminCategories(params: ListAdminCategoriesParams = {}, enabled = true) {
  return useQuery<AdminCategoryPagedResponse>({
    queryKey: adminCategoryKeys.list(params),
    queryFn: () => listAdminCategories(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminCategory(categoryUuid: string, enabled = true) {
  return useQuery<AdminCategoryResponse>({
    queryKey: adminCategoryKeys.detail(categoryUuid),
    queryFn: () => getAdminCategory(categoryUuid),
    enabled: enabled && !!categoryUuid,
  });
}

export function useCreateAdminCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => createAdminCategory(data),
    onSuccess: (category) => {
      queryClient.invalidateQueries({ queryKey: adminCategoryKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.categories });
      queryClient.setQueryData(adminCategoryKeys.detail(category.uuid), category);
    },
  });
}

export function useUpdateAdminCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ categoryUuid, data }: { categoryUuid: string; data: UpdateCategoryRequest }) =>
      updateAdminCategory(categoryUuid, data),
    onSuccess: (category) => {
      queryClient.setQueryData(adminCategoryKeys.detail(category.uuid), category);
      queryClient.invalidateQueries({ queryKey: adminCategoryKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.categories });
    },
  });
}

export function useDeleteAdminCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (categoryUuid: string) => deleteAdminCategory(categoryUuid),
    onSuccess: (_, categoryUuid) => {
      queryClient.removeQueries({ queryKey: adminCategoryKeys.detail(categoryUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategoryKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.categories });
    },
  });
}

