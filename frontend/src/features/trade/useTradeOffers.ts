import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createTradeOffer,
  listIncomingTradeOffers,
  listSentTradeOffers,
  getTradeOfferByUuid,
  acceptTradeOffer,
  confirmTradeOfferCompletion,
  rejectTradeOffer,
  cancelTradeOffer,
  createTradeReview,
  type ListTradeOffersParams,
} from "@/api/tradeOfferApi.ts";
import type {
  TradeOfferPagedResponse,
  TradeOfferResponse,
  CreateTradeOfferRequest,
  CreateTradeReviewRequest,
} from "@/api/generated/types.ts";
import { catalogKeys } from "@/features/catalog/useCatalog.ts";
import { profileKeys } from "@/features/profile/useProfile.ts";
import { notificationKeys } from "@/features/notifications/useNotifications.ts";

// ─── Query keys ─────────────────────────────────────────────────────────────

export const tradeOfferKeys = {
  all: ["trade-offers"] as const,
  incoming: (params: ListTradeOffersParams) => ["trade-offers", "incoming", params] as const,
  sent: (params: ListTradeOffersParams) => ["trade-offers", "sent", params] as const,
  detail: (uuid: string) => ["trade-offers", uuid] as const,
  pendingIncoming: ["trade-offers", "pending-incoming-count"] as const,
  pendingSent: ["trade-offers", "pending-sent-count"] as const,
};

// ─── Query hooks ────────────────────────────────────────────────────────────

export function useIncomingTradeOffers(params: ListTradeOffersParams = {}) {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.incoming(params),
    queryFn: () => listIncomingTradeOffers(params),
    refetchInterval: 20_000,
  });
}

export function useSentTradeOffers(params: ListTradeOffersParams = {}) {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.sent(params),
    queryFn: () => listSentTradeOffers(params),
    refetchInterval: 30_000,
  });
}

export function useTradeOffer(uuid: string) {
  return useQuery<TradeOfferResponse>({
    queryKey: tradeOfferKeys.detail(uuid),
    queryFn: () => getTradeOfferByUuid(uuid),
    enabled: !!uuid,
  });
}

// ─── Badge count hooks (lightweight, for sidebar) ───────────────────────────

export function usePendingIncomingCount() {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.pendingIncoming,
    queryFn: () => listIncomingTradeOffers({ status: "PENDING", page: 0, size: 1 }),
    refetchInterval: 20_000,
    staleTime: 10_000,
  });
}

export function usePendingSentCount() {
  return useQuery<TradeOfferPagedResponse>({
    queryKey: tradeOfferKeys.pendingSent,
    queryFn: () => listSentTradeOffers({ status: "PENDING", page: 0, size: 1 }),
    refetchInterval: 30_000,
    staleTime: 15_000,
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

export function useConfirmTradeOfferCompletion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (uuid: string) => confirmTradeOfferCompletion(uuid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
      queryClient.invalidateQueries({ queryKey: profileKeys.all });
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
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

export function useCreateTradeReview() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tradeOfferUuid, data }: { tradeOfferUuid: string; data: CreateTradeReviewRequest }) =>
      createTradeReview(tradeOfferUuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tradeOfferKeys.all });
      queryClient.invalidateQueries({ queryKey: profileKeys.all });
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

