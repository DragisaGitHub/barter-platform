package com.barterplatform.application.reputation.service.impl;

import com.barterplatform.api.model.ReputationSummaryResponse;
import com.barterplatform.application.reputation.service.ReputationService;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationServiceImpl implements ReputationService {

    private final TradeReviewRepository tradeReviewRepository;

    public ReputationServiceImpl(TradeReviewRepository tradeReviewRepository) {
        this.tradeReviewRepository = tradeReviewRepository;
    }

    @Override
    public ReputationSummaryResponse getReputationSummary(Long reviewedUserId) {
        long positiveCount = tradeReviewRepository.countByReviewedUserIdAndRating(reviewedUserId, TradeReviewRating.POSITIVE);
        long negativeCount = tradeReviewRepository.countByReviewedUserIdAndRating(reviewedUserId, TradeReviewRating.NEGATIVE);
        long totalCount = tradeReviewRepository.countByReviewedUserId(reviewedUserId);

        Double positivePercentage = totalCount == 0
                ? null
                : BigDecimal.valueOf((positiveCount * 100.0d) / totalCount)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        return new ReputationSummaryResponse()
                .positiveReviewCount(Math.toIntExact(positiveCount))
                .negativeReviewCount(Math.toIntExact(negativeCount))
                .totalReviewCount(Math.toIntExact(totalCount))
                .positivePercentage(positivePercentage);
    }
}

