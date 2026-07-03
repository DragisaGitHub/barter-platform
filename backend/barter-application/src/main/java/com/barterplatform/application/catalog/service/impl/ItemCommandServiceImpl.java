package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemListingEntryRequest;
import com.barterplatform.api.model.ListingTemplateMetadata;
import com.barterplatform.api.model.UpdateItemRequest;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.service.ItemCommandService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.ItemTagId;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.enums.ListingMode;
import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemListingEntryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItemCommandServiceImpl implements ItemCommandService {

    private static final int MAX_LISTING_ENTRIES = 20;

    private final ListingTemplateMetadataSupport listingTemplateMetadataSupport = new ListingTemplateMetadataSupport();

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ItemTagRepository itemTagRepository;
    private final ItemListingEntryRepository itemListingEntryRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;
    private final ItemFieldValueSupport itemFieldValueSupport;

    public ItemCommandServiceImpl(ItemRepository itemRepository,
                                  CategoryRepository categoryRepository,
                                  TagRepository tagRepository,
                                  ItemTagRepository itemTagRepository,
                                   ItemListingEntryRepository itemListingEntryRepository,
                                  UserRepository userRepository,
                                  ItemMapper itemMapper,
                                  ItemFieldValueSupport itemFieldValueSupport) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.itemTagRepository = itemTagRepository;
        this.itemListingEntryRepository = itemListingEntryRepository;
        this.userRepository = userRepository;
        this.itemMapper = itemMapper;
        this.itemFieldValueSupport = itemFieldValueSupport;
    }

    // ── Create ───────────────────────────────────────────────────

    @Override
    public ItemDetailResponse createItem(UUID ownerUuid, CreateItemRequest request) {
        UserEntity owner = resolveUser(ownerUuid);
        CategoryEntity category = resolveCategoryByUuid(request.getCategoryUuid());

        ItemEntity item = new ItemEntity();
        item.setOwnerId(owner.getId());
        item.setCategoryId(category.getId());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setExchangeLocation(normalizeOptionalText(request.getExchangeLocation()));
        item.setExchangeCity(normalizeOptionalText(request.getExchangeCity()));
        item.setExchangeArea(normalizeOptionalText(request.getExchangeArea()));
        item.setCondition(mapConditionToDomain(request.getCondition()));
        ListingMode listingMode = mapListingModeToDomain(request.getListingMode());
        item.setListingMode(listingMode);
        ListingTemplateType listingTemplateType = listingTemplateMetadataSupport.resolveTemplateType(
                request.getListingTemplateType(), listingMode, null);
        item.setListingTemplateType(listingTemplateType);
        item.setTemplateMetadataJson(listingTemplateMetadataSupport.validateAndSerializeMetadata(
                request.getTemplateMetadata(), listingTemplateType, listingMode));
        List<ItemListingEntryEntity> entries = validateAndBuildEntries(request.getEntries(), listingMode);

        // Default to DRAFT unless an explicit status is provided
        if (request.getStatus() != null) {
            item.setStatus(mapStatusToDomain(request.getStatus()));
        } else {
            item.setStatus(ItemStatus.DRAFT);
        }

        ItemEntity saved = itemRepository.save(item);

        // Save tags
        List<TagEntity> tags = resolveAndSaveTags(saved.getId(), request.getTagUuids());
        List<ItemListingEntryEntity> savedEntries = saveEntries(saved.getId(), entries);
        List<com.barterplatform.api.model.SchemaFieldValueResponse> schemaFieldValues =
                itemFieldValueSupport.replaceValues(saved.getId(), category.getId(), request.getSchemaFieldValues());

        ItemDetailResponse response = itemMapper.toDetailResponse(saved, category, tags, owner.getUuid(), owner.getUsername(),
                null, java.util.List.of(), savedEntries);
        enrichTemplateFields(response, saved);
        response.setSchemaFieldValues(schemaFieldValues);
        return response;
    }

    // ── Update ───────────────────────────────────────────────────

    @Override
    public ItemDetailResponse updateItem(UUID ownerUuid, UUID itemUuid, UpdateItemRequest request) {
        UserEntity owner = resolveUser(ownerUuid);
        ItemEntity item = resolveItem(itemUuid);
        enforceOwnership(item, owner);

        // Apply partial updates
        if (request.getTitle() != null) {
            item.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getExchangeLocation() != null) {
            item.setExchangeLocation(normalizeOptionalText(request.getExchangeLocation()));
        }
        if (request.getExchangeCity() != null) {
            item.setExchangeCity(normalizeOptionalText(request.getExchangeCity()));
        }
        if (request.getExchangeArea() != null) {
            item.setExchangeArea(normalizeOptionalText(request.getExchangeArea()));
        }
        if (request.getCondition() != null) {
            item.setCondition(mapConditionToDomain(request.getCondition()));
        }
        ListingMode requestedListingMode = request.getListingMode() != null
                ? mapListingModeToDomain(request.getListingMode())
                : defaultListingMode(item.getListingMode());
        List<ItemListingEntryEntity> replacementEntries = null;
        if (request.getEntries() != null) {
            replacementEntries = validateAndBuildEntries(request.getEntries(), requestedListingMode);
        } else {
            validateExistingEntriesForMode(item.getId(), requestedListingMode);
        }
        ListingTemplateType requestedTemplateType = listingTemplateMetadataSupport.resolveTemplateType(
                request.getListingTemplateType(), requestedListingMode, item.getListingTemplateType());
        item.setListingMode(requestedListingMode);
        item.setListingTemplateType(requestedTemplateType);
        if (request.getTemplateMetadata() != null || request.getListingTemplateType() != null) {
            item.setTemplateMetadataJson(listingTemplateMetadataSupport.validateAndSerializeMetadata(
                    request.getTemplateMetadata(), requestedTemplateType, requestedListingMode));
        } else if (item.getTemplateMetadataJson() != null) {
            item.setTemplateMetadataJson(listingTemplateMetadataSupport.validateAndSerializeMetadata(
                    listingTemplateMetadataSupport.deserialize(item.getTemplateMetadataJson()),
                    requestedTemplateType,
                    requestedListingMode));
        }
        if (request.getStatus() != null) {
            validateStatusTransition(item.getStatus(), mapStatusToDomain(request.getStatus()));
            item.setStatus(mapStatusToDomain(request.getStatus()));
        }

        // Update category if provided
        CategoryEntity category;
        if (request.getCategoryUuid() != null) {
            category = resolveCategoryByUuid(request.getCategoryUuid());
            item.setCategoryId(category.getId());
        } else {
            category = categoryRepository.findById(item.getCategoryId())
                    .orElseThrow(() -> notFound("Category for item '%s' was not found.", itemUuid));
        }

        ItemEntity saved = itemRepository.save(item);

        // Replace tags if provided
        List<TagEntity> tags;
        if (request.getTagUuids() != null) {
            itemTagRepository.deleteByIdItemId(saved.getId());
            tags = resolveAndSaveTags(saved.getId(), request.getTagUuids());
        } else {
            tags = loadTagsForItem(saved.getId());
        }

        List<ItemListingEntryEntity> entries;
        if (replacementEntries != null || requestedListingMode == ListingMode.SINGLE) {
            itemListingEntryRepository.deleteByItemId(saved.getId());
            entries = replacementEntries == null ? List.of() : saveEntries(saved.getId(), replacementEntries);
        } else {
            entries = itemListingEntryRepository.findByItemIdOrderBySortOrderAsc(saved.getId());
        }

        List<com.barterplatform.api.model.SchemaFieldValueResponse> schemaFieldValues;
        if (request.getSchemaFieldValues() != null) {
            schemaFieldValues = itemFieldValueSupport.replaceValues(
                    saved.getId(), category.getId(), request.getSchemaFieldValues());
        } else {
            schemaFieldValues = itemFieldValueSupport.loadResponses(saved.getId());
        }

        ItemDetailResponse response = itemMapper.toDetailResponse(saved, category, tags, owner.getUuid(), owner.getUsername(),
                null, java.util.List.of(), entries);
        enrichTemplateFields(response, saved);
        response.setSchemaFieldValues(schemaFieldValues);
        return response;
    }

    // ── Archive ──────────────────────────────────────────────────

    @Override
    public ItemDetailResponse archiveItem(UUID ownerUuid, UUID itemUuid, ArchiveItemRequest request) {
        UserEntity owner = resolveUser(ownerUuid);
        ItemEntity item = resolveItem(itemUuid);
        enforceOwnership(item, owner);

        item.setStatus(ItemStatus.ARCHIVED);
        item.setArchivedAt(OffsetDateTime.now());

        ItemEntity saved = itemRepository.save(item);

        CategoryEntity category = categoryRepository.findById(saved.getCategoryId())
                .orElseThrow(() -> notFound("Category for item '%s' was not found.", itemUuid));
        List<TagEntity> tags = loadTagsForItem(saved.getId());
        List<ItemListingEntryEntity> entries = itemListingEntryRepository.findByItemIdOrderBySortOrderAsc(saved.getId());

        ItemDetailResponse response = itemMapper.toDetailResponse(saved, category, tags, owner.getUuid(), owner.getUsername(),
                null, java.util.List.of(), entries);
        enrichTemplateFields(response, saved);
        response.setSchemaFieldValues(itemFieldValueSupport.loadResponses(saved.getId()));
        return response;
    }

    // ── Private helpers ──────────────────────────────────────────

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private CategoryEntity resolveCategoryByUuid(UUID categoryUuid) {
        return categoryRepository.findByUuid(categoryUuid)
                .orElseThrow(() -> notFound("Category with uuid '%s' was not found.", categoryUuid));
    }

    private ItemEntity resolveItem(UUID itemUuid) {
        ItemEntity item = itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));

        if (item.getDeletedAt() != null || item.getStatus() == ItemStatus.REMOVED) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }
        return item;
    }

    private void enforceOwnership(ItemEntity item, UserEntity owner) {
        if (!item.getOwnerId().equals(owner.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "You are not the owner of this item.");
        }
    }

    private void validateStatusTransition(ItemStatus current, ItemStatus requested) {
        // Owner can only transition between DRAFT and ACTIVE
        boolean allowed = (current == ItemStatus.DRAFT && requested == ItemStatus.ACTIVE)
                || (current == ItemStatus.ACTIVE && requested == ItemStatus.DRAFT);

        if (!allowed && current != requested) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Status transition from %s to %s is not allowed.".formatted(current, requested));
        }
    }

    private List<TagEntity> resolveAndSaveTags(Long itemId, List<UUID> tagUuids) {
        if (tagUuids == null || tagUuids.isEmpty()) {
            return List.of();
        }

        List<TagEntity> tags = tagUuids.stream()
                .map(uuid -> tagRepository.findByUuid(uuid)
                        .orElseThrow(() -> notFound("Tag with uuid '%s' was not found.", uuid)))
                .toList();

        OffsetDateTime now = OffsetDateTime.now();
        for (TagEntity tag : tags) {
            ItemTagId id = new ItemTagId();
            id.setItemId(itemId);
            id.setTagId(tag.getId());

            ItemTagEntity itemTag = new ItemTagEntity();
            itemTag.setId(id);
            itemTag.setAssignedAt(now);
            itemTagRepository.save(itemTag);
        }

        return tags;
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

    private ItemStatus mapStatusToDomain(com.barterplatform.api.model.ItemStatus apiStatus) {
        return ItemStatus.valueOf(apiStatus.name());
    }

    private ItemCondition mapConditionToDomain(com.barterplatform.api.model.ItemCondition apiCondition) {
        return ItemCondition.valueOf(apiCondition.name());
    }

    private ListingMode mapListingModeToDomain(com.barterplatform.api.model.ListingMode apiListingMode) {
        return apiListingMode == null ? ListingMode.SINGLE : ListingMode.valueOf(apiListingMode.name());
    }

    private ListingMode defaultListingMode(ListingMode listingMode) {
        return listingMode == null ? ListingMode.SINGLE : listingMode;
    }

    private void enrichTemplateFields(ItemDetailResponse response, ItemEntity item) {
        response.setListingTemplateType(listingTemplateMetadataSupport.toApiType(
                item.getListingTemplateType(),
                defaultListingMode(item.getListingMode())));
        ListingTemplateMetadata metadata = listingTemplateMetadataSupport.deserialize(item.getTemplateMetadataJson());
        response.setTemplateMetadata(metadata);
    }

    private List<ItemListingEntryEntity> validateAndBuildEntries(
            List<ItemListingEntryRequest> requestedEntries, ListingMode listingMode) {
        List<ItemListingEntryRequest> entries = requestedEntries == null ? List.of() : requestedEntries;
        if (entries.size() > MAX_LISTING_ENTRIES) {
            throw badRequest("A listing can contain at most %d entries.".formatted(MAX_LISTING_ENTRIES));
        }
        if (listingMode == ListingMode.SINGLE && !entries.isEmpty()) {
            throw badRequest("SINGLE listings cannot include structured entries.");
        }
        if ((listingMode == ListingMode.BUNDLE || listingMode == ListingMode.PICK_ANY) && entries.isEmpty()) {
            throw badRequest("BUNDLE and PICK_ANY listings require at least one entry.");
        }

        List<ItemListingEntryEntity> normalized = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ItemListingEntryRequest entryRequest = entries.get(i);
            String title = normalizeOptionalText(entryRequest.getTitle());
            if (title == null) {
                throw badRequest("Entry title is required.");
            }
            Integer quantity = entryRequest.getQuantity();
            if (quantity != null && quantity < 1) {
                throw badRequest("Entry quantity must be at least 1 when provided.");
            }

            ItemListingEntryEntity entry = new ItemListingEntryEntity();
            entry.setTitle(title);
            entry.setDescription(normalizeOptionalText(entryRequest.getDescription()));
            entry.setQuantity(quantity);
            entry.setSortOrder(i);
            normalized.add(entry);
        }
        return normalized;
    }

    private void validateExistingEntriesForMode(Long itemId, ListingMode listingMode) {
        if (listingMode == ListingMode.BUNDLE || listingMode == ListingMode.PICK_ANY) {
            long existingEntryCount = itemListingEntryRepository.countByItemId(itemId);
            if (existingEntryCount == 0) {
                throw badRequest("BUNDLE and PICK_ANY listings require at least one entry.");
            }
            if (existingEntryCount > MAX_LISTING_ENTRIES) {
                throw badRequest("A listing can contain at most %d entries.".formatted(MAX_LISTING_ENTRIES));
            }
        }
    }

    private List<ItemListingEntryEntity> saveEntries(Long itemId, List<ItemListingEntryEntity> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .peek(entry -> entry.setItemId(itemId))
                .map(itemListingEntryRepository::save)
                .toList();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                message);
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

