package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.api.model.CreateCategoryRequest;
import com.barterplatform.api.model.UpdateCategoryRequest;
import java.util.UUID;

public interface AdminCategoryService {

    AdminCategoryPagedResponse searchCategories(Integer page, Integer size, String sort, String q, Boolean includeDeleted);

    AdminCategoryResponse createCategory(CreateCategoryRequest request);

    AdminCategoryResponse getCategory(UUID categoryUuid);

    AdminCategoryResponse updateCategory(UUID categoryUuid, UpdateCategoryRequest request);

    void deleteCategory(UUID categoryUuid);
}

