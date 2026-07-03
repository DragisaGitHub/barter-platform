package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.ItemFieldValueOptionEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemFieldValueOptionRepository extends JpaRepository<ItemFieldValueOptionEntity, Long> {

    List<ItemFieldValueOptionEntity> findByItemFieldValueIdIn(Collection<Long> itemFieldValueIds);

    void deleteByItemFieldValueIdIn(Collection<Long> itemFieldValueIds);
}

