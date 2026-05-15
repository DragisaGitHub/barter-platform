package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.OwnerListingModerationSummary;
import com.barterplatform.api.model.PopularCategoryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.mapper.CategoryMapper;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.mapper.TagMapper;
import com.barterplatform.application.catalog.service.CatalogQueryService;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.moderation.ListingModerationActionEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.catalog.repository.PopularCategoryProjection;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogQueryServiceImpl implements CatalogQueryService {

    private static final String DEFAULT_ITEM_SORT_FIELD = "createdAt";
    private static final int DEFAULT_POPULAR_CATEGORY_LIMIT = 6;
    private static final int MIN_POPULAR_CATEGORY_LIMIT = 1;
    private static final int MAX_POPULAR_CATEGORY_LIMIT = 20;
    private static final Set<String> ALLOWED_ITEM_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "status");

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;
    private final UserRepository userRepository;
    private final ItemImageRepository itemImageRepository;
    private final ListingModerationActionRepository listingModerationActionRepository;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public CatalogQueryServiceImpl(CategoryRepository categoryRepository,
                                   TagRepository tagRepository,
                                   ItemRepository itemRepository,
                                   ItemTagRepository itemTagRepository,
                                   UserRepository userRepository,
                                   ItemImageRepository itemImageRepository,
                                   ListingModerationActionRepository listingModerationActionRepository,
                                   CategoryMapper categoryMapper,
                                   TagMapper tagMapper,
                                   ItemMapper itemMapper,
                                   ItemImageMapper itemImageMapper,
                                   PageRequestFactory pageRequestFactory,
                                   PageResponseMapper pageResponseMapper) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
        this.userRepository = userRepository;
        this.itemImageRepository = itemImageRepository;
        this.listingModerationActionRepository = listingModerationActionRepository;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    // ── Categories & Tags ────────────────────────────────────────

    @Override
    public List<com.barterplatform.api.model.CategoryResponse> listCategories() {
        return categoryMapper.toResponseList(
                categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc());
    }

    @Override
    public List<PopularCategoryResponse> listPopularCategories(Integer limit) {
        int resolvedLimit = resolvePopularCategoryLimit(limit);
        return categoryRepository.findPopularCategories(ItemStatus.ACTIVE, PageRequest.of(0, resolvedLimit)).stream()
                .map(this::mapPopularCategoryResponse)
                .toList();
    }

    @Override
    public List<com.barterplatform.api.model.TagResponse> listTags() {
        return tagMapper.toResponseList(
                tagRepository.findAllByDeletedAtIsNullOrderByNameAsc());
    }

    // ── Public item search ───────────────────────────────────────

    @Override
    public ItemPagedResponse searchItems(Integer page, Integer size, String sort,
                                         String q, UUID categoryUuid, List<UUID> tagUuids,
                                         ItemStatus status, ItemCondition condition) {

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_ITEM_SORT_FIELD, ALLOWED_ITEM_SORT_FIELDS);

        Specification<ItemEntity> spec = buildSearchSpecification(q, categoryUuid, tagUuids, condition);

        Page<ItemEntity> itemPage = itemRepository.findAll(spec, pageRequest.pageable());

        List<ItemSummaryResponse> content = mapItemSummaries(itemPage.getContent());

        return pageResponseMapper.toItemPagedResponse(itemPage, content, pageRequest.sort());
    }

    // ── Item detail ──────────────────────────────────────────────

    @Override
    public ItemDetailResponse getItemByUuid(UUID itemUuid, UUID requesterUuid, boolean isAdmin) {
        ItemEntity item = itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));

        if (item.getDeletedAt() != null) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }

        CategoryEntity category = categoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> notFound("Category for item '%s' was not found.", itemUuid));

        UserEntity owner = userRepository.findById(item.getOwnerId())
                .orElseThrow(() -> notFound("Owner for item '%s' was not found.", itemUuid));

        boolean ownerAccess = requesterUuid != null && owner.getUuid().equals(requesterUuid);
        boolean elevatedAccess = isAdmin || ownerAccess;
        if (!elevatedAccess && item.getStatus() != ItemStatus.ACTIVE) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }

        List<TagEntity> tags = loadTagsForItem(item.getId());

        List<ItemImageEntity> imageEntities = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());
        List<ItemImageResponse> images = itemImageMapper.toResponseList(imageEntities);
        String primaryImageUrl = imageEntities.stream()
                .filter(ItemImageEntity::isPrimary)
                .findFirst()
                .map(img -> itemImageMapper.toResponse(img).getUrl())
                .orElse(null);

        ItemDetailResponse response = itemMapper.toDetailResponse(
                item, category, tags, owner.getUuid(), owner.getUsername(), primaryImageUrl, images);
        if (elevatedAccess) {
            response.setModerationSummary(loadModerationSummary(item.getId()));
        }
        return response;
    }

    // ── My items ─────────────────────────────────────────────────

    @Override
    public ItemPagedResponse listMyItems(UUID ownerUuid, Integer page, Integer size, String sort,
                                         ItemStatus status) {
        UserEntity owner = userRepository.findByUuid(ownerUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", ownerUuid));

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_ITEM_SORT_FIELD, ALLOWED_ITEM_SORT_FIELDS);

        Page<ItemEntity> itemPage;
        if (status != null) {
            itemPage = itemRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(
                    owner.getId(), status, pageRequest.pageable());
        } else {
            itemPage = itemRepository.findByOwnerIdAndDeletedAtIsNull(owner.getId(), pageRequest.pageable());
        }

        List<ItemSummaryResponse> content = mapItemSummaries(itemPage.getContent());

        return pageResponseMapper.toItemPagedResponse(itemPage, content, pageRequest.sort());
    }

    // ── Private helpers ──────────────────────────────────────────

    private Specification<ItemEntity> buildSearchSpecification(String q, UUID categoryUuid,
                                                               List<UUID> tagUuids,
                                                               ItemCondition condition) {
        List<Specification<ItemEntity>> specs = new ArrayList<>();

        // Always exclude soft-deleted items
        specs.add(ItemSpecifications.deletedAtIsNull());

        // Public marketplace visibility is ACTIVE only.
        specs.add(ItemSpecifications.statusEquals(ItemStatus.ACTIVE));

        if (condition != null) {
            specs.add(ItemSpecifications.conditionEquals(condition));
        }

        if (categoryUuid != null) {
            CategoryEntity category = categoryRepository.findByUuid(categoryUuid)
                    .orElseThrow(() -> notFound("Category with uuid '%s' was not found.", categoryUuid));
            specs.add(ItemSpecifications.categoryIdEquals(category.getId()));
        }

        if (q != null && !q.isBlank()) {
            specs.add(ItemSpecifications.titleContainsIgnoreCase(q.trim()));
        }

        if (tagUuids != null && !tagUuids.isEmpty()) {
            List<Long> tagIds = tagUuids.stream()
                    .map(uuid -> tagRepository.findByUuid(uuid).map(TagEntity::getId).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            if (!tagIds.isEmpty()) {
                specs.add(ItemSpecifications.hasAnyTagId(tagIds));
            }
        }

        return Specification.allOf(specs);
    }

    private int resolvePopularCategoryLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_POPULAR_CATEGORY_LIMIT;
        }

        if (limit < MIN_POPULAR_CATEGORY_LIMIT || limit > MAX_POPULAR_CATEGORY_LIMIT) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Popular category limit must be between %d and %d."
                            .formatted(MIN_POPULAR_CATEGORY_LIMIT, MAX_POPULAR_CATEGORY_LIMIT));
        }

        return limit;
    }

    private PopularCategoryResponse mapPopularCategoryResponse(PopularCategoryProjection projection) {
        return new PopularCategoryResponse()
                .uuid(projection.getUuid())
                .name(projection.getName())
                .slug(projection.getSlug())
                .description(projection.getDescription())
                .sortOrder(projection.getSortOrder())
                .activeItemCount(projection.getActiveItemCount());
    }

    private List<ItemSummaryResponse> mapItemSummaries(List<ItemEntity> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        // Batch-load categories
        Set<Long> categoryIds = items.stream()
                .map(ItemEntity::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        // Batch-load owners
        Set<Long> ownerIds = items.stream()
                .map(ItemEntity::getOwnerId)
                .collect(Collectors.toSet());
        Map<Long, UserEntity> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        // Batch-load primary images
        Set<Long> itemIds = items.stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toSet());
        Map<Long, String> primaryImageUrlByItemId = new HashMap<>();
        for (Long itemId : itemIds) {
            itemImageRepository.findFirstByItemIdAndPrimaryTrue(itemId)
                    .ifPresent(img -> primaryImageUrlByItemId.put(itemId,
                            itemImageMapper.toResponse(img).getUrl()));
        }

        return items.stream().map(item -> {
            CategoryEntity category = categoriesById.get(item.getCategoryId());
            UserEntity owner = ownersById.get(item.getOwnerId());
            String primaryImageUrl = primaryImageUrlByItemId.get(item.getId());
            return itemMapper.toSummaryResponse(
                    item,
                    category,
                    owner != null ? owner.getUuid() : null,
                    owner != null ? owner.getUsername() : null,
                    primaryImageUrl);
        }).toList();
    }

    private List<TagEntity> loadTagsForItem(Long itemId) {
        List<ItemTagEntity> itemTags = itemTagRepository.findByIdItemId(itemId);
        if (itemTags.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = itemTags.stream()
                .map(it -> it.getId().getTagId())
                .toList();
        return tagRepository.findAllById(tagIds);
    }

    private OwnerListingModerationSummary loadModerationSummary(Long itemId) {
        return listingModerationActionRepository.findFirstByItemIdOrderByCreatedAtDesc(itemId)
                .map(this::toModerationSummary)
                .orElse(null);
    }

    private OwnerListingModerationSummary toModerationSummary(ListingModerationActionEntity action) {
        return new OwnerListingModerationSummary()
                .actionType(com.barterplatform.api.model.ListingModerationActionType.valueOf(action.getActionType().name()))
                .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.valueOf(action.getReasonCode().name()))
                .sourceType(com.barterplatform.api.model.ListingModerationSourceType.valueOf(action.getSourceType().name()))
                .actionAt(action.getCreatedAt())
                .userMessage(action.getUserMessage());
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

