package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.PermissionsApi;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.application.identity.service.PermissionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionsController implements PermissionsApi {

    private final PermissionService permissionService;

    public PermissionsController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }
}

