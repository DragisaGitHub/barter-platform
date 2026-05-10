package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.UpdateItemRequest;
import java.util.UUID;

public interface ItemCommandService {

    /**
     * Create a new item listing.
     * Resolves owner, category, and tags. Defaults status to DRAFT if not provided.
     *
     * @param ownerUuid the authenticated user's UUID
     * @param request   creation payload
     * @return full item detail including category, tags, and owner info
     */
    ItemDetailResponse createItem(UUID ownerUuid, CreateItemRequest request);

    /**
     * Update an existing item listing.
     * Only the item owner may update. Item must not be deleted or removed.
     *
     * @param ownerUuid the authenticated user's UUID — must match item owner
     * @param itemUuid  the item to update
     * @param request   partial update payload
     * @return updated item detail
     */
    ItemDetailResponse updateItem(UUID ownerUuid, UUID itemUuid, UpdateItemRequest request);

    /**
     * Archive an item listing.
     * Only the item owner may archive. Sets status to ARCHIVED and records archivedAt.
     *
     * @param ownerUuid the authenticated user's UUID — must match item owner
     * @param itemUuid  the item to archive
     * @param request   optional archive reason
     * @return updated item detail
     */
    ItemDetailResponse archiveItem(UUID ownerUuid, UUID itemUuid, ArchiveItemRequest request);
}

