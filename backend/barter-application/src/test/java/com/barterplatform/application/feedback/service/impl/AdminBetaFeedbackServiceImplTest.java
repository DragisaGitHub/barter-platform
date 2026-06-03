package com.barterplatform.application.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.feedback.mapper.AdminBetaFeedbackMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import com.barterplatform.domain.feedback.enums.BetaFeedbackCategory;
import com.barterplatform.domain.feedback.enums.BetaFeedbackStatus;
import com.barterplatform.infrastructure.feedback.repository.BetaFeedbackRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminBetaFeedbackServiceImplTest {

    @Mock
    private BetaFeedbackRepository betaFeedbackRepository;

    private AdminBetaFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminBetaFeedbackServiceImpl(
                new PageRequestFactory(),
                new PageResponseMapper(),
                betaFeedbackRepository,
                new AdminBetaFeedbackMapper());
    }

    @Test
    void listFeedbackFiltersByStatus() {
        BetaFeedbackEntity feedback = feedback(
                UUID.randomUUID(),
                BetaFeedbackStatus.NEW,
                BetaFeedbackCategory.ONBOARDING,
                "Feedback message with enough detail to persist.");
        when(betaFeedbackRepository.findAllByStatus(any(BetaFeedbackStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(feedback)));

        AdminBetaFeedbackPagedResponse response = service.listFeedback(0, 20, "createdAt,desc", "NEW");

        assertEquals(1, response.getContent().size());
        assertEquals("alex99", response.getContent().get(0).getUsername());
        assertEquals(com.barterplatform.api.model.BetaFeedbackStatus.NEW, response.getContent().get(0).getStatus());
    }

    @Test
    void updateStatusMarksFeedbackReviewed() {
        UUID feedbackUuid = UUID.randomUUID();
        BetaFeedbackEntity feedback = feedback(
                feedbackUuid,
                BetaFeedbackStatus.NEW,
                BetaFeedbackCategory.LISTINGS,
                "Review the listings onboarding guidance please.");
        when(betaFeedbackRepository.findByUuid(feedbackUuid)).thenReturn(Optional.of(feedback));
        when(betaFeedbackRepository.save(any(BetaFeedbackEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateStatus(feedbackUuid, "REVIEWED");

        assertEquals(com.barterplatform.api.model.BetaFeedbackStatus.REVIEWED, response.getStatus());
        assertNotNull(response.getReviewedAt());
        verify(betaFeedbackRepository).save(feedback);
    }

    @Test
    void updateStatusMarksFeedbackResolved() {
        UUID feedbackUuid = UUID.randomUUID();
        BetaFeedbackEntity feedback = feedback(
                feedbackUuid,
                BetaFeedbackStatus.NEW,
                BetaFeedbackCategory.OFFERS,
                "Offer flow needs a clearer next step on the confirmation screen.");
        when(betaFeedbackRepository.findByUuid(feedbackUuid)).thenReturn(Optional.of(feedback));
        when(betaFeedbackRepository.save(any(BetaFeedbackEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateStatus(feedbackUuid, "RESOLVED");

        assertEquals(com.barterplatform.api.model.BetaFeedbackStatus.RESOLVED, response.getStatus());
        assertNotNull(response.getReviewedAt());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void updateStatusRejectsNewTargetStatus() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateStatus(UUID.randomUUID(), "NEW"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private BetaFeedbackEntity feedback(
            UUID feedbackUuid,
            BetaFeedbackStatus status,
            BetaFeedbackCategory category,
            String message) {
        BetaFeedbackEntity entity = BetaFeedbackEntity.create(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "alex99",
                "alex@example.com",
                category,
                message,
                "/dashboard");
        entity.setId(1L);
        entity.setUuid(feedbackUuid);
        entity.setCreatedAt(OffsetDateTime.parse("2026-06-01T09:30:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-06-01T09:30:00Z"));
        if (status == BetaFeedbackStatus.REVIEWED) {
            entity.markReviewed();
        }
        if (status == BetaFeedbackStatus.RESOLVED) {
            entity.markResolved();
        }
        return entity;
    }
}

