package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminOperationsApi;
import com.barterplatform.api.model.AdminOperationsOverviewResponse;
import com.barterplatform.web.admin.service.AdminOperationsOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController implements AdminOperationsApi {

    private final AdminOperationsOverviewService adminOperationsOverviewService;

    public AdminOperationsController(AdminOperationsOverviewService adminOperationsOverviewService) {
        this.adminOperationsOverviewService = adminOperationsOverviewService;
    }

    @Override
    public ResponseEntity<AdminOperationsOverviewResponse> getAdminOperationsOverview() {
        return ResponseEntity.ok(adminOperationsOverviewService.getOverview());
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

