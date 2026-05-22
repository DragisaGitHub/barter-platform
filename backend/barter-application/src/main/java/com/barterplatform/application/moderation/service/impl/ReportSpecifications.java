package com.barterplatform.application.moderation.service.impl;

import com.barterplatform.domain.moderation.report.ReportEntity;
import com.barterplatform.domain.moderation.report.ReportReasonCode;
import com.barterplatform.domain.moderation.report.ReportStatus;
import com.barterplatform.domain.moderation.report.ReportTargetType;
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
}

