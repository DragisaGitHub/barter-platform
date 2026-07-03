package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.CategoryFilterFieldResponse;
import com.barterplatform.api.model.CategoryFormFieldOptionResponse;
import com.barterplatform.api.model.CategoryFiltersResponse;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps category schema domain entities to the public {@link CategoryFiltersResponse} contract
 * consumed by marketplace clients to render dynamic category filter controls (Marketplace Schema
 * Engine, Phase 6). Only {@code filterable=true} fields are surfaced; internal database
 * identifiers are never exposed, only UUIDs.
 */
@Component
public class CategoryFiltersMapper {

    /** Empty filters response returned when a category has no ACTIVE schema. */
    public CategoryFiltersResponse toEmptyResponse(UUID categoryUuid) {
        return new CategoryFiltersResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(null)
                .schemaVersion(null)
                .filters(List.of());
    }

    public CategoryFiltersResponse toResponse(
            UUID categoryUuid,
            CategorySchemaEntity schema,
            List<CategorySchemaFieldEntity> filterableFields,
            Map<Long, List<FieldOptionEntity>> optionsByFieldId) {
        List<CategoryFilterFieldResponse> filters = filterableFields.stream()
                .map(field -> toFilterFieldResponse(field, optionsByFieldId.getOrDefault(field.getId(), List.of())))
                .toList();

        return new CategoryFiltersResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(schema.getUuid())
                .schemaVersion(schema.getVersion())
                .filters(filters);
    }

    private CategoryFilterFieldResponse toFilterFieldResponse(CategorySchemaFieldEntity entity, List<FieldOptionEntity> options) {
        return new CategoryFilterFieldResponse()
                .fieldUuid(entity.getUuid())
                .key(entity.getKey())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .fieldType(com.barterplatform.api.model.CategorySchemaFieldType.valueOf(entity.getFieldType().name()))
                .unit(entity.getUnit())
                .displayOrder(entity.getDisplayOrder())
                .options(options.stream().map(this::toOptionResponse).toList());
    }

    private CategoryFormFieldOptionResponse toOptionResponse(FieldOptionEntity entity) {
        return new CategoryFormFieldOptionResponse()
                .optionUuid(entity.getUuid())
                .value(entity.getValue())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .displayOrder(entity.getDisplayOrder());
    }
}

