package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.RecommendationPagedResponse;
import com.barterplatform.api.model.RecommendationReason;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.ItemTagId;
import com.barterplatform.domain.catalog.entity.SavedSearchEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.SavedSearchRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LightweightRecommendationServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemTagRepository itemTagRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private SavedSearchRepository savedSearchRepository;
    @Mock private TradeOfferRepository tradeOfferRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;

    private LightweightRecommendationServiceImpl service;
    private CategoryEntity books;
    private CategoryEntity games;
    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void setUp() {
        service = new LightweightRecommendationServiceImpl(
                itemRepository,
                itemTagRepository,
                categoryRepository,
                tagRepository,
                userRepository,
                savedSearchRepository,
                tradeOfferRepository,
                itemImageRepository,
                itemMapper,
                itemImageMapper);

        books = category(10L, UUID.fromString("10000000-0000-4000-8000-000000000001"), "Books");
        games = category(20L, UUID.fromString("10000000-0000-4000-8000-000000000002"), "Games");
        alice = user(1L, UUID.fromString("20000000-0000-4000-8000-000000000001"), "alice");
        bob = user(2L, UUID.fromString("20000000-0000-4000-8000-000000000002"), "bob");

        lenient().when(itemTagRepository.findByIdItemIdIn(any())).thenReturn(List.of());
        lenient().when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(anyLong())).thenReturn(Optional.empty());
        lenient().when(categoryRepository.findAllById(any())).thenReturn(List.of(books, games));
        lenient().when(userRepository.findAllById(any())).thenReturn(List.of(alice, bob));
        lenient().when(itemMapper.toSummaryResponse(any(ItemEntity.class), any(CategoryEntity.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ItemEntity item = invocation.getArgument(0);
                    CategoryEntity category = invocation.getArgument(1);
                    UUID ownerUuid = invocation.getArgument(2);
                    String ownerUsername = invocation.getArgument(3);
                    return new ItemSummaryResponse()
                            .uuid(item.getUuid())
                            .title(item.getTitle())
                            .status(com.barterplatform.api.model.ItemStatus.valueOf(item.getStatus().name()))
                            .condition(com.barterplatform.api.model.ItemCondition.valueOf(item.getCondition().name()))
                            .categoryUuid(category.getUuid())
                            .categoryName(category.getName())
                            .ownerUuid(ownerUuid)
                            .ownerUsername(ownerUsername)
                            .createdAt(item.getCreatedAt());
                });
    }

    @Test
    void personalizedRecommendationsUseSavedSearchSignals() {
        ItemEntity matching = item(101L, UUID.fromString("30000000-0000-4000-8000-000000000001"), bob.getId(), books.getId(), "Vintage book set", OffsetDateTime.parse("2026-05-01T10:00:00Z"), ItemStatus.ACTIVE);
        ItemEntity newerNonMatching = item(102L, UUID.fromString("30000000-0000-4000-8000-000000000002"), bob.getId(), games.getId(), "Board game", OffsetDateTime.parse("2026-05-02T10:00:00Z"), ItemStatus.ACTIVE);
        SavedSearchEntity savedSearch = savedSearch("{\"categoryUuid\":\"%s\"}".formatted(books.getUuid()));

        stubAuthenticatedNoListingOrTradeSignals();
        when(savedSearchRepository.findByUserId(eq(alice.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedSearch)));
        when(categoryRepository.findByUuid(books.getUuid())).thenReturn(Optional.of(books));
        when(itemRepository.findByStatusAndDeletedAtIsNull(eq(ItemStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newerNonMatching, matching)));

        RecommendationPagedResponse result = service.listRecommendations(alice.getUuid(), 0, 12, null);

        assertEquals(2, result.getContent().size());
        assertEquals(matching.getUuid(), result.getContent().getFirst().getItem().getUuid());
        assertEquals(RecommendationReason.BECAUSE_OF_INTERESTS, result.getContent().getFirst().getReason());
    }

    @Test
    void anonymousFallbackUsesRecentPopularOrdering() {
        ItemEntity older = item(101L, UUID.fromString("30000000-0000-4000-8000-000000000001"), alice.getId(), books.getId(), "Older listing", OffsetDateTime.parse("2026-05-01T10:00:00Z"), ItemStatus.ACTIVE);
        ItemEntity newer = item(102L, UUID.fromString("30000000-0000-4000-8000-000000000002"), bob.getId(), games.getId(), "Newer listing", OffsetDateTime.parse("2026-05-02T10:00:00Z"), ItemStatus.ACTIVE);
        when(itemRepository.findByStatusAndDeletedAtIsNull(eq(ItemStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(older, newer)));

        RecommendationPagedResponse result = service.listRecommendations(null, 0, 12, null);

        assertEquals(newer.getUuid(), result.getContent().getFirst().getItem().getUuid());
        assertEquals(RecommendationReason.POPULAR_RECENTLY, result.getContent().getFirst().getReason());
        assertEquals(RecommendationReason.POPULAR_RECENTLY, result.getContent().get(1).getReason());
    }

    @Test
    void exclusionRulesRemoveOwnAndUnavailableItems() {
        ItemEntity ownActive = item(101L, UUID.fromString("30000000-0000-4000-8000-000000000001"), alice.getId(), books.getId(), "Own listing", OffsetDateTime.parse("2026-05-03T10:00:00Z"), ItemStatus.ACTIVE);
        ItemEntity archived = item(102L, UUID.fromString("30000000-0000-4000-8000-000000000002"), bob.getId(), books.getId(), "Archived listing", OffsetDateTime.parse("2026-05-02T10:00:00Z"), ItemStatus.ARCHIVED);
        ItemEntity activeOther = item(103L, UUID.fromString("30000000-0000-4000-8000-000000000003"), bob.getId(), books.getId(), "Other active", OffsetDateTime.parse("2026-05-01T10:00:00Z"), ItemStatus.ACTIVE);

        stubAuthenticatedNoSignals();
        when(itemRepository.findByStatusAndDeletedAtIsNull(eq(ItemStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownActive, archived, activeOther)));

        RecommendationPagedResponse result = service.listRecommendations(alice.getUuid(), 0, 12, null);

        assertEquals(1, result.getContent().size());
        assertEquals(activeOther.getUuid(), result.getContent().getFirst().getItem().getUuid());
        verify(itemRepository).findByStatusAndDeletedAtIsNull(eq(ItemStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void deterministicOrderingUsesUuidTieBreakerAfterScoreAndCreatedAt() {
        ItemEntity higherUuid = item(101L, UUID.fromString("30000000-0000-4000-8000-000000000002"), bob.getId(), books.getId(), "Second UUID", OffsetDateTime.parse("2026-05-01T10:00:00Z"), ItemStatus.ACTIVE);
        ItemEntity lowerUuid = item(102L, UUID.fromString("30000000-0000-4000-8000-000000000001"), bob.getId(), books.getId(), "First UUID", OffsetDateTime.parse("2026-05-01T10:00:00Z"), ItemStatus.ACTIVE);
        SavedSearchEntity savedSearch = savedSearch("{\"categoryUuid\":\"%s\"}".formatted(books.getUuid()));

        stubAuthenticatedNoListingOrTradeSignals();
        when(savedSearchRepository.findByUserId(eq(alice.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedSearch)));
        when(categoryRepository.findByUuid(books.getUuid())).thenReturn(Optional.of(books));
        when(itemRepository.findByStatusAndDeletedAtIsNull(eq(ItemStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(higherUuid, lowerUuid)));

        RecommendationPagedResponse result = service.listRecommendations(alice.getUuid(), 0, 12, null);

        assertEquals(lowerUuid.getUuid(), result.getContent().getFirst().getItem().getUuid());
        assertEquals(higherUuid.getUuid(), result.getContent().get(1).getItem().getUuid());
    }

    private void stubAuthenticatedNoSignals() {
        stubAuthenticatedNoListingOrTradeSignals();
        when(savedSearchRepository.findByUserId(eq(alice.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private void stubAuthenticatedNoListingOrTradeSignals() {
        when(userRepository.findByUuid(alice.getUuid())).thenReturn(Optional.of(alice));
        when(itemRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(eq(alice.getId()), eq(ItemStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(tradeOfferRepository.findBySenderUserIdOrReceiverUserId(eq(alice.getId()), eq(alice.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private CategoryEntity category(Long id, UUID uuid, String name) {
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setUuid(uuid);
        category.setName(name);
        category.setSlug(name.toLowerCase());
        category.setSortOrder(0);
        category.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        category.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return category;
    }

    private UserEntity user(Long id, UUID uuid, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUuid(uuid);
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    private ItemEntity item(Long id, UUID uuid, Long ownerId, Long categoryId, String title, OffsetDateTime createdAt, ItemStatus status) {
        ItemEntity item = new ItemEntity();
        item.setId(id);
        item.setUuid(uuid);
        item.setOwnerId(ownerId);
        item.setCategoryId(categoryId);
        item.setTitle(title);
        item.setStatus(status);
        item.setCondition(ItemCondition.GOOD);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(createdAt);
        return item;
    }

    @SuppressWarnings("unused")
    private ItemTagEntity itemTag(Long itemId, Long tagId) {
        ItemTagId id = new ItemTagId();
        id.setItemId(itemId);
        id.setTagId(tagId);
        ItemTagEntity itemTag = new ItemTagEntity();
        itemTag.setId(id);
        itemTag.setAssignedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return itemTag;
    }

    private SavedSearchEntity savedSearch(String criteriaPayload) {
        SavedSearchEntity savedSearch = new SavedSearchEntity();
        savedSearch.setId(500L);
        savedSearch.setUuid(UUID.randomUUID());
        savedSearch.setUserId(alice.getId());
        savedSearch.setName("Saved search");
        savedSearch.setCriteriaPayload(criteriaPayload);
        savedSearch.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        savedSearch.setUpdatedAt(OffsetDateTime.parse("2026-05-01T00:00:00Z"));
        return savedSearch;
    }
}

