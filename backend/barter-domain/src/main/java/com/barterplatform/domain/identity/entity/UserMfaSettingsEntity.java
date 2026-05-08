package com.barterplatform.domain.identity.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "user_mfa_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_mfa_settings_user_id",
                columnNames = "user_id"
        )
)
public class UserMfaSettingsEntity extends AuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "secret_encrypted", nullable = false, columnDefinition = "TEXT")
    private String secretEncrypted;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "configured_at")
    private OffsetDateTime configuredAt;
}

