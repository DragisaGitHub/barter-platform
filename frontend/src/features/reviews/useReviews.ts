import { useQuery } from "@tanstack/react-query";
import { listReviews, type ListReviewsParams } from "@/api/reviewsApi.ts";
import type { UserTradeReviewPagedResponse } from "@/api/generated/types.ts";

export const reviewKeys = {
  all: ["reviews"] as const,
  list: (params: ListReviewsParams) => ["reviews", "list", params] as const,
};

export function useReviews(params: ListReviewsParams) {
  return useQuery<UserTradeReviewPagedResponse>({
    queryKey: reviewKeys.list(params),
    queryFn: () => listReviews(params),
    staleTime: 30_000,
  });
}

