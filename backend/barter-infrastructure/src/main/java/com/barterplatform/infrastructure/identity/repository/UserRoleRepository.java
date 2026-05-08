package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

	List<UserRoleEntity> findAllByIdUserIdOrderByAssignedAtAsc(Long userId);
}

