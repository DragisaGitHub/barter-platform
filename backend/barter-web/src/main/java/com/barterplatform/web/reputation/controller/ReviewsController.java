package com.barterplatform.web.reputation.controller;

import com.barterplatform.api.controller.ReviewsApi;
import com.barterplatform.api.model.ReviewDirection;
import com.barterplatform.api.model.TradeReviewRating;
import com.barterplatform.api.model.UserTradeReviewPagedResponse;
import com.barterplatform.application.reputation.service.TradeReviewService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewsController implements ReviewsApi {

    private final TradeReviewService tradeReviewService;

    public ReviewsController(TradeReviewService tradeReviewService) {
        this.tradeReviewService = tradeReviewService;
    }

    @Override
    public ResponseEntity<UserTradeReviewPagedResponse> listReviews(
            ReviewDirection direction,
            Integer page,
            Integer size,
            String sort,
            TradeReviewRating rating) {
        return ResponseEntity.ok(tradeReviewService.listReviews(
                currentUserUuid(),
                direction == null ? null : TradeReviewService.Direction.valueOf(direction.name()),
                page,
                size,
                sort,
                rating == null ? null : com.barterplatform.domain.reputation.enums.TradeReviewRating.valueOf(rating.name())));
    }

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;
        return principal.getUserUuid();
    }
}

