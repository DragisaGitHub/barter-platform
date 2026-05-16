import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { listAdminReviews, type ListAdminReviewsParams } from "@/api/adminReviewsApi.ts";
import type { AdminTradeReviewPagedResponse } from "@/api/generated/types.ts";

export const adminReviewKeys = {
  all: ["admin", "reviews"] as const,
  lists: () => ["admin", "reviews", "list"] as const,
  list: (params: ListAdminReviewsParams) => ["admin", "reviews", "list", params] as const,
};

export function useAdminReviews(params: ListAdminReviewsParams = {}, enabled = true) {
  return useQuery<AdminTradeReviewPagedResponse>({
    queryKey: adminReviewKeys.list(params),
    queryFn: () => listAdminReviews(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

