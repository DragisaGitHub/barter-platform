package com.barterplatform.application.moderation.mapper;

import com.barterplatform.api.model.ReportDetailResponse;
import com.barterplatform.api.model.ReportReasonCode;
import com.barterplatform.api.model.ReportStatus;
import com.barterplatform.api.model.ReportSummaryResponse;
import com.barterplatform.api.model.ReportTargetSummaryResponse;
import com.barterplatform.api.model.ReportTargetType;
import com.barterplatform.api.model.ReportUserSummaryResponse;
import com.barterplatform.application.moderation.service.ReportTargetResolver;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.moderation.report.ReportEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportSummaryResponse toSummaryResponse(
            ReportEntity entity,
            UserEntity reporter,
            UserEntity assignedModerator,
            ReportTargetResolver.TargetSummary targetSummary) {
        return new ReportSummaryResponse()
                .uuid(entity.getUuid())
                .targetType(map(entity.getTargetType()))
                .targetUuid(entity.getTargetUuid())
                .reasonCode(map(entity.getReasonCode()))
                .status(map(entity.getStatus()))
                .reporter(toUserSummary(reporter))
                .assignedModerator(toUserSummary(assignedModerator))
                .targetSummary(toTargetSummary(targetSummary))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt());
    }

    public ReportDetailResponse toDetailResponse(
            ReportEntity entity,
            UserEntity reporter,
            UserEntity assignedModerator,
            ReportTargetResolver.TargetSummary targetSummary) {
        return new ReportDetailResponse()
                .uuid(entity.getUuid())
                .targetType(map(entity.getTargetType()))
                .targetUuid(entity.getTargetUuid())
                .reasonCode(map(entity.getReasonCode()))
                .details(entity.getDetails())
                .status(map(entity.getStatus()))
                .reporter(toUserSummary(reporter))
                .assignedModerator(toUserSummary(assignedModerator))
                .resolutionNote(entity.getResolutionNote())
                .targetSummary(toTargetSummary(targetSummary))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt());
    }

    public ReportTargetType map(com.barterplatform.domain.moderation.report.ReportTargetType targetType) {
        return targetType == null ? null : ReportTargetType.valueOf(targetType.name());
    }

    public com.barterplatform.domain.moderation.report.ReportTargetType map(ReportTargetType targetType) {
        return targetType == null ? null : com.barterplatform.domain.moderation.report.ReportTargetType.valueOf(targetType.name());
    }

    public ReportReasonCode map(com.barterplatform.domain.moderation.report.ReportReasonCode reasonCode) {
        return reasonCode == null ? null : ReportReasonCode.valueOf(reasonCode.name());
    }

    public com.barterplatform.domain.moderation.report.ReportReasonCode map(ReportReasonCode reasonCode) {
        return reasonCode == null ? null : com.barterplatform.domain.moderation.report.ReportReasonCode.valueOf(reasonCode.name());
    }

    public ReportStatus map(com.barterplatform.domain.moderation.report.ReportStatus status) {
        return status == null ? null : ReportStatus.valueOf(status.name());
    }

    public com.barterplatform.domain.moderation.report.ReportStatus map(ReportStatus status) {
        return status == null ? null : com.barterplatform.domain.moderation.report.ReportStatus.valueOf(status.name());
    }

    private ReportUserSummaryResponse toUserSummary(@Nullable UserEntity user) {
        if (user == null) {
            return null;
        }
        return new ReportUserSummaryResponse()
                .uuid(user.getUuid())
                .username(user.getUsername());
    }

    private ReportTargetSummaryResponse toTargetSummary(ReportTargetResolver.TargetSummary targetSummary) {
        return new ReportTargetSummaryResponse()
                .title(targetSummary.title())
                .subtitle(targetSummary.subtitle())
                .preview(targetSummary.preview());
    }
}

