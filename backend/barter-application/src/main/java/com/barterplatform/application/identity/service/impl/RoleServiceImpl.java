package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.service.RoleService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private static final Sort ROLE_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<RoleResponse> listRoles() {
        return roleMapper.toResponseList(roleRepository.findAll(ROLE_SORT));
    }

    @Override
    public RoleResponse getRoleByCode(RoleCode code) {
        return roleRepository.findByCode(code)
                .map(roleMapper::toResponse)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Role with code '%s' was not found.".formatted(code)));
    }
}

