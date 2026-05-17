package com.barterplatform.application.reputation.service;

import com.barterplatform.api.model.CreateTradeReviewRequest;
import com.barterplatform.api.model.TradeReviewResponse;
import com.barterplatform.api.model.UserTradeReviewPagedResponse;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import java.util.UUID;

public interface TradeReviewService {

    enum Direction {
        RECEIVED,
        GIVEN
    }

    UserTradeReviewPagedResponse listReviews(
            UUID currentUserUuid,
            Direction direction,
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating);

    TradeReviewResponse createReview(UUID currentUserUuid, UUID tradeOfferUuid, CreateTradeReviewRequest request);
}

