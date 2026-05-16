package com.barterplatform.web.identity.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.CurrentUserResponse;
import com.barterplatform.api.model.PreferredLanguage;
import com.barterplatform.api.model.UserStatus;
import com.barterplatform.application.identity.service.AuthService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.time.OffsetDateTime;
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

class AuthMeControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(USER_UUID, "alex99", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        CurrentUserResponse response = new CurrentUserResponse()
                .uuid(USER_UUID)
                .username("alex99")
                .email("alex@example.com")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mfaEnabled(false)
                .preferredLanguage(PreferredLanguage.SR)
                .roles(List.of())
                .permissions(List.of())
                .oauthAccounts(List.of())
                .createdAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));

        when(authService.getCurrentUser(USER_UUID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me")
                        .contextPath("/api/v1")
                        .servletPath("/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.username").value("alex99"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.mfaEnabled").value(false))
                .andExpect(jsonPath("$.preferredLanguage").value("SR"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.oauthAccounts").isArray())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(authService).getCurrentUser(USER_UUID);
    }
}

