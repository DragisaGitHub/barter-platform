package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import org.springframework.data.jpa.domain.Specification;

final class CategorySpecifications {

    private CategorySpecifications() {
    }

    static Specification<CategoryEntity> deletedAtIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    static Specification<CategoryEntity> nameOrSlugContainsIgnoreCase(String q) {
        String likeValue = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), likeValue),
                cb.like(cb.lower(root.get("slug")), likeValue)
        );
    }
}

