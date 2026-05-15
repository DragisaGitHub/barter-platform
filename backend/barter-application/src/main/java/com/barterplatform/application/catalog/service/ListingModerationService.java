package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminRemoveListingRequest;
import com.barterplatform.api.model.AdminRestoreListingRequest;
import java.util.UUID;

public interface ListingModerationService {

    AdminListingDetailResponse removeListing(UUID adminUserUuid, UUID listingUuid, AdminRemoveListingRequest request);

    AdminListingDetailResponse restoreListing(UUID adminUserUuid, UUID listingUuid, AdminRestoreListingRequest request);
}

