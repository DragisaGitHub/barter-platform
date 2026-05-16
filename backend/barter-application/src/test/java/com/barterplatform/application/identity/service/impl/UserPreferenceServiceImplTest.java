package com.barterplatform.application.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.PreferredLanguage;
import com.barterplatform.api.model.UpdateUserPreferencesRequest;
import com.barterplatform.api.model.UserPreferencesResponse;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class UserPreferenceServiceImplTest {

    private UserRepository userRepository;
    private UserPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        service = new UserPreferenceServiceImpl(userRepository);
    }

    @Test
    void shouldGetCurrentUserPreferences() {
        UserEntity user = userWithPreferredLanguage(com.barterplatform.domain.identity.enums.PreferredLanguage.SR);
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));

        UserPreferencesResponse response = service.getCurrentUserPreferences(user.getUuid());

        assertEquals(PreferredLanguage.SR, response.getPreferredLanguage());
    }

    @Test
    void shouldUpdatePreferencesFromSrToEn() {
        UserEntity user = userWithPreferredLanguage(com.barterplatform.domain.identity.enums.PreferredLanguage.SR);
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserPreferencesResponse response = service.updateCurrentUserPreferences(
                user.getUuid(),
                new UpdateUserPreferencesRequest(PreferredLanguage.EN));

        assertEquals(com.barterplatform.domain.identity.enums.PreferredLanguage.EN, user.getPreferredLanguage());
        assertEquals(PreferredLanguage.EN, response.getPreferredLanguage());
        verify(userRepository).save(user);
    }

    @Test
    void shouldUpdatePreferencesFromEnToSr() {
        UserEntity user = userWithPreferredLanguage(com.barterplatform.domain.identity.enums.PreferredLanguage.EN);
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserPreferencesResponse response = service.updateCurrentUserPreferences(
                user.getUuid(),
                new UpdateUserPreferencesRequest(PreferredLanguage.SR));

        assertEquals(com.barterplatform.domain.identity.enums.PreferredLanguage.SR, user.getPreferredLanguage());
        assertEquals(PreferredLanguage.SR, response.getPreferredLanguage());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        UUID userUuid = UUID.randomUUID();
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getCurrentUserPreferences(userUuid));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void shouldFallbackToSrWhenPreferredLanguageIsNull() {
        UserEntity user = userWithPreferredLanguage(null);
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));

        UserPreferencesResponse response = service.getCurrentUserPreferences(user.getUuid());

        assertEquals(PreferredLanguage.SR, response.getPreferredLanguage());
    }

    private UserEntity userWithPreferredLanguage(com.barterplatform.domain.identity.enums.PreferredLanguage preferredLanguage) {
        UserEntity user = new UserEntity();
        user.setUuid(UUID.randomUUID());
        user.setPreferredLanguage(preferredLanguage);
        return user;
    }
}

