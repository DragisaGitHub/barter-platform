import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createTradeOffer,
  listIncomingTradeOffers,
  listSentTradeOffers,
  getTradeOfferByUuid,
  acceptTradeOffer,
  rejectTradeOffer,
  cancelTradeOffer,
  type ListTradeOffersParams,
} from "@/api/tradeOfferApi.ts";
import type {
  TradeOfferPagedResponse,
  TradeOfferResponse,
  CreateTradeOfferRequest,
} from "@/api/generated/types.ts";
import { catalogKeys } from "@/features/catalog/useCatalog.ts";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const tradeOfferKeys = {
  all: ["trade-offers"] as const,
  incoming: (params: ListTradeOffersParams) => ["trade-offers", "incoming", params] as const,
  sent: (params: ListTradeOffersParams) => ["trade-offers", "sent", params] as const,
  detail: (uuid: string) => ["trade-offers", uuid] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function useIncomingTradeOffers(params: ListTradeOffersParams = {}) {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.incoming(params),
    queryFn: () => listIncomingTradeOffers(params),
  });
}

export function useSentTradeOffers(params: ListTradeOffersParams = {}) {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.sent(params),
    queryFn: () => listSentTradeOffers(params),
  });
}

export function useTradeOffer(uuid: string) {
  return useQuery<TradeOfferResponse>({
    queryKey: tradeOfferKeys.detail(uuid),
    queryFn: () => getTradeOfferByUuid(uuid),
    enabled: !!uuid,
  });
}

// ─── Mutation hooks ─────────────────────────────────────────────────────────

export function useCreateTradeOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTradeOfferRequest) => createTradeOffer(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
    },
  });
}

export function useAcceptTradeOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (uuid: string) => acceptTradeOffer(uuid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
      queryClient.invalidateQueries({ queryKey: catalogKeys.items });
    },
  });
}

export function useRejectTradeOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (uuid: string) => rejectTradeOffer(uuid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
    },
  });
}

export function useCancelTradeOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (uuid: string) => cancelTradeOffer(uuid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
    },
  });
}

