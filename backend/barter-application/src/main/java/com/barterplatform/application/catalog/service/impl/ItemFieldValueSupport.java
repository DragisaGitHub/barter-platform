package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.CategoryFormFieldOptionResponse;
import com.barterplatform.api.model.SchemaFieldValueRequest;
import com.barterplatform.api.model.SchemaFieldValueResponse;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.domain.catalog.entity.ItemFieldValueEntity;
import com.barterplatform.domain.catalog.entity.ItemFieldValueOptionEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemFieldValueOptionRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemFieldValueRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validates and persists dynamic category-schema field values entered on items (Marketplace
 * Schema Engine, Phase 4). Values are always fully replaced together with the owning item
 * create/update transaction; there is no partial patch semantics for individual field values.
 *
 * <p>Only fields belonging to the category's current ACTIVE schema are accepted. Soft-deleted
 * fields/options are treated as non-existent.
 */
@Component
@RequiredArgsConstructor
public class ItemFieldValueSupport {

    private final CategorySchemaRepository categorySchemaRepository;
    private final CategorySchemaFieldRepository categorySchemaFieldRepository;
    private final FieldOptionRepository fieldOptionRepository;
    private final ItemFieldValueRepository itemFieldValueRepository;
    private final ItemFieldValueOptionRepository itemFieldValueOptionRepository;

    /**
     * Validates the requested schema field values against the category's ACTIVE schema, replaces
     * any existing values for the item, and returns the freshly persisted values as response DTOs.
     */
    public List<SchemaFieldValueResponse> replaceValues(Long itemId, Long categoryId,
                                                          List<SchemaFieldValueRequest> requests) {
        List<SchemaFieldValueRequest> safeRequests = requests == null ? List.of() : requests;

        Optional<CategorySchemaEntity> activeSchema = categorySchemaRepository
                .findByCategoryIdAndStatusAndDeletedAtIsNull(categoryId, CategorySchemaStatus.ACTIVE);

        if (activeSchema.isEmpty()) {
            if (!safeRequests.isEmpty()) {
                throw badRequest("This category does not have an active schema; schema field values are not allowed.");
            }
            clearValues(itemId);
            return List.of();
        }

        CategorySchemaEntity schema = activeSchema.get();
        List<CategorySchemaFieldEntity> activeFields = categorySchemaFieldRepository
                .findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(schema.getId());
        Map<UUID, CategorySchemaFieldEntity> fieldsByUuid = activeFields.stream()
                .collect(Collectors.toMap(CategorySchemaFieldEntity::getUuid, f -> f, (a, b) -> a, LinkedHashMap::new));

        List<PreparedValue> prepared = new ArrayList<>();
        Set<UUID> seenFieldUuids = new HashSet<>();
        Set<UUID> providedFieldUuids = new HashSet<>();

        for (SchemaFieldValueRequest request : safeRequests) {
            UUID fieldUuid = request.getFieldUuid();
            if (fieldUuid == null) {
                throw badRequest("Each schema field value must reference a fieldUuid.");
            }
            if (!seenFieldUuids.add(fieldUuid)) {
                throw badRequest("Duplicate schema field value for field '%s'.".formatted(fieldUuid));
            }
            CategorySchemaFieldEntity field = fieldsByUuid.get(fieldUuid);
            if (field == null) {
                throw badRequest("Field '%s' does not belong to the active schema for this category.".formatted(fieldUuid));
            }

            PreparedValue value = prepareValue(field, request);
            if (value != null) {
                providedFieldUuids.add(fieldUuid);
                prepared.add(value);
            }
        }

        for (CategorySchemaFieldEntity field : activeFields) {
            if (field.isRequired() && !providedFieldUuids.contains(field.getUuid())) {
                throw badRequest("Field '%s' is required.".formatted(field.getLabel()));
            }
        }

        clearValues(itemId);

        for (PreparedValue value : prepared) {
            ItemFieldValueEntity entity = new ItemFieldValueEntity();
            entity.setItemId(itemId);
            entity.setSchemaFieldId(value.field.getId());
            entity.setValueText(value.valueText);
            entity.setValueNumber(value.valueNumber);
            entity.setValueBoolean(value.valueBoolean);
            entity.setValueDate(value.valueDate);
            entity.setOptionId(value.singleOptionId);
            ItemFieldValueEntity saved = itemFieldValueRepository.save(entity);

            if (value.multiOptionIds != null && !value.multiOptionIds.isEmpty()) {
                for (Long optionId : value.multiOptionIds) {
                    ItemFieldValueOptionEntity link = new ItemFieldValueOptionEntity();
                    link.setItemFieldValueId(saved.getId());
                    link.setFieldOptionId(optionId);
                    itemFieldValueOptionRepository.save(link);
                }
            }
        }

        return loadResponses(itemId);
    }

    /** Loads the currently persisted schema field values for an item, for read/detail/edit prefill flows. */
    public List<SchemaFieldValueResponse> loadResponses(Long itemId) {
        List<ItemFieldValueEntity> values = itemFieldValueRepository.findByItemId(itemId);
        if (values.isEmpty()) {
            return List.of();
        }

        List<Long> fieldIds = values.stream().map(ItemFieldValueEntity::getSchemaFieldId).distinct().toList();
        Map<Long, CategorySchemaFieldEntity> fieldsById = categorySchemaFieldRepository.findAllById(fieldIds).stream()
                .collect(Collectors.toMap(CategorySchemaFieldEntity::getId, f -> f));

        List<Long> valueIds = values.stream().map(ItemFieldValueEntity::getId).toList();
        Map<Long, List<Long>> multiOptionIdsByValueId = itemFieldValueOptionRepository.findByItemFieldValueIdIn(valueIds).stream()
                .collect(Collectors.groupingBy(ItemFieldValueOptionEntity::getItemFieldValueId,
                        Collectors.mapping(ItemFieldValueOptionEntity::getFieldOptionId, Collectors.toList())));

        Set<Long> allOptionIds = new HashSet<>();
        multiOptionIdsByValueId.values().forEach(allOptionIds::addAll);
        values.stream().map(ItemFieldValueEntity::getOptionId).filter(Objects::nonNull).forEach(allOptionIds::add);
        Map<Long, FieldOptionEntity> optionsById = allOptionIds.isEmpty() ? Map.of()
                : fieldOptionRepository.findAllById(allOptionIds).stream()
                        .collect(Collectors.toMap(FieldOptionEntity::getId, o -> o));

        List<SchemaFieldValueResponse> responses = new ArrayList<>();
        for (ItemFieldValueEntity value : values) {
            CategorySchemaFieldEntity field = fieldsById.get(value.getSchemaFieldId());
            if (field == null || field.getDeletedAt() != null) {
                continue;
            }

            SchemaFieldValueResponse response = new SchemaFieldValueResponse()
                    .fieldUuid(field.getUuid())
                    .key(field.getKey())
                    .label(field.getLabel())
                    .fieldType(com.barterplatform.api.model.CategorySchemaFieldType.valueOf(field.getFieldType().name()))
                    .valueText(value.getValueText())
                    .valueNumber(value.getValueNumber() == null ? null : value.getValueNumber().doubleValue())
                    .valueBoolean(value.getValueBoolean())
                    .valueDate(value.getValueDate());

            List<CategoryFormFieldOptionResponse> options = new ArrayList<>();
            if (value.getOptionId() != null) {
                FieldOptionEntity option = optionsById.get(value.getOptionId());
                if (option != null && option.getDeletedAt() == null) {
                    options.add(toOptionResponse(option));
                }
            }
            for (Long optionId : multiOptionIdsByValueId.getOrDefault(value.getId(), List.of())) {
                FieldOptionEntity option = optionsById.get(optionId);
                if (option != null && option.getDeletedAt() == null) {
                    options.add(toOptionResponse(option));
                }
            }
            response.setOptions(options);
            responses.add(response);
        }
        return responses;
    }

    private void clearValues(Long itemId) {
        List<ItemFieldValueEntity> existing = itemFieldValueRepository.findByItemId(itemId);
        if (!existing.isEmpty()) {
            List<Long> existingIds = existing.stream().map(ItemFieldValueEntity::getId).toList();
            itemFieldValueOptionRepository.deleteByItemFieldValueIdIn(existingIds);
        }
        itemFieldValueRepository.deleteByItemId(itemId);
    }

    private PreparedValue prepareValue(CategorySchemaFieldEntity field, SchemaFieldValueRequest request) {
        CategorySchemaFieldType type = field.getFieldType();

        boolean hasText = request.getValueText() != null && !request.getValueText().isBlank();
        boolean hasNumber = request.getValueNumber() != null;
        boolean hasBoolean = request.getValueBoolean() != null;
        boolean hasDate = request.getValueDate() != null;
        boolean hasOption = request.getOptionUuid() != null;
        boolean hasOptions = request.getOptionUuids() != null && !request.getOptionUuids().isEmpty();

        PreparedValue value = new PreparedValue();
        value.field = field;

        switch (type) {
            case TEXT -> {
                rejectOtherChannels(field, hasNumber, hasBoolean, hasDate, hasOption, hasOptions);
                if (!hasText) {
                    return null;
                }
                value.valueText = request.getValueText().trim();
            }
            case NUMBER -> {
                rejectOtherChannels(field, hasText, hasBoolean, hasDate, hasOption, hasOptions);
                if (!hasNumber) {
                    return null;
                }
                value.valueNumber = BigDecimal.valueOf(request.getValueNumber());
            }
            case BOOLEAN -> {
                rejectOtherChannels(field, hasText, hasNumber, hasDate, hasOption, hasOptions);
                if (!hasBoolean) {
                    return null;
                }
                value.valueBoolean = request.getValueBoolean();
            }
            case DATE -> {
                rejectOtherChannels(field, hasText, hasNumber, hasBoolean, hasOption, hasOptions);
                if (!hasDate) {
                    return null;
                }
                value.valueDate = request.getValueDate();
            }
            case SINGLE_SELECT -> {
                rejectOtherChannels(field, hasText, hasNumber, hasBoolean, hasDate, hasOptions);
                if (!hasOption) {
                    return null;
                }
                FieldOptionEntity option = fieldOptionRepository.findByUuid(request.getOptionUuid())
                        .filter(o -> o.getDeletedAt() == null && o.getFieldId().equals(field.getId()))
                        .orElseThrow(() -> badRequest(
                                "Option '%s' does not belong to field '%s'.".formatted(request.getOptionUuid(), field.getKey())));
                value.singleOptionId = option.getId();
            }
            case MULTI_SELECT -> {
                rejectOtherChannels(field, hasText, hasNumber, hasBoolean, hasDate, hasOption);
                if (!hasOptions) {
                    return null;
                }
                List<UUID> distinctOptionUuids = request.getOptionUuids().stream().distinct().toList();
                List<Long> optionIds = new ArrayList<>();
                for (UUID optionUuid : distinctOptionUuids) {
                    FieldOptionEntity option = fieldOptionRepository.findByUuid(optionUuid)
                            .filter(o -> o.getDeletedAt() == null && o.getFieldId().equals(field.getId()))
                            .orElseThrow(() -> badRequest(
                                    "Option '%s' does not belong to field '%s'.".formatted(optionUuid, field.getKey())));
                    optionIds.add(option.getId());
                }
                value.multiOptionIds = optionIds;
            }
        }

        return value;
    }

    private void rejectOtherChannels(CategorySchemaFieldEntity field, boolean... others) {
        for (boolean other : others) {
            if (other) {
                throw badRequest("Value type does not match the expected type for field '%s'.".formatted(field.getKey()));
            }
        }
    }

    private CategoryFormFieldOptionResponse toOptionResponse(FieldOptionEntity option) {
        return new CategoryFormFieldOptionResponse()
                .optionUuid(option.getUuid())
                .value(option.getValue())
                .label(option.getLabel())
                .labelSr(option.getLabelSr())
                .displayOrder(option.getDisplayOrder());
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private static final class PreparedValue {
        CategorySchemaFieldEntity field;
        String valueText;
        BigDecimal valueNumber;
        Boolean valueBoolean;
        LocalDate valueDate;
        Long singleOptionId;
        List<Long> multiOptionIds;
    }
}

