package com.barterplatform.application.trade.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.SendTradeOfferMessageRequest;
import com.barterplatform.api.model.TradeOfferMessageResponse;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.trade.mapper.TradeOfferMessageMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
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
        verify(notificationService).createNotification(
                eq(2L),
                eq(NotificationType.TRADE_MESSAGE_RECEIVED),
                argThat(metadata -> metadata != null
                        && "alice".equals(metadata.get("actorUsername"))
                        && offerUuid.toString().equals(metadata.get("tradeOfferUuid"))),
                isNull(),
                isNull(),
                eq(offerUuid),
                eq("TRADE_OFFER"));
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

    // ── getUnreadMessageCount tests ──────────────────────────────

    @Test
    @DisplayName("no unread messages returns 0")
    void unreadCountReturnsZeroWhenNoUnreadMessages() {
        UUID userUuid = UUID.randomUUID();
        UserEntity currentUser = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(currentUser));
        when(tradeOfferMessageRepository.countByRecipientUserIdAndReadFalse(1L)).thenReturn(0L);

        long count = service.getUnreadMessageCount(userUuid);

        assertEquals(0, count);
    }

    @Test
    @DisplayName("multiple unread messages return correct count")
    void unreadCountReturnsCorrectCount() {
        UUID userUuid = UUID.randomUUID();
        UserEntity currentUser = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(currentUser));
        when(tradeOfferMessageRepository.countByRecipientUserIdAndReadFalse(1L)).thenReturn(5L);

        long count = service.getUnreadMessageCount(userUuid);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("sent unread messages are not counted - only recipient messages count")
    void unreadCountOnlyCountsRecipientMessages() {
        UUID userUuid = UUID.randomUUID();
        UserEntity currentUser = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(currentUser));
        // Repository only counts messages where recipientUserId matches, not sender
        when(tradeOfferMessageRepository.countByRecipientUserIdAndReadFalse(1L)).thenReturn(3L);

        long count = service.getUnreadMessageCount(userUuid);

        assertEquals(3, count);
        verify(tradeOfferMessageRepository).countByRecipientUserIdAndReadFalse(1L);
    }
}

