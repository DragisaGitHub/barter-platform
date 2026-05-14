package com.barterplatform.infrastructure.catalog.repository;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>, JpaSpecificationExecutor<CategoryEntity> {

    Optional<CategoryEntity> findByUuid(UUID uuid);

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndUuidNot(String slug, UUID uuid);

    List<CategoryEntity> findAllByDeletedAtIsNullOrderBySortOrderAscNameAsc();

    @Query("""
            select c.uuid as uuid,
                   c.name as name,
                   c.slug as slug,
                   c.description as description,
                   c.sortOrder as sortOrder,
                   count(i.id) as activeItemCount
            from CategoryEntity c
            join ItemEntity i on i.categoryId = c.id
            where c.deletedAt is null
              and i.deletedAt is null
              and i.status = :activeStatus
            group by c.uuid, c.name, c.slug, c.description, c.sortOrder
            order by count(i.id) desc, c.sortOrder asc, c.name asc
            """)
    List<PopularCategoryProjection> findPopularCategories(@Param("activeStatus") ItemStatus activeStatus,
                                                          Pageable pageable);
}

