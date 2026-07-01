package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminOperationsApi;
import com.barterplatform.api.model.AdminOperationsBackupsResponse;
import com.barterplatform.api.model.AdminOperationsDeploymentsResponse;
import com.barterplatform.api.model.AdminOperationsOverviewResponse;
import com.barterplatform.web.admin.service.AdminOperationsBackupsService;
import com.barterplatform.web.admin.service.AdminOperationsDeploymentsService;
import com.barterplatform.web.admin.service.AdminOperationsOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController implements AdminOperationsApi {

    private final AdminOperationsOverviewService adminOperationsOverviewService;
    private final AdminOperationsBackupsService adminOperationsBackupsService;
    private final AdminOperationsDeploymentsService adminOperationsDeploymentsService;

    public AdminOperationsController(
            AdminOperationsOverviewService adminOperationsOverviewService,
            AdminOperationsBackupsService adminOperationsBackupsService,
            AdminOperationsDeploymentsService adminOperationsDeploymentsService) {
        this.adminOperationsOverviewService = adminOperationsOverviewService;
        this.adminOperationsBackupsService = adminOperationsBackupsService;
        this.adminOperationsDeploymentsService = adminOperationsDeploymentsService;
    }

    @Override
    public ResponseEntity<AdminOperationsOverviewResponse> getAdminOperationsOverview() {
        return ResponseEntity.ok(adminOperationsOverviewService.getOverview());
    }

    @Override
    public ResponseEntity<AdminOperationsBackupsResponse> getAdminOperationsBackups() {
        return ResponseEntity.ok(adminOperationsBackupsService.getBackups());
    }

    @Override
    public ResponseEntity<AdminOperationsDeploymentsResponse> getAdminOperationsDeployments() {
        return ResponseEntity.ok(adminOperationsDeploymentsService.getDeployments());
    }

    /**
     * POST /admin/system/sentry-test
     *
     * Deliberately throws a RuntimeException so the global exception handler
     * captures and forwards it to Sentry. Used by admins to verify that
     * backend error tracking is active and correctly configured.
     * Always results in HTTP 500 — that is the intended outcome.
     */
    @Override
    public ResponseEntity<Void> triggerAdminSystemSentryTest() {
        throw new RuntimeException("Manual backend Sentry diagnostics test");
    }
}

