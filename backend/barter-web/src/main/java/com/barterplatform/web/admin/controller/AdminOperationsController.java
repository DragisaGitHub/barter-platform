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
}

