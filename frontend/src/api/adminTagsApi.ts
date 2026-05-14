import { apiClient } from "./axios";
import type {
  AdminTagPagedResponse,
  AdminTagResponse,
  CreateTagRequest,
  UpdateTagRequest,
} from "./generated/types";

export interface ListAdminTagsParams {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  includeDeleted?: boolean;
}

export async function listAdminTags(params: ListAdminTagsParams = {}): Promise<AdminTagPagedResponse> {
  const response = await apiClient.get<AdminTagPagedResponse>("/admin/tags", { params });
  return response.data;
}

export async function getAdminTag(tagUuid: string): Promise<AdminTagResponse> {
  const response = await apiClient.get<AdminTagResponse>(`/admin/tags/${tagUuid}`);
  return response.data;
}

export async function createAdminTag(data: CreateTagRequest): Promise<AdminTagResponse> {
  const response = await apiClient.post<AdminTagResponse>("/admin/tags", data);
  return response.data;
}

export async function updateAdminTag(tagUuid: string, data: UpdateTagRequest): Promise<AdminTagResponse> {
  const response = await apiClient.patch<AdminTagResponse>(`/admin/tags/${tagUuid}`, data);
  return response.data;
}

export async function deleteAdminTag(tagUuid: string): Promise<void> {
  await apiClient.delete(`/admin/tags/${tagUuid}`);
}

