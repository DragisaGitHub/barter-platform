package com.barterplatform.application.reputation.service;

import com.barterplatform.api.model.CreateTradeReviewRequest;
import com.barterplatform.api.model.TradeReviewResponse;
import java.util.UUID;

public interface TradeReviewService {

    TradeReviewResponse createReview(UUID currentUserUuid, UUID tradeOfferUuid, CreateTradeReviewRequest request);
}

