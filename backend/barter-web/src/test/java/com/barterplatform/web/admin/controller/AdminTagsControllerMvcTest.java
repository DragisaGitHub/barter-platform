package com.barterplatform.web.admin.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.application.catalog.service.AdminTagService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminTagsControllerMvcTest {

    private MockMvc mockMvc;
    private AdminTagService adminTagService;

    @BeforeEach
    void setUp() {
        adminTagService = mock(AdminTagService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminTagsController(adminTagService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateTag() throws Exception {
        UUID tagUuid = UUID.randomUUID();
        when(adminTagService.createTag(new com.barterplatform.api.model.CreateTagRequest()
                .name("Vintage")
                .slug("vintage")))
                .thenReturn(response(tagUuid, "Vintage", "vintage"));

        mockMvc.perform(apiPost("/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Vintage",
                                  "slug": "vintage"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(tagUuid.toString()))
                .andExpect(jsonPath("$.name").value("Vintage"))
                .andExpect(jsonPath("$.slug").value("vintage"));

        verify(adminTagService).createTag(new com.barterplatform.api.model.CreateTagRequest()
                .name("Vintage")
                .slug("vintage"));
        verifyNoMoreInteractions(adminTagService);
    }

    @Test
    void shouldReturnConflictWhenSlugAlreadyExists() throws Exception {
        when(adminTagService.createTag(new com.barterplatform.api.model.CreateTagRequest()
                .name("Vintage")
                .slug("vintage")))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Tag slug 'vintage' already exists."));

        mockMvc.perform(apiPost("/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Vintage",
                                  "slug": "vintage"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldUpdateTag() throws Exception {
        UUID tagUuid = UUID.randomUUID();
        when(adminTagService.updateTag(tagUuid, new com.barterplatform.api.model.UpdateTagRequest()
                .name("Rare Vintage")))
                .thenReturn(response(tagUuid, "Rare Vintage", "rare-vintage"));

        mockMvc.perform(apiPatch("/admin/tags/" + tagUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rare Vintage"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rare Vintage"))
                .andExpect(jsonPath("$.slug").value("rare-vintage"));

        verify(adminTagService).updateTag(tagUuid, new com.barterplatform.api.model.UpdateTagRequest()
                .name("Rare Vintage"));
    }

    @Test
    void shouldDeleteTag() throws Exception {
        UUID tagUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/admin/tags/" + tagUuid))
                .andExpect(status().isNoContent());

        verify(adminTagService).deleteTag(tagUuid);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    private AdminTagResponse response(UUID uuid, String name, String slug) {
        return new AdminTagResponse()
                .uuid(uuid)
                .name(name)
                .slug(slug)
                .deleted(false)
                .createdAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
    }
}

