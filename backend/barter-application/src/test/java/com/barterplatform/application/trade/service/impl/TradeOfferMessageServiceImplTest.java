package com.barterplatform.application.trade.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.SendTradeOfferMessageRequest;
import com.barterplatform.api.model.TradeOfferMessageResponse;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.trade.mapper.TradeOfferMessageMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
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
class TradeOfferMessageServiceImplTest {

    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private TradeOfferMessageRepository tradeOfferMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private TradeOfferMessageMapper tradeOfferMessageMapper;
    @Mock private NotificationService notificationService;

    private TradeOfferMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TradeOfferMessageServiceImpl(
                tradeOfferRepository,
                tradeOfferMessageRepository,
                userRepository,
                tradeOfferMessageMapper,
                notificationService);
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

    private TradeOfferEntity offer(UUID uuid, TradeOfferStatus status) {
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setId(50L);
        offer.setUuid(uuid);
        offer.setSenderUserId(1L);
        offer.setReceiverUserId(2L);
        offer.setSenderItemId(10L);
        offer.setReceiverItemId(20L);
        offer.setMode(TradeOfferMode.ITEM_EXCHANGE);
        offer.setStatus(status);
        offer.setCreatedAt(OffsetDateTime.now());
        return offer;
    }

    @Test
    @DisplayName("messaging remains writable for ACCEPTED offers")
    void messagingWritableForAcceptedOffer() {
        UUID senderUuid = UUID.randomUUID();
        UUID offerUuid = UUID.randomUUID();
        UserEntity sender = user(1L, senderUuid, "alice");
        UserEntity receiver = user(2L, UUID.randomUUID(), "bob");
        TradeOfferEntity acceptedOffer = offer(offerUuid, TradeOfferStatus.ACCEPTED);
        SendTradeOfferMessageRequest request = new SendTradeOfferMessageRequest().content("Ready to meet tomorrow");

        when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
        when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(acceptedOffer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(tradeOfferMessageRepository.save(any(TradeOfferMessageEntity.class))).thenAnswer(invocation -> {
            TradeOfferMessageEntity message = invocation.getArgument(0);
            message.setId(90L);
            message.setUuid(UUID.randomUUID());
            message.setCreatedAt(OffsetDateTime.now());
            return message;
        });
        when(tradeOfferMessageMapper.toResponse(any(), any(), any(), any()))
                .thenReturn(new TradeOfferMessageResponse().content("Ready to meet tomorrow"));

        TradeOfferMessageResponse response = service.sendMessage(senderUuid, offerUuid, request);

        assertNotNull(response);
        assertEquals("Ready to meet tomorrow", response.getContent());
    }

    @Test
    @DisplayName("messaging is read-only for COMPLETED offers")
    void messagingLockedForCompletedOffer() {
        UUID senderUuid = UUID.randomUUID();
        UUID offerUuid = UUID.randomUUID();
        UserEntity sender = user(1L, senderUuid, "alice");
        TradeOfferEntity completedOffer = offer(offerUuid, TradeOfferStatus.COMPLETED);
        SendTradeOfferMessageRequest request = new SendTradeOfferMessageRequest().content("Too late");

        when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
        when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(completedOffer));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendMessage(senderUuid, offerUuid, request));

        assertEquals(409, ex.getStatus().value());
        assertEquals("Messages can only be sent while the trade offer is pending or awaiting completion.", ex.getMessage());
    }
}

