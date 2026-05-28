package com.barterplatform.application.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import com.barterplatform.api.model.PublicProfileReviewSnippetResponse;
import com.barterplatform.api.model.ReputationSummaryResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.reputation.service.ReputationService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock private TradeReviewRepository tradeReviewRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;
    @Mock private PageResponseMapper pageResponseMapper;
    @Mock private ReputationService reputationService;

    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    private PublicProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicProfileServiceImpl(
                userRepository, itemRepository, tradeOfferRepository,
                categoryRepository, itemImageRepository, tradeReviewRepository,
                itemMapper, itemImageMapper,
                pageRequestFactory, pageResponseMapper,
                reputationService);
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

    private ItemEntity item(UUID uuid) {
        ItemEntity e = new ItemEntity();
        e.setId(1L);
        e.setUuid(uuid);
        e.setOwnerId(10L);
        e.setCategoryId(20L);
        e.setTitle("Test Item");
        e.setStatus(ItemStatus.ACTIVE);
        e.setCondition(ItemCondition.GOOD);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private ReputationSummaryResponse emptyReputationSummary() {
        return new ReputationSummaryResponse()
                .positiveReviewCount(0)
                .negativeReviewCount(0)
                .totalReviewCount(0)
                .positivePercentage(null);
    }

    private record ReviewSnippetProjection(
            UUID uuid,
            String reviewerUsername,
            TradeReviewRating rating,
            String comment,
            OffsetDateTime createdAt)
            implements TradeReviewRepository.PublicProfileReviewSnippetProjection {
        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getReviewerUsername() {
            return reviewerUsername;
        }

        @Override
        public TradeReviewRating getRating() {
            return rating;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }
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
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(2L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(1L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(1L);
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(4)
                    .negativeReviewCount(1)
                    .totalReviewCount(5)
                    .positivePercentage(80.0));

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertNotNull(result);
            assertEquals(uuid, result.getUuid());
            assertEquals("alice", result.getUsername());
            assertNotNull(result.getJoinedAt());
            assertEquals(3, result.getActiveItemCount());
            assertEquals(3, result.getCompletedTradeCount());
            assertEquals(1, result.getCancelledTradeCount());
            assertNull(result.getAverageRating());
            assertNotNull(result.getReputationSummary());
            assertEquals(4, result.getReputationSummary().getPositiveReviewCount());
            assertEquals(80.0, result.getReputationSummary().getPositivePercentage());
            assertNotNull(result.getRecentReviews());
        }

        @Test
        @DisplayName("includes latest public-safe recent review snippets")
        void includesRecentReviewSnippets() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);
            OffsetDateTime newest = OffsetDateTime.now();
            String longComment = "Reliable and friendly trader. ".repeat(8) + "Kept communication clear.";

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(reputationService.getReputationSummary(10L)).thenReturn(emptyReputationSummary());
            when(tradeReviewRepository.findLatestCommentedReviewsForReviewedUser(
                    eq(10L), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(List.of(
                            new ReviewSnippetProjection(UUID.randomUUID(), "reviewer-one",
                                    TradeReviewRating.POSITIVE, longComment, newest),
                            new ReviewSnippetProjection(UUID.randomUUID(), "reviewer-two",
                                    TradeReviewRating.NEGATIVE, "Item condition was different than expected.", newest.minusDays(1))));

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(2, result.getRecentReviews().size());
            PublicProfileReviewSnippetResponse first = result.getRecentReviews().getFirst();
            assertEquals("reviewer-one", first.getReviewerUsername());
            assertEquals(com.barterplatform.api.model.TradeReviewRating.POSITIVE, first.getRating());
            assertEquals(newest, first.getCreatedAt());
            assertFalse(first.getCommentSnippet().isBlank());
            assertTrue(first.getCommentSnippet().codePointCount(0, first.getCommentSnippet().length()) <= 160);
            assertEquals("…", first.getCommentSnippet().substring(first.getCommentSnippet().length() - 1));
        }

        @Test
        @DisplayName("returns empty recent reviews when no commented reviews exist")
        void returnsEmptyRecentReviews() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(reputationService.getReputationSummary(10L)).thenReturn(emptyReputationSummary());
            when(tradeReviewRepository.findLatestCommentedReviewsForReviewedUser(
                    eq(10L), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(List.of());

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertNotNull(result.getRecentReviews());
            assertEquals(List.of(), result.getRecentReviews());
        }

        @Test
        @DisplayName("requests recent reviews only from active reviewers and limits to three")
        void inactiveReviewerSnippetsAreOmittedByRepositoryFilter() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(eq(10L), any())).thenReturn(0L);
            when(reputationService.getReputationSummary(10L)).thenReturn(emptyReputationSummary());
            when(tradeReviewRepository.findLatestCommentedReviewsForReviewedUser(
                    eq(10L), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(List.of());

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(List.of(), result.getRecentReviews());
            verify(tradeReviewRepository).findLatestCommentedReviewsForReviewedUser(
                    eq(10L), eq(UserStatus.ACTIVE), pageableCaptor.capture());
            assertEquals(3, pageableCaptor.getValue().getPageSize());
        }

        @Test
        @DisplayName("review snippets expose no sensitive fields")
        void recentReviewSnippetsExposeNoSensitiveFields() {
            List<String> publicGetterNames = Stream.of(PublicProfileReviewSnippetResponse.class.getMethods())
                    .map(Method::getName)
                    .toList();

            assertFalse(publicGetterNames.contains("getTradeOfferUuid"));
            assertFalse(publicGetterNames.contains("getReviewerEmail"));
            assertFalse(publicGetterNames.contains("getReviewerStatus"));
            assertFalse(publicGetterNames.contains("getReviewerUserId"));
            assertFalse(publicGetterNames.contains("getId"));
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
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(0)
                    .negativeReviewCount(0)
                    .totalReviewCount(0)
                    .positivePercentage(null));

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
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(0)
                    .negativeReviewCount(0)
                    .totalReviewCount(0)
                    .positivePercentage(null));

            PublicProfileResponse result = service.getPublicProfile(uuid);

            assertEquals(7, result.getActiveItemCount());
        }

        @Test
        @DisplayName("completedTradeCount counts COMPLETED offers as sender and receiver")
        void completedTradeCountCountsBoth() {
            UUID uuid = UUID.randomUUID();
            UserEntity activeUser = user(uuid, "alice", UserStatus.ACTIVE);

            when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(activeUser));
            when(itemRepository.countByOwnerIdAndStatus(10L, ItemStatus.ACTIVE)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(5L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(3L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(0L);
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(0)
                    .negativeReviewCount(0)
                    .totalReviewCount(0)
                    .positivePercentage(null));

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
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(0L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.COMPLETED)).thenReturn(0L);
            when(tradeOfferRepository.countBySenderUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(2L);
            when(tradeOfferRepository.countByReceiverUserIdAndStatus(10L, TradeOfferStatus.CANCELLED)).thenReturn(4L);
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(0)
                    .negativeReviewCount(0)
                    .totalReviewCount(0)
                    .positivePercentage(null));

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
            when(reputationService.getReputationSummary(10L)).thenReturn(new ReputationSummaryResponse()
                    .positiveReviewCount(0)
                    .negativeReviewCount(0)
                    .totalReviewCount(0)
                    .positivePercentage(null));

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

            ItemEntity activeItem = item(UUID.randomUUID());
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

