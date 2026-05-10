package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByUuid(UUID uuid);

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc();
}

