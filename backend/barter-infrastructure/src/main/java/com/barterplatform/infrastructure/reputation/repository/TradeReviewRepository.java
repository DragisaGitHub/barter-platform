package com.barterplatform.infrastructure.reputation.repository;

import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TradeReviewRepository extends JpaRepository<TradeReviewEntity, Long>, JpaSpecificationExecutor<TradeReviewEntity> {

    boolean existsByTradeOfferIdAndReviewerUserId(Long tradeOfferId, Long reviewerUserId);

    Optional<TradeReviewEntity> findByTradeOfferIdAndReviewerUserId(Long tradeOfferId, Long reviewerUserId);

    List<TradeReviewEntity> findByTradeOfferId(Long tradeOfferId);

    long countByReviewedUserIdAndRating(Long reviewedUserId, TradeReviewRating rating);

    long countByReviewedUserId(Long reviewedUserId);
}

