package com.barterplatform.application.feedback.service;

import com.barterplatform.api.model.MessageResponse;
import java.util.UUID;

public interface BetaFeedbackService {

    MessageResponse submitFeedback(
            UUID userUuid,
            String category,
            String message,
            String sourcePage);
}

