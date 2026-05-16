import { apiClient } from "./axios";
import type {
  CreateTradeOfferRequest,
  CreateTradeReviewRequest,
  TradeOfferResponse,
  TradeOfferPagedResponse,
  TradeReviewResponse,
  TradeOfferStatus,
} from "./generated/types";

// ─── Query parameter types ──────────────────────────────────────────────────

export interface ListTradeOffersParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: TradeOfferStatus;
}

// ─── API functions ──────────────────────────────────────────────────────────

export async function createTradeOffer(data: CreateTradeOfferRequest): Promise<TradeOfferResponse> {
  const response = await apiClient.post<TradeOfferResponse>("/trade-offers", data);
  return response.data;
}

export async function listIncomingTradeOffers(
  params: ListTradeOffersParams = {},
): Promise<TradeOfferPagedResponse> {
  const response = await apiClient.get<TradeOfferPagedResponse>("/trade-offers/incoming", { params });
  return response.data;
}

export async function listSentTradeOffers(
  params: ListTradeOffersParams = {},
): Promise<TradeOfferPagedResponse> {
  const response = await apiClient.get<TradeOfferPagedResponse>("/trade-offers/sent", { params });
  return response.data;
}

export async function getTradeOfferByUuid(uuid: string): Promise<TradeOfferResponse> {
  const response = await apiClient.get<TradeOfferResponse>(`/trade-offers/${uuid}`);
  return response.data;
}

export async function acceptTradeOffer(uuid: string): Promise<TradeOfferResponse> {
  const response = await apiClient.post<TradeOfferResponse>(`/trade-offers/${uuid}/accept`);
  return response.data;
}

export async function confirmTradeOfferCompletion(uuid: string): Promise<TradeOfferResponse> {
  const response = await apiClient.post<TradeOfferResponse>(`/trade-offers/${uuid}/confirm-completion`);
  return response.data;
}

export async function rejectTradeOffer(uuid: string): Promise<TradeOfferResponse> {
  const response = await apiClient.post<TradeOfferResponse>(`/trade-offers/${uuid}/reject`);
  return response.data;
}

export async function cancelTradeOffer(uuid: string): Promise<TradeOfferResponse> {
  const response = await apiClient.post<TradeOfferResponse>(`/trade-offers/${uuid}/cancel`);
  return response.data;
}

export async function createTradeReview(
  tradeOfferUuid: string,
  data: CreateTradeReviewRequest,
): Promise<TradeReviewResponse> {
  const response = await apiClient.post<TradeReviewResponse>(`/trade-offers/${tradeOfferUuid}/reviews`, data);
  return response.data;
}

