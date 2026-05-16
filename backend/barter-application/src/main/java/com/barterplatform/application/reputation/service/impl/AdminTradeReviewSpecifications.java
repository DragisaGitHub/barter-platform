package com.barterplatform.application.reputation.service.impl;

import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

final class AdminTradeReviewSpecifications {

    private AdminTradeReviewSpecifications() {
    }

    static Specification<TradeReviewEntity> ratingEquals(TradeReviewRating rating) {
        return (root, query, cb) -> cb.equal(root.get("rating"), rating);
    }

    static Specification<TradeReviewEntity> negativeReasonEquals(TradeReviewNegativeReason negativeReason) {
        return (root, query, cb) -> cb.equal(root.get("negativeReason"), negativeReason);
    }

    static Specification<TradeReviewEntity> reviewedUserIdIn(Iterable<Long> reviewedUserIds) {
        return inClause("reviewedUserId", reviewedUserIds);
    }

    static Specification<TradeReviewEntity> reviewerUserIdIn(Iterable<Long> reviewerUserIds) {
        return inClause("reviewerUserId", reviewerUserIds);
    }

    private static Specification<TradeReviewEntity> inClause(String fieldName, Iterable<Long> values) {
        return (root, query, cb) -> {
            if (values == null) {
                return cb.disjunction();
            }

            CriteriaBuilder.In<Long> inClause = cb.in(root.get(fieldName));
            boolean hasValues = false;
            for (Long value : values) {
                inClause.value(value);
                hasValues = true;
            }

            return hasValues ? inClause : cb.disjunction();
        };
    }
}

