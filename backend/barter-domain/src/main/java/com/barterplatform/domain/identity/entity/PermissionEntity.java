package com.barterplatform.domain.identity.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.identity.enums.PermissionCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class PermissionEntity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 120, unique = true)
    private PermissionCode code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description")
    private String description;
}

