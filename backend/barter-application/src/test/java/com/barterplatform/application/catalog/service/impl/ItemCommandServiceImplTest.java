package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.UpdateItemRequest;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.ItemTagId;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
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

@ExtendWith(MockitoExtension.class)
class ItemCommandServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ItemTagRepository itemTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemMapper itemMapper;

    private ItemCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemCommandServiceImpl(
                itemRepository, categoryRepository, tagRepository,
                itemTagRepository, userRepository, itemMapper);
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

    private CategoryEntity category(Long id, UUID uuid, String name) {
        CategoryEntity e = new CategoryEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setName(name);
        e.setSlug(name.toLowerCase());
        e.setSortOrder(0);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private TagEntity tag(Long id, UUID uuid, String name) {
        TagEntity e = new TagEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setName(name);
        e.setSlug(name.toLowerCase());
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private ItemEntity item(Long id, UUID uuid, Long ownerId, Long categoryId,
                            ItemStatus status, ItemCondition condition) {
        ItemEntity e = new ItemEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setOwnerId(ownerId);
        e.setCategoryId(categoryId);
        e.setTitle("Test Item");
        e.setStatus(status);
        e.setCondition(condition);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    // ── createItem ───────────────────────────────────────────────

    @Nested
    @DisplayName("createItem")
    class CreateItem {

        @Test
        @DisplayName("creates item successfully with default DRAFT status")
        void createSuccess() {
            UUID ownerUuid = UUID.randomUUID();
            UUID catUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid, "alice");
            CategoryEntity cat = category(10L, catUuid, "Books");

            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(categoryRepository.findByUuid(catUuid)).thenReturn(Optional.of(cat));

            CreateItemRequest request = new CreateItemRequest("My Book", catUuid,
                    com.barterplatform.api.model.ItemCondition.GOOD);

            // The save returns the entity with an id
            when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> {
                ItemEntity arg = invocation.getArgument(0);
                arg.setId(100L);
                return arg;
            });

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(UUID.randomUUID()).title("My Book");
            when(itemMapper.toDetailResponse(any(ItemEntity.class), any(), any(), any(), any()))
                    .thenReturn(expectedResponse);

            ItemDetailResponse result = service.createItem(ownerUuid, request);

            assertNotNull(result);
            assertEquals("My Book", result.getTitle());

            // Verify the saved entity has DRAFT status
            ArgumentCaptor<ItemEntity> captor = ArgumentCaptor.forClass(ItemEntity.class);
            verify(itemRepository).save(captor.capture());
            assertEquals(ItemStatus.DRAFT, captor.getValue().getStatus());
            assertEquals(ItemCondition.GOOD, captor.getValue().getCondition());
            assertEquals(1L, captor.getValue().getOwnerId());
            assertEquals(10L, captor.getValue().getCategoryId());
        }

        @Test
        @DisplayName("throws NOT_FOUND when category does not exist")
        void createMissingCategory() {
            UUID ownerUuid = UUID.randomUUID();
            UUID catUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid, "alice");

            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(categoryRepository.findByUuid(catUuid)).thenReturn(Optional.empty());

            CreateItemRequest request = new CreateItemRequest("My Book", catUuid,
                    com.barterplatform.api.model.ItemCondition.GOOD);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.createItem(ownerUuid, request));
            assertEquals(404, ex.getStatus().value());
            verify(itemRepository, never()).save(any());
        }
    }

    // ── updateItem ───────────────────────────────────────────────

    @Nested
    @DisplayName("updateItem")
    class UpdateItem {

        @Test
        @DisplayName("throws FORBIDDEN when non-owner tries to update")
        void updateNonOwnerForbidden() {
            UUID ownerUuid = UUID.randomUUID();
            UUID attackerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();

            UserEntity attacker = user(2L, attackerUuid, "bob");
            ItemEntity existingItem = item(50L, itemUuid, 1L, 10L, ItemStatus.DRAFT, ItemCondition.GOOD);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(existingItem));

            UpdateItemRequest request = new UpdateItemRequest().title("Hacked title");

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.updateItem(attackerUuid, itemUuid, request));
            assertEquals(403, ex.getStatus().value());
            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("replaces tags when tagUuids provided in update")
        void updateTagsReplacement() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID tag1Uuid = UUID.randomUUID();
            UUID tag2Uuid = UUID.randomUUID();

            UserEntity owner = user(1L, ownerUuid, "alice");
            CategoryEntity cat = category(10L, UUID.randomUUID(), "Books");
            ItemEntity existingItem = item(50L, itemUuid, 1L, 10L, ItemStatus.DRAFT, ItemCondition.GOOD);
            TagEntity newTag1 = tag(5L, tag1Uuid, "Vintage");
            TagEntity newTag2 = tag(6L, tag2Uuid, "Rare");

            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(existingItem));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));
            when(tagRepository.findByUuid(tag1Uuid)).thenReturn(Optional.of(newTag1));
            when(tagRepository.findByUuid(tag2Uuid)).thenReturn(Optional.of(newTag2));
            when(itemRepository.save(any(ItemEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(itemTagRepository.save(any(ItemTagEntity.class))).thenAnswer(i -> i.getArgument(0));

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(itemUuid).title("Test Item");
            when(itemMapper.toDetailResponse(any(), any(), any(), any(), any()))
                    .thenReturn(expectedResponse);

            UpdateItemRequest request = new UpdateItemRequest()
                    .tagUuids(List.of(tag1Uuid, tag2Uuid));

            ItemDetailResponse result = service.updateItem(ownerUuid, itemUuid, request);

            assertNotNull(result);

            // Verify old tags were deleted and new ones saved (2 tags)
            verify(itemTagRepository).deleteByIdItemId(50L);
            verify(itemTagRepository, org.mockito.Mockito.times(2)).save(any(ItemTagEntity.class));
        }
    }

    // ── archiveItem ──────────────────────────────────────────────

    @Nested
    @DisplayName("archiveItem")
    class ArchiveItem {

        @Test
        @DisplayName("archives item successfully and sets archivedAt")
        void archiveSuccess() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();

            UserEntity owner = user(1L, ownerUuid, "alice");
            CategoryEntity cat = category(10L, UUID.randomUUID(), "Books");
            ItemEntity existingItem = item(50L, itemUuid, 1L, 10L, ItemStatus.ACTIVE, ItemCondition.GOOD);

            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(existingItem));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));
            when(itemTagRepository.findByIdItemId(50L)).thenReturn(List.of());
            when(itemRepository.save(any(ItemEntity.class))).thenAnswer(i -> i.getArgument(0));

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(itemUuid).title("Test Item");
            when(itemMapper.toDetailResponse(any(), any(), any(), any(), any()))
                    .thenReturn(expectedResponse);

            ArchiveItemRequest request = new ArchiveItemRequest().reason("No longer needed");

            ItemDetailResponse result = service.archiveItem(ownerUuid, itemUuid, request);

            assertNotNull(result);

            // Verify status and archivedAt were set
            ArgumentCaptor<ItemEntity> captor = ArgumentCaptor.forClass(ItemEntity.class);
            verify(itemRepository).save(captor.capture());
            assertEquals(ItemStatus.ARCHIVED, captor.getValue().getStatus());
            assertNotNull(captor.getValue().getArchivedAt());
        }

        @Test
        @DisplayName("throws FORBIDDEN when non-owner tries to archive")
        void archiveNonOwnerForbidden() {
            UUID attackerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();

            UserEntity attacker = user(2L, attackerUuid, "bob");
            ItemEntity existingItem = item(50L, itemUuid, 1L, 10L, ItemStatus.ACTIVE, ItemCondition.GOOD);

            when(userRepository.findByUuid(attackerUuid)).thenReturn(Optional.of(attacker));
            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(existingItem));

            ArchiveItemRequest request = new ArchiveItemRequest();

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.archiveItem(attackerUuid, itemUuid, request));
            assertEquals(403, ex.getStatus().value());
            verify(itemRepository, never()).save(any());
        }
    }
}

