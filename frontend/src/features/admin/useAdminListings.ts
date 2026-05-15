import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAdminListing,
  listAdminListings,
  listListingModerationActions,
  removeAdminListing,
  restoreAdminListing,
  type ListAdminListingsParams,
} from "@/api/adminListingsApi.ts";
import type {
  AdminListingDetailResponse,
  AdminListingPagedResponse,
  AdminRemoveListingRequest,
  AdminRestoreListingRequest,
  ListingModerationActionResponse,
} from "@/api/generated/types.ts";
import { catalogKeys } from "@/features/catalog/useCatalog.ts";

export const adminListingKeys = {
  all: ["admin", "listings"] as const,
  lists: () => ["admin", "listings", "list"] as const,
  list: (params: ListAdminListingsParams) => ["admin", "listings", "list", params] as const,
  details: () => ["admin", "listings", "detail"] as const,
  detail: (listingUuid: string) => ["admin", "listings", "detail", listingUuid] as const,
  moderationActions: (listingUuid: string) => ["admin", "listings", "moderation-actions", listingUuid] as const,
};

export function useAdminListings(params: ListAdminListingsParams = {}, enabled = true) {
  return useQuery<AdminListingPagedResponse>({
    queryKey: adminListingKeys.list(params),
    queryFn: () => listAdminListings(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminListing(listingUuid: string, enabled = true) {
  return useQuery<AdminListingDetailResponse>({
    queryKey: adminListingKeys.detail(listingUuid),
    queryFn: () => getAdminListing(listingUuid),
    enabled: enabled && !!listingUuid,
  });
}

export function useListingModerationActions(listingUuid: string, enabled = true) {
  return useQuery<ListingModerationActionResponse[]>({
    queryKey: adminListingKeys.moderationActions(listingUuid),
    queryFn: () => listListingModerationActions(listingUuid),
    enabled: enabled && !!listingUuid,
    placeholderData: keepPreviousData,
  });
}

function invalidateListingRelatedQueries(queryClient: ReturnType<typeof useQueryClient>, listingUuid: string) {
  queryClient.invalidateQueries({ queryKey: adminListingKeys.all });
  queryClient.invalidateQueries({ queryKey: catalogKeys.items });
  queryClient.invalidateQueries({ queryKey: catalogKeys.itemDetail(listingUuid) });
  queryClient.invalidateQueries({ queryKey: catalogKeys.favorites });
}

export function useRemoveAdminListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ listingUuid, data }: { listingUuid: string; data: AdminRemoveListingRequest }) =>
      removeAdminListing(listingUuid, data),
    onSuccess: (listing, { listingUuid }) => {
      queryClient.setQueryData(adminListingKeys.detail(listingUuid), listing);
      invalidateListingRelatedQueries(queryClient, listingUuid);
      queryClient.invalidateQueries({ queryKey: adminListingKeys.moderationActions(listingUuid) });
    },
  });
}

export function useRestoreAdminListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ listingUuid, data }: { listingUuid: string; data: AdminRestoreListingRequest }) =>
      restoreAdminListing(listingUuid, data),
    onSuccess: (listing, { listingUuid }) => {
      queryClient.setQueryData(adminListingKeys.detail(listingUuid), listing);
      invalidateListingRelatedQueries(queryClient, listingUuid);
      queryClient.invalidateQueries({ queryKey: adminListingKeys.moderationActions(listingUuid) });
    },
  });
}

