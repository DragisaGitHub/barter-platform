package com.barterplatform.application.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserManagementServiceImplTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        service = new UserManagementServiceImpl(userRepository, userMapper);
    }

    @Test
    void shouldUpdateUserStatus() {
        UUID userUuid = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setStatus(UserStatus.ACTIVE);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(
                com.barterplatform.api.model.UserStatus.SUSPENDED);

        UserResponse expectedResponse = new UserResponse()
                .uuid(userUuid)
                .status(com.barterplatform.api.model.UserStatus.SUSPENDED);

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = service.updateUserStatus(userUuid, request);

        assertEquals(com.barterplatform.api.model.UserStatus.SUSPENDED, result.getStatus());
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        verify(userRepository).findByUuid(userUuid);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        UUID userUuid = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(
                com.barterplatform.api.model.UserStatus.ACTIVE);

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateUserStatus(userUuid, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        verify(userRepository).findByUuid(userUuid);
        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldThrowConflictWhenSettingStatusToDeleted() {
        UUID userUuid = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(
                com.barterplatform.api.model.UserStatus.DELETED);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateUserStatus(userUuid, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldUpdateStatusToBanned() {
        UUID userUuid = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setStatus(UserStatus.ACTIVE);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(
                com.barterplatform.api.model.UserStatus.BANNED);

        UserResponse expectedResponse = new UserResponse()
                .uuid(userUuid)
                .status(com.barterplatform.api.model.UserStatus.BANNED);

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = service.updateUserStatus(userUuid, request);

        assertEquals(com.barterplatform.api.model.UserStatus.BANNED, result.getStatus());
        assertEquals(UserStatus.BANNED, user.getStatus());
    }

    @Test
    void shouldUpdateStatusToPendingVerification() {
        UUID userUuid = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setStatus(UserStatus.ACTIVE);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(
                com.barterplatform.api.model.UserStatus.PENDING_VERIFICATION);

        UserResponse expectedResponse = new UserResponse()
                .uuid(userUuid)
                .status(com.barterplatform.api.model.UserStatus.PENDING_VERIFICATION);

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = service.updateUserStatus(userUuid, request);

        assertEquals(com.barterplatform.api.model.UserStatus.PENDING_VERIFICATION, result.getStatus());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    }
}

