package com.barterplatform.application.reputation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.AdminTradeReviewPagedResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminTradeReviewServiceImplTest {

    @Mock private TradeReviewRepository tradeReviewRepository;
    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private UserRepository userRepository;

    private AdminTradeReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminTradeReviewServiceImpl(
                tradeReviewRepository,
                tradeOfferRepository,
                userRepository,
                new TradeReviewMapper(),
                new PageRequestFactory(),
                new PageResponseMapper());
    }

    @Test
    @DisplayName("lists admin reviews with rating, reason, and user query filters")
    @SuppressWarnings("unchecked")
    void listsAdminReviewsWithFilters() {
        TradeReviewEntity review = review(100L, 50L, 1L, 2L, TradeReviewRating.NEGATIVE, TradeReviewNegativeReason.NO_SHOW);
        TradeOfferEntity offer = offer(50L, review.getTradeOfferId());
        UserEntity reviewer = user(1L, "alice");
        UserEntity reviewed = user(2L, "bob");

        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("bob")).thenReturn(List.of(2L));
        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("alice")).thenReturn(List.of(1L));
        when(tradeReviewRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(tradeOfferRepository.findAllById(any(Iterable.class))).thenReturn(List.of(offer));
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(reviewer, reviewed));

        AdminTradeReviewPagedResponse response = service.listReviews(
                0,
                20,
                "createdAt,desc",
                TradeReviewRating.NEGATIVE,
                TradeReviewNegativeReason.NO_SHOW,
                "bob",
                "alice");

        assertEquals(1, response.getContent().size());
        assertEquals(com.barterplatform.api.model.TradeReviewRating.NEGATIVE, response.getContent().getFirst().getRating());
        assertEquals(com.barterplatform.api.model.TradeReviewNegativeReason.NO_SHOW, response.getContent().getFirst().getNegativeReason());
        assertEquals("alice", response.getContent().getFirst().getReviewerUsername());
        assertEquals("bob", response.getContent().getFirst().getReviewedUsername());
        verify(userRepository).findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(eq("bob"));
        verify(userRepository).findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(eq("alice"));
    }

    @Test
    @DisplayName("returns empty page without querying reviews when user query has no matches")
    void emptyUserQueryResultReturnsEmptyPage() {
        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("unknown")).thenReturn(List.of());

        AdminTradeReviewPagedResponse response = service.listReviews(
                0,
                20,
                "createdAt,desc",
                null,
                null,
                "unknown",
                null);

        assertEquals(0, response.getContent().size());
        assertEquals(0, response.getTotalElements());
    }

    private TradeReviewEntity review(
            Long id,
            Long tradeOfferId,
            Long reviewerUserId,
            Long reviewedUserId,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason) {
        TradeReviewEntity review = TradeReviewEntity.create(
                tradeOfferId,
                reviewerUserId,
                reviewedUserId,
                rating,
                negativeReason,
                "Comment");
        review.setId(id);
        review.setUuid(UUID.randomUUID());
        review.setCreatedAt(OffsetDateTime.now());
        review.setUpdatedAt(review.getCreatedAt());
        return review;
    }

    private TradeOfferEntity offer(Long id, Long ignoredTradeOfferId) {
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setId(id);
        offer.setUuid(UUID.randomUUID());
        offer.setSenderUserId(1L);
        offer.setReceiverUserId(2L);
        offer.setSenderItemId(11L);
        offer.setReceiverItemId(22L);
        offer.setMode(TradeOfferMode.ITEM_EXCHANGE);
        offer.setStatus(TradeOfferStatus.COMPLETED);
        return offer;
    }

    private UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUuid(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}

