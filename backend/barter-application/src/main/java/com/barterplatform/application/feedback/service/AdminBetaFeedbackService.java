package com.barterplatform.application.feedback.service;

import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import java.util.UUID;

public interface AdminBetaFeedbackService {

    AdminBetaFeedbackPagedResponse listFeedback(
            Integer page,
            Integer size,
            String sort,
            String status);

    AdminBetaFeedbackSummaryResponse updateStatus(UUID feedbackUuid, String status);
}

