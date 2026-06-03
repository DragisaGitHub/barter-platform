package com.barterplatform.application.feedback.service.impl;

import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse1;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.feedback.mapper.AdminBetaFeedbackMapper;
import com.barterplatform.application.feedback.service.AdminBetaFeedbackService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import com.barterplatform.domain.feedback.enums.BetaFeedbackStatus;
import com.barterplatform.infrastructure.feedback.repository.BetaFeedbackRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminBetaFeedbackServiceImpl implements AdminBetaFeedbackService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "category", "username");

    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final BetaFeedbackRepository betaFeedbackRepository;
    private final AdminBetaFeedbackMapper adminBetaFeedbackMapper;

    public AdminBetaFeedbackServiceImpl(
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            BetaFeedbackRepository betaFeedbackRepository,
            AdminBetaFeedbackMapper adminBetaFeedbackMapper) {
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.betaFeedbackRepository = betaFeedbackRepository;
        this.adminBetaFeedbackMapper = adminBetaFeedbackMapper;
    }

    @Override
    public AdminBetaFeedbackPagedResponse listFeedback(Integer page, Integer size, String sort, String status) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        BetaFeedbackStatus statusFilter = parseStatus(status, true);
        Page<BetaFeedbackEntity> feedbackPage = statusFilter == null
                ? betaFeedbackRepository.findAll(pageRequest.pageable())
                : betaFeedbackRepository.findAllByStatus(statusFilter, pageRequest.pageable());

        List<AdminBetaFeedbackSummaryResponse1> content = feedbackPage.getContent().stream()
                .map(adminBetaFeedbackMapper::toPagedSummaryResponse)
                .toList();
        return pageResponseMapper.toAdminBetaFeedbackPagedResponse(feedbackPage, content, pageRequest.sort());
    }

    @Override
    @Transactional
    public AdminBetaFeedbackSummaryResponse updateStatus(UUID feedbackUuid, String status) {
        BetaFeedbackStatus targetStatus = parseStatus(status, false);
        if (targetStatus == BetaFeedbackStatus.NEW) {
            throw badRequest("Admin feedback status updates support REVIEWED or RESOLVED only.");
        }

        BetaFeedbackEntity feedback = betaFeedbackRepository.findByUuid(feedbackUuid)
                .orElseThrow(() -> notFound("Beta feedback was not found."));

        try {
            if (targetStatus == BetaFeedbackStatus.REVIEWED) {
                feedback.markReviewed();
            } else {
                feedback.markResolved();
            }
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, ex.getMessage());
        }

        BetaFeedbackEntity saved = betaFeedbackRepository.save(feedback);
        return adminBetaFeedbackMapper.toSummaryResponse(saved);
    }

    private BetaFeedbackStatus parseStatus(String status, boolean allowNull) {
        if (status == null || status.isBlank()) {
            if (allowNull) {
                return null;
            }
            throw badRequest("Beta feedback status is required.");
        }

        try {
            return BetaFeedbackStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("Unsupported beta feedback status '%s'.".formatted(status));
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }
}

