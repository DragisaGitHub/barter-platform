package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>, JpaSpecificationExecutor<CategoryEntity> {

    Optional<CategoryEntity> findByUuid(UUID uuid);

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndUuidNot(String slug, UUID uuid);

    List<CategoryEntity> findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc();
}

