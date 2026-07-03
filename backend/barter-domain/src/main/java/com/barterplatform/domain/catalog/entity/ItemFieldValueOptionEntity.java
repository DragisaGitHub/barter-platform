package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * One selected option for a MULTI_SELECT {@link ItemFieldValueEntity}.
 * A single item field value may have zero or more selected options.
 */
@Getter
@Setter
@Entity
@Table(name = "item_field_value_options")
public class ItemFieldValueOptionEntity extends BaseEntity {

    @Column(name = "item_field_value_id", nullable = false)
    private Long itemFieldValueId;

    @Column(name = "field_option_id", nullable = false)
    private Long fieldOptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    private void prePersistCreatedAt() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

