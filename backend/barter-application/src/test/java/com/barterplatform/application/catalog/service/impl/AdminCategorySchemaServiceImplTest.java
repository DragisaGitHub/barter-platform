package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CategorySchemaFieldType;
import com.barterplatform.api.model.CategorySchemaResponse;
import com.barterplatform.api.model.CreateCategorySchemaFieldRequest;
import com.barterplatform.api.model.CreateCategorySchemaRequest;
import com.barterplatform.api.model.CreateFieldOptionRequest;
import com.barterplatform.api.model.FieldOptionResponse;
import com.barterplatform.application.catalog.mapper.AdminCategorySchemaMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminCategorySchemaServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategorySchemaRepository categorySchemaRepository;

    @Mock
    private CategorySchemaFieldRepository categorySchemaFieldRepository;

    @Mock
    private FieldOptionRepository fieldOptionRepository;

    private AdminCategorySchemaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminCategorySchemaServiceImpl(
                new PageRequestFactory(),
                new PageResponseMapper(),
                categoryRepository,
                categorySchemaRepository,
                categorySchemaFieldRepository,
                fieldOptionRepository,
                new AdminCategorySchemaMapper());
    }

    @Test
    void createSchemaSuccessAssignsFirstVersionAndDraftStatus() {
        UUID categoryUuid = UUID.randomUUID();
        CategoryEntity category = category(1L, categoryUuid, "Electronics");
        when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(category));
        when(categorySchemaRepository.findMaxVersionByCategoryId(1L)).thenReturn(0);
        when(categorySchemaRepository.save(any(CategorySchemaEntity.class))).thenAnswer(invocation -> persistSchema(invocation.getArgument(0)));

        CategorySchemaResponse response = service.createSchema(
                categoryUuid,
                new CreateCategorySchemaRequest().name("Base spec").description("Initial"));

        assertEquals("Base spec", response.getName());
        assertEquals(1, response.getVersion());
        assertEquals(com.barterplatform.api.model.CategorySchemaStatus.DRAFT, response.getStatus());
        assertEquals(categoryUuid, response.getCategoryUuid());
    }

    @Test
    void createSchemaThrowsNotFoundWhenCategoryMissing() {
        UUID categoryUuid = UUID.randomUUID();
        when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createSchema(categoryUuid, new CreateCategorySchemaRequest().name("X")));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createFieldRejectsDuplicateKey() {
        UUID schemaUuid = UUID.randomUUID();
        CategorySchemaEntity schema = schema(1L, schemaUuid, 5L);
        when(categorySchemaRepository.findByUuid(schemaUuid)).thenReturn(Optional.of(schema));
        when(categorySchemaFieldRepository.existsBySchemaIdAndKeyAndDeletedAtIsNull(1L, "brand")).thenReturn(true);

        CreateCategorySchemaFieldRequest request = new CreateCategorySchemaFieldRequest()
                .key("brand")
                .label("Brand")
                .fieldType(CategorySchemaFieldType.TEXT);

        ApiException exception = assertThrows(ApiException.class, () -> service.createField(schemaUuid, request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(categorySchemaFieldRepository, never()).save(any(CategorySchemaFieldEntity.class));
    }

    @Test
    void createFieldSucceedsForUniqueKey() {
        UUID schemaUuid = UUID.randomUUID();
        CategorySchemaEntity schema = schema(1L, schemaUuid, 5L);
        when(categorySchemaRepository.findByUuid(schemaUuid)).thenReturn(Optional.of(schema));
        when(categorySchemaFieldRepository.existsBySchemaIdAndKeyAndDeletedAtIsNull(1L, "brand")).thenReturn(false);
        when(categorySchemaFieldRepository.save(any(CategorySchemaFieldEntity.class)))
                .thenAnswer(invocation -> persistField(invocation.getArgument(0)));

        var response = service.createField(schemaUuid, new CreateCategorySchemaFieldRequest()
                .key("brand")
                .label("Brand")
                .fieldType(CategorySchemaFieldType.TEXT));

        assertEquals("brand", response.getKey());
        assertEquals(CategorySchemaFieldType.TEXT, response.getFieldType());
    }

    @Test
    void createOptionRejectsNonSelectFieldTypes() {
        UUID fieldUuid = UUID.randomUUID();
        CategorySchemaFieldEntity field = field(1L, fieldUuid, 1L, "brand",
                com.barterplatform.domain.catalog.enums.CategorySchemaFieldType.TEXT);
        when(categorySchemaFieldRepository.findByUuid(fieldUuid)).thenReturn(Optional.of(field));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createOption(fieldUuid, new CreateFieldOptionRequest().value("red").label("Red")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fieldOptionRepository, never()).save(any(FieldOptionEntity.class));
    }

    @Test
    void createOptionSucceedsForSingleSelectField() {
        UUID fieldUuid = UUID.randomUUID();
        CategorySchemaFieldEntity field = field(1L, fieldUuid, 1L, "color",
                com.barterplatform.domain.catalog.enums.CategorySchemaFieldType.SINGLE_SELECT);
        when(categorySchemaFieldRepository.findByUuid(fieldUuid)).thenReturn(Optional.of(field));
        when(fieldOptionRepository.existsByFieldIdAndValueAndDeletedAtIsNull(1L, "red")).thenReturn(false);
        when(fieldOptionRepository.save(any(FieldOptionEntity.class))).thenAnswer(invocation -> persistOption(invocation.getArgument(0)));

        FieldOptionResponse response = service.createOption(fieldUuid, new CreateFieldOptionRequest().value("red").label("Red"));

        assertEquals("red", response.getValue());
        assertEquals("Red", response.getLabel());
    }

    @Test
    void createOptionRejectsDuplicateValue() {
        UUID fieldUuid = UUID.randomUUID();
        CategorySchemaFieldEntity field = field(1L, fieldUuid, 1L, "color",
                com.barterplatform.domain.catalog.enums.CategorySchemaFieldType.MULTI_SELECT);
        when(categorySchemaFieldRepository.findByUuid(fieldUuid)).thenReturn(Optional.of(field));
        when(fieldOptionRepository.existsByFieldIdAndValueAndDeletedAtIsNull(1L, "red")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createOption(fieldUuid, new CreateFieldOptionRequest().value("red").label("Red")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void activateSchemaArchivesPreviousActiveSchema() {
        UUID categoryUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID previousUuid = UUID.randomUUID();

        CategorySchemaEntity target = schema(2L, targetUuid, 10L);
        target.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.DRAFT);

        CategorySchemaEntity previousActive = schema(1L, previousUuid, 10L);
        previousActive.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE);

        when(categorySchemaRepository.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(categorySchemaRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(
                10L, com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE))
                .thenReturn(Optional.of(previousActive));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category(10L, categoryUuid, "Electronics")));
        when(categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(any()))
                .thenReturn(List.of());
        when(categorySchemaRepository.save(any(CategorySchemaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategorySchemaResponse response = service.activateSchema(targetUuid);

        assertEquals(com.barterplatform.api.model.CategorySchemaStatus.ACTIVE, response.getStatus());
        assertEquals(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ARCHIVED, previousActive.getStatus());
        verify(categorySchemaRepository).save(previousActive);
        verify(categorySchemaRepository).save(target);
    }

    @Test
    void deleteSchemaRejectsActiveSchema() {
        UUID schemaUuid = UUID.randomUUID();
        CategorySchemaEntity schema = schema(1L, schemaUuid, 10L);
        schema.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE);
        when(categorySchemaRepository.findByUuid(schemaUuid)).thenReturn(Optional.of(schema));

        ApiException exception = assertThrows(ApiException.class, () -> service.deleteSchema(schemaUuid));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(categorySchemaRepository, never()).save(any(CategorySchemaEntity.class));
    }

    @Test
    void deleteSchemaSoftDeletesNonActiveSchema() {
        UUID schemaUuid = UUID.randomUUID();
        CategorySchemaEntity schema = schema(1L, schemaUuid, 10L);
        schema.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.DRAFT);
        when(categorySchemaRepository.findByUuid(schemaUuid)).thenReturn(Optional.of(schema));
        when(categorySchemaRepository.save(any(CategorySchemaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteSchema(schemaUuid);

        assertEquals(true, schema.getDeletedAt() != null);
        verify(categorySchemaRepository).save(schema);
    }

    private CategoryEntity category(Long id, UUID uuid, String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setUuid(uuid);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        entity.setSortOrder(0);
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    private CategorySchemaEntity schema(Long id, UUID uuid, Long categoryId) {
        CategorySchemaEntity entity = new CategorySchemaEntity();
        entity.setId(id);
        entity.setUuid(uuid);
        entity.setCategoryId(categoryId);
        entity.setVersion(1);
        entity.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.DRAFT);
        entity.setName("Schema");
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    private CategorySchemaFieldEntity field(
            Long id, UUID uuid, Long schemaId, String key, com.barterplatform.domain.catalog.enums.CategorySchemaFieldType type) {
        CategorySchemaFieldEntity entity = new CategorySchemaFieldEntity();
        entity.setId(id);
        entity.setUuid(uuid);
        entity.setSchemaId(schemaId);
        entity.setKey(key);
        entity.setLabel(key);
        entity.setFieldType(type);
        entity.setDisplayOrder(0);
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    private CategorySchemaEntity persistSchema(CategorySchemaEntity entity) {
        if (entity.getId() == null) {
            entity.setId(999L);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        return entity;
    }

    private CategorySchemaFieldEntity persistField(CategorySchemaFieldEntity entity) {
        if (entity.getId() == null) {
            entity.setId(999L);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        return entity;
    }

    private FieldOptionEntity persistOption(FieldOptionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(999L);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        return entity;
    }
}

