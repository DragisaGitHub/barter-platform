package com.barterplatform.application.reputation.service.impl;

import static com.barterplatform.application.notification.support.NotificationMetadataUtils.metadataOf;

import com.barterplatform.api.model.CreateTradeReviewRequest;
import com.barterplatform.api.model.TradeReviewResponse;
import com.barterplatform.api.model.UserTradeReviewPagedResponse;
import com.barterplatform.api.model.UserTradeReviewSummaryResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.application.reputation.service.TradeReviewService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeReviewServiceImpl implements TradeReviewService {

    private static final String DEFAULT_LIST_SORT = "createdAt,desc";
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "rating");

    private final UserRepository userRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final ItemRepository itemRepository;
    private final TradeReviewMapper tradeReviewMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final NotificationService notificationService;

    public TradeReviewServiceImpl(
            UserRepository userRepository,
            TradeOfferRepository tradeOfferRepository,
            TradeReviewRepository tradeReviewRepository,
            ItemRepository itemRepository,
            TradeReviewMapper tradeReviewMapper,
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeReviewRepository = tradeReviewRepository;
        this.itemRepository = itemRepository;
        this.tradeReviewMapper = tradeReviewMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserTradeReviewPagedResponse listReviews(
            UUID currentUserUuid,
            Direction direction,
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating) {
        UserEntity user = resolveUser(currentUserUuid);
        if (direction == null) {
            throw badRequest("Review direction is required.");
        }

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_LIST_SORT : sort,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Page<TradeReviewEntity> reviewPage = switch (direction) {
            case RECEIVED -> rating == null
                    ? tradeReviewRepository.findByReviewedUserId(user.getId(), pageRequest.pageable())
                    : tradeReviewRepository.findByReviewedUserIdAndRating(user.getId(), rating, pageRequest.pageable());
            case GIVEN -> rating == null
                    ? tradeReviewRepository.findByReviewerUserId(user.getId(), pageRequest.pageable())
                    : tradeReviewRepository.findByReviewerUserIdAndRating(user.getId(), rating, pageRequest.pageable());
        };

        List<UserTradeReviewSummaryResponse> content = mapUserReviewContent(reviewPage.getContent());
        return pageResponseMapper.toUserTradeReviewPagedResponse(reviewPage, content, pageRequest.sort());
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
        ItemEntity receiverItem = itemRepository.findById(tradeOffer.getReceiverItemId())
                .orElse(null);

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
                metadataOf(
                        "actorUsername", reviewer.getUsername(),
                        "counterpartyUsername", reviewedUser.getUsername(),
                        "itemTitle", receiverItem == null ? null : receiverItem.getTitle(),
                        "tradeOfferUuid", tradeOffer.getUuid(),
                        "reviewUuid", saved.getUuid()),
                null,
                null,
                tradeOffer.getUuid(),
                "TRADE_OFFER");

        return tradeReviewMapper.toResponse(saved, tradeOffer, reviewer, reviewedUser);
    }

    private List<UserTradeReviewSummaryResponse> mapUserReviewContent(List<TradeReviewEntity> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }

        Map<Long, TradeOfferEntity> tradeOffersById = tradeOfferRepository.findAllById(reviews.stream()
                        .map(TradeReviewEntity::getTradeOfferId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(TradeOfferEntity::getId, Function.identity()));

        Map<Long, UserEntity> usersById = userRepository.findAllById(reviews.stream()
                        .flatMap(review -> java.util.stream.Stream.of(review.getReviewerUserId(), review.getReviewedUserId()))
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        Map<Long, ItemEntity> receiverItemsById = itemRepository.findAllById(tradeOffersById.values().stream()
                        .map(TradeOfferEntity::getReceiverItemId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ItemEntity::getId, Function.identity()));

        return reviews.stream()
                .map(review -> {
                    TradeOfferEntity tradeOffer = resolveTradeOffer(tradeOffersById, review.getTradeOfferId());
                    return tradeReviewMapper.toUserSummaryResponse(
                            review,
                            tradeOffer,
                            resolveUser(usersById, review.getReviewerUserId(), "Reviewer"),
                            resolveUser(usersById, review.getReviewedUserId(), "Reviewed user"),
                            receiverItemsById.get(tradeOffer.getReceiverItemId()));
                })
                .toList();
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

    private TradeOfferEntity resolveTradeOffer(Map<Long, TradeOfferEntity> tradeOffersById, Long tradeOfferId) {
        TradeOfferEntity tradeOffer = tradeOffersById.get(tradeOfferId);
        if (tradeOffer == null) {
            throw notFound("Trade offer for review was not found.");
        }
        return tradeOffer;
    }

    private UserEntity resolveUser(Map<Long, UserEntity> usersById, Long userId, String label) {
        UserEntity user = usersById.get(userId);
        if (user == null) {
            throw notFound(label + " for review was not found.");
        }
        return user;
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

