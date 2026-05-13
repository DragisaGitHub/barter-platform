package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.FavoriteItemEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.FavoriteItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class FavoriteItemServiceImplTest {

    @Mock private FavoriteItemRepository favoriteItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private ItemImageMapper itemImageMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private PageResponseMapper pageResponseMapper;

    private FavoriteItemServiceImpl service;
    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    @BeforeEach
    void setUp() {
        service = new FavoriteItemServiceImpl(
                favoriteItemRepository,
                userRepository,
                itemRepository,
                categoryRepository,
                itemImageRepository,
                itemImageMapper,
                itemMapper,
                pageRequestFactory,
                pageResponseMapper);
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

    private ItemEntity item(Long id, UUID uuid, Long ownerId, Long categoryId) {
        ItemEntity item = new ItemEntity();
        item.setId(id);
        item.setUuid(uuid);
        item.setOwnerId(ownerId);
        item.setCategoryId(categoryId);
        item.setTitle("Vintage Toy");
        item.setStatus(ItemStatus.ACTIVE);
        item.setCondition(ItemCondition.GOOD);
        item.setCreatedAt(OffsetDateTime.now());
        return item;
    }

    private FavoriteItemEntity favorite(Long id, Long userId, Long itemId) {
        FavoriteItemEntity favorite = new FavoriteItemEntity();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favorite.setCreatedAt(OffsetDateTime.now());
        return favorite;
    }

    private CategoryEntity category(Long id, UUID uuid, String name) {
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setUuid(uuid);
        category.setName(name);
        category.setSlug(name.toLowerCase());
        category.setSortOrder(0);
        category.setCreatedAt(OffsetDateTime.now());
        return category;
    }

    @Nested
    @DisplayName("favoriteItem")
    class FavoriteItem {

        @Test
        @DisplayName("saves a new favorite successfully")
        void favoriteSuccess() {
            UUID userUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity user = user(1L, userUuid, "alice");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(item));
            when(favoriteItemRepository.existsByUserIdAndItemId(1L, 10L)).thenReturn(false);

            MessageResponse response = service.favoriteItem(userUuid, itemUuid);

            assertNotNull(response);
            assertEquals("Item favorited successfully.", response.getMessage());

            ArgumentCaptor<FavoriteItemEntity> captor = ArgumentCaptor.forClass(FavoriteItemEntity.class);
            verify(favoriteItemRepository).save(captor.capture());
            assertEquals(1L, captor.getValue().getUserId());
            assertEquals(10L, captor.getValue().getItemId());
        }

        @Test
        @DisplayName("returns success without inserting when already favorited")
        void favoriteIdempotent() {
            UUID userUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity user = user(1L, userUuid, "alice");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(item));
            when(favoriteItemRepository.existsByUserIdAndItemId(1L, 10L)).thenReturn(true);

            MessageResponse response = service.favoriteItem(userUuid, itemUuid);

            assertEquals("Item favorited successfully.", response.getMessage());
            verify(favoriteItemRepository, never()).save(any(FavoriteItemEntity.class));
        }

        @Test
        @DisplayName("throws NOT_FOUND when item is removed")
        void favoriteMissingRemovedItem() {
            UUID userUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity user = user(1L, userUuid, "alice");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);
            item.setStatus(ItemStatus.REMOVED);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(item));

            ApiException exception = assertThrows(ApiException.class,
                    () -> service.favoriteItem(userUuid, itemUuid));

            assertEquals(404, exception.getStatus().value());
            verify(favoriteItemRepository, never()).save(any(FavoriteItemEntity.class));
        }
    }

    @Nested
    @DisplayName("unfavoriteItem")
    class UnfavoriteItem {

        @Test
        @DisplayName("deletes an existing favorite")
        void unfavoriteSuccess() {
            UUID userUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity user = user(1L, userUuid, "alice");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);
            FavoriteItemEntity favorite = favorite(100L, 1L, 10L);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(item));
            when(favoriteItemRepository.findByUserIdAndItemId(1L, 10L)).thenReturn(Optional.of(favorite));

            service.unfavoriteItem(userUuid, itemUuid);

            verify(favoriteItemRepository).delete(favorite);
        }

        @Test
        @DisplayName("does nothing when the favorite does not exist")
        void unfavoriteIdempotent() {
            UUID userUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity user = user(1L, userUuid, "alice");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);

            when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(item));
            when(favoriteItemRepository.findByUserIdAndItemId(1L, 10L)).thenReturn(Optional.empty());

            service.unfavoriteItem(userUuid, itemUuid);

            verify(favoriteItemRepository, never()).delete(any(FavoriteItemEntity.class));
        }
    }

    @Nested
    @DisplayName("listFavoriteItems")
    class ListFavoriteItems {

        @Test
        @DisplayName("returns paged favorites for the authenticated user")
        void listFavoritesSuccess() {
            UUID currentUserUuid = UUID.randomUUID();
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID categoryUuid = UUID.randomUUID();
            UserEntity currentUser = user(1L, currentUserUuid, "alice");
            UserEntity owner = user(2L, ownerUuid, "bob");
            ItemEntity item = item(10L, itemUuid, 2L, 20L);
            FavoriteItemEntity favorite = favorite(100L, 1L, 10L);
            CategoryEntity category = category(20L, categoryUuid, "Toys");
            ItemImageEntity primaryImage = new ItemImageEntity();
            primaryImage.setId(500L);
            primaryImage.setItemId(10L);
            Page<FavoriteItemEntity> favoritePage = new PageImpl<>(
                    List.of(favorite),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1);
            ItemSummaryResponse summary = new ItemSummaryResponse().uuid(itemUuid).title("Vintage Toy")
                    .primaryImageUrl("https://cdn.example/items/10-primary.jpg");
            ItemPagedResponse expected = new ItemPagedResponse()
                    .content(List.of(summary))
                    .page(0)
                    .size(20)
                    .totalElements(1L)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .sort("createdAt,desc");

            when(userRepository.findByUuid(currentUserUuid)).thenReturn(Optional.of(currentUser));
            when(favoriteItemRepository.findVisibleByUserId(eq(1L), eq(ItemStatus.REMOVED), any(Pageable.class)))
                    .thenReturn(favoritePage);
            when(itemRepository.findAllById(any())).thenReturn(List.of(item));
            when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
            when(userRepository.findAllById(any())).thenReturn(List.of(owner));
            when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(10L)).thenReturn(Optional.of(primaryImage));
            when(itemImageMapper.toResponse(primaryImage)).thenReturn(
                    new com.barterplatform.api.model.ItemImageResponse()
                            .url("https://cdn.example/items/10-primary.jpg"));
            when(itemMapper.toSummaryResponse(item, category, ownerUuid, "bob",
                    "https://cdn.example/items/10-primary.jpg"))
                    .thenReturn(summary);
            when(pageResponseMapper.toItemPagedResponse(eq(favoritePage), eq(List.of(summary)), eq("createdAt,desc")))
                    .thenReturn(expected);

            ItemPagedResponse result = service.listFavoriteItems(currentUserUuid, 0, 20, null);

            assertNotNull(result);
            assertEquals(1L, result.getTotalElements());
            assertEquals("Vintage Toy", result.getContent().getFirst().getTitle());
            verify(favoriteItemRepository).findVisibleByUserId(eq(1L), eq(ItemStatus.REMOVED), any(Pageable.class));
        }
    }
}

