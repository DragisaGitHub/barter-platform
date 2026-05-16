package com.barterplatform.web.identity.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.PermissionCode;
import com.barterplatform.api.model.PreferredLanguage;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.api.model.UpdateUserPreferencesRequest;
import com.barterplatform.api.model.UpdateUserStatusRequest;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserPreferencesResponse;
import com.barterplatform.api.model.UserResponse;
import com.barterplatform.api.model.UserStatus;
import com.barterplatform.api.model.UserSummaryResponse;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.application.identity.service.UserPreferenceService;
import com.barterplatform.application.identity.service.UserQueryService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UsersControllerMvcTest {

    private MockMvc mockMvc;
    private UserQueryService userQueryService;
    private UserManagementService userManagementService;
    private UserPreferenceService userPreferenceService;
    private static final UUID CURRENT_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        userQueryService = mock(UserQueryService.class);
        userManagementService = mock(UserManagementService.class);
        userPreferenceService = mock(UserPreferenceService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UsersController(userQueryService, userManagementService, userPreferenceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetUserByUuid() throws Exception {
        UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(userQueryService.getUserByUuid(userUuid)).thenReturn(userResponse(userUuid));

        mockMvc.perform(apiGet("/users/" + userUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(userUuid.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0].code").value("USER"))
                .andExpect(jsonPath("$.permissions[0].code").value("USER_VIEW"))
                .andExpect(jsonPath("$.oauthAccounts").isArray())
                .andExpect(jsonPath("$.mfaSettings").doesNotExist());

        verify(userQueryService).getUserByUuid(userUuid);
        verifyNoMoreInteractions(userQueryService);
    }

    @Test
    void shouldListUsersWithPagination() throws Exception {
        UserPagedResponse pagedResponse = new UserPagedResponse()
                .content(List.of(userSummaryResponse()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .sort("username,asc");

        when(userQueryService.listUsers(0, 20, "username,asc")).thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/users")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.sort").value("username,asc"))
                .andExpect(jsonPath("$.content[0].uuid").exists())
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        verify(userQueryService).listUsers(0, 20, "username,asc");
        verifyNoMoreInteractions(userQueryService);
    }

    @Test
    void shouldUpdateUserStatus() throws Exception {
        UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED);
        UserResponse expectedResponse = new UserResponse()
                .uuid(userUuid)
                .username("alice")
                .email("alice@example.com")
                .status(UserStatus.SUSPENDED)
                .emailVerified(true)
                .mfaEnabled(false)
                .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"));

        when(userManagementService.updateUserStatus(userUuid, request)).thenReturn(expectedResponse);

        mockMvc.perform(apiPatch("/users/" + userUuid + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(userUuid.toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(userManagementService).updateUserStatus(userUuid, request);
        verifyNoMoreInteractions(userManagementService);
    }

    @Test
    void shouldReturnConflictWhenSettingStatusToDeleted() throws Exception {
        UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(userManagementService.updateUserStatus(userUuid,
                new UpdateUserStatusRequest(UserStatus.DELETED)))
                .thenThrow(new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCode.CONFLICT,
                        "Setting status to DELETED is not allowed through this endpoint."));

        mockMvc.perform(apiPatch("/users/" + userUuid + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isConflict());

        verify(userManagementService).updateUserStatus(userUuid,
                new UpdateUserStatusRequest(UserStatus.DELETED));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID userUuid = UUID.fromString("99999999-9999-9999-9999-999999999999");

        when(userManagementService.updateUserStatus(userUuid,
                new UpdateUserStatusRequest(UserStatus.ACTIVE)))
                .thenThrow(new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User with uuid '%s' was not found.".formatted(userUuid)));

        mockMvc.perform(apiPatch("/users/" + userUuid + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());

        verify(userManagementService).updateUserStatus(userUuid,
                new UpdateUserStatusRequest(UserStatus.ACTIVE));
    }

    @Test
    void shouldGetCurrentUserPreferences() throws Exception {
        setAuthenticatedUser();
        when(userPreferenceService.getCurrentUserPreferences(CURRENT_USER_UUID))
                .thenReturn(new UserPreferencesResponse().preferredLanguage(PreferredLanguage.SR));

        mockMvc.perform(apiGet("/users/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("SR"));

        verify(userPreferenceService).getCurrentUserPreferences(CURRENT_USER_UUID);
    }

    @Test
    void shouldUpdateCurrentUserPreferences() throws Exception {
        setAuthenticatedUser();
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(PreferredLanguage.EN);
        when(userPreferenceService.updateCurrentUserPreferences(CURRENT_USER_UUID, request))
                .thenReturn(new UserPreferencesResponse().preferredLanguage(PreferredLanguage.EN));

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"EN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("EN"));

        verify(userPreferenceService).updateCurrentUserPreferences(CURRENT_USER_UUID, request);
    }

    @Test
    void shouldReturnBadRequestForInvalidPreferredLanguage() throws Exception {
        setAuthenticatedUser();

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"DE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/preferences"));

        verifyNoInteractions(userPreferenceService);
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private UserResponse userResponse(UUID userUuid) {
        return new UserResponse()
                .uuid(userUuid)
                .username("alice")
                .email("alice@example.com")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mfaEnabled(false)
                .roles(List.of(new RoleResponse()
                        .uuid(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                        .code(com.barterplatform.api.model.RoleCode.USER)
                        .name("User")
                        .description("Default user role")
                        .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"))))
                .permissions(List.of(new PermissionResponse()
                        .uuid(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                        .code(PermissionCode.USER_VIEW)
                        .name("User View")
                        .description("Allows viewing users")
                        .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"))))
                .oauthAccounts(List.of())
                .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"));
    }

    private UserSummaryResponse userSummaryResponse() {
        return new UserSummaryResponse()
                .uuid(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .username("alice")
                .email("alice@example.com")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mfaEnabled(false)
                .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"));
    }

    private void setAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(CURRENT_USER_UUID, "alex99", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

