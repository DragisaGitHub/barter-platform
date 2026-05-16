package com.barterplatform.application.reputation.service.impl;

import com.barterplatform.api.model.CreateTradeReviewRequest;
import com.barterplatform.api.model.TradeReviewResponse;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.application.reputation.service.TradeReviewService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeReviewServiceImpl implements TradeReviewService {

    private final UserRepository userRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final TradeReviewMapper tradeReviewMapper;
    private final NotificationService notificationService;

    public TradeReviewServiceImpl(
            UserRepository userRepository,
            TradeOfferRepository tradeOfferRepository,
            TradeReviewRepository tradeReviewRepository,
            TradeReviewMapper tradeReviewMapper,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeReviewRepository = tradeReviewRepository;
        this.tradeReviewMapper = tradeReviewMapper;
        this.notificationService = notificationService;
    }

    @Override
    public TradeReviewResponse createReview(UUID currentUserUuid, UUID tradeOfferUuid, CreateTradeReviewRequest request) {
        UserEntity reviewer = resolveUser(currentUserUuid);
        TradeOfferEntity tradeOffer = resolveTradeOffer(tradeOfferUuid);

        if (tradeOffer.getStatus() != TradeOfferStatus.COMPLETED) {
            throw conflict("Trade reviews are only allowed for completed trades.");
        }

        if (!isParticipant(tradeOffer, reviewer.getId())) {
            throw forbidden("Only trade participants can review each other.");
        }

        Long reviewedUserId = tradeOffer.getSenderUserId().equals(reviewer.getId())
                ? tradeOffer.getReceiverUserId()
                : tradeOffer.getSenderUserId();

        if (reviewedUserId.equals(reviewer.getId())) {
            throw forbidden("You cannot review yourself.");
        }

        if (tradeReviewRepository.existsByTradeOfferIdAndReviewerUserId(tradeOffer.getId(), reviewer.getId())) {
            throw conflict("You have already reviewed this trade.");
        }

        TradeReviewRating rating = tradeReviewMapper.map(request.getRating());
        TradeReviewNegativeReason negativeReason = tradeReviewMapper.map(request.getNegativeReason());

        validateRequest(rating, negativeReason, request.getComment());

        UserEntity reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> notFound("Reviewed user was not found."));

        TradeReviewEntity saved = tradeReviewRepository.save(TradeReviewEntity.create(
                tradeOffer.getId(),
                reviewer.getId(),
                reviewedUserId,
                rating,
                negativeReason,
                request.getComment()));

        notificationService.createNotification(
                reviewedUser.getId(),
                NotificationType.TRADE_REVIEW_RECEIVED,
                reviewer.getUsername() + " reviewed your completed trade",
                reviewer.getUsername() + " left a review for your completed trade.",
                tradeOffer.getUuid(),
                "TRADE_OFFER");

        return tradeReviewMapper.toResponse(saved, tradeOffer, reviewer, reviewedUser);
    }

    private void validateRequest(TradeReviewRating rating, TradeReviewNegativeReason negativeReason, String comment) {
        if (rating == null) {
            throw badRequest("Rating is required.");
        }
        if (rating == TradeReviewRating.POSITIVE && negativeReason != null) {
            throw badRequest("Positive reviews must not include a negative reason.");
        }
        if (rating == TradeReviewRating.NEGATIVE && negativeReason == null) {
            throw badRequest("Negative reviews require a negative reason.");
        }
        if (negativeReason == TradeReviewNegativeReason.OTHER && (comment == null || comment.isBlank())) {
            throw badRequest("Comment is required when negative reason is OTHER.");
        }
    }

    private boolean isParticipant(TradeOfferEntity tradeOffer, Long userId) {
        return tradeOffer.getSenderUserId().equals(userId) || tradeOffer.getReceiverUserId().equals(userId);
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private TradeOfferEntity resolveTradeOffer(UUID tradeOfferUuid) {
        return tradeOfferRepository.findByUuid(tradeOfferUuid)
                .orElseThrow(() -> notFound("Trade offer with uuid '%s' was not found.", tradeOfferUuid));
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, messageTemplate.formatted(args));
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}

