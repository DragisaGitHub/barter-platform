package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Persists a single dynamic category-schema field value entered on an item.
 * Exactly one of the value columns (or {@code optionId} for SINGLE_SELECT) is populated,
 * depending on the owning {@link CategorySchemaFieldEntity#getFieldType()}.
 * MULTI_SELECT selections are stored separately via {@link ItemFieldValueOptionEntity}.
 */
@Getter
@Setter
@Entity
@Table(name = "item_field_values")
public class ItemFieldValueEntity extends AuditableEntity {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "schema_field_id", nullable = false)
    private Long schemaFieldId;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    @Column(name = "value_number", precision = 20, scale = 6)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private LocalDate valueDate;

    /** Selected option for SINGLE_SELECT fields. Null otherwise. */
    @Column(name = "option_id")
    private Long optionId;
}

