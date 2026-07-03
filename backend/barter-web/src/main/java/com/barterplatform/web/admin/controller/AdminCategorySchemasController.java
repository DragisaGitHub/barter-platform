package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminCategorySchemasApi;
import com.barterplatform.api.model.CategorySchemaFieldResponse;
import com.barterplatform.api.model.CategorySchemaPagedResponse;
import com.barterplatform.api.model.CategorySchemaResponse;
import com.barterplatform.api.model.CategorySchemaStatus;
import com.barterplatform.api.model.CreateCategorySchemaFieldRequest;
import com.barterplatform.api.model.CreateCategorySchemaRequest;
import com.barterplatform.api.model.CreateFieldOptionRequest;
import com.barterplatform.api.model.FieldOptionResponse;
import com.barterplatform.api.model.UpdateCategorySchemaFieldRequest;
import com.barterplatform.api.model.UpdateCategorySchemaRequest;
import com.barterplatform.api.model.UpdateFieldOptionRequest;
import com.barterplatform.application.catalog.service.AdminCategorySchemaService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategorySchemasController implements AdminCategorySchemasApi {

    private final AdminCategorySchemaService adminCategorySchemaService;

    public AdminCategorySchemasController(AdminCategorySchemaService adminCategorySchemaService) {
        this.adminCategorySchemaService = adminCategorySchemaService;
    }

    @Override
    public ResponseEntity<CategorySchemaPagedResponse> listAdminCategorySchemas(
            Integer page,
            Integer size,
            String sort,
            UUID categoryUuid,
            CategorySchemaStatus status,
            Boolean includeDeleted) {
        return ResponseEntity.ok(
                adminCategorySchemaService.searchSchemas(page, size, sort, categoryUuid, status, includeDeleted));
    }

    @Override
    public ResponseEntity<CategorySchemaResponse> createAdminCategorySchema(
            UUID categoryUuid,
            CreateCategorySchemaRequest createCategorySchemaRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategorySchemaService.createSchema(categoryUuid, createCategorySchemaRequest));
    }

    @Override
    public ResponseEntity<CategorySchemaResponse> getAdminCategorySchemaByUuid(UUID schemaUuid) {
        return ResponseEntity.ok(adminCategorySchemaService.getSchema(schemaUuid));
    }

    @Override
    public ResponseEntity<CategorySchemaResponse> updateAdminCategorySchema(
            UUID schemaUuid,
            UpdateCategorySchemaRequest updateCategorySchemaRequest) {
        return ResponseEntity.ok(adminCategorySchemaService.updateSchema(schemaUuid, updateCategorySchemaRequest));
    }

    @Override
    public ResponseEntity<Void> deleteAdminCategorySchema(UUID schemaUuid) {
        adminCategorySchemaService.deleteSchema(schemaUuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CategorySchemaResponse> activateAdminCategorySchema(UUID schemaUuid) {
        return ResponseEntity.ok(adminCategorySchemaService.activateSchema(schemaUuid));
    }

    @Override
    public ResponseEntity<CategorySchemaFieldResponse> createAdminCategorySchemaField(
            UUID schemaUuid,
            CreateCategorySchemaFieldRequest createCategorySchemaFieldRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategorySchemaService.createField(schemaUuid, createCategorySchemaFieldRequest));
    }

    @Override
    public ResponseEntity<CategorySchemaFieldResponse> updateAdminCategorySchemaField(
            UUID fieldUuid,
            UpdateCategorySchemaFieldRequest updateCategorySchemaFieldRequest) {
        return ResponseEntity.ok(
                adminCategorySchemaService.updateField(fieldUuid, updateCategorySchemaFieldRequest));
    }

    @Override
    public ResponseEntity<Void> deleteAdminCategorySchemaField(UUID fieldUuid) {
        adminCategorySchemaService.deleteField(fieldUuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FieldOptionResponse> createAdminCategorySchemaFieldOption(
            UUID fieldUuid,
            CreateFieldOptionRequest createFieldOptionRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategorySchemaService.createOption(fieldUuid, createFieldOptionRequest));
    }

    @Override
    public ResponseEntity<FieldOptionResponse> updateAdminCategorySchemaFieldOption(
            UUID optionUuid,
            UpdateFieldOptionRequest updateFieldOptionRequest) {
        return ResponseEntity.ok(adminCategorySchemaService.updateOption(optionUuid, updateFieldOptionRequest));
    }

    @Override
    public ResponseEntity<Void> deleteAdminCategorySchemaFieldOption(UUID optionUuid) {
        adminCategorySchemaService.deleteOption(optionUuid);
        return ResponseEntity.noContent().build();
    }
}

