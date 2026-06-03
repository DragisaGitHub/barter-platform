package com.barterplatform.web.feedback.controller;

import com.barterplatform.api.controller.FeedbackApi;
import com.barterplatform.api.model.BetaFeedbackRequest;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.feedback.service.BetaFeedbackService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BetaFeedbackController implements FeedbackApi {

    private final BetaFeedbackService betaFeedbackService;

    public BetaFeedbackController(BetaFeedbackService betaFeedbackService) {
        this.betaFeedbackService = betaFeedbackService;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> submitBetaFeedback(@Valid @RequestBody BetaFeedbackRequest betaFeedbackRequest) {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;

        MessageResponse response = betaFeedbackService.submitFeedback(
                principal.getUserUuid(),
                betaFeedbackRequest.getCategory().getValue().trim(),
                betaFeedbackRequest.getMessage().trim(),
                betaFeedbackRequest.getSourcePage() == null ? null : betaFeedbackRequest.getSourcePage().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

