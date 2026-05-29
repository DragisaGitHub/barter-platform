package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.SavedSearchEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearchEntity, Long> {

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    Page<SavedSearchEntity> findByUserId(Long userId, Pageable pageable);

    Optional<SavedSearchEntity> findByUuidAndUserId(UUID uuid, Long userId);
}

