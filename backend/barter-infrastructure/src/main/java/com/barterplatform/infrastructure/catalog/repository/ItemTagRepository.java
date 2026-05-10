package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.ItemTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemTagRepository extends JpaRepository<ItemTagEntity, ItemTagId> {

    List<ItemTagEntity> findByIdItemId(Long itemId);

    void deleteByIdItemId(Long itemId);
}

