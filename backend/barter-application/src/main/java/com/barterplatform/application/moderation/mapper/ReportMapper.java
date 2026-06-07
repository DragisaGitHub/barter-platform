package com.barterplatform.application.moderation.mapper;

import com.barterplatform.api.model.*;
import com.barterplatform.application.moderation.service.ReportTargetResolver;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.moderation.report.entity.ReportEntity;
import com.barterplatform.domain.moderation.report.entity.ReportHistoryEntryEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
            ReportTargetResolver.TargetSummary targetSummary,
            List<ReportHistoryEntryEntity> historyEntries,
            Map<Long, UserEntity> historyUsersById) {
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
                .history(toHistoryResponses(historyEntries, historyUsersById))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt());
    }

    public ReportTargetType map(com.barterplatform.domain.moderation.report.enums.ReportTargetType targetType) {
        return targetType == null ? null : ReportTargetType.valueOf(targetType.name());
    }

    public com.barterplatform.domain.moderation.report.enums.ReportTargetType map(ReportTargetType targetType) {
        return targetType == null ? null : com.barterplatform.domain.moderation.report.enums.ReportTargetType.valueOf(targetType.name());
    }

    public ReportReasonCode map(com.barterplatform.domain.moderation.report.enums.ReportReasonCode reasonCode) {
        return reasonCode == null ? null : ReportReasonCode.valueOf(reasonCode.name());
    }

    public com.barterplatform.domain.moderation.report.enums.ReportReasonCode map(ReportReasonCode reasonCode) {
        return reasonCode == null ? null : com.barterplatform.domain.moderation.report.enums.ReportReasonCode.valueOf(reasonCode.name());
    }

    public ReportStatus map(com.barterplatform.domain.moderation.report.enums.ReportStatus status) {
        return status == null ? null : ReportStatus.valueOf(status.name());
    }

    public com.barterplatform.domain.moderation.report.enums.ReportStatus map(ReportStatus status) {
        return status == null ? null : com.barterplatform.domain.moderation.report.enums.ReportStatus.valueOf(status.name());
    }

    public List<ReportHistoryEntryResponse> toHistoryResponses(
            List<ReportHistoryEntryEntity> entries,
            Map<Long, UserEntity> usersById) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> toHistoryResponse(entry, usersById))
                .toList();
    }

    private ReportHistoryEntryResponse toHistoryResponse(
            ReportHistoryEntryEntity entry,
            Map<Long, UserEntity> usersById) {
        return new ReportHistoryEntryResponse()
                .uuid(entry.getUuid())
                .eventType(map(entry.getEventType()))
                .actor(toUserSummary(usersById.get(entry.getActorUserId())))
                .previousStatus(map(entry.getPreviousStatus()))
                .newStatus(map(entry.getNewStatus()))
                .previousAssignedModerator(toUserSummary(usersById.get(entry.getPreviousAssignedModeratorUserId())))
                .newAssignedModerator(toUserSummary(usersById.get(entry.getNewAssignedModeratorUserId())))
                .note(entry.getNote())
                .createdAt(entry.getCreatedAt());
    }

    private ReportHistoryEventType map(
            com.barterplatform.domain.moderation.report.enums.ReportHistoryEventType eventType) {
        return eventType == null ? null : ReportHistoryEventType.valueOf(eventType.name());
    }

    private ReportUserSummaryResponse toUserSummary(UserEntity user) {
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

