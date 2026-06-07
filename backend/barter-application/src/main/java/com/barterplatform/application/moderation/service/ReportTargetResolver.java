package com.barterplatform.application.moderation.service;

import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import java.util.UUID;

public interface ReportTargetResolver {

    void validateForCreate(ReportTargetType targetType, UUID targetUuid, UserEntity reporter);

    TargetSummary resolveSummary(ReportTargetType targetType, UUID targetUuid);

    record TargetSummary(String title, String subtitle, String preview) {
    }
}

