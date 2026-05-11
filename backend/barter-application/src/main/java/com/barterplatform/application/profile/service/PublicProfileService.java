package com.barterplatform.application.profile.service;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import java.util.UUID;

public interface PublicProfileService {

    PublicProfileResponse getPublicProfile(UUID userUuid);

    ItemPagedResponse listPublicItems(UUID userUuid, Integer page, Integer size, String sort);
}

