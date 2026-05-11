package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntity, Long> {

    Optional<EmailVerificationCodeEntity> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);
}

