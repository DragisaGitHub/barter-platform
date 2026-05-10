package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.mapper.CategoryMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.mapper.TagMapper;
import com.barterplatform.application.catalog.service.CatalogQueryService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogQueryServiceImpl implements CatalogQueryService {

    private static final String DEFAULT_ITEM_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_ITEM_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "status");

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ItemMapper itemMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public CatalogQueryServiceImpl(CategoryRepository categoryRepository,
                                   TagRepository tagRepository,
                                   ItemRepository itemRepository,
                                   ItemTagRepository itemTagRepository,
                                   UserRepository userRepository,
                                   CategoryMapper categoryMapper,
                                   TagMapper tagMapper,
                                   ItemMapper itemMapper,
                                   PageRequestFactory pageRequestFactory,
                                   PageResponseMapper pageResponseMapper) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
        this.userRepository = userRepository;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.itemMapper = itemMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    // ── Categories & Tags ────────────────────────────────────────

    @Override
    public List<CategoryResponse> listCategories() {
        return categoryMapper.toResponseList(
                categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc());
    }

    @Override
    public List<TagResponse> listTags() {
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

        Specification<ItemEntity> spec = buildSearchSpecification(q, categoryUuid, status, condition);

        // TODO: tagUuids filtering deferred to a future version — requires Specification
        //       subquery joining item_tags. Parameter is accepted but currently ignored.

        Page<ItemEntity> itemPage = itemRepository.findAll(spec, pageRequest.pageable());

        List<ItemSummaryResponse> content = mapItemSummaries(itemPage.getContent());

        return pageResponseMapper.toItemPagedResponse(itemPage, content, pageRequest.sort());
    }

    // ── Item detail ──────────────────────────────────────────────

    @Override
    public ItemDetailResponse getItemByUuid(UUID itemUuid) {
        ItemEntity item = itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));

        if (item.getDeletedAt() != null || item.getStatus() == ItemStatus.REMOVED) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }

        CategoryEntity category = categoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> notFound("Category for item '%s' was not found.", itemUuid));

        UserEntity owner = userRepository.findById(item.getOwnerId())
                .orElseThrow(() -> notFound("Owner for item '%s' was not found.", itemUuid));

        List<TagEntity> tags = loadTagsForItem(item.getId());

        return itemMapper.toDetailResponse(item, category, tags, owner.getUuid(), owner.getUsername());
    }

    // ── My items ─────────────────────────────────────────────────

    @Override
    public ItemPagedResponse listMyItems(UUID ownerUuid, Integer page, Integer size, String sort) {
        UserEntity owner = userRepository.findByUuid(ownerUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", ownerUuid));

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_ITEM_SORT_FIELD, ALLOWED_ITEM_SORT_FIELDS);

        Page<ItemEntity> itemPage = itemRepository.findByOwnerIdAndStatusNotAndDeletedAtIsNull(
                owner.getId(), ItemStatus.REMOVED, pageRequest.pageable());

        List<ItemSummaryResponse> content = mapItemSummaries(itemPage.getContent());

        return pageResponseMapper.toItemPagedResponse(itemPage, content, pageRequest.sort());
    }

    // ── Private helpers ──────────────────────────────────────────

    private Specification<ItemEntity> buildSearchSpecification(String q, UUID categoryUuid,
                                                               ItemStatus status, ItemCondition condition) {
        List<Specification<ItemEntity>> specs = new ArrayList<>();

        // Always exclude soft-deleted items
        specs.add(ItemSpecifications.deletedAtIsNull());

        // Always exclude REMOVED from public search
        specs.add(ItemSpecifications.statusNotEqual(ItemStatus.REMOVED));

        // Default to ACTIVE when no explicit status filter
        specs.add(ItemSpecifications.statusEquals(Objects.requireNonNullElse(status, ItemStatus.ACTIVE)));

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

        return Specification.allOf(specs);
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

        return items.stream().map(item -> {
            CategoryEntity category = categoriesById.get(item.getCategoryId());
            UserEntity owner = ownersById.get(item.getOwnerId());
            return itemMapper.toSummaryResponse(
                    item,
                    category,
                    owner != null ? owner.getUuid() : null,
                    owner != null ? owner.getUsername() : null);
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

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

