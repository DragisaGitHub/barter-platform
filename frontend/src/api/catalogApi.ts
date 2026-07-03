import { apiClient } from "./axios";
import type {
  CategoryResponse,
  PopularCategoryResponse,
  TagResponse,
  ItemPagedResponse,
  RecommendationPagedResponse,
  ItemDetailResponse,
  MessageResponse,
  CreateItemRequest,
  UpdateItemRequest,
  ArchiveItemRequest,
  ItemStatus,
  ItemCondition,
  CategoryFormSchemaResponse,
  CategoryFiltersResponse,
} from "./generated/types";

// ─── Query parameter types ──────────────────────────────────────────────────

/**
 * Dynamic category-schema field filter values keyed by the schema field's `key` (Marketplace
 * Schema Engine, Phase 6). Values are sent to the backend as `field.<key>` query parameters;
 * arrays are sent as repeated parameters (OR-matched, used for MULTI_SELECT).
 */
export type SchemaFieldFilterValues = Record<string, string | boolean | string[] | undefined>;

export interface SearchItemsParams {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  categoryUuid?: string;
  tagUuids?: string[];
  condition?: ItemCondition;
  location?: string;
  fieldFilters?: SchemaFieldFilterValues;
}

export interface MyItemsParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: ItemStatus;
}

export interface FavoriteItemsParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PopularCategoriesParams {
  limit?: number;
}

export interface RecommendationParams {
  page?: number;
  size?: number;
  sort?: string;
}

// ─── API functions ──────────────────────────────────────────────────────────

export async function listCategories(): Promise<CategoryResponse[]> {
  const response = await apiClient.get<CategoryResponse[]>("/catalog/categories");
  return response.data;
}

export async function listPopularCategories(
  params: PopularCategoriesParams = {}
): Promise<PopularCategoryResponse[]> {
  const response = await apiClient.get<PopularCategoryResponse[]>("/catalog/categories/popular", {
    params,
  });
  return response.data;
}

export async function listTags(): Promise<TagResponse[]> {
  const response = await apiClient.get<TagResponse[]>("/catalog/tags");
  return response.data;
}

export async function getCategoryFormSchema(categoryUuid: string): Promise<CategoryFormSchemaResponse> {
  const response = await apiClient.get<CategoryFormSchemaResponse>(
    `/categories/${categoryUuid}/form-schema`
  );
  return response.data;
}

export async function getCategoryFilters(categoryUuid: string): Promise<CategoryFiltersResponse> {
  const response = await apiClient.get<CategoryFiltersResponse>(
    `/categories/${categoryUuid}/filters`
  );
  return response.data;
}

export async function searchItems(params: SearchItemsParams = {}): Promise<ItemPagedResponse> {
  const { fieldFilters, ...rest } = params;
  const response = await apiClient.get<ItemPagedResponse>("/catalog/items", {
    params: { ...rest, ...buildFieldFilterQueryParams(fieldFilters) },
  });
  return response.data;
}

/**
 * Converts dynamic schema field filter values into the `field.<key>` query parameter convention
 * expected by the backend. Blank strings and undefined values are omitted so unrelated params are
 * not sent for cleared filters.
 */
function buildFieldFilterQueryParams(fieldFilters?: SchemaFieldFilterValues): Record<string, string | string[]> {
  if (!fieldFilters) {
    return {};
  }

  const result: Record<string, string | string[]> = {};
  for (const [key, value] of Object.entries(fieldFilters)) {
    if (value === undefined || value === null) {
      continue;
    }
    if (typeof value === "boolean") {
      result[`field.${key}`] = String(value);
      continue;
    }
    if (Array.isArray(value)) {
      const nonEmpty = value.filter((v) => v !== undefined && v !== null && v !== "");
      if (nonEmpty.length > 0) {
        result[`field.${key}`] = nonEmpty;
      }
      continue;
    }
    if (value !== "") {
      result[`field.${key}`] = value;
    }
  }
  return result;
}

export async function listRecommendations(
  params: RecommendationParams = {}
): Promise<RecommendationPagedResponse> {
  const response = await apiClient.get<RecommendationPagedResponse>("/catalog/recommendations", { params });
  return response.data;
}

export async function getItemByUuid(uuid: string): Promise<ItemDetailResponse> {
  const response = await apiClient.get<ItemDetailResponse>(`/catalog/items/${uuid}`);
  return response.data;
}

export async function listMyItems(params: MyItemsParams = {}): Promise<ItemPagedResponse> {
  const response = await apiClient.get<ItemPagedResponse>("/catalog/items/mine", { params });
  return response.data;
}

export async function listFavoriteItems(
  params: FavoriteItemsParams = {}
): Promise<ItemPagedResponse> {
  const response = await apiClient.get<ItemPagedResponse>("/catalog/favorites", { params });
  return response.data;
}

export async function createItem(data: CreateItemRequest): Promise<ItemDetailResponse> {
  const response = await apiClient.post<ItemDetailResponse>("/catalog/items", data);
  return response.data;
}

export async function updateItem(uuid: string, data: UpdateItemRequest): Promise<ItemDetailResponse> {
  const response = await apiClient.patch<ItemDetailResponse>(`/catalog/items/${uuid}`, data);
  return response.data;
}

export async function archiveItem(uuid: string, data?: ArchiveItemRequest): Promise<ItemDetailResponse> {
  const response = await apiClient.post<ItemDetailResponse>(`/catalog/items/${uuid}/archive`, data ?? {});
  return response.data;
}

export async function favoriteItem(itemUuid: string): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>(`/catalog/items/${itemUuid}/favorite`);
  return response.data;
}

export async function unfavoriteItem(itemUuid: string): Promise<void> {
  await apiClient.delete(`/catalog/items/${itemUuid}/favorite`);
}

