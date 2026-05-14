package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.AdminTagPagedResponse;
import com.barterplatform.api.model.CreateTagRequest;
import com.barterplatform.api.model.UpdateTagRequest;
import com.barterplatform.application.catalog.mapper.AdminTagMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminTagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    private AdminTagServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminTagServiceImpl(
                new PageRequestFactory(),
                new PageResponseMapper(),
                tagRepository,
                new AdminTagMapper());
    }

    @Test
    void createTagSuccess() {
        when(tagRepository.existsBySlug("vintage")).thenReturn(false);
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        CreateTagRequest request = new CreateTagRequest()
                .name("Vintage")
                .slug("vintage");

        var response = service.createTag(request);

        assertEquals("Vintage", response.getName());
        assertEquals("vintage", response.getSlug());
        assertFalse(response.getDeleted());
    }

    @Test
    void createTagGeneratesSlugWhenOmitted() {
        when(tagRepository.existsBySlug("rare-finds")).thenReturn(false);
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = service.createTag(new CreateTagRequest().name("Rare Finds"));

        assertEquals("rare-finds", response.getSlug());

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).save(captor.capture());
        assertEquals("rare-finds", captor.getValue().getSlug());
    }

    @Test
    void createTagRejectsDuplicateSlug() {
        when(tagRepository.existsBySlug("vintage")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createTag(new CreateTagRequest().name("Vintage").slug("vintage")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Tag slug 'vintage' already exists.", exception.getMessage());

        verify(tagRepository, never()).save(any(TagEntity.class));
    }

    @Test
    void updateTagSuccess() {
        UUID tagUuid = UUID.randomUUID();
        TagEntity existing = tag(1L, tagUuid, "Vintage", "vintage");

        when(tagRepository.findByUuid(tagUuid)).thenReturn(Optional.of(existing));
        when(tagRepository.existsBySlugAndUuidNot("rare-vintage", tagUuid)).thenReturn(false);
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        UpdateTagRequest request = new UpdateTagRequest()
                .name("Rare Vintage")
                .slug("rare-vintage");

        var response = service.updateTag(tagUuid, request);

        assertEquals("Rare Vintage", response.getName());
        assertEquals("rare-vintage", response.getSlug());
    }

    @Test
    void updateTagRejectsDuplicateSlug() {
        UUID tagUuid = UUID.randomUUID();
        TagEntity existing = tag(1L, tagUuid, "Vintage", "vintage");
        when(tagRepository.findByUuid(tagUuid)).thenReturn(Optional.of(existing));
        when(tagRepository.existsBySlugAndUuidNot("collectible", tagUuid)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateTag(tagUuid, new UpdateTagRequest().slug("collectible")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Tag slug 'collectible' already exists.", exception.getMessage());
    }

    @Test
    void deleteTagSoftDeletes() {
        UUID tagUuid = UUID.randomUUID();
        TagEntity existing = tag(1L, tagUuid, "Vintage", "vintage");
        when(tagRepository.findByUuid(tagUuid)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteTag(tagUuid);

        assertNotNull(existing.getDeletedAt());
        verify(tagRepository).save(existing);
    }

    @Test
    void adminSearchCanIncludeDeletedWhenRequested() {
        TagEntity active = tag(1L, UUID.randomUUID(), "Vintage", "vintage");
        TagEntity deleted = tag(2L, UUID.randomUUID(), "Archived", "archived");
        deleted.setDeletedAt(OffsetDateTime.now());
        when(tagRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active, deleted)));

        AdminTagPagedResponse response = service.searchTags(0, 20, null, null, true);

        assertEquals(2, response.getContent().size());
        assertEquals("Vintage", response.getContent().get(0).getName());
        assertEquals("Archived", response.getContent().get(1).getName());
        assertTrue(response.getContent().get(1).getDeleted());
    }

    private TagEntity tag(Long id, UUID uuid, String name, String slug) {
        TagEntity entity = new TagEntity();
        entity.setId(id);
        entity.setUuid(uuid);
        entity.setName(name);
        entity.setSlug(slug);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
        return entity;
    }

    private TagEntity persisted(TagEntity entity) {
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

