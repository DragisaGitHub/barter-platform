package com.barterplatform.domain.notification.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class NotificationEntity extends AuditableEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "reference_uuid")
    private UUID referenceUuid;

    @Column(name = "reference_type", length = 64)
    private String referenceType;

    // Field named isRead: Lombok generates getter isRead() and setter setRead(boolean).
    // JPQL uses field name "isRead"; MapStruct sees property "read" via the boolean getter convention.
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    // --- Domain method ---

    /**
     * Marks this notification as read. Idempotent — has no effect if already read.
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = OffsetDateTime.now();
        }
    }
}

