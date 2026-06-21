package com.barterplatform.application.moderation.service.impl;

import com.barterplatform.domain.moderation.report.entity.ReportEntity;
import com.barterplatform.domain.moderation.report.enums.ReportReasonCode;
import com.barterplatform.domain.moderation.report.enums.ReportStatus;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;

final class ReportSpecifications {

    private ReportSpecifications() {
    }

    static Specification<ReportEntity> statusEquals(ReportStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    static Specification<ReportEntity> targetTypeEquals(ReportTargetType targetType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("targetType"), targetType);
    }

    static Specification<ReportEntity> reasonCodeEquals(ReportReasonCode reasonCode) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("reasonCode"), reasonCode);
    }

    static Specification<ReportEntity> assignedToModerator(Long moderatorUserId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignedModeratorUserId"), moderatorUserId);
    }

    static Specification<ReportEntity> unassigned() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("assignedModeratorUserId"));
    }

    static Specification<ReportEntity> staleBefore(OffsetDateTime threshold) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), ReportStatus.OPEN),
                criteriaBuilder.lessThan(root.get("createdAt"), threshold));
    }
}

