package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.AdminListingSummaryResponse;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.api.model.ListingModerationActionResponse;
import com.barterplatform.api.model.OwnerListingModerationSummary;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.moderation.ListingModerationActionEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminListingQueryServiceImpl implements AdminListingQueryService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "status", "archivedAt", "removedAt");

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ItemTagRepository itemTagRepository;
    private final ItemImageRepository itemImageRepository;
    private final ListingModerationActionRepository listingModerationActionRepository;
    private final UserRepository userRepository;
    private final ItemImageMapper itemImageMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public AdminListingQueryServiceImpl(
            ItemRepository itemRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            ItemTagRepository itemTagRepository,
            ItemImageRepository itemImageRepository,
            ListingModerationActionRepository listingModerationActionRepository,
            UserRepository userRepository,
            ItemImageMapper itemImageMapper,
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.itemTagRepository = itemTagRepository;
        this.itemImageRepository = itemImageRepository;
        this.listingModerationActionRepository = listingModerationActionRepository;
        this.userRepository = userRepository;
        this.itemImageMapper = itemImageMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public AdminListingPagedResponse listListings(
            Integer page,
            Integer size,
            String sort,
            String q,
            String ownerQuery,
            UUID categoryUuid,
            ItemStatus status) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        List<Long> filteredOwnerIds = resolveOwnerIds(ownerQuery);
        if (ownerQuery != null && !ownerQuery.isBlank() && filteredOwnerIds.isEmpty()) {
            Page<ItemEntity> emptyPage = new PageImpl<>(List.of(), pageRequest.pageable(), 0);
            return pageResponseMapper.toAdminListingPagedResponse(emptyPage, List.of(), pageRequest.sort());
        }

        Specification<ItemEntity> spec = buildSpecification(q, filteredOwnerIds, categoryUuid, status);
        Page<ItemEntity> itemPage = itemRepository.findAll(spec, pageRequest.pageable());
        List<AdminListingSummaryResponse> content = mapSummaries(itemPage.getContent());
        return pageResponseMapper.toAdminListingPagedResponse(itemPage, content, pageRequest.sort());
    }

    @Override
    public AdminListingDetailResponse getListing(UUID listingUuid) {
        ItemEntity item = findListing(listingUuid);
        CategoryEntity category = loadCategory(item);
        UserEntity owner = loadOwner(item);
        List<TagEntity> tags = loadTagsForItem(item.getId());
        List<ItemImageEntity> imageEntities = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());
        List<ItemImageResponse> images = itemImageMapper.toResponseList(imageEntities);
        return toDetailResponse(item, category, owner, tags, imageEntities, images, latestModerationSummary(item.getId()));
    }

    @Override
    public List<ListingModerationActionResponse> listModerationActions(UUID listingUuid) {
        ItemEntity item = findListing(listingUuid);
        Map<Long, UserEntity> usersById = userRepository.findAllById(
                        listingModerationActionRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).stream()
                                .map(ListingModerationActionEntity::getPerformedByUserId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return listingModerationActionRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).stream()
                .map(action -> toActionResponse(action, item.getUuid(), usersById.get(action.getPerformedByUserId())))
                .toList();
    }

    private Specification<ItemEntity> buildSpecification(
            String q,
            List<Long> ownerIds,
            UUID categoryUuid,
            ItemStatus status) {
        List<Specification<ItemEntity>> specs = new ArrayList<>();
        specs.add(AdminListingSpecifications.deletedAtIsNull());

        if (q != null && !q.isBlank()) {
            specs.add(AdminListingSpecifications.titleContainsIgnoreCase(q.trim()));
        }

        if (ownerIds != null && !ownerIds.isEmpty()) {
            specs.add(AdminListingSpecifications.ownerIdIn(ownerIds));
        }

        if (categoryUuid != null) {
            CategoryEntity category = categoryRepository.findByUuid(categoryUuid)
                    .orElseThrow(() -> notFound("Category with uuid '%s' was not found.", categoryUuid));
            specs.add(AdminListingSpecifications.categoryIdEquals(category.getId()));
        }

        if (status != null) {
            specs.add(AdminListingSpecifications.statusEquals(status));
        }

        return Specification.allOf(specs);
    }

    private List<Long> resolveOwnerIds(String ownerQuery) {
        if (ownerQuery == null || ownerQuery.isBlank()) {
            return null;
        }

        String normalized = ownerQuery.trim().toLowerCase(Locale.ROOT);
        return userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(normalized);
    }

    private List<AdminListingSummaryResponse> mapSummaries(List<ItemEntity> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(items.stream()
                        .map(ItemEntity::getCategoryId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        Map<Long, UserEntity> ownersById = userRepository.findAllById(items.stream()
                        .map(ItemEntity::getOwnerId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        Map<Long, String> primaryImageUrlByItemId = new HashMap<>();
        for (Long itemId : items.stream().map(ItemEntity::getId).collect(Collectors.toSet())) {
            itemImageRepository.findFirstByItemIdAndPrimaryTrue(itemId)
                    .ifPresent(image -> primaryImageUrlByItemId.put(itemId, itemImageMapper.toResponse(image).getUrl()));
        }

        return items.stream()
                .map(item -> toSummaryResponse(
                        item,
                        categoriesById.get(item.getCategoryId()),
                        ownersById.get(item.getOwnerId()),
                        primaryImageUrlByItemId.get(item.getId())))
                .toList();
    }

    private AdminListingSummaryResponse toSummaryResponse(
            ItemEntity item,
            CategoryEntity category,
            UserEntity owner,
            String primaryImageUrl) {
        return new AdminListingSummaryResponse()
                .uuid(item.getUuid())
                .title(item.getTitle())
                .status(com.barterplatform.api.model.ItemStatus.valueOf(item.getStatus().name()))
                .condition(com.barterplatform.api.model.ItemCondition.valueOf(item.getCondition().name()))
                .categoryUuid(category.getUuid())
                .categoryName(category.getName())
                .ownerUuid(owner.getUuid())
                .ownerUsername(owner.getUsername())
                .primaryImageUrl(primaryImageUrl)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .archivedAt(item.getArchivedAt())
                .removedAt(item.getRemovedAt());
    }

    private AdminListingDetailResponse toDetailResponse(
            ItemEntity item,
            CategoryEntity category,
            UserEntity owner,
            List<TagEntity> tags,
            List<ItemImageEntity> imageEntities,
            List<ItemImageResponse> images,
            OwnerListingModerationSummary moderationSummary) {
        CategoryResponse categoryResponse = new CategoryResponse()
                .uuid(category.getUuid())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder());

        List<TagResponse> tagResponses = tags.stream()
                .map(tag -> new TagResponse().uuid(tag.getUuid()).name(tag.getName()).slug(tag.getSlug()))
                .toList();

        String primaryImageUrl = imageEntities.stream()
                .filter(ItemImageEntity::isPrimary)
                .findFirst()
                .map(image -> itemImageMapper.toResponse(image).getUrl())
                .orElse(null);

        return new AdminListingDetailResponse()
                .uuid(item.getUuid())
                .title(item.getTitle())
                .description(item.getDescription())
                .status(com.barterplatform.api.model.ItemStatus.valueOf(item.getStatus().name()))
                .condition(com.barterplatform.api.model.ItemCondition.valueOf(item.getCondition().name()))
                .category(categoryResponse)
                .tags(tagResponses)
                .ownerUuid(owner.getUuid())
                .ownerUsername(owner.getUsername())
                .primaryImageUrl(primaryImageUrl)
                .images(images)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .archivedAt(item.getArchivedAt())
                .removedAt(item.getRemovedAt())
                .moderationSummary(moderationSummary);
    }

    private ListingModerationActionResponse toActionResponse(
            ListingModerationActionEntity action,
            UUID listingUuid,
            UserEntity actor) {
        return new ListingModerationActionResponse()
                .uuid(action.getUuid())
                .listingUuid(listingUuid)
                .actionType(com.barterplatform.api.model.ListingModerationActionType.valueOf(action.getActionType().name()))
                .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.valueOf(action.getReasonCode().name()))
                .sourceType(com.barterplatform.api.model.ListingModerationSourceType.valueOf(action.getSourceType().name()))
                .performedByUserUuid(actor != null ? actor.getUuid() : null)
                .performedByUsername(actor != null ? actor.getUsername() : null)
                .userMessage(action.getUserMessage())
                .internalNote(action.getInternalNote())
                .createdAt(action.getCreatedAt());
    }

    private OwnerListingModerationSummary latestModerationSummary(Long itemId) {
        return listingModerationActionRepository.findFirstByItemIdOrderByCreatedAtDesc(itemId)
                .map(action -> new OwnerListingModerationSummary()
                        .actionType(com.barterplatform.api.model.ListingModerationActionType.valueOf(action.getActionType().name()))
                        .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.valueOf(action.getReasonCode().name()))
                        .sourceType(com.barterplatform.api.model.ListingModerationSourceType.valueOf(action.getSourceType().name()))
                        .actionAt(action.getCreatedAt())
                        .userMessage(action.getUserMessage()))
                .orElse(null);
    }

    private ItemEntity findListing(UUID listingUuid) {
        ItemEntity item = itemRepository.findByUuid(listingUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", listingUuid));
        if (item.getDeletedAt() != null) {
            throw notFound("Item with uuid '%s' was not found.", listingUuid);
        }
        return item;
    }

    private CategoryEntity loadCategory(ItemEntity item) {
        return categoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> notFound("Category for item '%s' was not found.", item.getUuid()));
    }

    private UserEntity loadOwner(ItemEntity item) {
        return userRepository.findById(item.getOwnerId())
                .orElseThrow(() -> notFound("Owner for item '%s' was not found.", item.getUuid()));
    }

    private List<TagEntity> loadTagsForItem(Long itemId) {
        List<ItemTagEntity> itemTags = itemTagRepository.findByIdItemId(itemId);
        if (itemTags.isEmpty()) {
            return List.of();
        }
        return tagRepository.findAllById(itemTags.stream().map(it -> it.getId().getTagId()).toList());
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, messageTemplate.formatted(args));
    }
}

