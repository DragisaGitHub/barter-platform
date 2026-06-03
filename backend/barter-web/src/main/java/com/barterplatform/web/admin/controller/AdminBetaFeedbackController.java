package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminFeedbackApi;
import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import com.barterplatform.api.model.AdminUpdateBetaFeedbackStatusRequest;
import com.barterplatform.api.model.BetaFeedbackStatus;
import com.barterplatform.application.feedback.service.AdminBetaFeedbackService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminBetaFeedbackController implements AdminFeedbackApi {

    private final AdminBetaFeedbackService adminBetaFeedbackService;

    public AdminBetaFeedbackController(AdminBetaFeedbackService adminBetaFeedbackService) {
        this.adminBetaFeedbackService = adminBetaFeedbackService;
    }

    @Override
    public ResponseEntity<AdminBetaFeedbackPagedResponse> listAdminBetaFeedback(
            Integer page,
            Integer size,
            String sort,
            BetaFeedbackStatus status) {
        return ResponseEntity.ok(adminBetaFeedbackService.listFeedback(
                page,
                size,
                sort,
                status == null ? null : status.getValue()));
    }

    @Override
    public ResponseEntity<AdminBetaFeedbackSummaryResponse> updateAdminBetaFeedbackStatus(
            UUID feedbackUuid,
            @Valid @RequestBody AdminUpdateBetaFeedbackStatusRequest adminUpdateBetaFeedbackStatusRequest) {
        return ResponseEntity.ok(adminBetaFeedbackService.updateStatus(
                feedbackUuid,
                adminUpdateBetaFeedbackStatusRequest.getStatus().getValue()));
    }
}

