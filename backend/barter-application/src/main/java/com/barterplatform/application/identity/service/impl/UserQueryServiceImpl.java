package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.UserResponse;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.identity.mapper.PermissionMapper;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.application.identity.service.UserQueryService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.PermissionEntity;
import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.RolePermissionEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.infrastructure.identity.repository.OAuthAccountRepository;
import com.barterplatform.infrastructure.identity.repository.PermissionRepository;
import com.barterplatform.infrastructure.identity.repository.RolePermissionRepository;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserMfaSettingsRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private static final String DEFAULT_USER_SORT_FIELD = "username";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "username",
            "email",
            "status",
            "createdAt",
            "updatedAt",
            "lastLoginAt",
            "uuid");

    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserMfaSettingsRepository userMfaSettingsRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public UserQueryServiceImpl(
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            OAuthAccountRepository oAuthAccountRepository,
            UserMfaSettingsRepository userMfaSettingsRepository,
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper) {
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.userMfaSettingsRepository = userMfaSettingsRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public UserPagedResponse listUsers(Integer page, Integer size, String sort) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort,
                DEFAULT_USER_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Page<UserEntity> userPage = userRepository.findAll(pageRequest.pageable());

        return pageResponseMapper.toUserPagedResponse(
                userPage,
                userMapper.toSummaryResponseList(userPage.getContent()),
                pageRequest.sort());
    }

    @Override
    public UserResponse getUserByUuid(UUID userUuid) {
        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User with uuid '%s' was not found.".formatted(userUuid)));

        List<RoleEntity> roles = getRolesForUser(user.getId());
        List<PermissionEntity> permissions = getPermissionsForRoles(roles);

        UserResponse response = userMapper.toResponse(user);
        response.setRoles(roleMapper.toResponseList(roles));
        response.setPermissions(permissionMapper.toResponseList(permissions));
        response.setOauthAccounts(userMapper.toOAuthAccountResponseList(
                oAuthAccountRepository.findAllByUserIdOrderByLinkedAtAsc(user.getId())));
        response.setMfaSettings(userMfaSettingsRepository.findByUserId(user.getId())
                .map(userMapper::toMfaSettingsResponse)
                .orElse(null));
        return response;
    }

    private List<RoleEntity> getRolesForUser(Long userId) {
        List<UserRoleEntity> userRoleEntities = userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(userId);
        if (userRoleEntities.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoleEntities.stream()
                .map(userRoleEntity -> userRoleEntity.getId().getRoleId())
                .toList();

        Map<Long, RoleEntity> rolesById = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));

        List<RoleEntity> orderedRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            RoleEntity role = rolesById.get(roleId);
            if (role != null) {
                orderedRoles.add(role);
            }
        }
        return orderedRoles;
    }

    private List<PermissionEntity> getPermissionsForRoles(List<RoleEntity> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roles.stream()
                .map(RoleEntity::getId)
                .toList();

        List<RolePermissionEntity> rolePermissionEntities =
                rolePermissionRepository.findAllByIdRoleIdInOrderByAssignedAtAsc(roleIds);
        if (rolePermissionEntities.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> permissionIds = rolePermissionEntities.stream()
                .map(rolePermissionEntity -> rolePermissionEntity.getId().getPermissionId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, PermissionEntity> permissionsById = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, Function.identity()));

        List<PermissionEntity> orderedPermissions = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            PermissionEntity permission = permissionsById.get(permissionId);
            if (permission != null) {
                orderedPermissions.add(permission);
            }
        }
        return orderedPermissions;
    }

}

