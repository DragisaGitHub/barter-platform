package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "saved_searches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_saved_searches_uuid", columnNames = "uuid")
        },
        indexes = {
                @Index(name = "idx_saved_searches_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_saved_searches_user_updated_at", columnList = "user_id, updated_at")
        })
public class SavedSearchEntity extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "criteria_payload", nullable = false, columnDefinition = "text")
    private String criteriaPayload;
}

