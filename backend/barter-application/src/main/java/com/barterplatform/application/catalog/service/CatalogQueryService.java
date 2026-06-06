package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.PopularCategoryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import java.util.List;
import java.util.UUID;

public interface CatalogQueryService {

    List<CategoryResponse> listCategories();

    List<PopularCategoryResponse> listPopularCategories(Integer limit);

    List<TagResponse> listTags();

    /**
     * Public item search with optional filters.
     * Public marketplace search always returns ACTIVE items only.
     * Only returns non-deleted items (deletedAt is null, status != REMOVED).
     *
     * @param page         page number (0-based), defaults to 0
     * @param size         page size, defaults to 20, max 100
     * @param sort         sort expression e.g. "createdAt,desc"
     * @param q            free-text search on title (contains, case-insensitive)
     * @param categoryUuid filter by category UUID
     * @param tagUuids     filter by tag UUIDs — items matching at least one tag are returned; ignored when null/empty
     * @param condition    filter by item condition
     * @param location     case-insensitive text filter across approximate exchange city/area/location fields
     */
    ItemPagedResponse searchItems(Integer page, Integer size, String sort,
                                  String q, UUID categoryUuid, List<UUID> tagUuids,
                                  ItemCondition condition,
                                  String location);

    /**
     * Get full item detail by UUID.
     * Public callers can only access ACTIVE items. Owners and administrators can access
     * their own/all non-deleted listings, including removed listings with moderation summary.
     */
    ItemDetailResponse getItemByUuid(UUID itemUuid, UUID requesterUuid, boolean isAdmin);

    /**
     * List items belonging to a specific owner.
     * If status is provided, returns only items with that status.
     * Otherwise, includes DRAFT, ACTIVE, RESERVED, ARCHIVED, and REMOVED items but excludes soft-deleted rows.
     * Throws ApiException NOT_FOUND if the owner UUID does not exist.
     */
    ItemPagedResponse listMyItems(UUID ownerUuid, Integer page, Integer size, String sort,
                                  ItemStatus status);
}

