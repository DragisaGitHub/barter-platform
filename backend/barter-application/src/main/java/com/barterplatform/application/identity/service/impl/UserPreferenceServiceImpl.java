package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.UpdateUserPreferencesRequest;
import com.barterplatform.api.model.UserPreferencesResponse;
import com.barterplatform.application.identity.service.UserPreferenceService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserRepository userRepository;

    public UserPreferenceServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences(UUID currentUserUuid) {
        UserEntity user = findUser(currentUserUuid);
        return new UserPreferencesResponse().preferredLanguage(mapToApi(normalizePreferredLanguage(user)));
    }

    @Override
    @Transactional
    public UserPreferencesResponse updateCurrentUserPreferences(UUID currentUserUuid, UpdateUserPreferencesRequest request) {
        UserEntity user = findUser(currentUserUuid);
        PreferredLanguage preferredLanguage = request == null || request.getPreferredLanguage() == null
                ? PreferredLanguage.SR
                : PreferredLanguage.valueOf(request.getPreferredLanguage().getValue());

        user.setPreferredLanguage(preferredLanguage);
        userRepository.save(user);

        return new UserPreferencesResponse().preferredLanguage(mapToApi(user.getPreferredLanguage()));
    }

    private UserEntity findUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User with uuid '%s' was not found.".formatted(userUuid)));
    }

    private PreferredLanguage normalizePreferredLanguage(UserEntity user) {
        return user.getPreferredLanguage() == null ? PreferredLanguage.SR : user.getPreferredLanguage();
    }

    private com.barterplatform.api.model.PreferredLanguage mapToApi(PreferredLanguage preferredLanguage) {
        PreferredLanguage resolved = preferredLanguage == null ? PreferredLanguage.SR : preferredLanguage;
        return com.barterplatform.api.model.PreferredLanguage.valueOf(resolved.name());
    }
}

