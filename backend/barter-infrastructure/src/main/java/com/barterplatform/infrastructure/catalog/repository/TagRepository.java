package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.TagEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TagRepository extends JpaRepository<TagEntity, Long>, JpaSpecificationExecutor<TagEntity> {

    Optional<TagEntity> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Optional<TagEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndUuidNot(String slug, UUID uuid);

    List<TagEntity> findAllByDeletedAtIsNullOrderByNameAsc();
}

