package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.PasswordResetTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity>
    findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);
}