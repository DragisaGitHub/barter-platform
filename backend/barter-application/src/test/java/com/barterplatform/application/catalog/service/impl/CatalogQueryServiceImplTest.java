package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.mapper.CategoryMapper;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.mapper.TagMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
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
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private ItemTagRepository itemTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private ListingModerationActionRepository listingModerationActionRepository;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TagMapper tagMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;
    @Mock private PageResponseMapper pageResponseMapper;

    private CatalogQueryServiceImpl service;

    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    @BeforeEach
    void setUp() {
        service = new CatalogQueryServiceImpl(
                categoryRepository, tagRepository, itemRepository,
                itemTagRepository, userRepository, itemImageRepository,
                listingModerationActionRepository,
                categoryMapper, tagMapper, itemMapper, itemImageMapper,
                pageRequestFactory, pageResponseMapper);
    }

    // ── Helpers ──────────────────────────────────────────────────

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

    private TagEntity tag(Long id, UUID uuid) {
        TagEntity e = new TagEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setName("Vintage");
        e.setSlug("Vintage".toLowerCase());
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private ItemEntity item(UUID uuid,
                            ItemStatus status, ItemCondition condition) {
        ItemEntity e = new ItemEntity();
        e.setId(1L);
        e.setUuid(uuid);
        e.setOwnerId(10L);
        e.setCategoryId(20L);
        e.setTitle("Test Item");
        e.setStatus(status);
        e.setCondition(condition);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private UserEntity user(UUID uuid, String username) {
        UserEntity u = new UserEntity();
        u.setId(10L);
        u.setUuid(uuid);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setCreatedAt(OffsetDateTime.now());
        return u;
    }

    // ── listCategories ───────────────────────────────────────────

    @Nested
    @DisplayName("listCategories")
    class ListCategories {

        @Test
        @DisplayName("returns mapped category list")
        void returnsMappedCategories() {
            CategoryEntity cat1 = category(1L, UUID.randomUUID(), "Books");
            CategoryEntity cat2 = category(2L, UUID.randomUUID(), "Toys");
            List<CategoryEntity> entities = List.of(cat1, cat2);

            CategoryResponse r1 = new CategoryResponse().uuid(cat1.getUuid()).name("Books");
            CategoryResponse r2 = new CategoryResponse().uuid(cat2.getUuid()).name("Toys");

            when(categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc())
                    .thenReturn(entities);
            when(categoryMapper.toResponseList(entities)).thenReturn(List.of(r1, r2));

            List<CategoryResponse> result = service.listCategories();

            assertEquals(2, result.size());
            assertEquals("Books", result.getFirst().getName());
        }
    }

    // ── listTags ─────────────────────────────────────────────────

    @Nested
    @DisplayName("listTags")
    class ListTags {

        @Test
        @DisplayName("returns mapped tag list")
        void returnsMappedTags() {
            TagEntity tag1 = tag(1L, UUID.randomUUID());
            List<TagEntity> entities = List.of(tag1);

            TagResponse tr = new TagResponse().uuid(tag1.getUuid()).name("Vintage");
            when(tagRepository.findAllByDeletedAtIsNullOrderByNameAsc()).thenReturn(entities);
            when(tagMapper.toResponseList(entities)).thenReturn(List.of(tr));

            List<TagResponse> result = service.listTags();

            assertEquals(1, result.size());
            assertEquals("Vintage", result.getFirst().getName());
        }
    }

    // ── getItemByUuid ────────────────────────────────────────────

    @Nested
    @DisplayName("getItemByUuid")
    class GetItemByUuid {

        @Test
        @DisplayName("throws NOT_FOUND when item does not exist")
        void throwsNotFoundWhenMissing() {
            UUID uuid = UUID.randomUUID();
            when(itemRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getItemByUuid(uuid, null, false));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND when item is soft-deleted")
        void throwsNotFoundWhenDeleted() {
            UUID uuid = UUID.randomUUID();
            ItemEntity entity = item(uuid, ItemStatus.ACTIVE, ItemCondition.GOOD);
            entity.setDeletedAt(OffsetDateTime.now());

            when(itemRepository.findByUuid(uuid)).thenReturn(Optional.of(entity));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getItemByUuid(uuid, null, false));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND when item has REMOVED status")
        void throwsNotFoundWhenRemoved() {
            UUID uuid = UUID.randomUUID();
            ItemEntity entity = item(uuid, ItemStatus.REMOVED, ItemCondition.GOOD);

            when(itemRepository.findByUuid(uuid)).thenReturn(Optional.of(entity));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getItemByUuid(uuid, null, false));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("returns detail response for valid item")
        void returnsDetailForValidItem() {
            UUID itemUuid = UUID.randomUUID();
            UUID ownerUuid = UUID.randomUUID();
            UUID catUuid = UUID.randomUUID();

            ItemEntity entity = item(itemUuid, ItemStatus.ACTIVE, ItemCondition.GOOD);
            CategoryEntity cat = category(20L, catUuid, "Books");
            UserEntity owner = user(ownerUuid, "alice");
            TagEntity t1 = tag(5L, UUID.randomUUID());

            ItemTagId tagId = new ItemTagId();
            tagId.setItemId(1L);
            tagId.setTagId(5L);
            ItemTagEntity ite = new ItemTagEntity();
            ite.setId(tagId);
            ite.setAssignedAt(OffsetDateTime.now());

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(entity));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(cat));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
            when(itemTagRepository.findByIdItemId(1L)).thenReturn(List.of(ite));
            when(tagRepository.findAllById(List.of(5L))).thenReturn(List.of(t1));
            when(itemImageRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
            when(itemImageMapper.toResponseList(List.of())).thenReturn(List.of());

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(itemUuid).title("Test Item");
            when(itemMapper.toDetailResponse(entity, cat, List.of(t1), ownerUuid, "alice", null, List.of()))
                    .thenReturn(expectedResponse);

            ItemDetailResponse result = service.getItemByUuid(itemUuid, null, false);

            assertNotNull(result);
            assertEquals(itemUuid, result.getUuid());
        }
    }

    // ── listMyItems ──────────────────────────────────────────────

    @Nested
    @DisplayName("listMyItems")
    class ListMyItems {

        @Test
        @DisplayName("throws NOT_FOUND when owner does not exist")
        void throwsNotFoundWhenOwnerMissing() {
            UUID uuid = UUID.randomUUID();
            when(userRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.listMyItems(uuid, 0, 20, null, null));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("returns paged items for valid owner")
        void returnsItemsForValidOwner() {
            UUID ownerUuid = UUID.randomUUID();
            UserEntity owner = user(ownerUuid, "bob");

            ItemEntity entity = item(UUID.randomUUID(), ItemStatus.DRAFT, ItemCondition.NEW);
            CategoryEntity cat = category(20L, UUID.randomUUID(), "Toys");
            Page<ItemEntity> itemPage = new PageImpl<>(List.of(entity),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 1);

            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemRepository.findByOwnerIdAndDeletedAtIsNull(eq(10L), any(Pageable.class)))
                    .thenReturn(itemPage);
            when(categoryRepository.findAllById(any())).thenReturn(List.of(cat));
            when(userRepository.findAllById(any())).thenReturn(List.of(owner));
            when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(1L))
                    .thenReturn(java.util.Optional.empty());

            ItemSummaryResponse summary = new ItemSummaryResponse().uuid(entity.getUuid());
            when(itemMapper.toSummaryResponse(entity, cat, ownerUuid, "bob", null))
                    .thenReturn(summary);

            ItemPagedResponse expected = new ItemPagedResponse()
                    .content(List.of(summary)).page(0).size(20).totalElements(1L).totalPages(1);
            when(pageResponseMapper.toItemPagedResponse(eq(itemPage), any(), any()))
                    .thenReturn(expected);

            ItemPagedResponse result = service.listMyItems(ownerUuid, 0, 20, null, null);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    // ── searchItems ──────────────────────────────────────────────

    @Nested
    @DisplayName("searchItems")
    class SearchItems {

        @Test
        @DisplayName("defaults to ACTIVE status when no status provided")
        @SuppressWarnings("unchecked")
        void defaultsToActiveStatus() {
            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ItemPagedResponse expected = new ItemPagedResponse()
                    .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(expected);

            ItemPagedResponse result = service.searchItems(
                    0, 20, null, null, null, null, null, null);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());

            // Verify that the repository was called (Specification includes ACTIVE default)
            verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("applies title/category/condition filters")
        @SuppressWarnings("unchecked")
        void appliesFilters() {
            UUID catUuid = UUID.randomUUID();
            CategoryEntity cat = category(5L, catUuid, "Books");

            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(categoryRepository.findByUuid(catUuid)).thenReturn(Optional.of(cat));
            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            ItemPagedResponse result = service.searchItems(
                    0, 20, null, "test", catUuid, null, ItemStatus.ACTIVE, ItemCondition.NEW);

            assertNotNull(result);
            verify(categoryRepository).findByUuid(catUuid);
            verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("null tagUuids — no tag lookup performed")
        @SuppressWarnings("unchecked")
        void nullTagUuids_noTagFilter() {
            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            service.searchItems(0, 20, null, null, null, null, null, null);

            verify(tagRepository, never()).findByUuid(any());
        }

        @Test
        @DisplayName("empty tagUuids list — no tag lookup performed")
        @SuppressWarnings("unchecked")
        void emptyTagUuids_noTagFilter() {
            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            service.searchItems(0, 20, null, null, null, List.of(), null, null);

            verify(tagRepository, never()).findByUuid(any());
        }

        @Test
        @DisplayName("non-empty tagUuids — tag UUIDs are resolved and filter is applied")
        @SuppressWarnings("unchecked")
        void nonEmptyTagUuids_filterApplied() {
            UUID tagUuid = UUID.randomUUID();
            TagEntity tagEntity = tag(7L, tagUuid);

            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(tagRepository.findByUuid(tagUuid)).thenReturn(Optional.of(tagEntity));
            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            service.searchItems(0, 20, null, null, null, List.of(tagUuid), null, null);

            verify(tagRepository).findByUuid(tagUuid);
            verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("unknown tagUuid is silently skipped — no filter added")
        @SuppressWarnings("unchecked")
        void unknownTagUuid_silentlySkipped() {
            UUID unknownUuid = UUID.randomUUID();

            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(tagRepository.findByUuid(unknownUuid)).thenReturn(Optional.empty());
            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            // Should not throw; unknown UUID is skipped
            ItemPagedResponse result = service.searchItems(
                    0, 20, null, null, null, List.of(unknownUuid), null, null);

            assertNotNull(result);
            verify(tagRepository).findByUuid(unknownUuid);
        }
    }
}

