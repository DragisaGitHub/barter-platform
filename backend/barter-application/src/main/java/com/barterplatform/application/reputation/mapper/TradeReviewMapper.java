package com.barterplatform.application.reputation.mapper;

import com.barterplatform.api.model.AdminTradeReviewSummaryResponse;
import com.barterplatform.api.model.TradeReviewNegativeReason;
import com.barterplatform.api.model.TradeReviewRating;
import com.barterplatform.api.model.TradeReviewResponse;
import com.barterplatform.api.model.TradeReviewResponse1;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import org.springframework.stereotype.Component;

@Component
public class TradeReviewMapper {

    public TradeReviewResponse toResponse(
            TradeReviewEntity entity,
            TradeOfferEntity tradeOffer,
            UserEntity reviewer,
            UserEntity reviewed) {
        return new TradeReviewResponse()
                .uuid(entity.getUuid())
                .tradeOfferUuid(tradeOffer.getUuid())
                .reviewerUserUuid(reviewer.getUuid())
                .reviewerUsername(reviewer.getUsername())
                .reviewedUserUuid(reviewed.getUuid())
                .reviewedUsername(reviewed.getUsername())
                .rating(map(entity.getRating()))
                .negativeReason(map(entity.getNegativeReason()))
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt());
    }

    public TradeReviewResponse1 toEmbeddedResponse(
            TradeReviewEntity entity,
            TradeOfferEntity tradeOffer,
            UserEntity reviewer,
            UserEntity reviewed) {
        return new TradeReviewResponse1()
                .uuid(entity.getUuid())
                .tradeOfferUuid(tradeOffer.getUuid())
                .reviewerUserUuid(reviewer.getUuid())
                .reviewerUsername(reviewer.getUsername())
                .reviewedUserUuid(reviewed.getUuid())
                .reviewedUsername(reviewed.getUsername())
                .rating(map(entity.getRating()))
                .negativeReason(map(entity.getNegativeReason()))
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt());
    }

    public AdminTradeReviewSummaryResponse toAdminSummaryResponse(
            TradeReviewEntity entity,
            TradeOfferEntity tradeOffer,
            UserEntity reviewer,
            UserEntity reviewed) {
        return new AdminTradeReviewSummaryResponse()
                .uuid(entity.getUuid())
                .tradeOfferUuid(tradeOffer.getUuid())
                .reviewerUserUuid(reviewer.getUuid())
                .reviewerUsername(reviewer.getUsername())
                .reviewedUserUuid(reviewed.getUuid())
                .reviewedUsername(reviewed.getUsername())
                .rating(map(entity.getRating()))
                .negativeReason(map(entity.getNegativeReason()))
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt());
    }

    public TradeReviewRating map(com.barterplatform.domain.reputation.enums.TradeReviewRating rating) {
        return rating == null ? null : TradeReviewRating.valueOf(rating.name());
    }

    public TradeReviewNegativeReason map(com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason reason) {
        return reason == null ? null : TradeReviewNegativeReason.valueOf(reason.name());
    }

    public com.barterplatform.domain.reputation.enums.TradeReviewRating map(TradeReviewRating rating) {
        return rating == null ? null : com.barterplatform.domain.reputation.enums.TradeReviewRating.valueOf(rating.name());
    }

    public com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason map(TradeReviewNegativeReason reason) {
        return reason == null ? null : com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason.valueOf(reason.name());
    }
}

