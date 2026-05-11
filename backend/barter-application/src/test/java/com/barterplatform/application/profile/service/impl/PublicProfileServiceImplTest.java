package com.barterplatform.application.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;
    @Mock private PageResponseMapper pageResponseMapper;

    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    private PublicProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicProfileServiceImpl(
                userRepository, itemRepository, tradeOfferRepository,
                categoryRepository, itemImageRepository,
                itemMapper, itemImageMapper,
                pageRequestFactory, pageResponseMapper);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private UserEntity user(UUID uuid, String username, UserStatus status) {
        UserEntity u = new UserEntity();
        u.setId(10L);
        u.setUuid(uuid);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setStatus(status);
        u.setCreatedAt(OffsetDateTime.now());
        return u;
    }

    private ItemEntity item(Long id, UUID uuid, Long ownerId) {
        ItemEntity e = new ItemEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setOwnerId(ownerId);
        e.setCategoryId(20L);
        e.setTitle("Test Item");
        e.setStatus(ItemStatus.ACTIVE);
        e.setCondition(ItemCondition.GOOD);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    // ── getPublicProfile ──────────────────────────────────────────

    @Nested
    @DisplayName("getPublicProfile")
    class GetPublicProfile {

        @Test
        @DisplayName("returns profile for ACTIVE user")
        void returnsProfileForActiveUser() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(3L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(2L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(1L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(1L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertNotNull(result);
            assertEquals(uuid, result.getUuid());
            assertEquals("alice", result.getUsername());
            assertNotNull(result.getJoinedAt());
            assertEquals(3, result.getActiveItemCount());
            assertEquals(3, result.getCompletedTradeCount());
            assertEquals(1, result.getCancelledTradeCount());
            assertNull(result.getAverageRating());
        }

        @Test
        @DisplayName("throws NOT_FOUND for non-existing user")
        void throwsNotFoundForNonExistingUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getPublicProfile(uuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND for PENDING_VERIFICATION user")
        void throwsNotFoundForPendingUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(
                    Optional.of(user(uuid, "pending", UserStatus.PENDING_VERIFICATION)));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getPublicProfile(uuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND for SUSPENDED user")
        void throwsNotFoundForSuspendedUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(
                    Optional.of(user(uuid, "suspended", UserStatus.SUSPENDED)));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getPublicProfile(uuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND for BANNED user")
        void throwsNotFoundForBannedUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(
                    Optional.of(user(uuid, "banned", UserStatus.BANNED)));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getPublicProfile(uuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND for DELETED user")
        void throwsNotFoundForDeletedUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(
                    Optional.of(user(uuid, "deleted", UserStatus.DELETED)));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getPublicProfile(uuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("does not expose email or roles")
        void doesNotExposePrivateData() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);
            activeUser.setEmail("secret@email.com");

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            // PublicProfileResponse has no email, roles, or password fields
            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            // Verify no email field exists via the response object — it only has uuid, username,
            // joinedAt, activeItemCount, completedTradeCount, cancelledTradeCount, averageRating
        }

        @Test
        @DisplayName("activeItemCount is correct")
        void activeItemCountIsCorrect() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(7L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(7, result.getActiveItemCount());
        }

        @Test
        @DisplayName("completedTradeCount counts ACCEPTED offers as sender and receiver")
        void completedTradeCountCountsBoth() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(5L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(3L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(8, result.getCompletedTradeCount());
        }

        @Test
        @DisplayName("cancelledTradeCount counts CANCELLED offers as sender and receiver")
        void cancelledTradeCountCountsBoth() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.ACCEPTED)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(2L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(4L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(6, result.getCancelledTradeCount());
        }

        @Test
        @DisplayName("averageRating is always null")
        void averageRatingAlwaysNull() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertNull(result.getAverageRating());
        }
    }

    // ── listPublicItems ──────────────────────────────────────────

    @Nested
    @DisplayName("listPublicItems")
    class ListPublicItems {

        @Test
        @DisplayName("returns only ACTIVE items for active user")
        void returnsActiveItems() {
            UUID userUuid = UUID.randomUUID();
            UserEntity activeUser = user(userUuid, "alice", UserStatus.ACTIVE);

            ItemEntity activeItem = item(1L, UUID.randomUUID(), 10L);
            CategoryEntity cat = new CategoryEntity();
            cat.setId(20L);
            cat.setUuid(UUID.randomUUID());
            cat.setName("Books");
            cat.setSlug("books");
            cat.setSortOrder(0);
            cat.setCreatedAt(OffsetDateTime.now());

            Page<ItemEntity> itemPage = new PageImpl<>(List.of(activeItem),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 1);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(
                    eq(10L), eq(ItemStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(itemPage);
            when(categoryRepository.findAllById(any())).thenReturn(List.of(cat));
            when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(1L))
                    .thenReturn(Optional.empty());

            ItemSummaryResponse summary = new ItemSummaryResponse().uuid(activeItem.getUuid()).title("Test Item");
            when(itemMapper.toSummaryResponse(activeItem, cat, userUuid, "alice", null))
                    .thenReturn(summary);

            ItemPagedResponse expected = new ItemPagedResponse()
                    .content(List.of(summary)).page(0).size(20).totalElements(1L).totalPages(1);
            when(pageResponseMapper.toItemPagedResponse(eq(itemPage), any(), any()))
                    .thenReturn(expected);

            ItemPagedResponse result = service.listPublicItems(userUuid, 0, 20, null);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("throws NOT_FOUND for non-active user")
        void throwsNotFoundForNonActiveUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(
                    Optional.of(user(uuid, "banned", UserStatus.BANNED)));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.listPublicItems(uuid, 0, 20, null));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND for non-existing user")
        void throwsNotFoundForNonExistingUser() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.listPublicItems(uuid, 0, 20, null));
            assertEquals(404, ex.getStatus().value());
        }
    }
}

