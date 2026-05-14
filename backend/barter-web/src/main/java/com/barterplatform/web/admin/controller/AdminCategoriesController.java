package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminCategoriesApi;
import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.api.model.CreateCategoryRequest;
import com.barterplatform.api.model.UpdateCategoryRequest;
import com.barterplatform.application.catalog.service.AdminCategoryService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoriesController implements AdminCategoriesApi {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoriesController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @Override
    public ResponseEntity<AdminCategoryPagedResponse> listAdminCategories(
            Integer page,
            Integer size,
            String sort,
            String q,
            Boolean includeDeleted) {
        return ResponseEntity.ok(adminCategoryService.searchCategories(page, size, sort, q, includeDeleted));
    }

    @Override
    public ResponseEntity<AdminCategoryResponse> createAdminCategory(CreateCategoryRequest createCategoryRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategoryService.createCategory(createCategoryRequest));
    }

    @Override
    public ResponseEntity<AdminCategoryResponse> getAdminCategoryByUuid(UUID categoryUuid) {
        return ResponseEntity.ok(adminCategoryService.getCategory(categoryUuid));
    }

    @Override
    public ResponseEntity<AdminCategoryResponse> updateAdminCategory(
            UUID categoryUuid,
            UpdateCategoryRequest updateCategoryRequest) {
        return ResponseEntity.ok(adminCategoryService.updateCategory(categoryUuid, updateCategoryRequest));
    }

    @Override
    public ResponseEntity<Void> deleteAdminCategory(UUID categoryUuid) {
        adminCategoryService.deleteCategory(categoryUuid);
        return ResponseEntity.noContent().build();
    }
}


