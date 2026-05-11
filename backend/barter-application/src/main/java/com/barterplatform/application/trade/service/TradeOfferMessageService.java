package com.barterplatform.application.trade.service;

import com.barterplatform.api.model.SendTradeOfferMessageRequest;
import com.barterplatform.api.model.TradeOfferMessageResponse;
import java.util.List;
import java.util.UUID;

public interface TradeOfferMessageService {

    List<TradeOfferMessageResponse> listMessages(UUID currentUserUuid, UUID tradeOfferUuid);

    TradeOfferMessageResponse sendMessage(
            UUID currentUserUuid,
            UUID tradeOfferUuid,
            SendTradeOfferMessageRequest request);
}
