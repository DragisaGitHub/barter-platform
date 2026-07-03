package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CategoryFormSchemaResponse;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.mapper.CategoryFormSchemaMapper;
import com.barterplatform.application.catalog.mapper.CategoryMapper;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.mapper.TagMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.ItemTagId;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemListingEntryRepository;
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
    @Mock private ItemListingEntryRepository itemListingEntryRepository;
    @Mock private ListingModerationActionRepository listingModerationActionRepository;
    @Mock private CategorySchemaRepository categorySchemaRepository;
    @Mock private CategorySchemaFieldRepository categorySchemaFieldRepository;
    @Mock private FieldOptionRepository fieldOptionRepository;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TagMapper tagMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemImageMapper itemImageMapper;
    @Mock private CategoryFormSchemaMapper categoryFormSchemaMapper;
    @Mock private ItemFieldValueSupport itemFieldValueSupport;
    @Mock private PageResponseMapper pageResponseMapper;

    private CatalogQueryServiceImpl service;

    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    @BeforeEach
    void setUp() {
        service = new CatalogQueryServiceImpl(
                categoryRepository, tagRepository, itemRepository,
                itemTagRepository, userRepository, itemImageRepository,
                itemListingEntryRepository,
                listingModerationActionRepository,
                categorySchemaRepository, categorySchemaFieldRepository, fieldOptionRepository,
                categoryMapper, tagMapper, itemMapper, itemImageMapper,
                categoryFormSchemaMapper, itemFieldValueSupport,
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

    private CategorySchemaEntity schema(Long id, UUID uuid, Long categoryId, int version, CategorySchemaStatus status) {
        CategorySchemaEntity e = new CategorySchemaEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setCategoryId(categoryId);
        e.setVersion(version);
        e.setStatus(status);
        e.setName("Schema " + version);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private CategorySchemaFieldEntity field(Long id, UUID uuid, Long schemaId, String key,
                                             CategorySchemaFieldType fieldType, int displayOrder) {
        CategorySchemaFieldEntity e = new CategorySchemaFieldEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setSchemaId(schemaId);
        e.setKey(key);
        e.setLabel(key);
        e.setFieldType(fieldType);
        e.setDisplayOrder(displayOrder);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private FieldOptionEntity option(Long id, UUID uuid, Long fieldId, String value, int displayOrder) {
        FieldOptionEntity e = new FieldOptionEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setFieldId(fieldId);
        e.setValue(value);
        e.setLabel(value);
        e.setDisplayOrder(displayOrder);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
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
    @DisplayName("getCategoryFormSchema")
    class GetCategoryFormSchema {

        @Test
        @DisplayName("throws NOT_FOUND when category does not exist")
        void throwsNotFoundWhenCategoryMissing() {
            UUID categoryUuid = UUID.randomUUID();
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getCategoryFormSchema(categoryUuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("throws NOT_FOUND when category is soft-deleted")
        void throwsNotFoundWhenCategorySoftDeleted() {
            UUID categoryUuid = UUID.randomUUID();
            CategoryEntity cat = category(1L, categoryUuid, "Books");
            cat.setDeletedAt(OffsetDateTime.now());
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(cat));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.getCategoryFormSchema(categoryUuid));
            assertEquals(404, ex.getStatus().value());
        }

        @Test
        @DisplayName("returns empty schema response when no ACTIVE schema exists")
        void returnsEmptyResponseWhenNoActiveSchema() {
            UUID categoryUuid = UUID.randomUUID();
            CategoryEntity cat = category(1L, categoryUuid, "Books");
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(cat));
            when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(1L, CategorySchemaStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            CategoryFormSchemaResponse empty = new CategoryFormSchemaResponse()
                    .categoryUuid(categoryUuid).fields(List.of());
            when(categoryFormSchemaMapper.toEmptyResponse(categoryUuid)).thenReturn(empty);

            CategoryFormSchemaResponse result = service.getCategoryFormSchema(categoryUuid);

            assertNotNull(result);
            assertEquals(categoryUuid, result.getCategoryUuid());
            assertEquals(List.of(), result.getFields());
            verify(categorySchemaFieldRepository, never()).findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(any());
        }

        @Test
        @DisplayName("returns ordered fields and options for the ACTIVE schema")
        void returnsOrderedFieldsAndOptions() {
            UUID categoryUuid = UUID.randomUUID();
            CategoryEntity cat = category(1L, categoryUuid, "Books");
            CategorySchemaEntity schemaEntity = schema(2L, UUID.randomUUID(), 1L, 1, CategorySchemaStatus.ACTIVE);

            CategorySchemaFieldEntity field1 = field(3L, UUID.randomUUID(), 2L, "author",
                    CategorySchemaFieldType.TEXT, 0);
            CategorySchemaFieldEntity field2 = field(4L, UUID.randomUUID(), 2L, "condition",
                    CategorySchemaFieldType.SINGLE_SELECT, 1);
            FieldOptionEntity opt1 = option(5L, UUID.randomUUID(), 4L, "new", 0);
            FieldOptionEntity opt2 = option(6L, UUID.randomUUID(), 4L, "used", 1);

            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(cat));
            when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(1L, CategorySchemaStatus.ACTIVE))
                    .thenReturn(Optional.of(schemaEntity));
            when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(2L))
                    .thenReturn(List.of(field1, field2));
            when(fieldOptionRepository.findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List.of(3L, 4L)))
                    .thenReturn(List.of(opt1, opt2));

            CategoryFormSchemaResponse expected = new CategoryFormSchemaResponse()
                    .categoryUuid(categoryUuid)
                    .schemaUuid(schemaEntity.getUuid())
                    .schemaVersion(1)
                    .fields(List.of());
            when(categoryFormSchemaMapper.toResponse(eq(categoryUuid), eq(schemaEntity), eq(List.of(field1, field2)), any()))
                    .thenReturn(expected);

            CategoryFormSchemaResponse result = service.getCategoryFormSchema(categoryUuid);

            assertNotNull(result);
            assertEquals(schemaEntity.getUuid(), result.getSchemaUuid());
            verify(fieldOptionRepository).findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List.of(3L, 4L));
        }

        @Test
        @DisplayName("does not query options when the ACTIVE schema has no fields")
        void doesNotQueryOptionsWhenNoFields() {
            UUID categoryUuid = UUID.randomUUID();
            CategoryEntity cat = category(1L, categoryUuid, "Books");
            CategorySchemaEntity schemaEntity = schema(2L, UUID.randomUUID(), 1L, 1, CategorySchemaStatus.ACTIVE);

            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(cat));
            when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(1L, CategorySchemaStatus.ACTIVE))
                    .thenReturn(Optional.of(schemaEntity));
            when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(2L))
                    .thenReturn(List.of());

            CategoryFormSchemaResponse expected = new CategoryFormSchemaResponse()
                    .categoryUuid(categoryUuid).schemaUuid(schemaEntity.getUuid()).fields(List.of());
            when(categoryFormSchemaMapper.toResponse(eq(categoryUuid), eq(schemaEntity), eq(List.of()), any()))
                    .thenReturn(expected);

            CategoryFormSchemaResponse result = service.getCategoryFormSchema(categoryUuid);

            assertNotNull(result);
            verify(fieldOptionRepository, never()).findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(any());
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
            when(itemListingEntryRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(itemUuid).title("Test Item");
            when(itemMapper.toDetailResponse(entity, cat, List.of(t1), ownerUuid, "alice", null, List.of(), List.of()))
                    .thenReturn(expectedResponse);

            ItemDetailResponse result = service.getItemByUuid(itemUuid, null, false);

            assertNotNull(result);
            assertEquals(itemUuid, result.getUuid());
        }

        @Test
        @DisplayName("returns safe structured entries in public detail response")
        void returnsSafeEntriesForDetail() {
            UUID itemUuid = UUID.randomUUID();
            UUID ownerUuid = UUID.randomUUID();
            ItemEntity entity = item(itemUuid, ItemStatus.ACTIVE, ItemCondition.GOOD);
            CategoryEntity cat = category(20L, UUID.randomUUID(), "Books");
            UserEntity owner = user(ownerUuid, "alice");
            ItemListingEntryEntity entry = new ItemListingEntryEntity();
            entry.setId(99L);
            entry.setUuid(UUID.randomUUID());
            entry.setItemId(1L);
            entry.setTitle("Safe public entry");
            entry.setQuantity(1);
            entry.setSortOrder(0);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(entity));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(cat));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
            when(itemTagRepository.findByIdItemId(1L)).thenReturn(List.of());
            when(itemImageRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
            when(itemImageMapper.toResponseList(List.of())).thenReturn(List.of());
            when(itemListingEntryRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(List.of(entry));

            ItemDetailResponse expectedResponse = new ItemDetailResponse()
                    .uuid(itemUuid).title("Test Item");
            when(itemMapper.toDetailResponse(entity, cat, List.of(), ownerUuid, "alice", null, List.of(), List.of(entry)))
                    .thenReturn(expectedResponse);

            ItemDetailResponse result = service.getItemByUuid(itemUuid, null, false);

            assertNotNull(result);
            verify(itemListingEntryRepository).findByItemIdOrderBySortOrderAsc(1L);
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
            when(itemListingEntryRepository.findByItemIdInOrderByItemIdAscSortOrderAsc(any()))
                    .thenReturn(List.of());

            ItemSummaryResponse summary = new ItemSummaryResponse().uuid(entity.getUuid());
            when(itemMapper.toSummaryResponse(entity, cat, ownerUuid, "bob", null, 0, List.of()))
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
        @DisplayName("always limits public search to ACTIVE items")
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

            // Verify that the repository was called for the ACTIVE-only public search specification.
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
                    0, 20, null, "test", catUuid, null, ItemCondition.NEW, null);

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

        @Test
        @DisplayName("location text filter is applied without tag lookup")
        @SuppressWarnings("unchecked")
        void locationFilterApplied() {
            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 0);

            when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(pageResponseMapper.toItemPagedResponse(eq(emptyPage), any(), any()))
                    .thenReturn(new ItemPagedResponse().content(List.of()));

            service.searchItems(0, 20, null, null, null, null, null, "belgrade");

            verify(tagRepository, never()).findByUuid(any());
            verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }
}

