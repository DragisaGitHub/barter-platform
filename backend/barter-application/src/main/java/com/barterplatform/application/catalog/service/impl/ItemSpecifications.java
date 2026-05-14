package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for building dynamic item queries.
 */
final class ItemSpecifications {

    private ItemSpecifications() {
    }

    static Specification<ItemEntity> deletedAtIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    static Specification<ItemEntity> statusEquals(ItemStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<ItemEntity> statusNotEqual(ItemStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    static Specification<ItemEntity> conditionEquals(ItemCondition condition) {
        return (root, query, cb) -> cb.equal(root.get("condition"), condition);
    }

    static Specification<ItemEntity> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    static Specification<ItemEntity> titleContainsIgnoreCase(String q) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")),
                "%" + q.toLowerCase() + "%"
        );
    }

    /**
     * Returns items that have at least one tag whose internal ID is in {@code tagIds}.
     * Implemented as an EXISTS subquery on the item_tags join table.
     */
    static Specification<ItemEntity> hasAnyTagId(List<Long> tagIds) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemTagEntity> it = sub.from(ItemTagEntity.class);
            sub.select(it.get("id").get("itemId"))
               .where(it.get("id").get("tagId").in(tagIds));
            return root.get("id").in(sub);
        };
    }
}

