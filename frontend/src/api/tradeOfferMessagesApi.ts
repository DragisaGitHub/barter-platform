import { apiClient } from "./axios";
import type {
    TradeOfferMessageResponse,
    SendTradeOfferMessageRequest,
} from "./generated/types";

export interface TradeMessageUnreadCountResponse {
    count: number;
}

export async function getUnreadTradeMessageCount(): Promise<TradeMessageUnreadCountResponse> {
    const response = await apiClient.get<TradeMessageUnreadCountResponse>(
        "/trade-offers/messages/unread-count",
    );
    return response.data;
}

export async function listTradeOfferMessages(
    tradeOfferUuid: string,
): Promise<TradeOfferMessageResponse[]> {
    const response = await apiClient.get<TradeOfferMessageResponse[]>(
        `/trade-offers/${tradeOfferUuid}/messages`,
    );

    return response.data;
}

export async function sendTradeOfferMessage(
    tradeOfferUuid: string,
    data: SendTradeOfferMessageRequest,
): Promise<TradeOfferMessageResponse> {
    const response = await apiClient.post<TradeOfferMessageResponse>(
        `/trade-offers/${tradeOfferUuid}/messages`,
        data,
    );

    return response.data;
}