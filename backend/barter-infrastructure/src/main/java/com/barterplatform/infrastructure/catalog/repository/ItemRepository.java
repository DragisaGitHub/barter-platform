package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<ItemEntity, Long>, JpaSpecificationExecutor<ItemEntity> {

    Optional<ItemEntity> findByUuid(UUID uuid);

    Page<ItemEntity> findByOwnerId(Long ownerId, Pageable pageable);

    Page<ItemEntity> findByOwnerIdAndDeletedAtIsNull(Long ownerId, Pageable pageable);

    Page<ItemEntity> findByOwnerIdAndStatusNotAndDeletedAtIsNull(
            Long ownerId, ItemStatus excludedStatus, Pageable pageable);

    Page<ItemEntity> findByOwnerIdAndStatusAndDeletedAtIsNull(
            Long ownerId, ItemStatus status, Pageable pageable);

    Page<ItemEntity> findByStatusAndDeletedAtIsNull(ItemStatus status, Pageable pageable);

    long countByOwnerIdAndStatus(Long ownerId, ItemStatus status);

    long countByStatus(ItemStatus status);

    long countByStatusAndDeletedAtIsNull(ItemStatus status);

    @Query("""
        select i
        from ItemEntity i
        where i.deletedAt is null
          and i.status = :activeStatus
          and i.id <> :wishlistItemId
          and i.ownerId <> :ownerId
          and i.categoryId = :categoryId
          and i.listingTemplateType in :templateTypes
        order by i.createdAt desc
        """)
    List<ItemEntity> findWishlistMatchCandidates(
            @Param("wishlistItemId") Long wishlistItemId,
            @Param("ownerId") Long ownerId,
            @Param("categoryId") Long categoryId,
            @Param("templateTypes") Collection<ListingTemplateType> templateTypes,
            @Param("activeStatus") ItemStatus activeStatus,
            Pageable pageable);
}

