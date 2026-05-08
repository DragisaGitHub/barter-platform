package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.UsersApi;
import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.application.identity.service.UserQueryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController implements UsersApi {

    private final UserQueryService userQueryService;
    private final UserManagementService userManagementService;

    public UsersController(UserQueryService userQueryService, UserManagementService userManagementService) {
        this.userQueryService = userQueryService;
        this.userManagementService = userManagementService;
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
}

