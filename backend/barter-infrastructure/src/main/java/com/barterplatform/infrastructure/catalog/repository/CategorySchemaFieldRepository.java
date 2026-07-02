package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorySchemaFieldRepository extends JpaRepository<CategorySchemaFieldEntity, Long> {

    Optional<CategorySchemaFieldEntity> findByUuid(UUID uuid);

    boolean existsBySchemaIdAndKeyAndDeletedAtIsNull(Long schemaId, String key);

    List<CategorySchemaFieldEntity> findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long schemaId);

    List<CategorySchemaFieldEntity> findAllBySchemaIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List<Long> schemaIds);
}

