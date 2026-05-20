package com.barterplatform.infrastructure.reputation.repository;

import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TradeReviewRepository extends JpaRepository<TradeReviewEntity, Long>, JpaSpecificationExecutor<TradeReviewEntity> {

    Optional<TradeReviewEntity> findByUuid(UUID uuid);

    boolean existsByTradeOfferIdAndReviewerUserId(Long tradeOfferId, Long reviewerUserId);

    Optional<TradeReviewEntity> findByTradeOfferIdAndReviewerUserId(Long tradeOfferId, Long reviewerUserId);

    List<TradeReviewEntity> findByTradeOfferId(Long tradeOfferId);

    Page<TradeReviewEntity> findByReviewedUserId(Long reviewedUserId, Pageable pageable);

    Page<TradeReviewEntity> findByReviewedUserIdAndRating(Long reviewedUserId, TradeReviewRating rating, Pageable pageable);

    Page<TradeReviewEntity> findByReviewerUserId(Long reviewerUserId, Pageable pageable);

    Page<TradeReviewEntity> findByReviewerUserIdAndRating(Long reviewerUserId, TradeReviewRating rating, Pageable pageable);

    long countByReviewedUserIdAndRating(Long reviewedUserId, TradeReviewRating rating);

    long countByReviewedUserId(Long reviewedUserId);
}

