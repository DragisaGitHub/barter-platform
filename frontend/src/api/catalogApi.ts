import { apiClient } from "./axios";
import type {
  CategoryResponse,
  TagResponse,
  ItemPagedResponse,
  ItemDetailResponse,
  MessageResponse,
  CreateItemRequest,
  UpdateItemRequest,
  ArchiveItemRequest,
  ItemStatus,
  ItemCondition,
} from "./generated/types";

// ─── Query parameter types ──────────────────────────────────────────────────

export interface SearchItemsParams {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  categoryUuid?: string;
  tagUuids?: string[];
  status?: ItemStatus;
  condition?: ItemCondition;
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

// ─── API functions ──────────────────────────────────────────────────────────

export async function listCategories(): Promise<CategoryResponse[]> {
  const response = await apiClient.get<CategoryResponse[]>("/catalog/categories");
  return response.data;
}

export async function listTags(): Promise<TagResponse[]> {
  const response = await apiClient.get<TagResponse[]>("/catalog/tags");
  return response.data;
}

export async function searchItems(params: SearchItemsParams = {}): Promise<ItemPagedResponse> {
  const response = await apiClient.get<ItemPagedResponse>("/catalog/items", { params });
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

