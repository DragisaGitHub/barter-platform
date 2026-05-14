package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.api.model.CreateCategoryRequest;
import com.barterplatform.api.model.UpdateCategoryRequest;
import com.barterplatform.application.catalog.mapper.AdminCategoryMapper;
import com.barterplatform.application.catalog.service.AdminCategoryService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private static final String DEFAULT_CATEGORY_SORT_FIELD = "sortOrder";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "slug",
            "sortOrder",
            "createdAt",
            "updatedAt",
            "deletedAt",
            "uuid");

    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final CategoryRepository categoryRepository;
    private final AdminCategoryMapper adminCategoryMapper;

    public AdminCategoryServiceImpl(
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            CategoryRepository categoryRepository,
            AdminCategoryMapper adminCategoryMapper) {
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.categoryRepository = categoryRepository;
        this.adminCategoryMapper = adminCategoryMapper;
    }

    @Override
    public AdminCategoryPagedResponse searchCategories(Integer page, Integer size, String sort, String q, Boolean includeDeleted) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort,
                DEFAULT_CATEGORY_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Specification<CategoryEntity> specification = Boolean.TRUE.equals(includeDeleted)
                ? (root, query, cb) -> cb.conjunction()
                : CategorySpecifications.deletedAtIsNull();
        if (q != null && !q.isBlank()) {
            specification = specification.and(CategorySpecifications.nameOrSlugContainsIgnoreCase(q.trim()));
        }

        Page<CategoryEntity> categoryPage = categoryRepository.findAll(specification, pageRequest.pageable());
        Map<Long, CategoryEntity> parentsById = resolveParents(categoryPage.getContent());

        return pageResponseMapper.toAdminCategoryPagedResponse(
                categoryPage,
                adminCategoryMapper.toResponseList(categoryPage.getContent(), parentsById),
                pageRequest.sort());
    }

    @Override
    @Transactional
    public AdminCategoryResponse createCategory(CreateCategoryRequest request) {
        if (request == null) {
            throw badRequest("Category payload is required.");
        }

        String name = requiredText(request.getName(), "Category name is required.");
        String slug = request.getSlug() == null || request.getSlug().isBlank()
                ? CategorySlugNormalizer.normalize(name)
                : CategorySlugNormalizer.normalize(request.getSlug());

        ensureUniqueSlug(slug, null);

        CategoryEntity entity = new CategoryEntity();
        entity.setName(name);
        entity.setSlug(slug);
        entity.setDescription(normalizeDescription(request.getDescription()));
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());

        CategoryEntity parent = resolveParent(request.getParentUuid(), null);
        entity.setParentId(parent != null ? parent.getId() : null);

        CategoryEntity saved = categoryRepository.save(entity);
        return adminCategoryMapper.toResponse(saved, parentMap(parent));
    }

    @Override
    public AdminCategoryResponse getCategory(UUID categoryUuid) {
        CategoryEntity category = getCategoryEntity(categoryUuid);
        CategoryEntity parent = category.getParentId() == null
                ? null
                : categoryRepository.findById(category.getParentId()).orElse(null);
        return adminCategoryMapper.toResponse(category, parentMap(parent));
    }

    @Override
    @Transactional
    public AdminCategoryResponse updateCategory(UUID categoryUuid, UpdateCategoryRequest request) {
        if (request == null) {
            throw badRequest("Category payload is required.");
        }

        CategoryEntity category = getCategoryEntity(categoryUuid);

        String resolvedName = category.getName();
        if (request.getName() != null) {
            resolvedName = requiredText(request.getName(), "Category name must not be blank.");
            category.setName(resolvedName);
        }

        if (request.getSlug() != null) {
            String source = request.getSlug().isBlank() ? resolvedName : request.getSlug();
            category.setSlug(CategorySlugNormalizer.normalize(source));
        }
        ensureUniqueSlug(category.getSlug(), category.getUuid());

        if (request.getDescription() != null) {
            category.setDescription(normalizeDescription(request.getDescription()));
        }

        CategoryEntity parent = null;
        if (request.getParentUuid() != null) {
            parent = resolveParent(request.getParentUuid(), category);
            category.setParentId(parent != null ? parent.getId() : null);
        } else if (category.getParentId() != null) {
            parent = categoryRepository.findById(category.getParentId()).orElse(null);
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        CategoryEntity saved = categoryRepository.save(category);
        return adminCategoryMapper.toResponse(saved, parentMap(parent));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryUuid) {
        CategoryEntity category = getCategoryEntity(categoryUuid);
        if (category.getDeletedAt() == null) {
            category.setDeletedAt(OffsetDateTime.now());
            categoryRepository.save(category);
        }
    }

    private CategoryEntity getCategoryEntity(UUID categoryUuid) {
        return categoryRepository.findByUuid(categoryUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Category with uuid '%s' was not found.".formatted(categoryUuid)));
    }

    private CategoryEntity resolveParent(UUID parentUuid, CategoryEntity currentCategory) {
        if (parentUuid == null) {
            return null;
        }
        if (currentCategory != null && parentUuid.equals(currentCategory.getUuid())) {
            throw badRequest("A category cannot be its own parent.");
        }

        CategoryEntity parent = categoryRepository.findByUuid(parentUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Parent category with uuid '%s' was not found.".formatted(parentUuid)));

        if (parent.getDeletedAt() != null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Parent category with uuid '%s' was not found.".formatted(parentUuid));
        }

        return parent;
    }

    private void ensureUniqueSlug(String slug, UUID currentCategoryUuid) {
        boolean exists = currentCategoryUuid == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndUuidNot(slug, currentCategoryUuid);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Category slug '%s' already exists.".formatted(slug));
        }
    }

    private Map<Long, CategoryEntity> resolveParents(List<CategoryEntity> categories) {
        List<Long> parentIds = categories.stream()
                .map(CategoryEntity::getParentId)
                .filter(parentId -> parentId != null)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, CategoryEntity> parentsById = new HashMap<>();
        for (CategoryEntity parent : categoryRepository.findAllById(parentIds)) {
            parentsById.put(parent.getId(), parent);
        }
        return parentsById;
    }

    private Map<Long, CategoryEntity> parentMap(CategoryEntity parent) {
        if (parent == null) {
            return Map.of();
        }
        return Map.of(parent.getId(), parent);
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}


