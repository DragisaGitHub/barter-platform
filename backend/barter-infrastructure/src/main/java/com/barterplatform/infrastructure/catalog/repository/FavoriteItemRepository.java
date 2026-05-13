package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.FavoriteItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItemEntity, Long> {

    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    Optional<FavoriteItemEntity> findByUserIdAndItemId(Long userId, Long itemId);

    void deleteByUserIdAndItemId(Long userId, Long itemId);

    @Query(
            value = """
                    select f
                    from FavoriteItemEntity f
                    join ItemEntity i on i.id = f.itemId
                    where f.userId = :userId
                      and f.deletedAt is null
                      and i.deletedAt is null
                      and i.status <> :removedStatus
                    """,
            countQuery = """
                    select count(f)
                    from FavoriteItemEntity f
                    join ItemEntity i on i.id = f.itemId
                    where f.userId = :userId
                      and f.deletedAt is null
                      and i.deletedAt is null
                      and i.status <> :removedStatus
                    """)
    Page<FavoriteItemEntity> findVisibleByUserId(@Param("userId") Long userId,
                                                 @Param("removedStatus") ItemStatus removedStatus,
                                                 Pageable pageable);
}

