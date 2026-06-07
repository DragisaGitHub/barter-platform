package com.barterplatform.domain.moderation.report.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.moderation.report.enums.ReportReasonCode;
import com.barterplatform.domain.moderation.report.enums.ReportStatus;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class ReportEntity extends AuditableEntity {

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private ReportTargetType targetType;

    @Column(name = "target_uuid", nullable = false)
    private UUID targetUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 64)
    private ReportReasonCode reasonCode;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReportStatus status;

    @Column(name = "assigned_moderator_user_id")
    private Long assignedModeratorUserId;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}

