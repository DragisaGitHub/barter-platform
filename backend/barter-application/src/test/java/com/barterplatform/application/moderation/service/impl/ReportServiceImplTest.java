package com.barterplatform.application.moderation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.*;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.moderation.mapper.ReportMapper;
import com.barterplatform.application.moderation.service.ReportTargetResolver;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.moderation.report.entity.ReportEntity;
import com.barterplatform.domain.moderation.report.entity.ReportHistoryEntryEntity;
import com.barterplatform.domain.moderation.report.enums.ReportReasonCode;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.moderation.repository.ReportHistoryEntryRepository;
import com.barterplatform.infrastructure.moderation.repository.ReportRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportTargetResolver reportTargetResolver;
    @Mock private ReportHistoryEntryRepository reportHistoryEntryRepository;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                reportRepository,
                reportHistoryEntryRepository,
                userRepository,
                reportTargetResolver,
                new ReportMapper(),
                new PageRequestFactory(),
                new PageResponseMapper());
    }

    @Test
    void createReportWritesReportCreatedHistoryEntry() {
        UUID reporterUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        UserEntity reporter = user(20L, reporterUuid, "reporter");

        when(userRepository.findByUuid(reporterUuid)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporterUserIdAndTargetTypeAndTargetUuidAndStatusIn(
                any(), any(), any(), any()))
                .thenReturn(false);
        when(reportRepository.save(any(ReportEntity.class))).thenAnswer(invocation -> {
            ReportEntity saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.setUuid(UUID.randomUUID());
            saved.setCreatedAt(OffsetDateTime.now());
            saved.setUpdatedAt(OffsetDateTime.now());
            return saved;
        });

        service.createReport(
                reporterUuid,
                new CreateReportRequest()
                        .targetType(com.barterplatform.api.model.ReportTargetType.ITEM)
                        .targetUuid(targetUuid)
                        .reasonCode(com.barterplatform.api.model.ReportReasonCode.PROHIBITED_ITEM)
                        .details("Unsafe listing"));

        ArgumentCaptor<ReportHistoryEntryEntity> captor =
                ArgumentCaptor.forClass(ReportHistoryEntryEntity.class);

        verify(reportHistoryEntryRepository).save(captor.capture());

        ReportHistoryEntryEntity historyEntry = captor.getValue();
        assertEquals(50L, historyEntry.getReportId());
        assertEquals(reporter.getId(), historyEntry.getActorUserId());
        assertEquals(
                com.barterplatform.domain.moderation.report.enums.ReportHistoryEventType.REPORT_CREATED,
                historyEntry.getEventType());
    }

    @Test
    void updateReportWritesStatusChangedHistoryEntry() {
        UUID actorUuid = UUID.randomUUID();
        UUID reportUuid = UUID.randomUUID();

        UserEntity actor = user(10L, actorUuid, "moderator");
        UserEntity reporter = user(20L, UUID.randomUUID(), "reporter");

        ReportEntity report = report(
                reportUuid,
                reporter.getId(),
                ReportTargetType.ITEM,
                ReportReasonCode.PROHIBITED_ITEM);

        report.setStatus(com.barterplatform.domain.moderation.report.enums.ReportStatus.OPEN);

        when(userRepository.findByUuid(actorUuid)).thenReturn(Optional.of(actor));
        when(reportRepository.findByUuid(reportUuid)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ReportEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(anyIterable()))
                .thenReturn(List.of(reporter));
        when(reportTargetResolver.resolveSummary(
                ReportTargetType.ITEM,
                report.getTargetUuid()))
                .thenReturn(new ReportTargetResolver.TargetSummary(
                        "Unsafe item",
                        "Listing",
                        "Preview"));

        service.updateReport(
                actorUuid,
                reportUuid,
                new AdminUpdateReportRequest()
                        .status(ReportStatus.IN_REVIEW));

        ArgumentCaptor<ReportHistoryEntryEntity> captor =
                ArgumentCaptor.forClass(ReportHistoryEntryEntity.class);

        verify(reportHistoryEntryRepository).save(captor.capture());

        ReportHistoryEntryEntity historyEntry = captor.getValue();

        assertEquals(
                com.barterplatform.domain.moderation.report.enums.ReportHistoryEventType.STATUS_CHANGED,
                historyEntry.getEventType());

        assertEquals(
                com.barterplatform.domain.moderation.report.enums.ReportStatus.OPEN,
                historyEntry.getPreviousStatus());

        assertEquals(
                com.barterplatform.domain.moderation.report.enums.ReportStatus.IN_REVIEW,
                historyEntry.getNewStatus());
    }

    @Test
    void updateReportDoesNotReassignActingModeratorWhenResolvingReport() {
        UUID actorUuid = UUID.randomUUID();
        UUID reportUuid = UUID.randomUUID();

        UserEntity actor = user(10L, actorUuid, "moderator-two");
        UserEntity reporter = user(20L, UUID.randomUUID(), "reporter");
        ReportEntity report = report(reportUuid, reporter.getId(), ReportTargetType.ITEM, ReportReasonCode.PROHIBITED_ITEM);
        report.setStatus(com.barterplatform.domain.moderation.report.enums.ReportStatus.IN_REVIEW);
        report.setAssignedModeratorUserId(99L);
        UserEntity assignedModerator = user(99L, UUID.randomUUID(), "moderator-one");

        when(userRepository.findByUuid(actorUuid)).thenReturn(Optional.of(actor));
        when(reportRepository.findByUuid(reportUuid)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(ReportEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(reporter, assignedModerator));
        when(reportTargetResolver.resolveSummary(ReportTargetType.ITEM, report.getTargetUuid()))
                .thenReturn(new ReportTargetResolver.TargetSummary("Unsafe item", "Listing by seller", "Flagged details"));

        ReportDetailResponse response = service.updateReport(
                actorUuid,
                reportUuid,
                new AdminUpdateReportRequest()
                        .status(ReportStatus.RESOLVED)
                        .resolutionNote("Confirmed unsafe listing and completed follow-up."));

        assertEquals(ReportStatus.RESOLVED, response.getStatus());
        assertEquals("Confirmed unsafe listing and completed follow-up.", response.getResolutionNote());
        assert response.getAssignedModerator() != null;
        assertEquals(assignedModerator.getUuid(), response.getAssignedModerator().getUuid());
        assertEquals(assignedModerator.getId(), report.getAssignedModeratorUserId());
        assertNotNull(report.getResolvedAt());
    }

    @Test
    void updateReportRequiresResolutionNoteForTerminalStatuses() {
        UUID actorUuid = UUID.randomUUID();
        UUID reportUuid = UUID.randomUUID();

        when(userRepository.findByUuid(actorUuid)).thenReturn(Optional.of(user(10L, actorUuid, "moderator")));
        when(reportRepository.findByUuid(reportUuid)).thenReturn(Optional.of(
                report(reportUuid, 20L, ReportTargetType.USER, ReportReasonCode.HARASSMENT)));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.updateReport(actorUuid, reportUuid, new AdminUpdateReportRequest().status(ReportStatus.RESOLVED)));

        assertEquals(400, exception.getStatus().value());
        verify(reportRepository, never()).save(any(ReportEntity.class));
    }

    @Test
    void getQueueSummaryReturnsOpenInReviewAndStaleOpenCounts() {
        when(reportRepository.countByStatus(com.barterplatform.domain.moderation.report.enums.ReportStatus.OPEN)).thenReturn(7L);
        when(reportRepository.countByStatus(com.barterplatform.domain.moderation.report.enums.ReportStatus.IN_REVIEW)).thenReturn(3L);
        when(reportRepository.countByStatusAndCreatedAtBefore(
                any(com.barterplatform.domain.moderation.report.enums.ReportStatus.class),
                any(OffsetDateTime.class)))
                .thenReturn(2L);

        AdminReportQueueSummaryResponse response = service.getQueueSummary();

        assertEquals(7L, response.getOpenCount());
        assertEquals(3L, response.getInReviewCount());
        assertEquals(2L, response.getStaleOpenCount());
        assertEquals(48, response.getStaleThresholdHours());
    }

    private UserEntity user(Long id, UUID uuid, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUuid(uuid);
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setCreatedAt(OffsetDateTime.now().minusDays(5));
        return user;
    }

    private ReportEntity report(UUID uuid, Long reporterUserId, ReportTargetType targetType, ReportReasonCode reasonCode) {
        ReportEntity report = new ReportEntity();
        report.setId(50L);
        report.setUuid(uuid);
        report.setReporterUserId(reporterUserId);
        report.setTargetType(targetType);
        report.setTargetUuid(UUID.randomUUID());
        report.setReasonCode(reasonCode);
        report.setStatus(com.barterplatform.domain.moderation.report.enums.ReportStatus.OPEN);
        report.setCreatedAt(OffsetDateTime.now().minusDays(3));
        report.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        return report;
    }
}

