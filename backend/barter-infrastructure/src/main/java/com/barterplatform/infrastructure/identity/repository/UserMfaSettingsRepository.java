package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.UserMfaSettingsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaSettingsRepository extends JpaRepository<UserMfaSettingsEntity, Long> {

    Optional<UserMfaSettingsEntity> findByUserId(Long userId);
}

