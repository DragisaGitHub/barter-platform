package com.barterplatform.web.catalog.controller;

import com.barterplatform.api.controller.CatalogApi;
import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemCondition;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemStatus;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.PopularCategoryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.api.model.UpdateItemRequest;
import com.barterplatform.application.catalog.service.CatalogQueryService;
import com.barterplatform.application.catalog.service.FavoriteItemService;
import com.barterplatform.application.catalog.service.ItemCommandService;
import com.barterplatform.application.catalog.service.ItemImageService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CatalogController implements CatalogApi {

    private final CatalogQueryService catalogQueryService;
    private final FavoriteItemService favoriteItemService;
    private final ItemCommandService itemCommandService;
    private final ItemImageService itemImageService;

    public CatalogController(CatalogQueryService catalogQueryService,
                             FavoriteItemService favoriteItemService,
                             ItemCommandService itemCommandService,
                             ItemImageService itemImageService) {
        this.catalogQueryService = catalogQueryService;
        this.favoriteItemService = favoriteItemService;
        this.itemCommandService = itemCommandService;
        this.itemImageService = itemImageService;
    }

    // ── Public endpoints ─────────────────────────────────────────

    @Override
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(catalogQueryService.listCategories());
    }

    @Override
    public ResponseEntity<List<PopularCategoryResponse>> listPopularCategories(@Nullable Integer limit) {
        return ResponseEntity.ok(catalogQueryService.listPopularCategories(limit));
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

    // ── Authenticated item endpoints ─────────────────────────────

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
    public ResponseEntity<ItemPagedResponse> listFavoriteItems(
            Integer page, Integer size, @Nullable String sort) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(favoriteItemService.listFavoriteItems(currentUserUuid, page, size, sort));
    }

    @Override
    public ResponseEntity<MessageResponse> favoriteItem(UUID itemUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(favoriteItemService.favoriteItem(currentUserUuid, itemUuid));
    }

    @Override
    public ResponseEntity<Void> unfavoriteItem(UUID itemUuid) {
        UUID currentUserUuid = currentUserUuid();
        favoriteItemService.unfavoriteItem(currentUserUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeItem(UUID itemUuid) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // ── Image endpoints ──────────────────────────────────────────

    @Override
    public ResponseEntity<ItemImageResponse> uploadItemImage(UUID itemUuid, MultipartFile file) {
        UUID currentUserUuid = currentUserUuid();
        ItemImageResponse response = itemImageService.uploadImage(currentUserUuid, itemUuid, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<ItemImageResponse>> listItemImages(UUID itemUuid) {
        return ResponseEntity.ok(itemImageService.listImages(itemUuid));
    }

    @Override
    public ResponseEntity<Void> deleteItemImage(UUID itemUuid, UUID imageUuid) {
        UUID currentUserUuid = currentUserUuid();
        itemImageService.deleteImage(currentUserUuid, itemUuid, imageUuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ItemImageResponse> setItemImageAsPrimary(UUID itemUuid, UUID imageUuid) {
        UUID currentUserUuid = currentUserUuid();
        ItemImageResponse response = itemImageService.setPrimaryImage(currentUserUuid, itemUuid, imageUuid);
        return ResponseEntity.ok(response);
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

