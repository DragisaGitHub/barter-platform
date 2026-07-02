package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import org.springframework.data.jpa.domain.Specification;

final class CategorySchemaSpecifications {

    private CategorySchemaSpecifications() {
    }

    static Specification<CategorySchemaEntity> deletedAtIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    static Specification<CategorySchemaEntity> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    static Specification<CategorySchemaEntity> statusEquals(CategorySchemaStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}

