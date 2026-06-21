package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminReportsApi;
import com.barterplatform.api.model.AdminAssignReportRequest;
import com.barterplatform.api.model.AdminReportQueueSummaryResponse;
import com.barterplatform.api.model.AdminUpdateReportRequest;
import com.barterplatform.api.model.ReportDetailResponse;
import com.barterplatform.api.model.ReportPagedResponse;
import com.barterplatform.api.model.ReportReasonCode;
import com.barterplatform.api.model.ReportStatus;
import com.barterplatform.api.model.ReportTargetType;
import com.barterplatform.application.moderation.service.ReportService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminReportsController implements AdminReportsApi {

    private final ReportService reportService;

    public AdminReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public ResponseEntity<AdminReportQueueSummaryResponse> getAdminReportQueueSummary() {
        return ResponseEntity.ok(reportService.getQueueSummary());
    }

    @Override
    public ResponseEntity<ReportPagedResponse> listAdminReports(
            Integer page,
            Integer size,
            String sort,
            ReportStatus status,
            ReportTargetType targetType,
            ReportReasonCode reasonCode,
            UUID assignedModeratorUuid,
            Boolean unassignedOnly,
            Boolean staleOnly) {
        return ResponseEntity.ok(reportService.listReports(
                page, size, sort, status, targetType, reasonCode,
                assignedModeratorUuid, unassignedOnly, staleOnly));
    }

    @Override
    public ResponseEntity<ReportDetailResponse> getAdminReportByUuid(UUID reportUuid) {
        return ResponseEntity.ok(reportService.getReport(reportUuid));
    }

    @Override
    public ResponseEntity<ReportDetailResponse> updateAdminReport(
            UUID reportUuid,
            AdminUpdateReportRequest adminUpdateReportRequest) {
        return ResponseEntity.ok(reportService.updateReport(currentUserUuid(), reportUuid, adminUpdateReportRequest));
    }

    public ResponseEntity<ReportDetailResponse> updateAdminReportAssignment(
            UUID reportUuid,
            AdminAssignReportRequest adminAssignReportRequest) {
        return ResponseEntity.ok(reportService.updateReportAssignment(
                currentUserUuid(),
                currentUserIsAdmin(),
                reportUuid,
                adminAssignReportRequest));
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getPrincipal());
        return principal.getUserUuid();
    }

    private boolean currentUserIsAdmin() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getPrincipal());
        return principal.getRoles().contains("ADMIN");
    }
}

