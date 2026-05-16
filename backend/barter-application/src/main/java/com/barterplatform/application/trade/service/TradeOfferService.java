package com.barterplatform.application.trade.service;

import com.barterplatform.api.model.CreateTradeOfferRequest;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import java.util.UUID;

public interface TradeOfferService {

    /**
     * Create a new trade offer. The current user is the sender.
     *
     * @param currentUserUuid the authenticated user's UUID (sender)
     * @param request         creation payload with senderItemUuid and receiverItemUuid
     * @return full trade offer detail
     */
    TradeOfferResponse createOffer(UUID currentUserUuid, CreateTradeOfferRequest request);

    /**
     * List incoming trade offers (where the current user is the receiver).
     */
    TradeOfferPagedResponse listIncoming(UUID currentUserUuid, Integer page, Integer size,
                                         String sort, TradeOfferStatus status);

    /**
     * List sent trade offers (where the current user is the sender).
     */
    TradeOfferPagedResponse listSent(UUID currentUserUuid, Integer page, Integer size,
                                      String sort, TradeOfferStatus status);

    /**
     * Get a single trade offer by UUID. Only sender or receiver may view.
     */
    TradeOfferResponse getOffer(UUID currentUserUuid, UUID offerUuid);

    /**
     * Accept a pending trade offer. Only the receiver may accept.
     * Both items are archived and competing pending offers are rejected.
     */
    TradeOfferResponse acceptOffer(UUID currentUserUuid, UUID offerUuid);

    /**
     * Confirm exchange completion for an accepted trade offer. Only the sender or receiver may confirm.
     */
    TradeOfferResponse confirmCompletion(UUID currentUserUuid, UUID offerUuid);

    /**
     * Reject a pending trade offer. Only the receiver may reject.
     */
    TradeOfferResponse rejectOffer(UUID currentUserUuid, UUID offerUuid);

    /**
     * Cancel a pending trade offer. Only the sender may cancel.
     */
    TradeOfferResponse cancelOffer(UUID currentUserUuid, UUID offerUuid);
}

