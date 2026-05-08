package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.application.identity.mapper.PermissionMapper;
import com.barterplatform.application.identity.service.PermissionService;
import com.barterplatform.infrastructure.identity.repository.PermissionRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Sort PERMISSION_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.toResponseList(permissionRepository.findAll(PERMISSION_SORT));
    }
}

