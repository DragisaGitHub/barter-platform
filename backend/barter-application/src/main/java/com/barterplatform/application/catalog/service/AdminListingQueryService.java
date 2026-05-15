package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.ListingModerationActionResponse;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import java.util.List;
import java.util.UUID;

public interface AdminListingQueryService {

    AdminListingPagedResponse listListings(
            Integer page,
            Integer size,
            String sort,
            String q,
            String ownerQuery,
            UUID categoryUuid,
            ItemStatus status);

    AdminListingDetailResponse getListing(UUID listingUuid);

    List<ListingModerationActionResponse> listModerationActions(UUID listingUuid);
}

