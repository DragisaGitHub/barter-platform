import { apiClient } from "./axios";
import type {
  AdminListingDetailResponse,
  AdminListingPagedResponse,
  AdminRemoveListingRequest,
  AdminRestoreListingRequest,
  ItemStatus,
  ListingModerationActionResponse,
} from "./generated/types";

export interface ListAdminListingsParams {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  ownerQuery?: string;
  categoryUuid?: string;
  status?: ItemStatus;
}

export async function listAdminListings(
  params: ListAdminListingsParams = {}
): Promise<AdminListingPagedResponse> {
  const response = await apiClient.get<AdminListingPagedResponse>("/admin/listings", { params });
  return response.data;
}

export async function getAdminListing(listingUuid: string): Promise<AdminListingDetailResponse> {
  const response = await apiClient.get<AdminListingDetailResponse>(`/admin/listings/${listingUuid}`);
  return response.data;
}

export async function listListingModerationActions(
  listingUuid: string
): Promise<ListingModerationActionResponse[]> {
  const response = await apiClient.get<ListingModerationActionResponse[]>(
    `/admin/listings/${listingUuid}/moderation-actions`
  );
  return response.data;
}

export async function removeAdminListing(
  listingUuid: string,
  data: AdminRemoveListingRequest
): Promise<AdminListingDetailResponse> {
  const response = await apiClient.post<AdminListingDetailResponse>(`/admin/listings/${listingUuid}/remove`, data);
  return response.data;
}

export async function restoreAdminListing(
  listingUuid: string,
  data: AdminRestoreListingRequest
): Promise<AdminListingDetailResponse> {
  const response = await apiClient.post<AdminListingDetailResponse>(`/admin/listings/${listingUuid}/restore`, data);
  return response.data;
}

