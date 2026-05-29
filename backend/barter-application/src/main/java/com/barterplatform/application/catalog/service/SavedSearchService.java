package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.CreateSavedSearchRequest;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import java.util.UUID;

public interface SavedSearchService {

    SavedSearchResponse createSavedSearch(UUID currentUserUuid, CreateSavedSearchRequest request);

    SavedSearchPagedResponse listSavedSearches(UUID currentUserUuid, Integer page, Integer size, String sort);

    void deleteSavedSearch(UUID currentUserUuid, UUID savedSearchUuid);
}

