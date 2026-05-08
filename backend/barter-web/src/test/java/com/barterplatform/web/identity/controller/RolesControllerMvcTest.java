package com.barterplatform.web.identity.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.application.identity.service.RoleService;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RolesControllerMvcTest {

    private MockMvc mockMvc;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RolesController(roleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListRoles() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(roleResponse(com.barterplatform.api.model.RoleCode.ADMIN, "Administrator")));

        mockMvc.perform(apiGet("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].code").value("ADMIN"))
                .andExpect(jsonPath("$[0].name").value("Administrator"))
                .andExpect(jsonPath("$[0].description").value("Administrator description"))
                .andExpect(jsonPath("$[0].createdAt").exists());

        verify(roleService).listRoles();
        verifyNoMoreInteractions(roleService);
    }

    @Test
    void shouldGetRoleByCode() throws Exception {
        when(roleService.getRoleByCode(RoleCode.ADMIN))
                .thenReturn(roleResponse(com.barterplatform.api.model.RoleCode.ADMIN, "Administrator"));

        mockMvc.perform(apiGet("/roles/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.code").value("ADMIN"))
                .andExpect(jsonPath("$.name").value("Administrator"))
                .andExpect(jsonPath("$.description").value("Administrator description"));

        verify(roleService).getRoleByCode(RoleCode.ADMIN);
        verifyNoMoreInteractions(roleService);
    }

    @Test
    void shouldReturnBadRequestForInvalidRoleCode() throws Exception {
        mockMvc.perform(apiGet("/roles/INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(roleService);
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private RoleResponse roleResponse(com.barterplatform.api.model.RoleCode code, String name) {
        return new RoleResponse()
                .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .code(code)
                .name(name)
                .description(name + " description")
                .createdAt(OffsetDateTime.parse("2026-05-07T10:15:30Z"));
    }
}

