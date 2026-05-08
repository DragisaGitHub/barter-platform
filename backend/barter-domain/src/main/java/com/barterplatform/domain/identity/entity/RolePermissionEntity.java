package com.barterplatform.domain.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {

    @EmbeddedId
    private RolePermissionId id;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;
}

