package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.PermissionResponse;
import java.util.List;

public interface PermissionService {

    List<PermissionResponse> listPermissions();
}

