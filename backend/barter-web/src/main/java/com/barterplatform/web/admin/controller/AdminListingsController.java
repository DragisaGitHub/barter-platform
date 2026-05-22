package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminListingsApi;
import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.AdminRemoveListingRequest;
import com.barterplatform.api.model.AdminRestoreListingRequest;
import com.barterplatform.api.model.ItemStatus;
import com.barterplatform.api.model.ListingModerationActionResponse;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.application.catalog.service.ListingModerationService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminListingsController implements AdminListingsApi {

    private final AdminListingQueryService adminListingQueryService;
    private final ListingModerationService listingModerationService;

    public AdminListingsController(
            AdminListingQueryService adminListingQueryService,
            ListingModerationService listingModerationService) {
        this.adminListingQueryService = adminListingQueryService;
        this.listingModerationService = listingModerationService;
    }

    @Override
    public ResponseEntity<AdminListingPagedResponse> listAdminListings(
            Integer page,
            Integer size,
            String sort,
            String q,
            String ownerQuery,
            UUID categoryUuid,
            ItemStatus status) {
        return ResponseEntity.ok(adminListingQueryService.listListings(
                page,
                size,
                sort,
                q,
                ownerQuery,
                categoryUuid,
                status == null ? null : com.barterplatform.domain.catalog.enums.ItemStatus.valueOf(status.name())));
    }

    @Override
    public ResponseEntity<AdminListingDetailResponse> getAdminListingByUuid(UUID itemUuid) {
        return ResponseEntity.ok(adminListingQueryService.getListing(itemUuid));
    }

    @Override
    public ResponseEntity<List<ListingModerationActionResponse>> listListingModerationActions(UUID itemUuid) {
        return ResponseEntity.ok(adminListingQueryService.listModerationActions(itemUuid));
    }

    @Override
    public ResponseEntity<AdminListingDetailResponse> removeAdminListing(
            UUID itemUuid,
            AdminRemoveListingRequest adminRemoveListingRequest) {
        return ResponseEntity.ok(listingModerationService.removeListing(
                currentUserUuid(), itemUuid, adminRemoveListingRequest));
    }

    @Override
    public ResponseEntity<AdminListingDetailResponse> restoreAdminListing(
            UUID itemUuid,
            AdminRestoreListingRequest adminRestoreListingRequest) {
        return ResponseEntity.ok(listingModerationService.restoreListing(
                currentUserUuid(), itemUuid, adminRestoreListingRequest));
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getPrincipal());
        return principal.getUserUuid();
    }
}

