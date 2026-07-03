package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves and validates dynamic category-schema field filters (query params of the form
 * {@code field.<key>=value}) into JPA {@link Specification} predicates for item search
 * (Marketplace Schema Engine, Phase 6).
 *
 * <p>Only fields belonging to the category's current ACTIVE schema, with {@code filterable=true}
 * and not soft-deleted, may be used as filters. Unknown or non-filterable field keys, missing
 * category context, and unparseable values all result in an {@link ApiException} with
 * {@link HttpStatus#BAD_REQUEST}, so client behavior is explicit and consistent.
 */
@Component
@RequiredArgsConstructor
public class ItemFieldFilterSupport {

    private final CategorySchemaRepository categorySchemaRepository;
    private final CategorySchemaFieldRepository categorySchemaFieldRepository;
    private final FieldOptionRepository fieldOptionRepository;

    /**
     * Builds one {@link Specification} per requested filter field. Returns an empty list when
     * {@code rawFilters} is null/empty. Throws BAD_REQUEST when filters are requested without a
     * category, when a field key is unknown or not filterable, or when a value cannot be parsed
     * for the field's type.
     */
    public List<Specification<ItemEntity>> buildSpecifications(Long categoryId, Map<String, List<String>> rawFilters) {
        if (rawFilters == null || rawFilters.isEmpty()) {
            return List.of();
        }

        if (categoryId == null) {
            throw badRequest("Field filters require categoryUuid to be provided.");
        }

        CategorySchemaEntity schema = categorySchemaRepository
                .findByCategoryIdAndStatusAndDeletedAtIsNull(categoryId, CategorySchemaStatus.ACTIVE)
                .orElseThrow(() -> badRequest(
                        "This category does not have an active schema; field filters are not supported."));

        List<CategorySchemaFieldEntity> fields = categorySchemaFieldRepository
                .findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(schema.getId());
        Map<String, CategorySchemaFieldEntity> fieldsByKey = new LinkedHashMap<>();
        for (CategorySchemaFieldEntity field : fields) {
            fieldsByKey.put(field.getKey(), field);
        }

        List<Specification<ItemEntity>> specs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : rawFilters.entrySet()) {
            String key = entry.getKey();
            List<String> rawValues = entry.getValue() == null ? List.of() : entry.getValue().stream()
                    .filter(v -> v != null && !v.isBlank())
                    .toList();
            if (rawValues.isEmpty()) {
                continue;
            }

            CategorySchemaFieldEntity field = fieldsByKey.get(key);
            if (field == null) {
                throw badRequest("Unknown filter field '%s'.".formatted(key));
            }
            if (!field.isFilterable()) {
                throw badRequest("Field '%s' is not filterable.".formatted(key));
            }

            specs.add(buildSpecForField(field, rawValues));
        }

        return specs;
    }

    private Specification<ItemEntity> buildSpecForField(CategorySchemaFieldEntity field, List<String> rawValues) {
        CategorySchemaFieldType type = field.getFieldType();
        String first = rawValues.get(0);

        return switch (type) {
            case TEXT -> ItemSpecifications.fieldTextContains(field.getId(), first);
            case NUMBER -> ItemSpecifications.fieldNumberEquals(field.getId(), parseNumber(field, first));
            case BOOLEAN -> ItemSpecifications.fieldBooleanEquals(field.getId(), parseBoolean(field, first));
            case DATE -> ItemSpecifications.fieldDateEquals(field.getId(), parseDate(field, first));
            case SINGLE_SELECT ->
                    ItemSpecifications.fieldSingleOptionIn(field.getId(), resolveOptionIds(field, rawValues));
            case MULTI_SELECT ->
                    ItemSpecifications.fieldMultiOptionIn(field.getId(), resolveOptionIds(field, rawValues));
        };
    }

    private BigDecimal parseNumber(CategorySchemaFieldEntity field, String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw badRequest("Invalid numeric value '%s' for field '%s'.".formatted(raw, field.getKey()));
        }
    }

    private Boolean parseBoolean(CategorySchemaFieldEntity field, String raw) {
        String normalized = raw.trim().toLowerCase();
        if (normalized.equals("true")) {
            return Boolean.TRUE;
        }
        if (normalized.equals("false")) {
            return Boolean.FALSE;
        }
        throw badRequest("Invalid boolean value '%s' for field '%s'; expected 'true' or 'false'.".formatted(raw, field.getKey()));
    }

    private LocalDate parseDate(CategorySchemaFieldEntity field, String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw badRequest("Invalid date value '%s' for field '%s'; expected ISO-8601 (yyyy-MM-dd).".formatted(raw, field.getKey()));
        }
    }

    private List<Long> resolveOptionIds(CategorySchemaFieldEntity field, List<String> rawValues) {
        List<Long> optionIds = new ArrayList<>();
        for (String raw : rawValues) {
            optionIds.add(resolveOptionId(field, raw.trim()));
        }
        return optionIds;
    }

    private Long resolveOptionId(CategorySchemaFieldEntity field, String raw) {
        Optional<FieldOptionEntity> byUuid = tryParseUuid(raw)
                .flatMap(fieldOptionRepository::findByUuid)
                .filter(o -> o.getDeletedAt() == null && o.getFieldId().equals(field.getId()));
        if (byUuid.isPresent()) {
            return byUuid.get().getId();
        }

        FieldOptionEntity option = fieldOptionRepository
                .findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(field.getId(), raw)
                .orElseThrow(() -> badRequest(
                        "Unknown option value '%s' for field '%s'.".formatted(raw, field.getKey())));
        return option.getId();
    }

    private Optional<UUID> tryParseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}

