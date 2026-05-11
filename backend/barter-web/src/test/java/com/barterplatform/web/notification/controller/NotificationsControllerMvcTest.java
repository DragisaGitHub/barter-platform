package com.barterplatform.web.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.NotificationUnreadCountResponse;
import com.barterplatform.application.notification.service.NotificationService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationsControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("aaaa1111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationsController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── listNotifications (unauthenticated) ──────────────────────

    @Test
    void listNotificationsUnauthenticatedShouldFail() throws Exception {
        mockMvc.perform(apiGet("/notifications"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }

    // ── listNotifications (authenticated) ────────────────────────

    @Test
    void listNotificationsAuthenticatedShouldDelegate() throws Exception {
        setAuthenticatedUser();

        NotificationPagedResponse pagedResponse = new NotificationPagedResponse()
                .content(List.of()).page(0).size(20).totalElements(0L)
                .totalPages(0).first(true).last(true).sort("createdAt,desc");
        when(notificationService.listNotifications(eq(USER_UUID), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/notifications")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(notificationService).listNotifications(eq(USER_UUID), eq(0), eq(20), any());
    }

    // ── getUnreadNotificationCount ───────────────────────────────

    @Test
    void getUnreadCountUnauthenticatedShouldFail() throws Exception {
        mockMvc.perform(apiGet("/notifications/unread-count"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }

    @Test
    void getUnreadCountAuthenticatedShouldDelegate() throws Exception {
        setAuthenticatedUser();

        NotificationUnreadCountResponse countResponse = new NotificationUnreadCountResponse().count(3L);
        when(notificationService.getUnreadCount(USER_UUID)).thenReturn(countResponse);

        mockMvc.perform(apiGet("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        verify(notificationService).getUnreadCount(USER_UUID);
    }

    // ── markNotificationAsRead ───────────────────────────────────

    @Test
    void markNotificationAsReadUnauthenticatedShouldFail() throws Exception {
        UUID notifUuid = UUID.randomUUID();
        mockMvc.perform(apiPost("/notifications/" + notifUuid + "/read"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }

    @Test
    void markNotificationAsReadAuthenticatedShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID notifUuid = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse()
                .uuid(notifUuid)
                .isRead(true);
        when(notificationService.markAsRead(USER_UUID, notifUuid)).thenReturn(response);

        mockMvc.perform(apiPost("/notifications/" + notifUuid + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(notifUuid.toString()))
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).markAsRead(USER_UUID, notifUuid);
    }

    // ── markAllNotificationsAsRead ───────────────────────────────

    @Test
    void markAllAsReadUnauthenticatedShouldFail() throws Exception {
        mockMvc.perform(apiPost("/notifications/read-all"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }

    @Test
    void markAllAsReadAuthenticatedShouldReturn204() throws Exception {
        setAuthenticatedUser();

        mockMvc.perform(apiPost("/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(USER_UUID);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void setAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_UUID, "alice", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

