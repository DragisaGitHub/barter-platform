package com.barterplatform.application.trade.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CreateTradeOfferRequest;
import com.barterplatform.api.model.TradeOfferMode;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.trade.mapper.TradeOfferMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferItemSide;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeOfferServiceImplTest {

    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private TradeOfferItemRepository tradeOfferItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TradeOfferMapper tradeOfferMapper;
    @Mock private PageRequestFactory pageRequestFactory;
    @Mock private PageResponseMapper pageResponseMapper;

    private TradeOfferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TradeOfferServiceImpl(
                tradeOfferRepository, tradeOfferItemRepository,
                userRepository, itemRepository,
                categoryRepository, tradeOfferMapper,
                pageRequestFactory, pageResponseMapper);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private UserEntity user(Long id, UUID uuid, String username) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUuid(uuid);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setCreatedAt(OffsetDateTime.now());
        return u;
    }

    private ItemEntity item(Long id, UUID uuid, Long ownerId, Long categoryId, ItemStatus status) {
        ItemEntity e = new ItemEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setOwnerId(ownerId);
        e.setCategoryId(categoryId);
        e.setTitle("Test Item " + id);
        e.setStatus(status);
        e.setCondition(ItemCondition.GOOD);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private CategoryEntity category(Long id, String name) {
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
        c.setUuid(UUID.randomUUID());
        c.setName(name);
        c.setCreatedAt(OffsetDateTime.now());
        return c;
    }

    private TradeOfferEntity offer(Long id, UUID uuid, Long senderUserId, Long receiverUserId,
                                   Long senderItemId, Long receiverItemId, TradeOfferStatus status) {
        TradeOfferEntity o = new TradeOfferEntity();
        o.setId(id);
        o.setUuid(uuid);
        o.setSenderUserId(senderUserId);
        o.setReceiverUserId(receiverUserId);
        o.setSenderItemId(senderItemId);
        o.setReceiverItemId(receiverItemId);
        o.setMode(com.barterplatform.domain.trade.enums.TradeOfferMode.ITEM_EXCHANGE);
        o.setStatus(status);
        o.setCreatedAt(OffsetDateTime.now());
        return o;
    }

    private void stubMapperToResponse() {
        when(tradeOfferMapper.toResponse(
                any(TradeOfferEntity.class), any(UserEntity.class), any(UserEntity.class),
                any(ItemEntity.class), any(CategoryEntity.class),
                anyList(), anyList()))
                .thenReturn(new TradeOfferResponse().uuid(UUID.randomUUID()));
    }

    private CreateTradeOfferRequest createItemExchangeRequest(UUID receiverItemUuid, UUID... senderItemUuids) {
        CreateTradeOfferRequest request = new CreateTradeOfferRequest(receiverItemUuid, TradeOfferMode.ITEM_EXCHANGE);
        request.setSenderItemUuids(List.of(senderItemUuids));
        return request;
    }

    // ── createOffer ─────────────────────────────────────────────

    @Nested
    @DisplayName("createOffer")
    class CreateOffer {

        @Test
        @DisplayName("creates offer successfully with status PENDING")
        void createSuccess() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            UserEntity receiver = user(2L, UUID.randomUUID(), "bob");
            ItemEntity senderItem = item(10L, senderItemUuid, 1L, 100L, ItemStatus.ACTIVE);
            ItemEntity receiverItem = item(20L, receiverItemUuid, 2L, 100L, ItemStatus.ACTIVE);
            CategoryEntity cat = category(100L, "Books");

            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
            when(itemRepository.findByUuid(senderItemUuid)).thenReturn(Optional.of(senderItem));
            when(itemRepository.findByUuid(receiverItemUuid)).thenReturn(Optional.of(receiverItem));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(categoryRepository.findById(100L)).thenReturn(Optional.of(cat));
            when(tradeOfferRepository.save(any(TradeOfferEntity.class))).thenAnswer(i -> {
                TradeOfferEntity arg = i.getArgument(0);
                if (arg.getId() == null) arg.setId(50L);
                if (arg.getUuid() == null) arg.setUuid(UUID.randomUUID());
                return arg;
            });
            stubMapperToResponse();

            CreateTradeOfferRequest request = createItemExchangeRequest(receiverItemUuid, senderItemUuid);
            request.setMessage("Let's trade!");

            TradeOfferResponse result = service.createOffer(senderUuid, request);

            assertNotNull(result);

            ArgumentCaptor<TradeOfferEntity> captor = ArgumentCaptor.forClass(TradeOfferEntity.class);
            verify(tradeOfferRepository, times(2)).save(captor.capture());
            TradeOfferEntity saved = captor.getAllValues().get(0);
            assertEquals(TradeOfferStatus.PENDING, saved.getStatus());
            assertEquals(1L, saved.getSenderUserId());
            assertEquals(2L, saved.getReceiverUserId());
            assertEquals(10L, saved.getSenderItemId());
            assertEquals(20L, saved.getReceiverItemId());
            assertEquals("Let's trade!", saved.getMessage());
            assertEquals(com.barterplatform.domain.trade.enums.TradeOfferMode.ITEM_EXCHANGE, saved.getMode());
        }

        @Test
        @DisplayName("throws FORBIDDEN when sender item not owned by sender")
        void createSenderItemNotOwned() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            ItemEntity senderItem = item(10L, senderItemUuid, 99L, 100L, ItemStatus.ACTIVE); // wrong owner
            ItemEntity receiverItem = item(20L, receiverItemUuid, 2L, 100L, ItemStatus.ACTIVE);

            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
            when(itemRepository.findByUuid(senderItemUuid)).thenReturn(Optional.of(senderItem));
            when(itemRepository.findByUuid(receiverItemUuid)).thenReturn(Optional.of(receiverItem));

            CreateTradeOfferRequest request = createItemExchangeRequest(receiverItemUuid, senderItemUuid);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(403, ex.getStatus().value());
            verify(tradeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws FORBIDDEN when receiver item owned by sender (self-offer)")
        void createReceiverItemOwnedBySender() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            ItemEntity senderItem = item(10L, senderItemUuid, 1L, 100L, ItemStatus.ACTIVE);
            ItemEntity receiverItem = item(20L, receiverItemUuid, 1L, 100L, ItemStatus.ACTIVE); // same owner

            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
            when(itemRepository.findByUuid(receiverItemUuid)).thenReturn(Optional.of(receiverItem));

            CreateTradeOfferRequest request = createItemExchangeRequest(receiverItemUuid, senderItemUuid);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(403, ex.getStatus().value());
            verify(tradeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CONFLICT when sender item is inactive")
        void createInactiveSenderItem() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            ItemEntity senderItem = item(10L, senderItemUuid, 1L, 100L, ItemStatus.ARCHIVED);
            ItemEntity receiverItem = item(20L, receiverItemUuid, 2L, 100L, ItemStatus.ACTIVE);

            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));
            when(itemRepository.findByUuid(senderItemUuid)).thenReturn(Optional.of(senderItem));
            when(itemRepository.findByUuid(receiverItemUuid)).thenReturn(Optional.of(receiverItem));

            CreateTradeOfferRequest request = createItemExchangeRequest(receiverItemUuid, senderItemUuid);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(409, ex.getStatus().value());
            verify(tradeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST for ITEM_EXCHANGE without sender items")
        void createItemExchangeWithoutSenderItems() {
            UUID senderUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));

            CreateTradeOfferRequest request = new CreateTradeOfferRequest(receiverItemUuid, TradeOfferMode.ITEM_EXCHANGE);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(400, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws BAD_REQUEST for GIFT with sender items")
        void createGiftWithSenderItems() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));

            CreateTradeOfferRequest request = new CreateTradeOfferRequest(receiverItemUuid, TradeOfferMode.GIFT);
            request.setSenderItemUuids(List.of(senderItemUuid));
            request.setMessage("Gift please");

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(400, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws BAD_REQUEST for duplicate sender item UUIDs")
        void createDuplicateSenderItems() {
            UUID senderUuid = UUID.randomUUID();
            UUID senderItemUuid = UUID.randomUUID();
            UUID receiverItemUuid = UUID.randomUUID();

            UserEntity sender = user(1L, senderUuid, "alice");
            when(userRepository.findByUuid(senderUuid)).thenReturn(Optional.of(sender));

            CreateTradeOfferRequest request = new CreateTradeOfferRequest(receiverItemUuid, TradeOfferMode.ITEM_EXCHANGE);
            request.setSenderItemUuids(List.of(senderItemUuid, senderItemUuid));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createOffer(senderUuid, request));
            assertEquals(400, ex.getStatus().value());
        }
    }

    // ── acceptOffer ─────────────────────────────────────────────

    @Nested
    @DisplayName("acceptOffer")
    class AcceptOffer {

        @Test
        @DisplayName("accepts offer, archives items, and rejects competing offers")
        void acceptSuccess() {
            UUID receiverUuid = UUID.randomUUID();
            UUID offerUuid = UUID.randomUUID();

            UserEntity sender = user(1L, UUID.randomUUID(), "alice");
            UserEntity receiver = user(2L, receiverUuid, "bob");
            TradeOfferEntity pendingOffer = offer(50L, offerUuid, 1L, 2L, 10L, 20L, TradeOfferStatus.PENDING);
            ItemEntity senderItem = item(10L, UUID.randomUUID(), 1L, 100L, ItemStatus.ACTIVE);
            ItemEntity receiverItem = item(20L, UUID.randomUUID(), 2L, 100L, ItemStatus.ACTIVE);
            CategoryEntity cat = category(100L, "Books");

            // Competing offer
            TradeOfferEntity competing = offer(51L, UUID.randomUUID(), 3L, 1L, 30L, 10L, TradeOfferStatus.PENDING);

            when(userRepository.findByUuid(receiverUuid)).thenReturn(Optional.of(receiver));
            when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(pendingOffer));
            when(itemRepository.findById(10L)).thenReturn(Optional.of(senderItem));
            when(itemRepository.findById(20L)).thenReturn(Optional.of(receiverItem));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(categoryRepository.findById(100L)).thenReturn(Optional.of(cat));
            when(tradeOfferRepository.save(any(TradeOfferEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(itemRepository.save(any(ItemEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(50L, TradeOfferItemSide.OFFERED))
                    .thenReturn(List.of(10L));
            when(tradeOfferRepository.findCompetingPendingOffers(eq(50L), any(Collection.class)))
                    .thenReturn(List.of(competing));
            stubMapperToResponse();

            TradeOfferResponse result = service.acceptOffer(receiverUuid, offerUuid);

            assertNotNull(result);

            // Verify offer was accepted
            assertEquals(TradeOfferStatus.ACCEPTED, pendingOffer.getStatus());
            assertNotNull(pendingOffer.getRespondedAt());

            // Verify both items archived
            assertEquals(ItemStatus.ARCHIVED, senderItem.getStatus());
            assertNotNull(senderItem.getArchivedAt());
            assertEquals(ItemStatus.ARCHIVED, receiverItem.getStatus());
            assertNotNull(receiverItem.getArchivedAt());

            // Verify competing offer was rejected
            assertEquals(TradeOfferStatus.REJECTED, competing.getStatus());
            assertNotNull(competing.getRespondedAt());

            // Items saved (2 archives), offer saved (1 accept), competing saved (1 reject)
            verify(itemRepository, times(2)).save(any(ItemEntity.class));
            verify(tradeOfferRepository, times(2)).save(any(TradeOfferEntity.class)); // main offer + competing
        }

        @Test
        @DisplayName("throws FORBIDDEN when non-receiver tries to accept")
        void acceptByNonReceiverForbidden() {
            UUID attackerUuid = UUID.randomUUID();
            UUID offerUuid = UUID.randomUUID();

            UserEntity attacker = user(3L, attackerUuid, "eve");
            TradeOfferEntity pendingOffer = offer(50L, offerUuid, 1L, 2L, 10L, 20L, TradeOfferStatus.PENDING);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(pendingOffer));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.acceptOffer(attackerUuid, offerUuid));
            assertEquals(403, ex.getStatus().value());
            verify(itemRepository, never()).save(any());
        }
    }

    // ── rejectOffer ─────────────────────────────────────────────

    @Nested
    @DisplayName("rejectOffer")
    class RejectOffer {

        @Test
        @DisplayName("throws FORBIDDEN when non-receiver tries to reject")
        void rejectByNonReceiverForbidden() {
            UUID attackerUuid = UUID.randomUUID();
            UUID offerUuid = UUID.randomUUID();

            UserEntity attacker = user(3L, attackerUuid, "eve");
            TradeOfferEntity pendingOffer = offer(50L, offerUuid, 1L, 2L, 10L, 20L, TradeOfferStatus.PENDING);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(pendingOffer));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.rejectOffer(attackerUuid, offerUuid));
            assertEquals(403, ex.getStatus().value());
            verify(tradeOfferRepository, never()).save(any());
        }
    }

    // ── cancelOffer ─────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOffer")
    class CancelOffer {

        @Test
        @DisplayName("throws FORBIDDEN when non-sender tries to cancel")
        void cancelByNonSenderForbidden() {
            UUID attackerUuid = UUID.randomUUID();
            UUID offerUuid = UUID.randomUUID();

            UserEntity attacker = user(3L, attackerUuid, "eve");
            TradeOfferEntity pendingOffer = offer(50L, offerUuid, 1L, 2L, 10L, 20L, TradeOfferStatus.PENDING);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(pendingOffer));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.cancelOffer(attackerUuid, offerUuid));
            assertEquals(403, ex.getStatus().value());
            verify(tradeOfferRepository, never()).save(any());
        }
    }

    // ── getOffer ────────────────────────────────────────────────

    @Nested
    @DisplayName("getOffer")
    class GetOffer {

        @Test
        @DisplayName("throws FORBIDDEN when non-participant tries to get offer")
        void getByNonParticipantForbidden() {
            UUID attackerUuid = UUID.randomUUID();
            UUID offerUuid = UUID.randomUUID();

            UserEntity attacker = user(3L, attackerUuid, "eve");
            TradeOfferEntity existingOffer = offer(50L, offerUuid, 1L, 2L, 10L, 20L, TradeOfferStatus.PENDING);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(tradeOfferRepository.findByUuid(offerUuid)).thenReturn(Optional.of(existingOffer));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getOffer(attackerUuid, offerUuid));
            assertEquals(403, ex.getStatus().value());
        }
    }
}

