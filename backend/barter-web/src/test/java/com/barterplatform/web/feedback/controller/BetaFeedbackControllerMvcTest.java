package com.barterplatform.web.feedback.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.feedback.service.BetaFeedbackService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BetaFeedbackControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private BetaFeedbackService betaFeedbackService;

    @BeforeEach
    void setUp() {
        betaFeedbackService = mock(BetaFeedbackService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BetaFeedbackController(betaFeedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSubmitBetaFeedbackWhenAuthenticated() throws Exception {
        when(betaFeedbackService.submitFeedback(
                USER_UUID,
                "ONBOARDING",
                "I was not sure whether to save a draft or publish my first listing.",
                "/dashboard"))
                .thenReturn(new MessageResponse().message("Thanks for the feedback."));

        setAuthenticatedUser();

        mockMvc.perform(post("/api/v1/feedback/beta")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "ONBOARDING",
                                  "message": "I was not sure whether to save a draft or publish my first listing.",
                                  "sourcePage": "/dashboard"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Thanks for the feedback."));

        verify(betaFeedbackService).submitFeedback(
                USER_UUID,
                "ONBOARDING",
                "I was not sure whether to save a draft or publish my first listing.",
                "/dashboard");
        verifyNoMoreInteractions(betaFeedbackService);
    }

    @Test
    void shouldRejectTooShortFeedbackMessage() throws Exception {
        setAuthenticatedUser();

        mockMvc.perform(post("/api/v1/feedback/beta")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "ONBOARDING",
                                  "message": "too short",
                                  "sourcePage": "/dashboard"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("message"));

        verifyNoMoreInteractions(betaFeedbackService);
    }

    private void setAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_UUID, "alex99", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

