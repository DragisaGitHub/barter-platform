package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.CreateSavedSearchRequest;
import com.barterplatform.api.model.SavedSearchCriteria;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import com.barterplatform.application.catalog.service.SavedSearchService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.SavedSearchEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.SavedSearchRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedSearchServiceImpl implements SavedSearchService {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_QUERY_LENGTH = 255;
    private static final int MAX_TAGS = 20;
    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name");
    private static final Set<String> ALLOWED_CATALOG_SORTS = Set.of(
            "createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "title,asc", "title,desc");

    private final SavedSearchRepository savedSearchRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SavedSearchServiceImpl(SavedSearchRepository savedSearchRepository,
                                  UserRepository userRepository,
                                  CategoryRepository categoryRepository,
                                  TagRepository tagRepository,
                                  PageRequestFactory pageRequestFactory,
                                  PageResponseMapper pageResponseMapper) {
        this.savedSearchRepository = savedSearchRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public SavedSearchResponse createSavedSearch(UUID currentUserUuid, CreateSavedSearchRequest request) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        String name = normalizeName(request.getName());
        SavedSearchCriteria criteria = normalizeCriteria(request.getCriteria());

        if (savedSearchRepository.existsByUserIdAndNameIgnoreCase(currentUser.getId(), name)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "A saved search named '%s' already exists.".formatted(name));
        }

        SavedSearchEntity entity = new SavedSearchEntity();
        entity.setUserId(currentUser.getId());
        entity.setName(name);
        entity.setCriteriaPayload(serializeCriteria(criteria));

        try {
            return toResponse(savedSearchRepository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "A saved search named '%s' already exists.".formatted(name));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SavedSearchPagedResponse listSavedSearches(UUID currentUserUuid, Integer page, Integer size, String sort) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_SORT : sort,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Page<SavedSearchEntity> savedSearchPage = savedSearchRepository.findByUserId(
                currentUser.getId(), pageRequest.pageable());
        List<SavedSearchResponse> content = savedSearchPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return pageResponseMapper.toSavedSearchPagedResponse(savedSearchPage, content, pageRequest.sort());
    }

    @Override
    public void deleteSavedSearch(UUID currentUserUuid, UUID savedSearchUuid) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        SavedSearchEntity savedSearch = savedSearchRepository.findByUuidAndUserId(savedSearchUuid, currentUser.getId())
                .orElseThrow(() -> notFound("Saved search with uuid '%s' was not found.", savedSearchUuid));
        savedSearchRepository.delete(savedSearch);
    }

    private SavedSearchResponse toResponse(SavedSearchEntity entity) {
        return new SavedSearchResponse()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .criteria(deserializeCriteria(entity.getCriteriaPayload()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
    }

    private String normalizeName(String value) {
        if (value == null || value.trim().isBlank()) {
            throw badRequest("Saved search name is required.");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw badRequest("Saved search name must be %d characters or fewer.".formatted(MAX_NAME_LENGTH));
        }
        return normalized;
    }

    private SavedSearchCriteria normalizeCriteria(SavedSearchCriteria criteria) {
        if (criteria == null) {
            throw badRequest("Saved search criteria is required.");
        }

        SavedSearchCriteria normalized = new SavedSearchCriteria();
        boolean hasCriteria = false;

        if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
            String query = criteria.getQ().trim();
            if (query.length() > MAX_QUERY_LENGTH) {
                throw badRequest("Search query must be %d characters or fewer.".formatted(MAX_QUERY_LENGTH));
            }
            normalized.setQ(query);
            hasCriteria = true;
        }

        if (criteria.getCategoryUuid() != null) {
            if (!categoryRepository.existsByUuid(criteria.getCategoryUuid())) {
                throw badRequest("Unsupported category filter.");
            }
            normalized.setCategoryUuid(criteria.getCategoryUuid());
            hasCriteria = true;
        }

        if (criteria.getTagUuids() != null && !criteria.getTagUuids().isEmpty()) {
            LinkedHashSet<UUID> tagUuids = new LinkedHashSet<>(criteria.getTagUuids());
            if (tagUuids.size() > MAX_TAGS) {
                throw badRequest("Saved searches support at most %d tag filters.".formatted(MAX_TAGS));
            }
            long existingTagCount = tagUuids.stream().filter(tagRepository::existsByUuid).count();
            if (existingTagCount != tagUuids.size()) {
                throw badRequest("Unsupported tag filter.");
            }
            normalized.setTagUuids(tagUuids);
            hasCriteria = true;
        }

        if (criteria.getCondition() != null) {
            normalized.setCondition(criteria.getCondition());
            hasCriteria = true;
        }

        if (criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
            String location = criteria.getLocation().trim().replaceAll("\\s+", " ");
            if (location.length() > MAX_QUERY_LENGTH) {
                throw badRequest("Location filter must be %d characters or fewer.".formatted(MAX_QUERY_LENGTH));
            }
            normalized.setLocation(location);
            hasCriteria = true;
        }

        if (criteria.getSort() != null && !criteria.getSort().isBlank()) {
            String sort = criteria.getSort().trim();
            if (!ALLOWED_CATALOG_SORTS.contains(sort)) {
                throw badRequest("Unsupported catalog sort for saved search.");
            }
            normalized.setSort(sort);
        }

        if (!hasCriteria) {
            throw badRequest("At least one search criterion is required.");
        }

        return normalized;
    }

    private String serializeCriteria(SavedSearchCriteria criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException ex) {
            throw badRequest("Saved search criteria could not be processed.");
        }
    }

    private SavedSearchCriteria deserializeCriteria(String payload) {
        try {
            return objectMapper.readValue(payload, SavedSearchCriteria.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.INTERNAL_ERROR,
                    "Saved search criteria could not be loaded.");
        }
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

