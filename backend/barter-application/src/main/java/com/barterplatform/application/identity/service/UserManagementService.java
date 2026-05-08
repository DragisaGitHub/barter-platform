package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserResponse;
import java.util.UUID;

public interface UserManagementService {

    UserResponse updateUserStatus(UUID userUuid, UpdateUserStatusRequest request);
}

