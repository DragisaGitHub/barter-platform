package com.barterplatform.web.catalog.controller;

import com.barterplatform.api.controller.CatalogApi;
import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemCondition;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemStatus;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.api.model.UpdateItemRequest;
import com.barterplatform.application.catalog.service.CatalogQueryService;
import com.barterplatform.application.catalog.service.ItemCommandService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController implements CatalogApi {

    private final CatalogQueryService catalogQueryService;
    private final ItemCommandService itemCommandService;

    public CatalogController(CatalogQueryService catalogQueryService,
                             ItemCommandService itemCommandService) {
        this.catalogQueryService = catalogQueryService;
        this.itemCommandService = itemCommandService;
    }

    // ── Public endpoints ─────────────────────────────────────────

    @Override
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(catalogQueryService.listCategories());
    }

    @Override
    public ResponseEntity<List<TagResponse>> listTags() {
        return ResponseEntity.ok(catalogQueryService.listTags());
    }

    @Override
    public ResponseEntity<ItemPagedResponse> searchItems(
            Integer page, Integer size, @Nullable String sort,
            @Nullable String q, @Nullable UUID categoryUuid,
            @Nullable List<UUID> tagUuids, @Nullable ItemStatus status,
            @Nullable ItemCondition condition) {
        return ResponseEntity.ok(catalogQueryService.searchItems(
                page, size, sort, q, categoryUuid, tagUuids,
                mapStatusToDomain(status),
                mapConditionToDomain(condition)));
    }

    @Override
    public ResponseEntity<ItemDetailResponse> getItemByUuid(UUID itemUuid) {
        return ResponseEntity.ok(catalogQueryService.getItemByUuid(itemUuid));
    }

    // ── Authenticated endpoints ──────────────────────────────────

    @Override
    public ResponseEntity<ItemDetailResponse> createItem(CreateItemRequest createItemRequest) {
        UUID ownerUuid = currentUserUuid();
        ItemDetailResponse response = itemCommandService.createItem(ownerUuid, createItemRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ItemDetailResponse> updateItem(UUID itemUuid,
                                                         UpdateItemRequest updateItemRequest) {
        UUID ownerUuid = currentUserUuid();
        return ResponseEntity.ok(itemCommandService.updateItem(ownerUuid, itemUuid, updateItemRequest));
    }

    @Override
    public ResponseEntity<ItemDetailResponse> archiveItem(UUID itemUuid,
                                                          @Nullable ArchiveItemRequest archiveItemRequest) {
        UUID ownerUuid = currentUserUuid();
        return ResponseEntity.ok(itemCommandService.archiveItem(ownerUuid, itemUuid, archiveItemRequest));
    }

    @Override
    public ResponseEntity<ItemPagedResponse> listMyItems(
            Integer page, Integer size, @Nullable String sort,
            @Nullable ItemStatus status) {
        UUID ownerUuid = currentUserUuid();
        return ResponseEntity.ok(catalogQueryService.listMyItems(
                ownerUuid, page, size, sort, mapStatusToDomain(status)));
    }

    @Override
    public ResponseEntity<Void> removeItem(UUID itemUuid) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // ── Private helpers ──────────────────────────────────────────

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUserUuid();
    }

    private com.barterplatform.domain.catalog.enums.ItemStatus mapStatusToDomain(
            @Nullable ItemStatus apiStatus) {
        return apiStatus == null ? null
                : com.barterplatform.domain.catalog.enums.ItemStatus.valueOf(apiStatus.name());
    }

    private com.barterplatform.domain.catalog.enums.ItemCondition mapConditionToDomain(
            @Nullable ItemCondition apiCondition) {
        return apiCondition == null ? null
                : com.barterplatform.domain.catalog.enums.ItemCondition.valueOf(apiCondition.name());
    }
}

