package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.UsersApi;
import com.barterplatform.api.model.UpdateUserPreferencesRequest;
import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserPreferencesResponse;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.application.identity.service.UserPreferenceService;
import com.barterplatform.application.identity.service.UserQueryService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController implements UsersApi {

    private final UserQueryService userQueryService;
    private final UserManagementService userManagementService;
    private final UserPreferenceService userPreferenceService;

    public UsersController(
            UserQueryService userQueryService,
            UserManagementService userManagementService,
            UserPreferenceService userPreferenceService) {
        this.userQueryService = userQueryService;
        this.userManagementService = userManagementService;
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<UserResponse> getUserByUuid(UUID userUuid) {
        return ResponseEntity.ok(userQueryService.getUserByUuid(userUuid));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<UserPagedResponse> listUsers(Integer page, Integer size, @Nullable String sort) {
        return ResponseEntity.ok(userQueryService.listUsers(page, size, sort));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(UUID userUuid, @Nullable UpdateUserStatusRequest updateUserStatusRequest) {
        UserResponse response = userManagementService.updateUserStatus(userUuid, updateUserStatusRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> getCurrentUserPreferences() {
        return ResponseEntity.ok(userPreferenceService.getCurrentUserPreferences(currentUserUuid()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> updateCurrentUserPreferences(
            @Nullable UpdateUserPreferencesRequest updateUserPreferencesRequest) {
        UserPreferencesResponse response = userPreferenceService.updateCurrentUserPreferences(
                currentUserUuid(),
                updateUserPreferencesRequest);
        return ResponseEntity.ok(response);
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        return principal.getUserUuid();
    }
}

