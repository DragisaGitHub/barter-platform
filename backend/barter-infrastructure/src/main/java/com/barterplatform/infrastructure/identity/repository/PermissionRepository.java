package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.PermissionEntity;
import com.barterplatform.domain.identity.enums.PermissionCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByCode(PermissionCode code);
}

