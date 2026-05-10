package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
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
}

