package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.moderation.ListingModerationActionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingModerationActionRepository extends JpaRepository<ListingModerationActionEntity, Long> {

    List<ListingModerationActionEntity> findByItemIdOrderByCreatedAtDesc(Long itemId);

    Optional<ListingModerationActionEntity> findFirstByItemIdOrderByCreatedAtDesc(Long itemId);
}

