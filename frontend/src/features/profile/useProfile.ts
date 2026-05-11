import { useQuery } from "@tanstack/react-query";
import {
  getPublicProfile,
  getPublicProfileItems,
  type PublicProfileItemsParams,
} from "@/api/profileApi.ts";
import type { PublicProfileResponse, ItemPagedResponse } from "@/api/generated/types.ts";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const profileKeys = {
  all: ["profiles"] as const,
  profile: (userUuid: string) => ["profiles", userUuid] as const,
  items: (userUuid: string, params: PublicProfileItemsParams) =>
    ["profiles", userUuid, "items", params] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function usePublicProfile(userUuid: string) {
  return useQuery<PublicProfileResponse>({
    queryKey: profileKeys.profile(userUuid),
    queryFn: () => getPublicProfile(userUuid),
    enabled: !!userUuid,
  });
}

export function usePublicProfileItems(
  userUuid: string,
  params: PublicProfileItemsParams = {}
) {
  return useQuery<ItemPagedResponse>({
    queryKey: profileKeys.items(userUuid, params),
    queryFn: () => getPublicProfileItems(userUuid, params),
    enabled: !!userUuid,
  });
}

