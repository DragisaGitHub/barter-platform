package com.barterplatform.application.reputation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ReputationSummaryResponse;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReputationServiceImplTest {

    @Mock
    private TradeReviewRepository tradeReviewRepository;

    private ReputationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReputationServiceImpl(tradeReviewRepository);
    }

    @Test
    @DisplayName("builds positive, negative, total, and percentage reputation summary")
    void buildsReputationSummary() {
        when(tradeReviewRepository.countByReviewedUserIdAndRating(10L, TradeReviewRating.POSITIVE)).thenReturn(3L);
        when(tradeReviewRepository.countByReviewedUserIdAndRating(10L, TradeReviewRating.NEGATIVE)).thenReturn(1L);
        when(tradeReviewRepository.countByReviewedUserId(10L)).thenReturn(4L);

        ReputationSummaryResponse response = service.getReputationSummary(10L);

        assertEquals(3, response.getPositiveReviewCount());
        assertEquals(1, response.getNegativeReviewCount());
        assertEquals(4, response.getTotalReviewCount());
        assertEquals(75.0, response.getPositivePercentage());
    }

    @Test
    @DisplayName("uses nullable positive percentage when user has no reviews")
    void noReviewsReturnsNullPercentage() {
        when(tradeReviewRepository.countByReviewedUserIdAndRating(10L, TradeReviewRating.POSITIVE)).thenReturn(0L);
        when(tradeReviewRepository.countByReviewedUserIdAndRating(10L, TradeReviewRating.NEGATIVE)).thenReturn(0L);
        when(tradeReviewRepository.countByReviewedUserId(10L)).thenReturn(0L);

        ReputationSummaryResponse response = service.getReputationSummary(10L);

        assertEquals(0, response.getPositiveReviewCount());
        assertEquals(0, response.getNegativeReviewCount());
        assertEquals(0, response.getTotalReviewCount());
        assertNull(response.getPositivePercentage());
    }
}

