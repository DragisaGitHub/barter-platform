import { apiClient } from "./axios";
import type {
  AdminTradeReviewPagedResponse,
  TradeReviewNegativeReason,
  TradeReviewRating,
} from "./generated/types";

export interface ListAdminReviewsParams {
  page?: number;
  size?: number;
  sort?: string;
  rating?: TradeReviewRating;
  negativeReason?: TradeReviewNegativeReason;
  reviewedUserQuery?: string;
  reviewerUserQuery?: string;
}

export async function listAdminReviews(
  params: ListAdminReviewsParams = {},
): Promise<AdminTradeReviewPagedResponse> {
  const response = await apiClient.get<AdminTradeReviewPagedResponse>("/admin/reviews", { params });
  return response.data;
}

