package com.barterplatform.domain.identity.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.identity.enums.RoleCode;
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
@Table(name = "roles")
public class RoleEntity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 80, unique = true)
    private RoleCode code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description")
    private String description;
}

