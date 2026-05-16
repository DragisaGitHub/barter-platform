package com.barterplatform.application.reputation.service.impl;

import com.barterplatform.api.model.AdminTradeReviewPagedResponse;
import com.barterplatform.api.model.AdminTradeReviewSummaryResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.application.reputation.service.AdminTradeReviewService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminTradeReviewServiceImpl implements AdminTradeReviewService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "rating");

    private final TradeReviewRepository tradeReviewRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final UserRepository userRepository;
    private final TradeReviewMapper tradeReviewMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public AdminTradeReviewServiceImpl(
            TradeReviewRepository tradeReviewRepository,
            TradeOfferRepository tradeOfferRepository,
            UserRepository userRepository,
            TradeReviewMapper tradeReviewMapper,
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper) {
        this.tradeReviewRepository = tradeReviewRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.userRepository = userRepository;
        this.tradeReviewMapper = tradeReviewMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public AdminTradeReviewPagedResponse listReviews(
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            String reviewedUserQuery,
            String reviewerUserQuery) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        List<Long> reviewedUserIds = resolveUserIds(reviewedUserQuery);
        if (reviewedUserQuery != null && !reviewedUserQuery.isBlank() && reviewedUserIds.isEmpty()) {
            return emptyPage(pageRequest);
        }

        List<Long> reviewerUserIds = resolveUserIds(reviewerUserQuery);
        if (reviewerUserQuery != null && !reviewerUserQuery.isBlank() && reviewerUserIds.isEmpty()) {
            return emptyPage(pageRequest);
        }

        Specification<TradeReviewEntity> specification = buildSpecification(
                rating,
                negativeReason,
                reviewedUserIds,
                reviewerUserIds);

        Page<TradeReviewEntity> reviewPage = tradeReviewRepository.findAll(specification, pageRequest.pageable());
        List<AdminTradeReviewSummaryResponse> content = mapContent(reviewPage.getContent());
        return pageResponseMapper.toAdminTradeReviewPagedResponse(reviewPage, content, pageRequest.sort());
    }

    private Specification<TradeReviewEntity> buildSpecification(
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            List<Long> reviewedUserIds,
            List<Long> reviewerUserIds) {
        List<Specification<TradeReviewEntity>> specs = new java.util.ArrayList<>();

        if (rating != null) {
            specs.add(AdminTradeReviewSpecifications.ratingEquals(rating));
        }
        if (negativeReason != null) {
            specs.add(AdminTradeReviewSpecifications.negativeReasonEquals(negativeReason));
        }
        if (reviewedUserIds != null && !reviewedUserIds.isEmpty()) {
            specs.add(AdminTradeReviewSpecifications.reviewedUserIdIn(reviewedUserIds));
        }
        if (reviewerUserIds != null && !reviewerUserIds.isEmpty()) {
            specs.add(AdminTradeReviewSpecifications.reviewerUserIdIn(reviewerUserIds));
        }

        return specs.isEmpty() ? Specification.unrestricted() : Specification.allOf(specs);
    }

    private List<AdminTradeReviewSummaryResponse> mapContent(List<TradeReviewEntity> reviews) {
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

        return reviews.stream()
                .map(review -> tradeReviewMapper.toAdminSummaryResponse(
                        review,
                        resolveTradeOffer(tradeOffersById, review.getTradeOfferId()),
                        resolveUser(usersById, review.getReviewerUserId(), "Reviewer"),
                        resolveUser(usersById, review.getReviewedUserId(), "Reviewed user")))
                .toList();
    }

    private List<Long> resolveUserIds(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return null;
        }
        return userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                userQuery.trim().toLowerCase(Locale.ROOT));
    }

    private AdminTradeReviewPagedResponse emptyPage(PageRequestFactory.ResolvedPageRequest pageRequest) {
        Page<TradeReviewEntity> emptyPage = new PageImpl<>(List.of(), pageRequest.pageable(), 0);
        return pageResponseMapper.toAdminTradeReviewPagedResponse(emptyPage, List.of(), pageRequest.sort());
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

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }
}

