package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.CategorySchemaFieldResponse;
import com.barterplatform.api.model.CategorySchemaFieldResponse1;
import com.barterplatform.api.model.CategorySchemaResponse;
import com.barterplatform.api.model.CategorySchemaResponse1;
import com.barterplatform.api.model.FieldOptionResponse;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps category schema domain entities to their generated OpenAPI response models.
 *
 * <p>Note: the OpenAPI generator produces two structurally-identical variants for the schema
 * and field response models ({@code CategorySchemaResponse}/{@code CategorySchemaResponse1} and
 * {@code CategorySchemaFieldResponse}/{@code CategorySchemaFieldResponse1}) because the same
 * schema is referenced both as a top-level response and as an item nested inside the paged
 * response's {@code content} array. Both variants are mapped here so each generated endpoint
 * signature compiles against the exact type it declares.
 */
@Component
public class AdminCategorySchemaMapper {

    public FieldOptionResponse toOptionResponse(FieldOptionEntity entity) {
        return new FieldOptionResponse()
                .uuid(entity.getUuid())
                .value(entity.getValue())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .displayOrder(entity.getDisplayOrder())
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    /** Used for the field create/update endpoint responses and nested inside {@link CategorySchemaResponse1}. */
    public CategorySchemaFieldResponse toFieldResponse(CategorySchemaFieldEntity entity, List<FieldOptionEntity> options) {
        return new CategorySchemaFieldResponse()
                .uuid(entity.getUuid())
                .key(entity.getKey())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .helpText(entity.getHelpText())
                .fieldType(toApiFieldType(entity.getFieldType()))
                .required(entity.isRequired())
                .searchable(entity.isSearchable())
                .filterable(entity.isFilterable())
                .sortable(entity.isSortable())
                .unit(entity.getUnit())
                .displayOrder(entity.getDisplayOrder())
                .validationJson(entity.getValidationJson())
                .options(options.stream().map(this::toOptionResponse).toList())
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    /** Nested field response variant required inside the top-level {@link CategorySchemaResponse}. */
    public CategorySchemaFieldResponse1 toNestedFieldResponse(CategorySchemaFieldEntity entity, List<FieldOptionEntity> options) {
        return new CategorySchemaFieldResponse1()
                .uuid(entity.getUuid())
                .key(entity.getKey())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .helpText(entity.getHelpText())
                .fieldType(toApiFieldType(entity.getFieldType()))
                .required(entity.isRequired())
                .searchable(entity.isSearchable())
                .filterable(entity.isFilterable())
                .sortable(entity.isSortable())
                .unit(entity.getUnit())
                .displayOrder(entity.getDisplayOrder())
                .validationJson(entity.getValidationJson())
                .options(options.stream().map(this::toOptionResponse).toList())
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    /** Used for the single-schema get/create/update/activate endpoint responses. */
    public CategorySchemaResponse toResponse(
            CategorySchemaEntity entity,
            CategoryEntity category,
            List<CategorySchemaFieldEntity> fields,
            Map<Long, List<FieldOptionEntity>> optionsByFieldId) {
        List<CategorySchemaFieldResponse1> fieldResponses = fields.stream()
                .map(field -> toNestedFieldResponse(field, optionsByFieldId.getOrDefault(field.getId(), List.of())))
                .toList();

        return new CategorySchemaResponse()
                .uuid(entity.getUuid())
                .categoryUuid(category != null ? category.getUuid() : null)
                .categoryName(category != null ? category.getName() : null)
                .version(entity.getVersion())
                .status(toApiStatus(entity.getStatus()))
                .name(entity.getName())
                .description(entity.getDescription())
                .fields(fieldResponses)
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    /** Used for schema items nested inside the {@code CategorySchemaPagedResponse.content} list. */
    public CategorySchemaResponse1 toPagedItemResponse(
            CategorySchemaEntity entity,
            CategoryEntity category,
            List<CategorySchemaFieldEntity> fields,
            Map<Long, List<FieldOptionEntity>> optionsByFieldId) {
        List<CategorySchemaFieldResponse> fieldResponses = fields.stream()
                .map(field -> toFieldResponse(field, optionsByFieldId.getOrDefault(field.getId(), List.of())))
                .toList();

        return new CategorySchemaResponse1()
                .uuid(entity.getUuid())
                .categoryUuid(category != null ? category.getUuid() : null)
                .categoryName(category != null ? category.getName() : null)
                .version(entity.getVersion())
                .status(toApiStatus(entity.getStatus()))
                .name(entity.getName())
                .description(entity.getDescription())
                .fields(fieldResponses)
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    public com.barterplatform.api.model.CategorySchemaStatus toApiStatus(
            com.barterplatform.domain.catalog.enums.CategorySchemaStatus status) {
        return com.barterplatform.api.model.CategorySchemaStatus.valueOf(status.name());
    }

    public com.barterplatform.domain.catalog.enums.CategorySchemaStatus toDomainStatus(
            com.barterplatform.api.model.CategorySchemaStatus status) {
        return com.barterplatform.domain.catalog.enums.CategorySchemaStatus.valueOf(status.name());
    }

    public com.barterplatform.api.model.CategorySchemaFieldType toApiFieldType(
            com.barterplatform.domain.catalog.enums.CategorySchemaFieldType fieldType) {
        return com.barterplatform.api.model.CategorySchemaFieldType.valueOf(fieldType.name());
    }

    public com.barterplatform.domain.catalog.enums.CategorySchemaFieldType toDomainFieldType(
            com.barterplatform.api.model.CategorySchemaFieldType fieldType) {
        return com.barterplatform.domain.catalog.enums.CategorySchemaFieldType.valueOf(fieldType.name());
    }
}

