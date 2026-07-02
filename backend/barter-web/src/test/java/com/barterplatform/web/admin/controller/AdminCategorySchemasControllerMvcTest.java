package com.barterplatform.web.admin.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.CategorySchemaFieldResponse;
import com.barterplatform.api.model.CategorySchemaFieldType;
import com.barterplatform.api.model.CategorySchemaResponse;
import com.barterplatform.api.model.CategorySchemaStatus;
import com.barterplatform.api.model.FieldOptionResponse;
import com.barterplatform.application.catalog.service.AdminCategorySchemaService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCategorySchemasControllerMvcTest {

    private MockMvc mockMvc;
    private AdminCategorySchemaService adminCategorySchemaService;

    @BeforeEach
    void setUp() {
        adminCategorySchemaService = mock(AdminCategorySchemaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCategorySchemasController(adminCategorySchemaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateSchema() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        UUID schemaUuid = UUID.randomUUID();
        when(adminCategorySchemaService.createSchema(
                org.mockito.ArgumentMatchers.eq(categoryUuid),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(schemaResponse(schemaUuid, categoryUuid, "Base spec"));

        mockMvc.perform(apiPost("/admin/categories/" + categoryUuid + "/schemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Base spec"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(schemaUuid.toString()))
                .andExpect(jsonPath("$.name").value("Base spec"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldReturnConflictWhenActivatingFailsBusinessRule() throws Exception {
        UUID schemaUuid = UUID.randomUUID();
        when(adminCategorySchemaService.activateSchema(schemaUuid))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Cannot activate."));

        mockMvc.perform(apiPost("/admin/category-schemas/" + schemaUuid + "/activate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldReturnConflictWhenDeletingActiveSchema() throws Exception {
        UUID schemaUuid = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Active schema cannot be deleted."))
                .when(adminCategorySchemaService).deleteSchema(schemaUuid);

        mockMvc.perform(apiDelete("/admin/category-schemas/" + schemaUuid))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateFieldOnSchema() throws Exception {
        UUID schemaUuid = UUID.randomUUID();
        UUID fieldUuid = UUID.randomUUID();
        when(adminCategorySchemaService.createField(org.mockito.ArgumentMatchers.eq(schemaUuid), org.mockito.ArgumentMatchers.any()))
                .thenReturn(fieldResponse(fieldUuid, "brand", CategorySchemaFieldType.TEXT));

        mockMvc.perform(apiPost("/admin/category-schemas/" + schemaUuid + "/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "brand",
                                  "label": "Brand",
                                  "fieldType": "TEXT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("brand"))
                .andExpect(jsonPath("$.fieldType").value("TEXT"));
    }

    @Test
    void shouldRejectOptionCreationForNonSelectField() throws Exception {
        UUID fieldUuid = UUID.randomUUID();
        when(adminCategorySchemaService.createOption(org.mockito.ArgumentMatchers.eq(fieldUuid), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                        "Options can only be added to SINGLE_SELECT or MULTI_SELECT fields."));

        mockMvc.perform(apiPost("/admin/category-schema-fields/" + fieldUuid + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": "red",
                                  "label": "Red"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateFieldOption() throws Exception {
        UUID optionUuid = UUID.randomUUID();
        when(adminCategorySchemaService.updateOption(org.mockito.ArgumentMatchers.eq(optionUuid), org.mockito.ArgumentMatchers.any()))
                .thenReturn(optionResponse(optionUuid, "red", "Bright Red"));

        mockMvc.perform(apiPatch("/admin/category-schema-field-options/" + optionUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Bright Red"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Bright Red"));
    }

    @Test
    void shouldDeleteFieldOption() throws Exception {
        UUID optionUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/admin/category-schema-field-options/" + optionUuid))
                .andExpect(status().isNoContent());

        verify(adminCategorySchemaService).deleteOption(optionUuid);
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

    private CategorySchemaResponse schemaResponse(UUID schemaUuid, UUID categoryUuid, String name) {
        return new CategorySchemaResponse()
                .uuid(schemaUuid)
                .categoryUuid(categoryUuid)
                .version(1)
                .status(CategorySchemaStatus.DRAFT)
                .name(name)
                .fields(List.of())
                .deleted(false)
                .createdAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
    }

    private CategorySchemaFieldResponse fieldResponse(UUID fieldUuid, String key, CategorySchemaFieldType type) {
        return new CategorySchemaFieldResponse()
                .uuid(fieldUuid)
                .key(key)
                .label(key)
                .fieldType(type)
                .required(false)
                .searchable(false)
                .filterable(false)
                .sortable(false)
                .displayOrder(0)
                .options(List.of())
                .deleted(false)
                .createdAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
    }

    private FieldOptionResponse optionResponse(UUID optionUuid, String value, String label) {
        return new FieldOptionResponse()
                .uuid(optionUuid)
                .value(value)
                .label(label)
                .displayOrder(0)
                .deleted(false)
                .createdAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
    }
}

