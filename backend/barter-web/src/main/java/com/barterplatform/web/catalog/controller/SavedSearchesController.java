package com.barterplatform.web.catalog.controller;

import com.barterplatform.api.controller.SavedSearchesApi;
import com.barterplatform.api.model.CreateSavedSearchRequest;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import com.barterplatform.application.catalog.service.SavedSearchService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SavedSearchesController implements SavedSearchesApi {

    private final SavedSearchService savedSearchService;

    public SavedSearchesController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @Override
    public ResponseEntity<SavedSearchResponse> createSavedSearch(CreateSavedSearchRequest createSavedSearchRequest) {
        SavedSearchResponse response = savedSearchService.createSavedSearch(currentUserUuid(), createSavedSearchRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<SavedSearchPagedResponse> listSavedSearches(Integer page, Integer size, String sort) {
        return ResponseEntity.ok(savedSearchService.listSavedSearches(currentUserUuid(), page, size, sort));
    }

    @Override
    public ResponseEntity<Void> deleteSavedSearch(UUID savedSearchUuid) {
        savedSearchService.deleteSavedSearch(currentUserUuid(), savedSearchUuid);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;
        return principal.getUserUuid();
    }
}

