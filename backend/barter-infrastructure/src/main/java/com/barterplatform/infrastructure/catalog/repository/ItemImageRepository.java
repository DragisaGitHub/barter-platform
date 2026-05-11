package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ItemImageRepository extends JpaRepository<ItemImageEntity, Long> {

    List<ItemImageEntity> findByItemIdOrderBySortOrderAsc(Long itemId);

    long countByItemId(Long itemId);

    Optional<ItemImageEntity> findByUuid(UUID uuid);

    Optional<ItemImageEntity> findByItemIdAndUuid(Long itemId, UUID uuid);

    Optional<ItemImageEntity> findFirstByItemIdOrderBySortOrderAsc(Long itemId);

    Optional<ItemImageEntity> findFirstByItemIdAndPrimaryTrue(Long itemId);

    @Modifying
    @Query("UPDATE ItemImageEntity i SET i.primary = false WHERE i.itemId = :itemId AND i.primary = true")
    void clearPrimaryForItem(Long itemId);
}
