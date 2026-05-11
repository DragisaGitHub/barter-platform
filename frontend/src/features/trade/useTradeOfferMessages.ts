import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    listTradeOfferMessages,
    sendTradeOfferMessage,
} from "@/api/tradeOfferMessagesApi";
import type { SendTradeOfferMessageRequest } from "@/api/generated/types";

export const tradeOfferMessageKeys = {
    all: ["trade-offer-messages"] as const,

    byOffer: (tradeOfferUuid: string) =>
        [...tradeOfferMessageKeys.all, tradeOfferUuid] as const,
};

export function useTradeOfferMessages(tradeOfferUuid: string) {
    return useQuery({
        queryKey: tradeOfferMessageKeys.byOffer(tradeOfferUuid),
        queryFn: () => listTradeOfferMessages(tradeOfferUuid),
        enabled: Boolean(tradeOfferUuid),
        refetchInterval: 15000,
    });
}

export function useSendTradeOfferMessage(tradeOfferUuid: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: SendTradeOfferMessageRequest) =>
            sendTradeOfferMessage(tradeOfferUuid, data),

        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: tradeOfferMessageKeys.byOffer(tradeOfferUuid),
            });
        },
    });
}