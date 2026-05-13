import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    listTradeOfferMessages,
    sendTradeOfferMessage,
} from "@/api/tradeOfferMessagesApi";
import type {
    SendTradeOfferMessageRequest,
    TradeOfferMessageResponse,
} from "@/api/generated/types";

export const tradeOfferMessageKeys = {
    all: ["trade-offer-messages"] as const,

    byOffer: (tradeOfferUuid: string) =>
        [...tradeOfferMessageKeys.all, tradeOfferUuid] as const,
};

export type TradeOfferMessageListItem = TradeOfferMessageResponse & {
    isOptimistic?: boolean;
};

interface MessageMutationContext {
    previousMessages: TradeOfferMessageListItem[];
    optimisticUuid: string;
}

interface CurrentMessageUser {
    uuid?: string;
    username?: string;
}

export function useTradeOfferMessages(tradeOfferUuid: string) {
    return useQuery<TradeOfferMessageListItem[]>({
        queryKey: tradeOfferMessageKeys.byOffer(tradeOfferUuid),
        queryFn: () => listTradeOfferMessages(tradeOfferUuid),
        enabled: Boolean(tradeOfferUuid),
        refetchInterval: 15000,
    });
}

export function useSendTradeOfferMessage(
    tradeOfferUuid: string,
    currentUser?: CurrentMessageUser,
) {
    const queryClient = useQueryClient();
    const queryKey = tradeOfferMessageKeys.byOffer(tradeOfferUuid);

    return useMutation<
        TradeOfferMessageResponse,
        unknown,
        SendTradeOfferMessageRequest,
        MessageMutationContext
    >({
        mutationFn: (data: SendTradeOfferMessageRequest) =>
            sendTradeOfferMessage(tradeOfferUuid, data),

        onMutate: async (data) => {
            await queryClient.cancelQueries({ queryKey });

            const previousMessages =
                queryClient.getQueryData<TradeOfferMessageListItem[]>(queryKey) ?? [];
            const optimisticUuid = `optimistic-${Date.now()}`;

            queryClient.setQueryData<TradeOfferMessageListItem[]>(queryKey, [
                ...previousMessages,
                {
                    uuid: optimisticUuid,
                    tradeOfferUuid,
                    senderUserUuid: currentUser?.uuid ?? "",
                    senderUsername: currentUser?.username ?? "You",
                    recipientUserUuid: "",
                    recipientUsername: "",
                    content: data.content,
                    isRead: true,
                    readAt: null,
                    createdAt: new Date().toISOString(),
                    isOptimistic: true,
                },
            ]);

            return {
                previousMessages,
                optimisticUuid,
            };
        },

        onError: (_error, _variables, context) => {
            if (!context) {
                return;
            }

            queryClient.setQueryData(queryKey, context.previousMessages);
        },

        onSuccess: async (message, _variables, context) => {
            queryClient.setQueryData<TradeOfferMessageListItem[]>(queryKey, (current = []) => {
                const withoutOptimistic = current.filter(
                    (existingMessage) => existingMessage.uuid !== context?.optimisticUuid,
                );

                if (withoutOptimistic.some((existingMessage) => existingMessage.uuid === message.uuid)) {
                    return withoutOptimistic;
                }

                return [...withoutOptimistic, message];
            });

            await queryClient.invalidateQueries({
                queryKey,
            });
        },
    });
}