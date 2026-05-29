package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemListingEntryRepository extends JpaRepository<ItemListingEntryEntity, Long> {

    List<ItemListingEntryEntity> findByItemIdOrderBySortOrderAsc(Long itemId);

    List<ItemListingEntryEntity> findTop3ByItemIdOrderBySortOrderAsc(Long itemId);

    List<ItemListingEntryEntity> findByItemIdInOrderByItemIdAscSortOrderAsc(Collection<Long> itemIds);

    long countByItemId(Long itemId);

    void deleteByItemId(Long itemId);
}

