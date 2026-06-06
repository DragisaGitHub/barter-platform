package com.barterplatform.web.catalog.controller;

import com.barterplatform.api.controller.CatalogApi;
import com.barterplatform.api.model.*;
import com.barterplatform.application.catalog.service.*;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class CatalogController implements CatalogApi {

    private final CatalogQueryService catalogQueryService;
    private final FavoriteItemService favoriteItemService;
    private final ItemCommandService itemCommandService;
    private final ItemImageService itemImageService;
    private final RecommendationService recommendationService;
    private final WishlistMatchService wishlistMatchService;

    // ── Public endpoints ─────────────────────────────────────────

    @Override
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(catalogQueryService.listCategories());
    }

    @Override
    public ResponseEntity<List<PopularCategoryResponse>> listPopularCategories(Integer limit) {
        return ResponseEntity.ok(catalogQueryService.listPopularCategories(limit));
    }

    @Override
    public ResponseEntity<List<TagResponse>> listTags() {
        return ResponseEntity.ok(catalogQueryService.listTags());
    }

    @Override
    public ResponseEntity<ItemPagedResponse> searchItems(
            Integer page, Integer size, String sort,
            String q, UUID categoryUuid,
            List<UUID> tagUuids,
            ItemCondition condition, String location) {
        return ResponseEntity.ok(catalogQueryService.searchItems(
                page, size, sort, q, categoryUuid, tagUuids,
                mapConditionToDomain(condition),
                location));
    }

    @Override
    public ResponseEntity<ItemDetailResponse> getItemByUuid(UUID itemUuid) {
        return ResponseEntity.ok(catalogQueryService.getItemByUuid(
                itemUuid,
                currentUserUuidOrNull(),
                currentUserIsAdmin()));
    }

    @Override
    public ResponseEntity<RecommendationPagedResponse> listRecommendations(Integer page, Integer size, String sort) {
        return ResponseEntity.ok(recommendationService.listRecommendations(
                currentUserUuidOrNull(), page, size, sort));
    }

    @Override
    public ResponseEntity<List<WishlistMatchResponse>> listWishlistMatches(UUID itemUuid) {
        return ResponseEntity.ok(wishlistMatchService.listWishlistMatches(
                currentUserUuid(),
                itemUuid));
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
                                                          ArchiveItemRequest archiveItemRequest) {
        UUID ownerUuid = currentUserUuid();
        return ResponseEntity.ok(itemCommandService.archiveItem(ownerUuid, itemUuid, archiveItemRequest));
    }

    @Override
    public ResponseEntity<ItemPagedResponse> listMyItems(
            Integer page, Integer size, String sort,
            ItemStatus status) {
        UUID ownerUuid = currentUserUuid();
        return ResponseEntity.ok(catalogQueryService.listMyItems(
                ownerUuid, page, size, sort, mapStatusToDomain(status)));
    }

    @Override
    public ResponseEntity<ItemPagedResponse> listFavoriteItems(
            Integer page, Integer size, String sort) {
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

    // ── Image endpoints ──────────────────────────────────────────

    @Override
    public ResponseEntity<ItemImageResponse> uploadItemImage(UUID itemUuid, MultipartFile file) {
        UUID currentUserUuid = currentUserUuid();
        ItemImageResponse response = itemImageService.uploadImage(currentUserUuid, itemUuid, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<ItemImageResponse>> listItemImages(UUID itemUuid) {
        return ResponseEntity.ok(itemImageService.listImages(
                itemUuid,
                currentUserUuidOrNull(),
                currentUserIsAdmin()));
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
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;
        return principal.getUserUuid();
    }

    private UUID currentUserUuidOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.getUserUuid();
    }

    private boolean currentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return false;
        }
        return principal.getRoles().contains("ADMIN");
    }

    private com.barterplatform.domain.catalog.enums.ItemStatus mapStatusToDomain(
            ItemStatus apiStatus) {
        return apiStatus == null ? null
                : com.barterplatform.domain.catalog.enums.ItemStatus.valueOf(apiStatus.name());
    }

    private com.barterplatform.domain.catalog.enums.ItemCondition mapConditionToDomain(
            ItemCondition apiCondition) {
        return apiCondition == null ? null
                : com.barterplatform.domain.catalog.enums.ItemCondition.valueOf(apiCondition.name());
    }
}

