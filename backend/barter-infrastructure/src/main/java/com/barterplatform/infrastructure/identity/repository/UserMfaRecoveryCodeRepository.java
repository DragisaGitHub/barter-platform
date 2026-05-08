package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.UserMfaRecoveryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaRecoveryCodeRepository extends JpaRepository<UserMfaRecoveryCodeEntity, Long> {
}

