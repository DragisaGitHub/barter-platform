package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository extends JpaRepository<ItemEntity, Long>, JpaSpecificationExecutor<ItemEntity> {

    Optional<ItemEntity> findByUuid(UUID uuid);

    Page<ItemEntity> findByOwnerId(Long ownerId, Pageable pageable);

    Page<ItemEntity> findByOwnerIdAndStatusNotAndDeletedAtIsNull(
            Long ownerId, ItemStatus excludedStatus, Pageable pageable);

    Page<ItemEntity> findByOwnerIdAndStatusAndDeletedAtIsNull(
            Long ownerId, ItemStatus status, Pageable pageable);

    long countByOwnerIdAndStatus(Long ownerId, ItemStatus status);
}

