package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.WishlistMatchReason;
import com.barterplatform.api.model.WishlistMatchResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemListingEntryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WishlistMatchServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemListingEntryRepository itemListingEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;

    private WishlistMatchServiceImpl service;
    private UserEntity wishlistOwner;
    private UserEntity candidateOwner;
    private CategoryEntity books;

    @BeforeEach
    void setUp() {
        service = new WishlistMatchServiceImpl(
                itemRepository,
                itemListingEntryRepository,
                userRepository,
                categoryRepository,
                itemImageRepository,
                itemMapper,
                itemImageMapper);

        wishlistOwner = user(1L, UUID.fromString("20000000-0000-4000-8000-000000000001"), "alice");
        candidateOwner = user(2L, UUID.fromString("20000000-0000-4000-8000-000000000002"), "bob");
        books = category(UUID.fromString("10000000-0000-4000-8000-000000000001"));

        lenient().when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(anyLong())).thenReturn(Optional.empty());
        lenient().when(itemMapper.toSummaryResponse(any(ItemEntity.class), any(CategoryEntity.class), any(), any(), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    ItemEntity item = invocation.getArgument(0);
                    CategoryEntity category = invocation.getArgument(1);
                    UUID ownerUuid = invocation.getArgument(2);
                    String ownerUsername = invocation.getArgument(3);
                    return new ItemSummaryResponse()
                            .uuid(item.getUuid())
                            .title(item.getTitle())
                            .categoryUuid(category.getUuid())
                            .categoryName(category.getName())
                            .ownerUuid(ownerUuid)
                            .ownerUsername(ownerUsername);
                });
    }

    @Test
    @DisplayName("owner can list matches for active wishlist")
    void ownerCanListMatchesForActiveWishlist() {
        ItemEntity wishlist = wishlistItem(UUID.fromString("30000000-0000-4000-8000-000000000001"), "Rare comics", "Looking for mint issues");
        ItemEntity candidate = candidateItem(UUID.fromString("30000000-0000-4000-8000-000000000002"), "Chess set", "Wooden board");

        when(userRepository.findByUuid(wishlistOwner.getUuid())).thenReturn(Optional.of(wishlistOwner));
        when(itemRepository.findByUuid(wishlist.getUuid())).thenReturn(Optional.of(wishlist));
        when(itemRepository.findWishlistMatchCandidates(
                eq(wishlist.getId()),
                eq(wishlist.getOwnerId()),
                eq(wishlist.getCategoryId()),
                eq(EnumSet.of(
                        ListingTemplateType.STANDARD_ITEM,
                        ListingTemplateType.PICK_FROM_COLLECTION,
                        ListingTemplateType.COLLECTION_ALBUM)),
                eq(ItemStatus.ACTIVE),
                eq(PageRequest.of(0, 50))))
                .thenReturn(List.of(candidate));
        when(itemListingEntryRepository.findByItemIdInOrderByItemIdAscSortOrderAsc(List.of(candidate.getId())))
                .thenReturn(List.of());
        when(categoryRepository.findById(candidate.getCategoryId())).thenReturn(Optional.of(books));
        when(userRepository.findById(candidate.getOwnerId())).thenReturn(Optional.of(candidateOwner));

        List<WishlistMatchResponse> result = service.listWishlistMatches(wishlistOwner.getUuid(), wishlist.getUuid());

        assertEquals(1, result.size());
        assertEquals(candidate.getUuid(), result.getFirst().getItem().getUuid());
        verify(itemRepository).findWishlistMatchCandidates(
                eq(wishlist.getId()),
                eq(wishlist.getOwnerId()),
                eq(wishlist.getCategoryId()),
                eq(EnumSet.of(
                        ListingTemplateType.STANDARD_ITEM,
                        ListingTemplateType.PICK_FROM_COLLECTION,
                        ListingTemplateType.COLLECTION_ALBUM)),
                eq(ItemStatus.ACTIVE),
                eq(PageRequest.of(0, 50)));
    }

    @Test
    @DisplayName("non-owner is rejected")
    void nonOwnerIsRejected() {
        UserEntity intruder = user(99L, UUID.fromString("20000000-0000-4000-8000-000000000099"), "mallory");
        ItemEntity wishlist = wishlistItem(UUID.fromString("30000000-0000-4000-8000-000000000003"), "Rare comics", "Looking for mint issues");

        when(userRepository.findByUuid(intruder.getUuid())).thenReturn(Optional.of(intruder));
        when(itemRepository.findByUuid(wishlist.getUuid())).thenReturn(Optional.of(wishlist));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.listWishlistMatches(intruder.getUuid(), wishlist.getUuid()));

        assertEquals("Only the wishlist owner can view matches.", ex.getMessage());
        verify(itemRepository, never()).findWishlistMatchCandidates(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("non-wishlist item is rejected")
    void nonWishlistItemIsRejected() {
        ItemEntity nonWishlist = baseItem(
                101L,
                UUID.fromString("30000000-0000-4000-8000-000000000004"),
                wishlistOwner.getId(),
                books.getId(),
                "Books to trade",
                "Open to swaps",
                ItemStatus.ACTIVE,
                ListingTemplateType.STANDARD_ITEM);

        when(userRepository.findByUuid(wishlistOwner.getUuid())).thenReturn(Optional.of(wishlistOwner));
        when(itemRepository.findByUuid(nonWishlist.getUuid())).thenReturn(Optional.of(nonWishlist));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.listWishlistMatches(wishlistOwner.getUuid(), nonWishlist.getUuid()));

        assertEquals("Item is not a wishlist listing.", ex.getMessage());
        verify(itemRepository, never()).findWishlistMatchCandidates(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("inactive wishlist is rejected")
    void inactiveWishlistIsRejected() {
        ItemEntity inactiveWishlist = baseItem(
                102L,
                UUID.fromString("30000000-0000-4000-8000-000000000005"),
                wishlistOwner.getId(),
                books.getId(),
                "Rare comics",
                "Looking for mint issues",
                ItemStatus.DRAFT,
                ListingTemplateType.WISHLIST);

        when(userRepository.findByUuid(wishlistOwner.getUuid())).thenReturn(Optional.of(wishlistOwner));
        when(itemRepository.findByUuid(inactiveWishlist.getUuid())).thenReturn(Optional.of(inactiveWishlist));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.listWishlistMatches(wishlistOwner.getUuid(), inactiveWishlist.getUuid()));

        assertEquals("Wishlist item must be active.", ex.getMessage());
        verify(itemRepository, never()).findWishlistMatchCandidates(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("same category plus compatible template returns score 50")
    void sameCategoryAndCompatibleTemplateReturnsScore50() {
        ItemEntity wishlist = wishlistItem(UUID.fromString("30000000-0000-4000-8000-000000000006"), "Rare comics", "Looking for mint issues");
        ItemEntity candidate = candidateItem(UUID.fromString("30000000-0000-4000-8000-000000000007"), "Chess set", "Wooden board");

        stubMatchLookup(wishlist, candidate, List.of());

        WishlistMatchResponse result = service.listWishlistMatches(wishlistOwner.getUuid(), wishlist.getUuid()).getFirst();

        assertEquals(50, result.getScore());
        assertIterableEquals(
                List.of(WishlistMatchReason.SAME_CATEGORY, WishlistMatchReason.COMPATIBLE_TEMPLATE),
                result.getReasons());
    }

    @Test
    @DisplayName("entry overlap adds ENTRY_MATCH")
    void entryOverlapAddsEntryMatchReason() {
        ItemEntity wishlist = wishlistItem(UUID.fromString("30000000-0000-4000-8000-000000000008"), "Harry Potter", "Looking for paperbacks");
        ItemEntity candidate = candidateItem(UUID.fromString("30000000-0000-4000-8000-000000000009"), "Mixed lot", "Collection bundle");
        ItemListingEntryEntity overlappingEntry = entry(candidate.getId());

        stubMatchLookup(wishlist, candidate, List.of(overlappingEntry));

        WishlistMatchResponse result = service.listWishlistMatches(wishlistOwner.getUuid(), wishlist.getUuid()).getFirst();

        assertEquals(80, result.getScore());
        assertTrue(result.getReasons().contains(WishlistMatchReason.ENTRY_MATCH));
    }

    private void stubMatchLookup(ItemEntity wishlist, ItemEntity candidate, List<ItemListingEntryEntity> entries) {
        when(userRepository.findByUuid(wishlistOwner.getUuid())).thenReturn(Optional.of(wishlistOwner));
        when(itemRepository.findByUuid(wishlist.getUuid())).thenReturn(Optional.of(wishlist));
        when(itemRepository.findWishlistMatchCandidates(
                eq(wishlist.getId()),
                eq(wishlist.getOwnerId()),
                eq(wishlist.getCategoryId()),
                eq(EnumSet.of(
                        ListingTemplateType.STANDARD_ITEM,
                        ListingTemplateType.PICK_FROM_COLLECTION,
                        ListingTemplateType.COLLECTION_ALBUM)),
                eq(ItemStatus.ACTIVE),
                eq(PageRequest.of(0, 50))))
                .thenReturn(List.of(candidate));
        when(itemListingEntryRepository.findByItemIdInOrderByItemIdAscSortOrderAsc(List.of(candidate.getId())))
                .thenReturn(entries);
        when(categoryRepository.findById(candidate.getCategoryId())).thenReturn(Optional.of(books));
        when(userRepository.findById(candidate.getOwnerId())).thenReturn(Optional.of(candidateOwner));
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

    private CategoryEntity category(UUID uuid) {
        CategoryEntity category = new CategoryEntity();
        category.setId(10L);
        category.setUuid(uuid);
        category.setName("Books");
        category.setSlug("Books".toLowerCase());
        category.setSortOrder(0);
        category.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        category.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return category;
    }

    private ItemEntity wishlistItem(UUID uuid, String title, String description) {
        return baseItem(100L, uuid, wishlistOwner.getId(), books.getId(), title, description, ItemStatus.ACTIVE, ListingTemplateType.WISHLIST);
    }

    private ItemEntity candidateItem(UUID uuid, String title, String description) {
        return baseItem(200L, uuid, candidateOwner.getId(), books.getId(), title, description, ItemStatus.ACTIVE, ListingTemplateType.STANDARD_ITEM);
    }

    private ItemEntity baseItem(
            Long id,
            UUID uuid,
            Long ownerId,
            Long categoryId,
            String title,
            String description,
            ItemStatus status,
            ListingTemplateType listingTemplateType) {
        ItemEntity item = new ItemEntity();
        item.setId(id);
        item.setUuid(uuid);
        item.setOwnerId(ownerId);
        item.setCategoryId(categoryId);
        item.setTitle(title);
        item.setDescription(description);
        item.setStatus(status);
        item.setCondition(ItemCondition.GOOD);
        item.setListingTemplateType(listingTemplateType);
        item.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        item.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return item;
    }

    private ItemListingEntryEntity entry(Long itemId) {
        ItemListingEntryEntity entry = new ItemListingEntryEntity();
        entry.setId(501L);
        entry.setUuid(UUID.randomUUID());
        entry.setItemId(itemId);
        entry.setTitle("Harry Potter paperback");
        entry.setDescription("Clean copy");
        entry.setQuantity(1);
        entry.setSortOrder(0);
        entry.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entry.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return entry;
    }
}
