package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.UpdateUserPreferencesRequest;
import com.barterplatform.api.model.UserPreferencesResponse;
import java.util.UUID;

public interface UserPreferenceService {

    UserPreferencesResponse getCurrentUserPreferences(UUID currentUserUuid);

    UserPreferencesResponse updateCurrentUserPreferences(UUID currentUserUuid, UpdateUserPreferencesRequest request);
}

