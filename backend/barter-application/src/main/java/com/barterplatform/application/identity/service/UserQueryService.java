package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.UserResponse;
import com.barterplatform.api.model.UserPagedResponse;
import java.util.UUID;

public interface UserQueryService {

    UserPagedResponse listUsers(Integer page, Integer size, String sort);

    UserResponse getUserByUuid(UUID userUuid);
}

