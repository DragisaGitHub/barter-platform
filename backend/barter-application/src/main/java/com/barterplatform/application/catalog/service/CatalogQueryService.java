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
     * Defaults to ACTIVE status when no explicit status is provided.
     * Only returns non-deleted items (deletedAt is null, status != REMOVED).
     *
     * @param page         page number (0-based), defaults to 0
     * @param size         page size, defaults to 20, max 100
     * @param sort         sort expression e.g. "createdAt,desc"
     * @param q            free-text search on title (contains, case-insensitive)
     * @param categoryUuid filter by category UUID
     * @param tagUuids     filter by tag UUIDs — accepted but ignored in v1 (TODO: implement via Specification subquery)
     * @param status       filter by item status; defaults to ACTIVE for public search
     * @param condition    filter by item condition
     */
    ItemPagedResponse searchItems(Integer page, Integer size, String sort,
                                  String q, UUID categoryUuid, List<UUID> tagUuids,
                                  ItemStatus status, ItemCondition condition);

    /**
     * Get full item detail by UUID.
     * Throws ApiException NOT_FOUND if the item is missing, soft-deleted, or has REMOVED status.
     * Includes category, tags, ownerUuid, and ownerUsername.
     */
    ItemDetailResponse getItemByUuid(UUID itemUuid);

    /**
     * List items belonging to a specific owner.
     * If status is provided, returns only items with that status.
     * Otherwise includes DRAFT, ACTIVE, RESERVED, ARCHIVED items but excludes REMOVED and soft-deleted.
     * Throws ApiException NOT_FOUND if the owner UUID does not exist.
     */
    ItemPagedResponse listMyItems(UUID ownerUuid, Integer page, Integer size, String sort,
                                  ItemStatus status);
}

