package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategorySchemaRepository
        extends JpaRepository<CategorySchemaEntity, Long>, JpaSpecificationExecutor<CategorySchemaEntity> {

    Optional<CategorySchemaEntity> findByUuid(UUID uuid);

    Optional<CategorySchemaEntity> findByCategoryIdAndStatusAndDeletedAtIsNull(Long categoryId, CategorySchemaStatus status);

    @Query("select coalesce(max(s.version), 0) from CategorySchemaEntity s where s.categoryId = :categoryId")
    int findMaxVersionByCategoryId(@Param("categoryId") Long categoryId);
}

