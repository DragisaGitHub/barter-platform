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

import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.CreateCategoryRequest;
import com.barterplatform.api.model.UpdateCategoryRequest;
import com.barterplatform.application.catalog.mapper.AdminCategoryMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
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
class AdminCategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private AdminCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminCategoryServiceImpl(
                new PageRequestFactory(),
                new PageResponseMapper(),
                categoryRepository,
                new AdminCategoryMapper());
    }

    @Test
    void createCategorySuccess() {
        UUID parentUuid = UUID.randomUUID();
        CategoryEntity parent = category(10L, parentUuid, "Collectibles", "collectibles");
        when(categoryRepository.findByUuid(parentUuid)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsBySlug("board-games")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("Board Games")
                .slug("board-games")
                .description("Indoor fun")
                .parentUuid(parentUuid)
                .sortOrder(7);

        var response = service.createCategory(request);

        assertEquals("Board Games", response.getName());
        assertEquals("board-games", response.getSlug());
        assertEquals("Indoor fun", response.getDescription());
        assertEquals(parentUuid, response.getParentUuid());
        assertEquals("Collectibles", response.getParentName());
        assertEquals(7, response.getSortOrder());
        assertFalse(response.getDeleted());
    }

    @Test
    void createCategoryGeneratesSlugWhenOmitted() {
        when(categoryRepository.existsBySlug("board-games-puzzles")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = service.createCategory(new CreateCategoryRequest().name("Board Games & Puzzles"));

        assertEquals("board-games-puzzles", response.getSlug());

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("board-games-puzzles", captor.getValue().getSlug());
        assertEquals(0, captor.getValue().getSortOrder());
    }

    @Test
    void createCategoryRejectsDuplicateSlug() {
        when(categoryRepository.existsBySlug("books")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createCategory(new CreateCategoryRequest().name("Books").slug("books")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Category slug 'books' already exists.", exception.getMessage());

        verify(categoryRepository, never()).save(any(CategoryEntity.class));
    }

    @Test
    void updateCategorySuccess() {
        UUID categoryUuid = UUID.randomUUID();
        UUID parentUuid = UUID.randomUUID();
        CategoryEntity existing = category(1L, categoryUuid, "Books", "books");
        existing.setDescription("Old description");
        CategoryEntity parent = category(2L, parentUuid, "Media", "media");

        when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByUuid(parentUuid)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsBySlugAndUuidNot("rare-books", categoryUuid)).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        UpdateCategoryRequest request = new UpdateCategoryRequest()
                .name("Rare Books")
                .slug("rare-books")
                .description("  Updated description  ")
                .parentUuid(parentUuid)
                .sortOrder(3);

        var response = service.updateCategory(categoryUuid, request);

        assertEquals("Rare Books", response.getName());
        assertEquals("rare-books", response.getSlug());
        assertEquals("Updated description", response.getDescription());
        assertEquals(parentUuid, response.getParentUuid());
        assertEquals("Media", response.getParentName());
        assertEquals(3, response.getSortOrder());
    }

    @Test
    void updateCategoryRejectsDuplicateSlug() {
        UUID categoryUuid = UUID.randomUUID();
        CategoryEntity existing = category(1L, categoryUuid, "Books", "books");
        when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndUuidNot("games", categoryUuid)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateCategory(categoryUuid, new UpdateCategoryRequest().slug("games")));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Category slug 'games' already exists.", exception.getMessage());
    }

    @Test
    void deleteCategorySoftDeletes() {
        UUID categoryUuid = UUID.randomUUID();
        CategoryEntity existing = category(1L, categoryUuid, "Books", "books");
        when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteCategory(categoryUuid);

        assertNotNull(existing.getDeletedAt());
        verify(categoryRepository).save(existing);
    }

    @Test
    void adminSearchCanIncludeDeletedWhenRequested() {
        CategoryEntity active = category(1L, UUID.randomUUID(), "Books", "books");
        CategoryEntity deleted = category(2L, UUID.randomUUID(), "Archived", "archived");
        deleted.setDeletedAt(OffsetDateTime.now());
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active, deleted)));

        AdminCategoryPagedResponse response = service.searchCategories(0, 20, null, null, true);

        assertEquals(2, response.getContent().size());
        assertEquals("Books", response.getContent().get(0).getName());
        assertEquals("Archived", response.getContent().get(1).getName());
        assertTrue(response.getContent().get(1).getDeleted());
    }

    private CategoryEntity category(Long id, UUID uuid, String name, String slug) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setUuid(uuid);
        entity.setName(name);
        entity.setSlug(slug);
        entity.setSortOrder(0);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-14T10:15:30Z"));
        return entity;
    }

    private CategoryEntity persisted(CategoryEntity entity) {
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


