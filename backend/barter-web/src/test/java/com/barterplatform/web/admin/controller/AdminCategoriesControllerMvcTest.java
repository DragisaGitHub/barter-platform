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

import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.application.catalog.service.AdminCategoryService;
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

class AdminCategoriesControllerMvcTest {

    private MockMvc mockMvc;
    private AdminCategoryService adminCategoryService;

    @BeforeEach
    void setUp() {
        adminCategoryService = mock(AdminCategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCategoriesController(adminCategoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateCategory() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        when(adminCategoryService.createCategory(new com.barterplatform.api.model.CreateCategoryRequest()
                .name("Books")
                .slug("books")
                .sortOrder(1)))
                .thenReturn(response(categoryUuid, "Books", "books"));

        mockMvc.perform(apiPost("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Books",
                                  "slug": "books",
                                  "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.slug").value("books"));

        verify(adminCategoryService).createCategory(new com.barterplatform.api.model.CreateCategoryRequest()
                .name("Books")
                .slug("books")
                .sortOrder(1));
        verifyNoMoreInteractions(adminCategoryService);
    }

    @Test
    void shouldReturnConflictWhenSlugAlreadyExists() throws Exception {
        when(adminCategoryService.createCategory(new com.barterplatform.api.model.CreateCategoryRequest()
                .name("Books")
                .slug("books")))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Category slug 'books' already exists."));

        mockMvc.perform(apiPost("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Books",
                                  "slug": "books"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        when(adminCategoryService.updateCategory(categoryUuid, new com.barterplatform.api.model.UpdateCategoryRequest()
                .name("Rare Books")
                .sortOrder(3)))
                .thenReturn(response(categoryUuid, "Rare Books", "rare-books"));

        mockMvc.perform(apiPatch("/admin/categories/" + categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rare Books",
                                  "sortOrder": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rare Books"))
                .andExpect(jsonPath("$.slug").value("rare-books"));

        verify(adminCategoryService).updateCategory(categoryUuid, new com.barterplatform.api.model.UpdateCategoryRequest()
                .name("Rare Books")
                .sortOrder(3));
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        UUID categoryUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/admin/categories/" + categoryUuid))
                .andExpect(status().isNoContent());

        verify(adminCategoryService).deleteCategory(categoryUuid);
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

    private AdminCategoryResponse response(UUID uuid, String name, String slug) {
        return new AdminCategoryResponse()
                .uuid(uuid)
                .name(name)
                .slug(slug)
                .sortOrder(0)
                .deleted(false)
                .createdAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
    }
}


