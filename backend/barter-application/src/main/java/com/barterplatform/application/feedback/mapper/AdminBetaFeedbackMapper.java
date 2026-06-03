package com.barterplatform.application.feedback.mapper;

import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse1;
import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import org.springframework.stereotype.Component;

@Component
public class AdminBetaFeedbackMapper {

	public AdminBetaFeedbackSummaryResponse1 toPagedSummaryResponse(BetaFeedbackEntity entity) {
		return new AdminBetaFeedbackSummaryResponse1()
				.uuid(entity.getUuid())
				.userUuid(entity.getUserUuid())
				.username(entity.getUsername())
				.email(entity.getEmail())
				.category(com.barterplatform.api.model.BetaFeedbackCategory.fromValue(entity.getCategory().name()))
				.message(entity.getMessage())
				.sourcePage(entity.getSourcePage())
				.status(com.barterplatform.api.model.BetaFeedbackStatus.fromValue(entity.getStatus().name()))
				.createdAt(entity.getCreatedAt())
				.reviewedAt(entity.getReviewedAt())
				.resolvedAt(entity.getResolvedAt());
	}

	public AdminBetaFeedbackSummaryResponse toSummaryResponse(BetaFeedbackEntity entity) {
		return new AdminBetaFeedbackSummaryResponse()
				.uuid(entity.getUuid())
				.userUuid(entity.getUserUuid())
				.username(entity.getUsername())
				.email(entity.getEmail())
				.category(com.barterplatform.api.model.BetaFeedbackCategory.fromValue(entity.getCategory().name()))
				.message(entity.getMessage())
				.sourcePage(entity.getSourcePage())
				.status(com.barterplatform.api.model.BetaFeedbackStatus.fromValue(entity.getStatus().name()))
				.createdAt(entity.getCreatedAt())
				.reviewedAt(entity.getReviewedAt())
				.resolvedAt(entity.getResolvedAt());
	}
}

