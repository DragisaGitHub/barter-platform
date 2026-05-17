import { apiClient } from "./axios";
import type {
  ReviewDirection,
  TradeReviewRating,
  UserTradeReviewPagedResponse,
} from "./generated/types";

export interface ListReviewsParams {
  direction: ReviewDirection;
  page?: number;
  size?: number;
  sort?: string;
  rating?: TradeReviewRating;
}

export async function listReviews(params: ListReviewsParams): Promise<UserTradeReviewPagedResponse> {
  const response = await apiClient.get<UserTradeReviewPagedResponse>("/reviews", { params });
  return response.data;
}

