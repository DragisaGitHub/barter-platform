package com.barterplatform.application.reputation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CreateTradeReviewRequest;
import com.barterplatform.api.model.TradeReviewNegativeReason;
import com.barterplatform.api.model.TradeReviewRating;
import com.barterplatform.api.model.TradeReviewResponse;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeReviewServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private TradeReviewRepository tradeReviewRepository;
    @Mock private NotificationService notificationService;

    private TradeReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TradeReviewServiceImpl(
                userRepository,
                tradeOfferRepository,
                tradeReviewRepository,
                new TradeReviewMapper(),
                notificationService);
    }

    @Test
    @DisplayName("creates a review for a completed trade and notifies the reviewed user")
    void createReviewSuccess() {
        UUID reviewerUuid = UUID.randomUUID();
        UUID tradeOfferUuid = UUID.randomUUID();
        UserEntity reviewer = user(1L, reviewerUuid, "alice");
        UserEntity reviewed = user(2L, UUID.randomUUID(), "bob");
        TradeOfferEntity offer = completedOffer(10L, tradeOfferUuid, 1L, 2L);
        CreateTradeReviewRequest request = new CreateTradeReviewRequest(TradeReviewRating.NEGATIVE)
                .negativeReason(TradeReviewNegativeReason.NO_SHOW)
                .comment("Never arrived");

        when(userRepository.findByUuid(reviewerUuid)).thenReturn(Optional.of(reviewer));
        when(tradeOfferRepository.findByUuid(tradeOfferUuid)).thenReturn(Optional.of(offer));
        when(tradeReviewRepository.existsByTradeOfferIdAndReviewerUserId(10L, 1L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewed));
        when(tradeReviewRepository.save(any(TradeReviewEntity.class))).thenAnswer(invocation -> {
            TradeReviewEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            entity.setUuid(UUID.randomUUID());
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(entity.getCreatedAt());
            return entity;
        });

        TradeReviewResponse response = service.createReview(reviewerUuid, tradeOfferUuid, request);

        assertNotNull(response.getUuid());
        assertEquals(tradeOfferUuid, response.getTradeOfferUuid());
        assertEquals(reviewerUuid, response.getReviewerUserUuid());
        assertEquals(reviewed.getUuid(), response.getReviewedUserUuid());
        assertEquals(TradeReviewRating.NEGATIVE, response.getRating());
        assertEquals(TradeReviewNegativeReason.NO_SHOW, response.getNegativeReason());
        assertEquals("Never arrived", response.getComment());

        verify(notificationService).createNotification(
                eq(2L),
                eq(NotificationType.TRADE_REVIEW_RECEIVED),
                any(String.class),
                any(String.class),
                eq(tradeOfferUuid),
                eq("TRADE_OFFER"));
    }

    @Test
    @DisplayName("rejects duplicate reviews from the same participant")
    void rejectDuplicateReview() {
        UUID reviewerUuid = UUID.randomUUID();
        UUID tradeOfferUuid = UUID.randomUUID();
        UserEntity reviewer = user(1L, reviewerUuid, "alice");
        TradeOfferEntity offer = completedOffer(10L, tradeOfferUuid, 1L, 2L);

        when(userRepository.findByUuid(reviewerUuid)).thenReturn(Optional.of(reviewer));
        when(tradeOfferRepository.findByUuid(tradeOfferUuid)).thenReturn(Optional.of(offer));
        when(tradeReviewRepository.existsByTradeOfferIdAndReviewerUserId(10L, 1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.createReview(
                reviewerUuid,
                tradeOfferUuid,
                new CreateTradeReviewRequest(TradeReviewRating.POSITIVE)));

        assertEquals(409, ex.getStatus().value());
    }

    @Test
    @DisplayName("requires a comment when negative reason is OTHER")
    void requiresCommentForOtherNegativeReason() {
        UUID reviewerUuid = UUID.randomUUID();
        UUID tradeOfferUuid = UUID.randomUUID();
        UserEntity reviewer = user(1L, reviewerUuid, "alice");
        TradeOfferEntity offer = completedOffer(10L, tradeOfferUuid, 1L, 2L);

        when(userRepository.findByUuid(reviewerUuid)).thenReturn(Optional.of(reviewer));
        when(tradeOfferRepository.findByUuid(tradeOfferUuid)).thenReturn(Optional.of(offer));
        when(tradeReviewRepository.existsByTradeOfferIdAndReviewerUserId(10L, 1L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> service.createReview(
                reviewerUuid,
                tradeOfferUuid,
                new CreateTradeReviewRequest(TradeReviewRating.NEGATIVE)
                        .negativeReason(TradeReviewNegativeReason.OTHER)
                        .comment("   ")));

        assertEquals(400, ex.getStatus().value());
    }

    private UserEntity user(Long id, UUID uuid, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUuid(uuid);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    private TradeOfferEntity completedOffer(Long id, UUID uuid, Long senderUserId, Long receiverUserId) {
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setId(id);
        offer.setUuid(uuid);
        offer.setSenderUserId(senderUserId);
        offer.setReceiverUserId(receiverUserId);
        offer.setSenderItemId(11L);
        offer.setReceiverItemId(22L);
        offer.setMode(TradeOfferMode.ITEM_EXCHANGE);
        offer.setStatus(TradeOfferStatus.COMPLETED);
        offer.setCreatedAt(OffsetDateTime.now().minusDays(2));
        offer.setCompletedAt(OffsetDateTime.now().minusDays(1));
        return offer;
    }
}

