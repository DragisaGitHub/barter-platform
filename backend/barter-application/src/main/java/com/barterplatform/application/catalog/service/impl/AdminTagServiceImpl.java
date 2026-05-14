package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.AdminTagPagedResponse;
import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.api.model.CreateTagRequest;
import com.barterplatform.api.model.UpdateTagRequest;
import com.barterplatform.application.catalog.mapper.AdminTagMapper;
import com.barterplatform.application.catalog.service.AdminTagService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminTagServiceImpl implements AdminTagService {

    private static final String DEFAULT_TAG_SORT_FIELD = "name";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "slug",
            "createdAt",
            "updatedAt",
            "deletedAt",
            "uuid");

    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final TagRepository tagRepository;
    private final AdminTagMapper adminTagMapper;

    public AdminTagServiceImpl(
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper,
            TagRepository tagRepository,
            AdminTagMapper adminTagMapper) {
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.tagRepository = tagRepository;
        this.adminTagMapper = adminTagMapper;
    }

    @Override
    public AdminTagPagedResponse searchTags(Integer page, Integer size, String sort, String q, Boolean includeDeleted) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort,
                DEFAULT_TAG_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Specification<TagEntity> specification = Boolean.TRUE.equals(includeDeleted)
                ? (root, query, cb) -> cb.conjunction()
                : TagSpecifications.deletedAtIsNull();
        if (q != null && !q.isBlank()) {
            specification = specification.and(TagSpecifications.nameOrSlugContainsIgnoreCase(q.trim()));
        }

        Page<TagEntity> tagPage = tagRepository.findAll(specification, pageRequest.pageable());
        return pageResponseMapper.toAdminTagPagedResponse(
                tagPage,
                adminTagMapper.toResponseList(tagPage.getContent()),
                pageRequest.sort());
    }

    @Override
    @Transactional
    public AdminTagResponse createTag(CreateTagRequest request) {
        if (request == null) {
            throw badRequest("Tag payload is required.");
        }

        String name = requiredText(request.getName(), "Tag name is required.");
        String slug = request.getSlug() == null || request.getSlug().isBlank()
                ? TagSlugNormalizer.normalize(name)
                : TagSlugNormalizer.normalize(request.getSlug());

        ensureUniqueSlug(slug, null);

        TagEntity entity = new TagEntity();
        entity.setName(name);
        entity.setSlug(slug);

        TagEntity saved = tagRepository.save(entity);
        return adminTagMapper.toResponse(saved);
    }

    @Override
    public AdminTagResponse getTag(UUID tagUuid) {
        return adminTagMapper.toResponse(getTagEntity(tagUuid));
    }

    @Override
    @Transactional
    public AdminTagResponse updateTag(UUID tagUuid, UpdateTagRequest request) {
        if (request == null) {
            throw badRequest("Tag payload is required.");
        }

        TagEntity tag = getTagEntity(tagUuid);

        String resolvedName = tag.getName();
        if (request.getName() != null) {
            resolvedName = requiredText(request.getName(), "Tag name must not be blank.");
            tag.setName(resolvedName);
        }

        if (request.getSlug() != null) {
            String source = request.getSlug().isBlank() ? resolvedName : request.getSlug();
            tag.setSlug(TagSlugNormalizer.normalize(source));
        }
        ensureUniqueSlug(tag.getSlug(), tag.getUuid());

        TagEntity saved = tagRepository.save(tag);
        return adminTagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagUuid) {
        TagEntity tag = getTagEntity(tagUuid);
        if (tag.getDeletedAt() == null) {
            tag.setDeletedAt(OffsetDateTime.now());
            tagRepository.save(tag);
        }
    }

    private TagEntity getTagEntity(UUID tagUuid) {
        return tagRepository.findByUuid(tagUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Tag with uuid '%s' was not found.".formatted(tagUuid)));
    }

    private void ensureUniqueSlug(String slug, UUID currentTagUuid) {
        boolean exists = currentTagUuid == null
                ? tagRepository.existsBySlug(slug)
                : tagRepository.existsBySlugAndUuidNot(slug, currentTagUuid);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Tag slug '%s' already exists.".formatted(slug));
        }
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}

