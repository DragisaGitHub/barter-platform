package com.barterplatform.web.moderation.controller;

import com.barterplatform.api.controller.ReportsApi;
import com.barterplatform.api.model.CreateReportRequest;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.moderation.service.ReportService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportsController implements ReportsApi {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> createReport(CreateReportRequest createReportRequest) {
        MessageResponse response = reportService.createReport(currentUserUuid(), createReportRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        return principal.getUserUuid();
    }
}

