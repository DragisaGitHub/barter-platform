package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserManagementServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID userUuid, UpdateUserStatusRequest request) {
        if (request.getStatus() == com.barterplatform.api.model.UserStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Setting status to DELETED is not allowed through this endpoint.");
        }

        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User with uuid '%s' was not found.".formatted(userUuid)));

        UserStatus newStatus = UserStatus.valueOf(request.getStatus().getValue());
        user.setStatus(newStatus);
        userRepository.save(user);

        return userMapper.toResponse(user);
    }
}

