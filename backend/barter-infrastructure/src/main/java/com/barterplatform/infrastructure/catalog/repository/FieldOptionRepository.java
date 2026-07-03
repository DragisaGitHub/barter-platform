package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldOptionRepository extends JpaRepository<FieldOptionEntity, Long> {

    Optional<FieldOptionEntity> findByUuid(UUID uuid);

    Optional<FieldOptionEntity> findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(Long fieldId, String value);

    boolean existsByFieldIdAndValueAndDeletedAtIsNull(Long fieldId, String value);

    List<FieldOptionEntity> findAllByFieldIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long fieldId);

    List<FieldOptionEntity> findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List<Long> fieldIds);
}

