package com.barterplatform.domain.feedback.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.feedback.enums.BetaFeedbackCategory;
import com.barterplatform.domain.feedback.enums.BetaFeedbackStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "beta_feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BetaFeedbackEntity extends AuditableEntity {

    private static final int USERNAME_MAX_LENGTH = 80;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int MESSAGE_MAX_LENGTH = 2000;
    private static final int SOURCE_PAGE_MAX_LENGTH = 255;

    @Column(name = "user_uuid", nullable = false, updatable = false)
    private UUID userUuid;

    @Column(name = "username", nullable = false, length = USERNAME_MAX_LENGTH, updatable = false)
    private String username;

    @Column(name = "email", length = EMAIL_MAX_LENGTH, updatable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64, updatable = false)
    private BetaFeedbackCategory category;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String message;

    @Column(name = "source_page", length = SOURCE_PAGE_MAX_LENGTH, updatable = false)
    private String sourcePage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BetaFeedbackStatus status;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public static BetaFeedbackEntity create(
            UUID userUuid,
            String username,
            String email,
            BetaFeedbackCategory category,
            String message,
            String sourcePage) {
        BetaFeedbackEntity entity = new BetaFeedbackEntity();
        entity.userUuid = requireUserUuid(userUuid);
        entity.username = normalizeRequired(username, "username", USERNAME_MAX_LENGTH);
        entity.email = normalizeOptional(email, "email", EMAIL_MAX_LENGTH);
        entity.category = requireCategory(category);
        entity.message = normalizeRequired(message, "message", MESSAGE_MAX_LENGTH);
        entity.sourcePage = normalizeOptional(sourcePage, "sourcePage", SOURCE_PAGE_MAX_LENGTH);
        entity.status = BetaFeedbackStatus.NEW;
        return entity;
    }

    public void markReviewed() {
        if (status == BetaFeedbackStatus.REVIEWED) {
            return;
        }
        if (status == BetaFeedbackStatus.RESOLVED) {
            throw new IllegalStateException("Resolved beta feedback cannot move back to REVIEWED.");
        }

        status = BetaFeedbackStatus.REVIEWED;
        if (reviewedAt == null) {
            reviewedAt = OffsetDateTime.now();
        }
    }

    public void markResolved() {
        if (status == BetaFeedbackStatus.RESOLVED) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (reviewedAt == null) {
            reviewedAt = now;
        }
        resolvedAt = now;
        status = BetaFeedbackStatus.RESOLVED;
    }

    private static UUID requireUserUuid(UUID userUuid) {
        if (userUuid == null) {
            throw new IllegalArgumentException("userUuid is required.");
        }
        return userUuid;
    }

    private static BetaFeedbackCategory requireCategory(BetaFeedbackCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("category is required.");
        }
        return category;
    }

    private static String normalizeRequired(String value, String field, int maxLength) {
        String normalized = normalizeOptional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds the maximum length of " + maxLength + ".");
        }
        return trimmed;
    }
}

