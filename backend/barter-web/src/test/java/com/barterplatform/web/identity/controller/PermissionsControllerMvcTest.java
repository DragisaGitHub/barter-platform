package com.barterplatform.web.identity.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.PermissionCode;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.application.identity.service.PermissionService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PermissionsControllerMvcTest {

    private MockMvc mockMvc;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PermissionsController(permissionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListPermissions() throws Exception {
        when(permissionService.listPermissions()).thenReturn(List.of(permissionResponse()));

        mockMvc.perform(apiGet("/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].code").value("USER_VIEW"))
                .andExpect(jsonPath("$[0].name").value("User View"))
                .andExpect(jsonPath("$[0].description").value("Allows viewing users"))
                .andExpect(jsonPath("$[0].createdAt").exists());

        verify(permissionService).listPermissions();
        verifyNoMoreInteractions(permissionService);
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private PermissionResponse permissionResponse() {
        return new PermissionResponse()
                .uuid(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .code(PermissionCode.USER_VIEW)
                .name("User View")
                .description("Allows viewing users")
                .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"));
    }
}

