package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.TagEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByUuid(UUID uuid);

    Optional<TagEntity> findBySlug(String slug);

    List<TagEntity> findAllByDeletedAtIsNullOrderByNameAsc();
}

