package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category_schema_fields")
public class CategorySchemaFieldEntity extends AuditableEntity {

    @Column(name = "schema_id", nullable = false)
    private Long schemaId;

    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "label_sr", length = 160)
    private String labelSr;

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 30)
    private CategorySchemaFieldType fieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "searchable", nullable = false)
    private boolean searchable;

    @Column(name = "filterable", nullable = false)
    private boolean filterable;

    @Column(name = "sortable", nullable = false)
    private boolean sortable;

    @Column(name = "unit", length = 40)
    private String unit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "validation_json", columnDefinition = "TEXT")
    private String validationJson;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}

