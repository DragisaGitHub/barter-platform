package com.barterplatform.application.catalog.service.impl;

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
import com.barterplatform.application.catalog.mapper.AdminCategorySchemaMapper;
import com.barterplatform.application.catalog.service.AdminCategorySchemaService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaEntity;
import com.barterplatform.domain.catalog.entity.CategorySchemaFieldEntity;
import com.barterplatform.domain.catalog.entity.FieldOptionEntity;
import com.barterplatform.domain.catalog.enums.CategorySchemaFieldType;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaFieldRepository;
import com.barterplatform.infrastructure.catalog.repository.CategorySchemaRepository;
import com.barterplatform.infrastructure.catalog.repository.FieldOptionRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCategorySchemaServiceImpl implements AdminCategorySchemaService {

    private static final String DEFAULT_SCHEMA_SORT_FIELD = "version";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "version",
            "status",
            "createdAt",
            "updatedAt",
            "deletedAt",
            "uuid");

    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final CategoryRepository categoryRepository;
    private final CategorySchemaRepository categorySchemaRepository;
    private final CategorySchemaFieldRepository categorySchemaFieldRepository;
    private final FieldOptionRepository fieldOptionRepository;
    private final AdminCategorySchemaMapper mapper;

    public AdminCategorySchemaServiceImpl(
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            CategoryRepository categoryRepository,
            CategorySchemaRepository categorySchemaRepository,
            CategorySchemaFieldRepository categorySchemaFieldRepository,
            FieldOptionRepository fieldOptionRepository,
            AdminCategorySchemaMapper mapper) {
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.categoryRepository = categoryRepository;
        this.categorySchemaRepository = categorySchemaRepository;
        this.categorySchemaFieldRepository = categorySchemaFieldRepository;
        this.fieldOptionRepository = fieldOptionRepository;
        this.mapper = mapper;
    }

    @Override
    public CategorySchemaPagedResponse searchSchemas(
            Integer page,
            Integer size,
            String sort,
            UUID categoryUuid,
            CategorySchemaStatus status,
            Boolean includeDeleted) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_SCHEMA_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Specification<CategorySchemaEntity> specification = Boolean.TRUE.equals(includeDeleted)
                ? (root, query, cb) -> cb.conjunction()
                : CategorySchemaSpecifications.deletedAtIsNull();

        if (categoryUuid != null) {
            CategoryEntity category = getCategoryEntity(categoryUuid);
            specification = specification.and(CategorySchemaSpecifications.categoryIdEquals(category.getId()));
        }

        if (status != null) {
            specification = specification.and(CategorySchemaSpecifications.statusEquals(mapper.toDomainStatus(status)));
        }

        Page<CategorySchemaEntity> schemaPage = categorySchemaRepository.findAll(specification, pageRequest.pageable());
        List<com.barterplatform.api.model.CategorySchemaResponse1> content = schemaPage.getContent().stream()
                .map(this::toPagedItemResponse)
                .toList();

        return pageResponseMapper.toCategorySchemaPagedResponse(schemaPage, content, pageRequest.sort());
    }

    @Override
    @Transactional
    public CategorySchemaResponse createSchema(UUID categoryUuid, CreateCategorySchemaRequest request) {
        if (request == null) {
            throw badRequest("Category schema payload is required.");
        }

        CategoryEntity category = getCategoryEntity(categoryUuid);
        String name = requiredText(request.getName(), "Category schema name is required.");

        int nextVersion = categorySchemaRepository.findMaxVersionByCategoryId(category.getId()) + 1;

        CategorySchemaEntity entity = new CategorySchemaEntity();
        entity.setCategoryId(category.getId());
        entity.setVersion(nextVersion);
        entity.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.DRAFT);
        entity.setName(name);
        entity.setDescription(normalizeText(request.getDescription()));

        CategorySchemaEntity saved = categorySchemaRepository.save(entity);
        return mapper.toResponse(saved, category, List.of(), Map.of());
    }

    @Override
    public CategorySchemaResponse getSchema(UUID schemaUuid) {
        return toFullResponse(getSchemaEntity(schemaUuid));
    }

    @Override
    @Transactional
    public CategorySchemaResponse updateSchema(UUID schemaUuid, UpdateCategorySchemaRequest request) {
        if (request == null) {
            throw badRequest("Category schema payload is required.");
        }

        CategorySchemaEntity schema = getSchemaEntity(schemaUuid);

        if (request.getName() != null) {
            schema.setName(requiredText(request.getName(), "Category schema name must not be blank."));
        }
        if (request.getDescription() != null) {
            schema.setDescription(normalizeText(request.getDescription()));
        }

        CategorySchemaEntity saved = categorySchemaRepository.save(schema);
        return toFullResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSchema(UUID schemaUuid) {
        CategorySchemaEntity schema = getSchemaEntity(schemaUuid);
        if (schema.getStatus() == com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "The active schema for this category cannot be deleted. Activate another schema first.");
        }
        if (schema.getDeletedAt() == null) {
            schema.setDeletedAt(OffsetDateTime.now());
            categorySchemaRepository.save(schema);
        }
    }

    @Override
    @Transactional
    public CategorySchemaResponse activateSchema(UUID schemaUuid) {
        CategorySchemaEntity schema = getSchemaEntity(schemaUuid);

        categorySchemaRepository
                .findByCategoryIdAndStatusAndDeletedAtIsNull(
                        schema.getCategoryId(), com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE)
                .filter(activeSchema -> !activeSchema.getId().equals(schema.getId()))
                .ifPresent(activeSchema -> {
                    activeSchema.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ARCHIVED);
                    categorySchemaRepository.save(activeSchema);
                });

        schema.setStatus(com.barterplatform.domain.catalog.enums.CategorySchemaStatus.ACTIVE);
        CategorySchemaEntity saved = categorySchemaRepository.save(schema);
        return toFullResponse(saved);
    }

    @Override
    @Transactional
    public CategorySchemaFieldResponse createField(UUID schemaUuid, CreateCategorySchemaFieldRequest request) {
        if (request == null) {
            throw badRequest("Category schema field payload is required.");
        }

        CategorySchemaEntity schema = getSchemaEntity(schemaUuid);
        String key = requiredText(request.getKey(), "Field key is required.");
        String label = requiredText(request.getLabel(), "Field label is required.");
        if (request.getFieldType() == null) {
            throw badRequest("Field type is required.");
        }

        if (categorySchemaFieldRepository.existsBySchemaIdAndKeyAndDeletedAtIsNull(schema.getId(), key)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Field key '%s' already exists on this schema.".formatted(key));
        }

        CategorySchemaFieldEntity entity = new CategorySchemaFieldEntity();
        entity.setSchemaId(schema.getId());
        entity.setKey(key);
        entity.setLabel(label);
        entity.setLabelSr(normalizeText(request.getLabelSr()));
        entity.setHelpText(normalizeText(request.getHelpText()));
        entity.setFieldType(mapper.toDomainFieldType(request.getFieldType()));
        entity.setRequired(Boolean.TRUE.equals(request.getRequired()));
        entity.setSearchable(Boolean.TRUE.equals(request.getSearchable()));
        entity.setFilterable(Boolean.TRUE.equals(request.getFilterable()));
        entity.setSortable(Boolean.TRUE.equals(request.getSortable()));
        entity.setUnit(normalizeText(request.getUnit()));
        entity.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        entity.setValidationJson(normalizeText(request.getValidationJson()));

        CategorySchemaFieldEntity saved = categorySchemaFieldRepository.save(entity);
        return mapper.toFieldResponse(saved, List.of());
    }

    @Override
    @Transactional
    public CategorySchemaFieldResponse updateField(UUID fieldUuid, UpdateCategorySchemaFieldRequest request) {
        if (request == null) {
            throw badRequest("Category schema field payload is required.");
        }

        CategorySchemaFieldEntity field = getFieldEntity(fieldUuid);

        if (request.getLabel() != null) {
            field.setLabel(requiredText(request.getLabel(), "Field label must not be blank."));
        }
        if (request.getLabelSr() != null) {
            field.setLabelSr(normalizeText(request.getLabelSr()));
        }
        if (request.getHelpText() != null) {
            field.setHelpText(normalizeText(request.getHelpText()));
        }
        if (request.getRequired() != null) {
            field.setRequired(request.getRequired());
        }
        if (request.getSearchable() != null) {
            field.setSearchable(request.getSearchable());
        }
        if (request.getFilterable() != null) {
            field.setFilterable(request.getFilterable());
        }
        if (request.getSortable() != null) {
            field.setSortable(request.getSortable());
        }
        if (request.getUnit() != null) {
            field.setUnit(normalizeText(request.getUnit()));
        }
        if (request.getDisplayOrder() != null) {
            field.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getValidationJson() != null) {
            field.setValidationJson(normalizeText(request.getValidationJson()));
        }

        CategorySchemaFieldEntity saved = categorySchemaFieldRepository.save(field);
        List<FieldOptionEntity> options = fieldOptionRepository.findAllByFieldIdAndDeletedAtIsNullOrderByDisplayOrderAsc(saved.getId());
        return mapper.toFieldResponse(saved, options);
    }

    @Override
    @Transactional
    public void deleteField(UUID fieldUuid) {
        CategorySchemaFieldEntity field = getFieldEntity(fieldUuid);
        if (field.getDeletedAt() == null) {
            field.setDeletedAt(OffsetDateTime.now());
            categorySchemaFieldRepository.save(field);
        }
    }

    @Override
    @Transactional
    public FieldOptionResponse createOption(UUID fieldUuid, CreateFieldOptionRequest request) {
        if (request == null) {
            throw badRequest("Field option payload is required.");
        }

        CategorySchemaFieldEntity field = getFieldEntity(fieldUuid);
        if (!field.getFieldType().supportsOptions()) {
            throw badRequest(
                    "Options can only be added to SINGLE_SELECT or MULTI_SELECT fields. Field type is %s."
                            .formatted(field.getFieldType()));
        }

        String value = requiredText(request.getValue(), "Option value is required.");
        String label = requiredText(request.getLabel(), "Option label is required.");

        if (fieldOptionRepository.existsByFieldIdAndValueAndDeletedAtIsNull(field.getId(), value)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Option value '%s' already exists on this field.".formatted(value));
        }

        FieldOptionEntity entity = new FieldOptionEntity();
        entity.setFieldId(field.getId());
        entity.setValue(value);
        entity.setLabel(label);
        entity.setLabelSr(normalizeText(request.getLabelSr()));
        entity.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());

        FieldOptionEntity saved = fieldOptionRepository.save(entity);
        return mapper.toOptionResponse(saved);
    }

    @Override
    @Transactional
    public FieldOptionResponse updateOption(UUID optionUuid, UpdateFieldOptionRequest request) {
        if (request == null) {
            throw badRequest("Field option payload is required.");
        }

        FieldOptionEntity option = getOptionEntity(optionUuid);

        if (request.getValue() != null) {
            String value = requiredText(request.getValue(), "Option value must not be blank.");
            if (!value.equals(option.getValue())
                    && fieldOptionRepository.existsByFieldIdAndValueAndDeletedAtIsNull(option.getFieldId(), value)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCode.CONFLICT,
                        "Option value '%s' already exists on this field.".formatted(value));
            }
            option.setValue(value);
        }
        if (request.getLabel() != null) {
            option.setLabel(requiredText(request.getLabel(), "Option label must not be blank."));
        }
        if (request.getLabelSr() != null) {
            option.setLabelSr(normalizeText(request.getLabelSr()));
        }
        if (request.getDisplayOrder() != null) {
            option.setDisplayOrder(request.getDisplayOrder());
        }

        FieldOptionEntity saved = fieldOptionRepository.save(option);
        return mapper.toOptionResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOption(UUID optionUuid) {
        FieldOptionEntity option = getOptionEntity(optionUuid);
        if (option.getDeletedAt() == null) {
            option.setDeletedAt(OffsetDateTime.now());
            fieldOptionRepository.save(option);
        }
    }

    private CategorySchemaResponse toFullResponse(CategorySchemaEntity schema) {
        CategoryEntity category = categoryRepository.findById(schema.getCategoryId()).orElse(null);
        List<CategorySchemaFieldEntity> fields =
                categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(schema.getId());
        List<Long> fieldIds = fields.stream().map(CategorySchemaFieldEntity::getId).toList();
        Map<Long, List<FieldOptionEntity>> optionsByFieldId = fieldIds.isEmpty()
                ? Map.of()
                : fieldOptionRepository.findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(fieldIds).stream()
                        .collect(Collectors.groupingBy(FieldOptionEntity::getFieldId));
        return mapper.toResponse(schema, category, fields, optionsByFieldId);
    }

    private com.barterplatform.api.model.CategorySchemaResponse1 toPagedItemResponse(CategorySchemaEntity schema) {
        CategoryEntity category = categoryRepository.findById(schema.getCategoryId()).orElse(null);
        List<CategorySchemaFieldEntity> fields =
                categorySchemaFieldRepository.findAllBySchemaIdAndDeletedAtIsNullOrderByDisplayOrderAsc(schema.getId());
        List<Long> fieldIds = fields.stream().map(CategorySchemaFieldEntity::getId).toList();
        Map<Long, List<FieldOptionEntity>> optionsByFieldId = fieldIds.isEmpty()
                ? Map.of()
                : fieldOptionRepository.findAllByFieldIdInAndDeletedAtIsNullOrderByDisplayOrderAsc(fieldIds).stream()
                        .collect(Collectors.groupingBy(FieldOptionEntity::getFieldId));
        return mapper.toPagedItemResponse(schema, category, fields, optionsByFieldId);
    }

    private CategoryEntity getCategoryEntity(UUID categoryUuid) {
        CategoryEntity category = categoryRepository.findByUuid(categoryUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Category with uuid '%s' was not found.".formatted(categoryUuid)));
        if (category.getDeletedAt() != null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Category with uuid '%s' was not found.".formatted(categoryUuid));
        }
        return category;
    }

    private CategorySchemaEntity getSchemaEntity(UUID schemaUuid) {
        CategorySchemaEntity schema = categorySchemaRepository.findByUuid(schemaUuid)
                .orElseThrow(() -> notFound("Category schema with uuid '%s' was not found.".formatted(schemaUuid)));
        if (schema.getDeletedAt() != null) {
            throw notFound("Category schema with uuid '%s' was not found.".formatted(schemaUuid));
        }
        return schema;
    }

    private CategorySchemaFieldEntity getFieldEntity(UUID fieldUuid) {
        CategorySchemaFieldEntity field = categorySchemaFieldRepository.findByUuid(fieldUuid)
                .orElseThrow(() -> notFound("Category schema field with uuid '%s' was not found.".formatted(fieldUuid)));
        if (field.getDeletedAt() != null) {
            throw notFound("Category schema field with uuid '%s' was not found.".formatted(fieldUuid));
        }
        return field;
    }

    private FieldOptionEntity getOptionEntity(UUID optionUuid) {
        FieldOptionEntity option = fieldOptionRepository.findByUuid(optionUuid)
                .orElseThrow(() -> notFound("Field option with uuid '%s' was not found.".formatted(optionUuid)));
        if (option.getDeletedAt() != null) {
            throw notFound("Field option with uuid '%s' was not found.".formatted(optionUuid));
        }
        return option;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }
}

