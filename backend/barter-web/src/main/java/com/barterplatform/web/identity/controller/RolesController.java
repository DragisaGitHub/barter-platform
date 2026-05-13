package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.RolesApi;
import com.barterplatform.api.model.RoleCode;
import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.application.identity.service.RoleService;
import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
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
    public ResponseEntity<RoleResponse> getRoleByCode(RoleCode code) {
        return ResponseEntity.ok(roleService.getRoleByCode(mapRoleCode(code)));
    }

    @InitBinder
    void initRoleCodeBinder(WebDataBinder binder) {
        binder.registerCustomEditor(RoleCode.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null) {
                    setValue(null);
                    return;
                }

                setValue(RoleCode.fromValue(text.trim().toUpperCase(Locale.ROOT)));
            }
        });
    }

    private com.barterplatform.domain.identity.enums.RoleCode mapRoleCode(RoleCode code) {
        return com.barterplatform.domain.identity.enums.RoleCode.valueOf(code.getValue());
    }
}
