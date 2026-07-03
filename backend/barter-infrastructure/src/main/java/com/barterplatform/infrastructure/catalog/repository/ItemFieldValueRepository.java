package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemFieldValueEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemFieldValueRepository extends JpaRepository<ItemFieldValueEntity, Long> {

    List<ItemFieldValueEntity> findByItemId(Long itemId);

    List<ItemFieldValueEntity> findByItemIdIn(Collection<Long> itemIds);

    void deleteByItemId(Long itemId);
}

