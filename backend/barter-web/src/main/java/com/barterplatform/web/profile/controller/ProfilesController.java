package com.barterplatform.web.profile.controller;

import com.barterplatform.api.controller.ProfilesApi;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import com.barterplatform.application.profile.service.PublicProfileService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfilesController implements ProfilesApi {

    private final PublicProfileService publicProfileService;

    public ProfilesController(PublicProfileService publicProfileService) {
        this.publicProfileService = publicProfileService;
    }

    @Override
    public ResponseEntity<PublicProfileResponse> getPublicProfile(UUID userUuid) {
        return ResponseEntity.ok(publicProfileService.getPublicProfile(userUuid));
    }

    @Override
    public ResponseEntity<ItemPagedResponse> listPublicItems(UUID userUuid,
                                                              Integer page,
                                                              Integer size,
                                                              String sort) {
        return ResponseEntity.ok(publicProfileService.listPublicItems(userUuid, page, size, sort));
    }
}

