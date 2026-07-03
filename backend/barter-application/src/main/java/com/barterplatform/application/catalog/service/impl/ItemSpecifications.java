package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemFieldValueEntity;
import com.barterplatform.domain.catalog.entity.ItemFieldValueOptionEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    static Specification<ItemEntity> locationContainsIgnoreCase(String location) {
        return (root, query, cb) -> {
            String pattern = "%" + location.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("exchangeLocation")), pattern),
                    cb.like(cb.lower(root.get("exchangeCity")), pattern),
                    cb.like(cb.lower(root.get("exchangeArea")), pattern)
            );
        };
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

    // ── Dynamic category-schema field-value filters (Marketplace Schema Engine, Phase 6) ──────

    /** Items with a TEXT field value on {@code fieldId} that case-insensitively contains {@code value}. */
    static Specification<ItemEntity> fieldTextContains(Long fieldId, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            sub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    cb.like(cb.lower(v.get("valueText")), "%" + value.toLowerCase() + "%")));
            return root.get("id").in(sub);
        };
    }

    /** Items with a NUMBER field value on {@code fieldId} exactly equal to {@code value}. */
    static Specification<ItemEntity> fieldNumberEquals(Long fieldId, BigDecimal value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            sub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    cb.equal(v.get("valueNumber"), value)));
            return root.get("id").in(sub);
        };
    }

    /** Items with a NUMBER field value on {@code fieldId} within [min, max] (either bound optional). */
    static Specification<ItemEntity> fieldNumberBetween(Long fieldId, BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            Predicate predicate = cb.equal(v.get("schemaFieldId"), fieldId);
            if (min != null) {
                predicate = cb.and(predicate, cb.ge(v.get("valueNumber"), min));
            }
            if (max != null) {
                predicate = cb.and(predicate, cb.le(v.get("valueNumber"), max));
            }
            sub.select(v.get("itemId")).where(predicate);
            return root.get("id").in(sub);
        };
    }

    /** Items with a BOOLEAN field value on {@code fieldId} equal to {@code value}. */
    static Specification<ItemEntity> fieldBooleanEquals(Long fieldId, Boolean value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            sub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    cb.equal(v.get("valueBoolean"), value)));
            return root.get("id").in(sub);
        };
    }

    /** Items with a DATE field value on {@code fieldId} exactly equal to {@code value}. */
    static Specification<ItemEntity> fieldDateEquals(Long fieldId, LocalDate value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            sub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    cb.equal(v.get("valueDate"), value)));
            return root.get("id").in(sub);
        };
    }

    /** Items with a DATE field value on {@code fieldId} within [from, to] (either bound optional). */
    static Specification<ItemEntity> fieldDateBetween(Long fieldId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            Predicate predicate = cb.equal(v.get("schemaFieldId"), fieldId);
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(v.get("valueDate"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(v.get("valueDate"), to));
            }
            sub.select(v.get("itemId")).where(predicate);
            return root.get("id").in(sub);
        };
    }

    /** Items whose SINGLE_SELECT field value on {@code fieldId} is one of {@code optionIds}. */
    static Specification<ItemEntity> fieldSingleOptionIn(Long fieldId, List<Long> optionIds) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = sub.from(ItemFieldValueEntity.class);
            sub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    v.get("optionId").in(optionIds)));
            return root.get("id").in(sub);
        };
    }

    /**
     * Items whose MULTI_SELECT field value on {@code fieldId} has at least one selected option
     * among {@code optionIds}.
     */
    static Specification<ItemEntity> fieldMultiOptionIn(Long fieldId, List<Long> optionIds) {
        return (root, query, cb) -> {
            Subquery<Long> valueIdsSub = query.subquery(Long.class);
            Root<ItemFieldValueOptionEntity> o = valueIdsSub.from(ItemFieldValueOptionEntity.class);
            valueIdsSub.select(o.get("itemFieldValueId")).where(o.get("fieldOptionId").in(optionIds));

            Subquery<Long> itemIdsSub = query.subquery(Long.class);
            Root<ItemFieldValueEntity> v = itemIdsSub.from(ItemFieldValueEntity.class);
            itemIdsSub.select(v.get("itemId")).where(cb.and(
                    cb.equal(v.get("schemaFieldId"), fieldId),
                    v.get("id").in(valueIdsSub)));

            return root.get("id").in(itemIdsSub);
        };
    }
}

