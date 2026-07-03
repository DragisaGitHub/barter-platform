package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.CategoryFormFieldOptionResponse;
import com.barterplatform.api.model.CategoryFormFieldResponse;
import com.barterplatform.api.model.CategoryFormSchemaResponse;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps category schema domain entities to the public {@link CategoryFormSchemaResponse} contract
 * consumed by clients to build dynamic listing forms.
 *
 * <p>Internal database identifiers are never exposed; only UUIDs are surfaced.
 */
@Component
public class CategoryFormSchemaMapper {

    /** Empty schema response returned when a category has no ACTIVE schema. */
    public CategoryFormSchemaResponse toEmptyResponse(UUID categoryUuid) {
        return new CategoryFormSchemaResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(null)
                .schemaVersion(null)
                .schemaStatus(null)
                .fields(List.of());
    }

    public CategoryFormSchemaResponse toResponse(
            UUID categoryUuid,
            CategorySchemaEntity schema,
            List<CategorySchemaFieldEntity> fields,
            Map<Long, List<FieldOptionEntity>> optionsByFieldId) {
        List<CategoryFormFieldResponse> fieldResponses = fields.stream()
                .map(field -> toFieldResponse(field, optionsByFieldId.getOrDefault(field.getId(), List.of())))
                .toList();

        return new CategoryFormSchemaResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(schema.getUuid())
                .schemaVersion(schema.getVersion())
                .schemaStatus(com.barterplatform.api.model.CategorySchemaStatus.valueOf(schema.getStatus().name()))
                .fields(fieldResponses);
    }

    private CategoryFormFieldResponse toFieldResponse(CategorySchemaFieldEntity entity, List<FieldOptionEntity> options) {
        return new CategoryFormFieldResponse()
                .fieldUuid(entity.getUuid())
                .key(entity.getKey())
                .label(entity.getLabel())
                .labelSr(entity.getLabelSr())
                .helpText(entity.getHelpText())
                .fieldType(com.barterplatform.api.model.CategorySchemaFieldType.valueOf(entity.getFieldType().name()))
                .required(entity.isRequired())
                .searchable(entity.isSearchable())
                .filterable(entity.isFilterable())
                .sortable(entity.isSortable())
                .unit(entity.getUnit())
                .displayOrder(entity.getDisplayOrder())
                .validationJson(entity.getValidationJson())
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

