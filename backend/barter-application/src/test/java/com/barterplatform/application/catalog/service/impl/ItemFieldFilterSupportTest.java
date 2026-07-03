package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import com.barterplatform.domain.catalog.enums.CategorySchemaStatus;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ItemFieldFilterSupportTest {

    @Mock private CategorySchemaRepository categorySchemaRepository;
    @Mock private CategorySchemaFieldRepository categorySchemaFieldRepository;
    @Mock private FieldOptionRepository fieldOptionRepository;

    private ItemFieldFilterSupport support;

    private static final Long CATEGORY_ID = 1L;
    private static final Long SCHEMA_ID = 2L;

    @BeforeEach
    void setUp() {
        support = new ItemFieldFilterSupport(categorySchemaRepository, categorySchemaFieldRepository, fieldOptionRepository);
    }

    private CategorySchemaEntity activeSchema() {
        CategorySchemaEntity schema = new CategorySchemaEntity();
        schema.setId(SCHEMA_ID);
        schema.setUuid(UUID.randomUUID());
        schema.setCategoryId(CATEGORY_ID);
        schema.setVersion(1);
        schema.setStatus(CategorySchemaStatus.ACTIVE);
        schema.setName("Electronics schema");
        schema.setCreatedAt(OffsetDateTime.now());
        return schema;
    }

    private CategorySchemaFieldEntity field(Long id, String key, CategorySchemaFieldType type, boolean filterable) {
        CategorySchemaFieldEntity field = new CategorySchemaFieldEntity();
        field.setId(id);
        field.setUuid(UUID.randomUUID());
        field.setSchemaId(SCHEMA_ID);
        field.setKey(key);
        field.setLabel(key);
        field.setFieldType(type);
        field.setFilterable(filterable);
        field.setCreatedAt(OffsetDateTime.now());
        return field;
    }

    @Test
    @DisplayName("returns empty list when no filters requested")
    void returnsEmptyWhenNoFilters() {
        assertTrue(support.buildSpecifications(CATEGORY_ID, Map.of()).isEmpty());
        assertTrue(support.buildSpecifications(CATEGORY_ID, null).isEmpty());
    }

    @Test
    @DisplayName("throws BAD_REQUEST when filters requested without categoryUuid")
    void throwsBadRequestWithoutCategory() {
        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(null, Map.of("brand", List.of("Samsung"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("throws BAD_REQUEST when category has no active schema")
    void throwsBadRequestWithoutActiveSchema() {
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(CATEGORY_ID, Map.of("brand", List.of("Samsung"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("throws BAD_REQUEST for unknown filter field key")
    void throwsBadRequestForUnknownField() {
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(CATEGORY_ID, Map.of("unknownField", List.of("x"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("throws BAD_REQUEST for non-filterable field")
    void throwsBadRequestForNonFilterableField() {
        CategorySchemaFieldEntity notes = field(3L, "notes", CategorySchemaFieldType.TEXT, false);
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(notes));

        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(CATEGORY_ID, Map.of("notes", List.of("hello"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("builds a spec for a BOOLEAN filterable field")
    void buildsSpecForBooleanField() {
        CategorySchemaFieldEntity has5g = field(4L, "has5g", CategorySchemaFieldType.BOOLEAN, true);
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(has5g));

        List<Specification<ItemEntity>> specs = support.buildSpecifications(CATEGORY_ID, Map.of("has5g", List.of("true")));

        assertEquals(1, specs.size());
    }

    @Test
    @DisplayName("throws BAD_REQUEST for invalid boolean value")
    void throwsBadRequestForInvalidBoolean() {
        CategorySchemaFieldEntity has5g = field(4L, "has5g", CategorySchemaFieldType.BOOLEAN, true);
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(has5g));

        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(CATEGORY_ID, Map.of("has5g", List.of("yes"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("builds a spec for a NUMBER filterable field")
    void buildsSpecForNumberField() {
        CategorySchemaFieldEntity storage = field(5L, "storage", CategorySchemaFieldType.NUMBER, true);
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(storage));

        List<Specification<ItemEntity>> specs = support.buildSpecifications(CATEGORY_ID, Map.of("storage", List.of("128")));

        assertEquals(1, specs.size());
    }

    @Test
    @DisplayName("builds a spec for a SINGLE_SELECT field resolved by option value")
    void buildsSpecForSingleSelectFieldByValue() {
        CategorySchemaFieldEntity brand = field(6L, "brand", CategorySchemaFieldType.SINGLE_SELECT, true);
        FieldOptionEntity samsungOption = new FieldOptionEntity();
        samsungOption.setId(60L);
        samsungOption.setUuid(UUID.randomUUID());
        samsungOption.setFieldId(6L);
        samsungOption.setValue("samsung");
        samsungOption.setLabel("Samsung");
        samsungOption.setCreatedAt(OffsetDateTime.now());

        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(brand));
        when(fieldOptionRepository.findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(6L, "Samsung"))
                .thenReturn(Optional.of(samsungOption));

        List<Specification<ItemEntity>> specs = support.buildSpecifications(CATEGORY_ID, Map.of("brand", List.of("Samsung")));

        assertEquals(1, specs.size());
    }

    @Test
    @DisplayName("throws BAD_REQUEST for unknown SINGLE_SELECT option value")
    void throwsBadRequestForUnknownOptionValue() {
        CategorySchemaFieldEntity brand = field(6L, "brand", CategorySchemaFieldType.SINGLE_SELECT, true);
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(brand));
        when(fieldOptionRepository.findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(6L, "Nokia"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> support.buildSpecifications(CATEGORY_ID, Map.of("brand", List.of("Nokia"))));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("builds a spec for a MULTI_SELECT field with multiple values")
    void buildsSpecForMultiSelectField() {
        CategorySchemaFieldEntity colors = field(7L, "colors", CategorySchemaFieldType.MULTI_SELECT, true);
        FieldOptionEntity red = new FieldOptionEntity();
        red.setId(70L);
        red.setFieldId(7L);
        red.setValue("red");
        red.setLabel("Red");
        red.setCreatedAt(OffsetDateTime.now());
        FieldOptionEntity blue = new FieldOptionEntity();
        blue.setId(71L);
        blue.setFieldId(7L);
        blue.setValue("blue");
        blue.setLabel("Blue");
        blue.setCreatedAt(OffsetDateTime.now());

        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(activeSchema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(List.of(colors));
        when(fieldOptionRepository.findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(7L, "red"))
                .thenReturn(Optional.of(red));
        when(fieldOptionRepository.findByFieldIdAndValueIgnoreCaseAndDeletedAtIsNull(7L, "blue"))
                .thenReturn(Optional.of(blue));

        List<Specification<ItemEntity>> specs = support.buildSpecifications(CATEGORY_ID, Map.of("colors", List.of("red", "blue")));

        assertEquals(1, specs.size());
    }
}

