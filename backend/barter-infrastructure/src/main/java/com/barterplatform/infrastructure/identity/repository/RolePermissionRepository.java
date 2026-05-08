package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.RolePermissionEntity;
import com.barterplatform.domain.identity.entity.RolePermissionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

	List<RolePermissionEntity> findAllByIdRoleIdInOrderByAssignedAtAsc(Collection<Long> roleIds);
}

