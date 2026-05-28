package com.barterplatform.infrastructure.reputation.repository;

import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.identity.enums.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByRating(TradeReviewRating rating);

    @Query("""
            select r.uuid as uuid,
                   reviewer.username as reviewerUsername,
                   r.rating as rating,
                   r.comment as comment,
                   r.createdAt as createdAt
            from TradeReviewEntity r
            join UserEntity reviewer on reviewer.id = r.reviewerUserId
            where r.reviewedUserId = :reviewedUserId
              and reviewer.status = :reviewerStatus
              and r.comment is not null
            order by r.createdAt desc
            """)
    List<PublicProfileReviewSnippetProjection> findLatestCommentedReviewsForReviewedUser(
            @Param("reviewedUserId") Long reviewedUserId,
            @Param("reviewerStatus") UserStatus reviewerStatus,
            Pageable pageable);

    interface PublicProfileReviewSnippetProjection {
        UUID getUuid();

        String getReviewerUsername();

        TradeReviewRating getRating();

        String getComment();

        OffsetDateTime getCreatedAt();
    }
}

