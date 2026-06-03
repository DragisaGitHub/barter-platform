package com.barterplatform.web.admin.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse1;
import com.barterplatform.api.model.BetaFeedbackCategory;
import com.barterplatform.api.model.BetaFeedbackStatus;
import com.barterplatform.application.feedback.service.AdminBetaFeedbackService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminBetaFeedbackControllerMvcTest {

    private MockMvc mockMvc;
    private AdminBetaFeedbackService adminBetaFeedbackService;

    @BeforeEach
    void setUp() {
        adminBetaFeedbackService = mock(AdminBetaFeedbackService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBetaFeedbackController(adminBetaFeedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListAdminBetaFeedback() throws Exception {
        UUID feedbackUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(adminBetaFeedbackService.listFeedback(0, 20, "createdAt,desc", "NEW"))
                .thenReturn(new AdminBetaFeedbackPagedResponse()
                        .content(List.of(new AdminBetaFeedbackSummaryResponse1()
                                .uuid(feedbackUuid)
                                .userUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                                .username("alex99")
                                .email("alex@example.com")
                                .category(BetaFeedbackCategory.ONBOARDING)
                                .message("Feedback with enough context for admin review.")
                                .sourcePage("/dashboard")
                                .status(BetaFeedbackStatus.NEW)
                                .createdAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"))))
                        .page(0)
                        .size(20)
                        .totalElements(1L)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .sort("createdAt,desc"));

        mockMvc.perform(get("/api/v1/admin/feedback/beta")
                        .contextPath("/api/v1")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc")
                        .param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].uuid").value(feedbackUuid.toString()))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));

        verify(adminBetaFeedbackService).listFeedback(0, 20, "createdAt,desc", "NEW");
        verifyNoMoreInteractions(adminBetaFeedbackService);
    }

    @Test
    void shouldUpdateAdminBetaFeedbackStatus() throws Exception {
        UUID feedbackUuid = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(adminBetaFeedbackService.updateStatus(feedbackUuid, "RESOLVED"))
                .thenReturn(new AdminBetaFeedbackSummaryResponse()
                        .uuid(feedbackUuid)
                        .userUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .username("alex99")
                        .category(BetaFeedbackCategory.GENERAL)
                        .message("General beta feedback ready for resolution.")
                        .status(BetaFeedbackStatus.RESOLVED)
                        .createdAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"))
                        .reviewedAt(OffsetDateTime.parse("2026-06-01T11:00:00Z"))
                        .resolvedAt(OffsetDateTime.parse("2026-06-01T11:15:00Z")));

        mockMvc.perform(patch("/api/v1/admin/feedback/beta/{feedbackUuid}/status", feedbackUuid)
                        .contextPath("/api/v1")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"status\": \"RESOLVED\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(adminBetaFeedbackService).updateStatus(feedbackUuid, "RESOLVED");
        verifyNoMoreInteractions(adminBetaFeedbackService);
    }
}

