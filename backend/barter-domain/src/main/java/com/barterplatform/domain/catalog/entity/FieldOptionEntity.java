package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "field_options")
public class FieldOptionEntity extends AuditableEntity {

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "label_sr", length = 160)
    private String labelSr;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}

