package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.domain.identity.enums.RoleCode;
import java.util.List;

public interface RoleService {

    List<RoleResponse> listRoles();

    RoleResponse getRoleByCode(RoleCode code);
}

