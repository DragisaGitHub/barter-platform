package com.barterplatform.application.identity.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.domain.identity.entity.RoleEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface RoleMapper {

    RoleResponse toResponse(RoleEntity roleEntity);

    List<RoleResponse> toResponseList(List<RoleEntity> roleEntities);

    default com.barterplatform.api.model.RoleCode map(com.barterplatform.domain.identity.enums.RoleCode roleCode) {
        return roleCode == null ? null : com.barterplatform.api.model.RoleCode.valueOf(roleCode.name());
    }
}

