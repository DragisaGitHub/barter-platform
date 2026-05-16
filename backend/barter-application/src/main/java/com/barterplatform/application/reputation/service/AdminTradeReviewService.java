package com.barterplatform.application.reputation.service;

import com.barterplatform.api.model.AdminTradeReviewPagedResponse;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;

public interface AdminTradeReviewService {

    AdminTradeReviewPagedResponse listReviews(
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            String reviewedUserQuery,
            String reviewerUserQuery);
}

