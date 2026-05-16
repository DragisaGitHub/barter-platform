package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminReviewsApi;
import com.barterplatform.api.model.AdminTradeReviewPagedResponse;
import com.barterplatform.api.model.TradeReviewNegativeReason;
import com.barterplatform.api.model.TradeReviewRating;
import com.barterplatform.application.reputation.service.AdminTradeReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewsController implements AdminReviewsApi {

    private final AdminTradeReviewService adminTradeReviewService;

    public AdminReviewsController(AdminTradeReviewService adminTradeReviewService) {
        this.adminTradeReviewService = adminTradeReviewService;
    }

    @Override
    public ResponseEntity<AdminTradeReviewPagedResponse> listAdminReviews(
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating,
            TradeReviewNegativeReason negativeReason,
            String reviewedUserQuery,
            String reviewerUserQuery) {
        return ResponseEntity.ok(adminTradeReviewService.listReviews(
                page,
                size,
                sort,
                rating == null ? null : com.barterplatform.domain.reputation.enums.TradeReviewRating.valueOf(rating.name()),
                negativeReason == null ? null : com.barterplatform.domain.reputation.enums.TradeReviewNegativeReason.valueOf(negativeReason.name()),
                reviewedUserQuery,
                reviewerUserQuery));
    }
}

