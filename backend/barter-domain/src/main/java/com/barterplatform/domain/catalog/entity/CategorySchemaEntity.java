package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
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
@Table(name = "category_schemas")
public class CategorySchemaEntity extends AuditableEntity {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CategorySchemaStatus status;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}

