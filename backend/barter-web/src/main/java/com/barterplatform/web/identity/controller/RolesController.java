package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.RolesApi;
import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.application.identity.service.RoleService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.enums.RoleCode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RolesController implements RolesApi {

    private final RoleService roleService;

    public RolesController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @Override
    public ResponseEntity<RoleResponse> getRoleByCode(Object code) {
        return ResponseEntity.ok(roleService.getRoleByCode(parseRoleCode(code)));
    }

    private RoleCode parseRoleCode(Object code) {
        if (code == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Role code is required.");
        }

        try {
            return RoleCode.valueOf(code.toString().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Unsupported role code '%s'.".formatted(code),
                    ex);
        }
    }
}

