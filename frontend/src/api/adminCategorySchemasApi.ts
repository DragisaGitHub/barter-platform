import { apiClient } from "./axios";
import type {
  CategorySchemaFieldResponse,
  CategorySchemaPagedResponse,
  CategorySchemaResponse,
  CategorySchemaStatus,
  CreateCategorySchemaFieldRequest,
  CreateCategorySchemaRequest,
  CreateFieldOptionRequest,
  FieldOptionResponse,
  UpdateCategorySchemaFieldRequest,
  UpdateCategorySchemaRequest,
  UpdateFieldOptionRequest,
} from "./generated/types";

export interface ListAdminCategorySchemasParams {
  page?: number;
  size?: number;
  sort?: string;
  categoryUuid?: string;
  status?: CategorySchemaStatus;
  includeDeleted?: boolean;
}

export async function listAdminCategorySchemas(
  params: ListAdminCategorySchemasParams = {}
): Promise<CategorySchemaPagedResponse> {
  const response = await apiClient.get<CategorySchemaPagedResponse>("/admin/category-schemas", { params });
  return response.data;
}

export async function getAdminCategorySchema(schemaUuid: string): Promise<CategorySchemaResponse> {
  const response = await apiClient.get<CategorySchemaResponse>(`/admin/category-schemas/${schemaUuid}`);
  return response.data;
}

export async function createAdminCategorySchema(
  categoryUuid: string,
  data: CreateCategorySchemaRequest
): Promise<CategorySchemaResponse> {
  const response = await apiClient.post<CategorySchemaResponse>(
    `/admin/categories/${categoryUuid}/schemas`,
    data
  );
  return response.data;
}

export async function updateAdminCategorySchema(
  schemaUuid: string,
  data: UpdateCategorySchemaRequest
): Promise<CategorySchemaResponse> {
  const response = await apiClient.patch<CategorySchemaResponse>(`/admin/category-schemas/${schemaUuid}`, data);
  return response.data;
}

export async function deleteAdminCategorySchema(schemaUuid: string): Promise<void> {
  await apiClient.delete(`/admin/category-schemas/${schemaUuid}`);
}

export async function activateAdminCategorySchema(schemaUuid: string): Promise<CategorySchemaResponse> {
  const response = await apiClient.post<CategorySchemaResponse>(`/admin/category-schemas/${schemaUuid}/activate`);
  return response.data;
}

export async function createAdminCategorySchemaField(
  schemaUuid: string,
  data: CreateCategorySchemaFieldRequest
): Promise<CategorySchemaFieldResponse> {
  const response = await apiClient.post<CategorySchemaFieldResponse>(
    `/admin/category-schemas/${schemaUuid}/fields`,
    data
  );
  return response.data;
}

export async function updateAdminCategorySchemaField(
  fieldUuid: string,
  data: UpdateCategorySchemaFieldRequest
): Promise<CategorySchemaFieldResponse> {
  const response = await apiClient.patch<CategorySchemaFieldResponse>(
    `/admin/category-schema-fields/${fieldUuid}`,
    data
  );
  return response.data;
}

export async function deleteAdminCategorySchemaField(fieldUuid: string): Promise<void> {
  await apiClient.delete(`/admin/category-schema-fields/${fieldUuid}`);
}

export async function createAdminCategorySchemaFieldOption(
  fieldUuid: string,
  data: CreateFieldOptionRequest
): Promise<FieldOptionResponse> {
  const response = await apiClient.post<FieldOptionResponse>(
    `/admin/category-schema-fields/${fieldUuid}/options`,
    data
  );
  return response.data;
}

export async function updateAdminCategorySchemaFieldOption(
  optionUuid: string,
  data: UpdateFieldOptionRequest
): Promise<FieldOptionResponse> {
  const response = await apiClient.patch<FieldOptionResponse>(
    `/admin/category-schema-field-options/${optionUuid}`,
    data
  );
  return response.data;
}

export async function deleteAdminCategorySchemaFieldOption(optionUuid: string): Promise<void> {
  await apiClient.delete(`/admin/category-schema-field-options/${optionUuid}`);
}

