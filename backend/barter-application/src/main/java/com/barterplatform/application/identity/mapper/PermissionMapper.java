package com.barterplatform.application.identity.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.domain.identity.entity.PermissionEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface PermissionMapper {

    PermissionResponse toResponse(PermissionEntity permissionEntity);

    List<PermissionResponse> toResponseList(List<PermissionEntity> permissionEntities);

    default com.barterplatform.api.model.PermissionCode map(
            com.barterplatform.domain.identity.enums.PermissionCode permissionCode) {
        return permissionCode == null ? null : com.barterplatform.api.model.PermissionCode.valueOf(permissionCode.name());
    }
}

