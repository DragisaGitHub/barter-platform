package com.barterplatform.domain.reputation.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "trade_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeReviewEntity extends AuditableEntity {

    @Column(name = "trade_offer_id", nullable = false, updatable = false)
    private Long tradeOfferId;

    @Column(name = "reviewer_user_id", nullable = false, updatable = false)
    private Long reviewerUserId;

    @Column(name = "reviewed_user_id", nullable = false, updatable = false)
    private Long reviewedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 32, updatable = false)
    private TradeReviewRating rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "negative_reason", length = 64, updatable = false)
    private TradeReviewNegativeReason negativeReason;

    @Column(name = "comment", columnDefinition = "TEXT", updatable = false)
    private String comment;

    public static TradeReviewEntity create(
            Long tradeOfferId,
            Long reviewerUserId,
            Long reviewedUserId,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            String comment) {
        validate(tradeOfferId, reviewerUserId, reviewedUserId, rating, negativeReason, comment);

        TradeReviewEntity entity = new TradeReviewEntity();
        entity.tradeOfferId = tradeOfferId;
        entity.reviewerUserId = reviewerUserId;
        entity.reviewedUserId = reviewedUserId;
        entity.rating = rating;
        entity.negativeReason = negativeReason;
        entity.comment = normalizeComment(comment);
        return entity;
    }


    private static void validate(
            Long tradeOfferId,
            Long reviewerUserId,
            Long reviewedUserId,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            String comment) {
        if (tradeOfferId == null) {
            throw new IllegalArgumentException("tradeOfferId is required.");
        }
        if (reviewerUserId == null) {
            throw new IllegalArgumentException("reviewerUserId is required.");
        }
        if (reviewedUserId == null) {
            throw new IllegalArgumentException("reviewedUserId is required.");
        }
        if (reviewerUserId.equals(reviewedUserId)) {
            throw new IllegalArgumentException("Reviewer and reviewed user must differ.");
        }
        if (rating == null) {
            throw new IllegalArgumentException("rating is required.");
        }
        if (rating == TradeReviewRating.POSITIVE && negativeReason != null) {
            throw new IllegalArgumentException("Positive reviews cannot include a negative reason.");
        }
        if (rating == TradeReviewRating.NEGATIVE && negativeReason == null) {
            throw new IllegalArgumentException("Negative reviews require a negative reason.");
        }
        if (negativeReason == TradeReviewNegativeReason.OTHER && normalizeComment(comment) == null) {
            throw new IllegalArgumentException("Comment is required when negative reason is OTHER.");
        }
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

