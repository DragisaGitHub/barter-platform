package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

final class AdminListingSpecifications {

    private AdminListingSpecifications() {
    }

    static Specification<ItemEntity> deletedAtIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    static Specification<ItemEntity> titleContainsIgnoreCase(String queryText) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + queryText.toLowerCase() + "%");
    }

    static Specification<ItemEntity> ownerIdIn(Iterable<Long> ownerIds) {
        return (root, query, cb) -> {
            if (ownerIds == null) {
                return cb.disjunction();
            }

            CriteriaBuilder.In<Long> inClause = cb.in(root.get("ownerId"));
            boolean hasValues = false;
            for (Long ownerId : ownerIds) {
                inClause.value(ownerId);
                hasValues = true;
            }

            return hasValues ? inClause : cb.disjunction();
        };
    }

    static Specification<ItemEntity> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    static Specification<ItemEntity> statusEquals(ItemStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}

