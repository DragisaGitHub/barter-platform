package com.barterplatform.domain.moderation.report.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.moderation.report.enums.ReportHistoryEventType;
import com.barterplatform.domain.moderation.report.enums.ReportStatus;
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
@Table(name = "report_history_entries")
public class ReportHistoryEntryEntity extends AuditableEntity {

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private ReportHistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private ReportStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 32)
    private ReportStatus newStatus;

    @Column(name = "previous_assigned_moderator_user_id")
    private Long previousAssignedModeratorUserId;

    @Column(name = "new_assigned_moderator_user_id")
    private Long newAssignedModeratorUserId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}