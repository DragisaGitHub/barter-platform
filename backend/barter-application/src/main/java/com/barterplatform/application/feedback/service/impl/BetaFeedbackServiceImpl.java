package com.barterplatform.application.feedback.service.impl;

import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.feedback.service.BetaFeedbackService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import com.barterplatform.domain.feedback.enums.BetaFeedbackCategory;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.feedback.repository.BetaFeedbackRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BetaFeedbackServiceImpl implements BetaFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(BetaFeedbackServiceImpl.class);

    private final UserRepository userRepository;
    private final BetaFeedbackRepository betaFeedbackRepository;

    public BetaFeedbackServiceImpl(UserRepository userRepository, BetaFeedbackRepository betaFeedbackRepository) {
        this.userRepository = userRepository;
        this.betaFeedbackRepository = betaFeedbackRepository;
    }

    @Override
    @Transactional
    public MessageResponse submitFeedback(
            UUID userUuid,
            String category,
            String message,
            String sourcePage) {
        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Authenticated user was not found."));

        BetaFeedbackEntity feedback = BetaFeedbackEntity.create(
                user.getUuid(),
                user.getUsername(),
                user.getEmail(),
                parseCategory(category),
                message,
                sourcePage);
        betaFeedbackRepository.save(feedback);

        log.info(
                "Beta feedback persisted feedbackUuid={} userUuid={} username={} category={} sourcePage={} status={}",
                feedback.getUuid(),
                feedback.getUserUuid(),
                feedback.getUsername(),
                feedback.getCategory(),
                feedback.getSourcePage(),
                feedback.getStatus());

        return new MessageResponse()
                .message("Beta feedback submitted. Thank you for helping improve the first-time experience.");
    }

    private BetaFeedbackCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Beta feedback category is required.");
        }

        try {
            return BetaFeedbackCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Unsupported beta feedback category '%s'.".formatted(category));
        }
    }
}

