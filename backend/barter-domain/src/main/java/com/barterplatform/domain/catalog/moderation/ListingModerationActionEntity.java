package com.barterplatform.domain.catalog.moderation;

import com.barterplatform.common.persistence.AuditableEntity;
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
@Table(name = "listing_moderation_actions")
public class ListingModerationActionEntity extends AuditableEntity {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private ListingModerationActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 64)
    private ListingModerationReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private ListingModerationSourceType sourceType;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;
}

