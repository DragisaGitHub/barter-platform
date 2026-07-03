package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.SchemaFieldValueRequest;
import com.barterplatform.api.model.SchemaFieldValueResponse;
import com.barterplatform.common.exception.ApiException;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemFieldValueSupportTest {

    private static final Long ITEM_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long SCHEMA_ID = 100L;

    @Mock private CategorySchemaRepository categorySchemaRepository;
    @Mock private CategorySchemaFieldRepository categorySchemaFieldRepository;
    @Mock private FieldOptionRepository fieldOptionRepository;
    @Mock private ItemFieldValueRepository itemFieldValueRepository;
    @Mock private ItemFieldValueOptionRepository itemFieldValueOptionRepository;

    private ItemFieldValueSupport support;

    private final List<ItemFieldValueEntity> persistedValues = new ArrayList<>();
    private final List<ItemFieldValueOptionEntity> persistedOptionLinks = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1000L);

    private CategorySchemaFieldEntity textField;
    private CategorySchemaFieldEntity numberField;
    private CategorySchemaFieldEntity boolField;
    private CategorySchemaFieldEntity dateField;
    private CategorySchemaFieldEntity singleField;
    private CategorySchemaFieldEntity multiField;
    private FieldOptionEntity redOption;
    private FieldOptionEntity sizeS;
    private FieldOptionEntity sizeM;

    @BeforeEach
    void setUp() {
        support = new ItemFieldValueSupport(categorySchemaRepository, categorySchemaFieldRepository,
                fieldOptionRepository, itemFieldValueRepository, itemFieldValueOptionRepository);

        textField = field(1L, "brand", CategorySchemaFieldType.TEXT, false);
        numberField = field(2L, "weight", CategorySchemaFieldType.NUMBER, false);
        boolField = field(3L, "isNew", CategorySchemaFieldType.BOOLEAN, false);
        dateField = field(4L, "purchaseDate", CategorySchemaFieldType.DATE, false);
        singleField = field(5L, "color", CategorySchemaFieldType.SINGLE_SELECT, false);
        multiField = field(6L, "sizes", CategorySchemaFieldType.MULTI_SELECT, false);
        redOption = option(50L, singleField.getId(), "red");
        sizeS = option(60L, multiField.getId(), "S");
        sizeM = option(61L, multiField.getId(), "M");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private CategorySchemaEntity schema() {
        CategorySchemaEntity s = new CategorySchemaEntity();
        s.setId(SCHEMA_ID);
        s.setUuid(UUID.randomUUID());
        s.setCategoryId(CATEGORY_ID);
        s.setVersion(1);
        s.setStatus(CategorySchemaStatus.ACTIVE);
        s.setName("Schema");
        s.setCreatedAt(OffsetDateTime.now());
        return s;
    }

    private CategorySchemaFieldEntity field(Long id, String key, CategorySchemaFieldType type, boolean required) {
        CategorySchemaFieldEntity f = new CategorySchemaFieldEntity();
        f.setId(id);
        f.setUuid(UUID.randomUUID());
        f.setSchemaId(SCHEMA_ID);
        f.setKey(key);
        f.setLabel(key);
        f.setFieldType(type);
        f.setRequired(required);
        f.setDisplayOrder(id.intValue());
        f.setCreatedAt(OffsetDateTime.now());
        return f;
    }

    private FieldOptionEntity option(Long id, Long fieldId, String value) {
        FieldOptionEntity o = new FieldOptionEntity();
        o.setId(id);
        o.setUuid(UUID.randomUUID());
        o.setFieldId(fieldId);
        o.setValue(value);
        o.setLabel(value);
        o.setDisplayOrder(0);
        o.setCreatedAt(OffsetDateTime.now());
        return o;
    }

    private List<CategorySchemaFieldEntity> allFields() {
        return List.of(textField, numberField, boolField, dateField, singleField, multiField);
    }

    private void stubActiveSchema() {
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(schema()));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(SCHEMA_ID))
                .thenReturn(allFields());
    }

    private void stubPersistence() {
        lenient().when(itemFieldValueRepository.findByItemId(ITEM_ID)).thenAnswer(inv -> new ArrayList<>(persistedValues));
        lenient().when(itemFieldValueRepository.save(any(ItemFieldValueEntity.class))).thenAnswer(inv -> {
            ItemFieldValueEntity entity = inv.getArgument(0);
            entity.setId(idSequence.incrementAndGet());
            persistedValues.add(entity);
            return entity;
        });
        lenient().when(itemFieldValueOptionRepository.save(any(ItemFieldValueOptionEntity.class))).thenAnswer(inv -> {
            ItemFieldValueOptionEntity entity = inv.getArgument(0);
            entity.setId(idSequence.incrementAndGet());
            persistedOptionLinks.add(entity);
            return entity;
        });
        lenient().when(itemFieldValueOptionRepository.findByItemFieldValueIdIn(any()))
                .thenAnswer(inv -> new ArrayList<>(persistedOptionLinks));
        lenient().doAnswer(inv -> {
            persistedValues.removeIf(v -> ITEM_ID.equals(v.getItemId()));
            return null;
        }).when(itemFieldValueRepository).deleteByItemId(ITEM_ID);
        lenient().doAnswer(inv -> {
            java.util.Collection<Long> ids = inv.getArgument(0);
            persistedOptionLinks.removeIf(link -> ids.contains(link.getItemFieldValueId()));
            return null;
        }).when(itemFieldValueOptionRepository).deleteByItemFieldValueIdIn(any());
        lenient().when(categorySchemaFieldRepository.findAllById(any())).thenReturn(allFields());
        lenient().when(fieldOptionRepository.findAllById(any())).thenReturn(List.of(redOption, sizeS, sizeM));
    }

    private void stubOptionLookups() {
        lenient().when(fieldOptionRepository.findByUuid(redOption.getUuid())).thenReturn(Optional.of(redOption));
        lenient().when(fieldOptionRepository.findByUuid(sizeS.getUuid())).thenReturn(Optional.of(sizeS));
        lenient().when(fieldOptionRepository.findByUuid(sizeM.getUuid())).thenReturn(Optional.of(sizeM));
    }

    // ── replaceValues: happy paths ───────────────────────────────

    @Nested
    @DisplayName("replaceValues")
    class ReplaceValues {

        @Test
        @DisplayName("persists TEXT, NUMBER, BOOLEAN, DATE, SINGLE_SELECT and MULTI_SELECT values")
        void persistsAllFieldTypes() {
            stubActiveSchema();
            stubPersistence();
            stubOptionLookups();

            List<SchemaFieldValueRequest> requests = List.of(
                    new SchemaFieldValueRequest(textField.getUuid()).valueText("Acme"),
                    new SchemaFieldValueRequest(numberField.getUuid()).valueNumber(2.5),
                    new SchemaFieldValueRequest(boolField.getUuid()).valueBoolean(true),
                    new SchemaFieldValueRequest(dateField.getUuid()).valueDate(LocalDate.of(2024, 1, 15)),
                    new SchemaFieldValueRequest(singleField.getUuid()).optionUuid(redOption.getUuid()),
                    new SchemaFieldValueRequest(multiField.getUuid()).optionUuids(List.of(sizeS.getUuid(), sizeM.getUuid())));

            List<SchemaFieldValueResponse> result = support.replaceValues(ITEM_ID, CATEGORY_ID, requests);

            assertEquals(6, result.size());
            SchemaFieldValueResponse textResponse = findByKey(result, "brand");
            assertEquals("Acme", textResponse.getValueText());
            SchemaFieldValueResponse numberResponse = findByKey(result, "weight");
            assertEquals(2.5, numberResponse.getValueNumber());
            SchemaFieldValueResponse boolResponse = findByKey(result, "isNew");
            assertEquals(true, boolResponse.getValueBoolean());
            SchemaFieldValueResponse dateResponse = findByKey(result, "purchaseDate");
            assertEquals(LocalDate.of(2024, 1, 15), dateResponse.getValueDate());
            SchemaFieldValueResponse singleResponse = findByKey(result, "color");
            assertEquals(1, singleResponse.getOptions().size());
            assertEquals("red", singleResponse.getOptions().get(0).getValue());
            SchemaFieldValueResponse multiResponse = findByKey(result, "sizes");
            assertEquals(2, multiResponse.getOptions().size());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when a required field is missing")
        void missingRequiredFieldFails() {
            textField.setRequired(true);
            stubActiveSchema();

            ApiException ex = assertThrows(ApiException.class,
                    () -> support.replaceValues(ITEM_ID, CATEGORY_ID, List.of()));
            assertEquals(400, ex.getStatus().value());
            verify(itemFieldValueRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when value type does not match field type")
        void wrongTypeFails() {
            stubActiveSchema();

            List<SchemaFieldValueRequest> requests = List.of(
                    new SchemaFieldValueRequest(textField.getUuid()).valueText("Acme").valueNumber(5.0));

            ApiException ex = assertThrows(ApiException.class,
                    () -> support.replaceValues(ITEM_ID, CATEGORY_ID, requests));
            assertEquals(400, ex.getStatus().value());
            verify(itemFieldValueRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when field does not belong to the active schema")
        void fieldFromAnotherSchemaFails() {
            stubActiveSchema();
            UUID foreignFieldUuid = UUID.randomUUID();

            List<SchemaFieldValueRequest> requests = List.of(
                    new SchemaFieldValueRequest(foreignFieldUuid).valueText("Acme"));

            ApiException ex = assertThrows(ApiException.class,
                    () -> support.replaceValues(ITEM_ID, CATEGORY_ID, requests));
            assertEquals(400, ex.getStatus().value());
            verify(itemFieldValueRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when option does not belong to the field")
        void optionFromAnotherFieldFails() {
            stubActiveSchema();
            FieldOptionEntity foreignOption = option(70L, multiField.getId(), "foreign");
            when(fieldOptionRepository.findByUuid(foreignOption.getUuid())).thenReturn(Optional.of(foreignOption));

            List<SchemaFieldValueRequest> requests = List.of(
                    new SchemaFieldValueRequest(singleField.getUuid()).optionUuid(foreignOption.getUuid()));

            ApiException ex = assertThrows(ApiException.class,
                    () -> support.replaceValues(ITEM_ID, CATEGORY_ID, requests));
            assertEquals(400, ex.getStatus().value());
            verify(itemFieldValueRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when category has no active schema but values are provided")
        void noActiveSchemaWithValuesFails() {
            when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(CATEGORY_ID, CategorySchemaStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            List<SchemaFieldValueRequest> requests = List.of(
                    new SchemaFieldValueRequest(UUID.randomUUID()).valueText("Acme"));

            ApiException ex = assertThrows(ApiException.class,
                    () -> support.replaceValues(ITEM_ID, CATEGORY_ID, requests));
            assertEquals(400, ex.getStatus().value());
        }

        @Test
        @DisplayName("replacing values clears fields that are no longer present")
        void updateClearsRemovedValues() {
            stubActiveSchema();
            stubPersistence();
            stubOptionLookups();

            support.replaceValues(ITEM_ID, CATEGORY_ID, List.of(
                    new SchemaFieldValueRequest(textField.getUuid()).valueText("Acme"),
                    new SchemaFieldValueRequest(numberField.getUuid()).valueNumber(2.5)));
            assertEquals(2, persistedValues.size());

            List<SchemaFieldValueResponse> result = support.replaceValues(ITEM_ID, CATEGORY_ID, List.of(
                    new SchemaFieldValueRequest(textField.getUuid()).valueText("Updated")));

            assertEquals(1, result.size());
            assertEquals("Updated", result.get(0).getValueText());
            verify(itemFieldValueRepository, org.mockito.Mockito.atLeastOnce()).deleteByItemId(ITEM_ID);
        }
    }

    // ── loadResponses ────────────────────────────────────────────

    @Nested
    @DisplayName("loadResponses")
    class LoadResponses {

        @Test
        @DisplayName("returns empty list when item has no field values")
        void emptyWhenNoValues() {
            when(itemFieldValueRepository.findByItemId(ITEM_ID)).thenReturn(List.of());

            List<SchemaFieldValueResponse> result = support.loadResponses(ITEM_ID);

            assertTrue(result.isEmpty());
        }
    }

    private SchemaFieldValueResponse findByKey(List<SchemaFieldValueResponse> responses, String key) {
        return responses.stream()
                .filter(r -> key.equals(r.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No response found for key " + key));
    }
}

