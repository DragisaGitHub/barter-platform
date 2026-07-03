package com.barterplatform.application.catalog.service;

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
import java.util.UUID;

public interface AdminCategorySchemaService {

    CategorySchemaPagedResponse searchSchemas(
            Integer page,
            Integer size,
            String sort,
            UUID categoryUuid,
            CategorySchemaStatus status,
            Boolean includeDeleted);

    CategorySchemaResponse createSchema(UUID categoryUuid, CreateCategorySchemaRequest request);

    CategorySchemaResponse getSchema(UUID schemaUuid);

    CategorySchemaResponse updateSchema(UUID schemaUuid, UpdateCategorySchemaRequest request);

    void deleteSchema(UUID schemaUuid);

    CategorySchemaResponse activateSchema(UUID schemaUuid);

    CategorySchemaFieldResponse createField(UUID schemaUuid, CreateCategorySchemaFieldRequest request);

    CategorySchemaFieldResponse updateField(UUID fieldUuid, UpdateCategorySchemaFieldRequest request);

    void deleteField(UUID fieldUuid);

    FieldOptionResponse createOption(UUID fieldUuid, CreateFieldOptionRequest request);

    FieldOptionResponse updateOption(UUID optionUuid, UpdateFieldOptionRequest request);

    void deleteOption(UUID optionUuid);
}

