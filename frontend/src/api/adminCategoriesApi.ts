import { apiClient } from "./axios";
import type {
  AdminCategoryPagedResponse,
  AdminCategoryResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from "./generated/types";

export interface ListAdminCategoriesParams {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  includeDeleted?: boolean;
}

export async function listAdminCategories(
  params: ListAdminCategoriesParams = {}
): Promise<AdminCategoryPagedResponse> {
  const response = await apiClient.get<AdminCategoryPagedResponse>("/admin/categories", { params });
  return response.data;
}

export async function getAdminCategory(categoryUuid: string): Promise<AdminCategoryResponse> {
  const response = await apiClient.get<AdminCategoryResponse>(`/admin/categories/${categoryUuid}`);
  return response.data;
}

export async function createAdminCategory(data: CreateCategoryRequest): Promise<AdminCategoryResponse> {
  const response = await apiClient.post<AdminCategoryResponse>("/admin/categories", data);
  return response.data;
}

export async function updateAdminCategory(
  categoryUuid: string,
  data: UpdateCategoryRequest
): Promise<AdminCategoryResponse> {
  const response = await apiClient.patch<AdminCategoryResponse>(`/admin/categories/${categoryUuid}`, data);
  return response.data;
}

export async function deleteAdminCategory(categoryUuid: string): Promise<void> {
  await apiClient.delete(`/admin/categories/${categoryUuid}`);
}

