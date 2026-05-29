package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CreateSavedSearchRequest;
import com.barterplatform.api.model.SavedSearchCriteria;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.SavedSearchEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.SavedSearchRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceImplTest {

    @Mock private SavedSearchRepository savedSearchRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private PageResponseMapper pageResponseMapper;

    private SavedSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SavedSearchServiceImpl(
                savedSearchRepository,
                userRepository,
                categoryRepository,
                tagRepository,
                new PageRequestFactory(),
                pageResponseMapper);
    }

    @Test
    @DisplayName("creates a saved search for the authenticated user")
    void createSavedSearchSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UUID tagUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");
        SavedSearchEntity saved = savedSearch(10L, UUID.randomUUID(), 1L, "Kids books", null);
        saved.setCriteriaPayload("{\"q\":\"books\",\"categoryUuid\":\"" + categoryUuid + "\",\"tagUuids\":[\"" + tagUuid + "\"],\"sort\":\"createdAt,desc\"}");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByUuid(categoryUuid)).thenReturn(true);
        when(tagRepository.existsByUuid(tagUuid)).thenReturn(true);
        when(savedSearchRepository.existsByUserIdAndNameIgnoreCase(1L, "Kids books")).thenReturn(false);
        when(savedSearchRepository.save(any(SavedSearchEntity.class))).thenReturn(saved);

        SavedSearchCriteria criteria = new SavedSearchCriteria()
                .q(" books ")
                .categoryUuid(categoryUuid)
                .tagUuids(Set.of(tagUuid))
                .sort("createdAt,desc");
        SavedSearchResponse response = service.createSavedSearch(
                userUuid,
                new CreateSavedSearchRequest().name("  Kids   books  ").criteria(criteria));

        assertNotNull(response);
        assertEquals("Kids books", response.getName());
        assertEquals("books", response.getCriteria().getQ());

        ArgumentCaptor<SavedSearchEntity> captor = ArgumentCaptor.forClass(SavedSearchEntity.class);
        verify(savedSearchRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("Kids books", captor.getValue().getName());
        assertEquals(true, captor.getValue().getCriteriaPayload().contains("\"q\":\"books\""));
    }

    @Test
    @DisplayName("rejects duplicate saved search names per user")
    void createSavedSearchRejectsDuplicateName() {
        UUID userUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(savedSearchRepository.existsByUserIdAndNameIgnoreCase(1L, "Morning finds")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.createSavedSearch(
                userUuid,
                new CreateSavedSearchRequest()
                        .name("Morning finds")
                        .criteria(new SavedSearchCriteria().q("bike"))));

        assertEquals(409, exception.getStatus().value());
        verify(savedSearchRepository, never()).save(any(SavedSearchEntity.class));
    }

    @Test
    @DisplayName("rejects unsupported criteria")
    void createSavedSearchRejectsUnsupportedCategory() {
        UUID userUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByUuid(categoryUuid)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> service.createSavedSearch(
                userUuid,
                new CreateSavedSearchRequest()
                        .name("Unknown category")
                        .criteria(new SavedSearchCriteria().categoryUuid(categoryUuid))));

        assertEquals(400, exception.getStatus().value());
        verify(savedSearchRepository, never()).save(any(SavedSearchEntity.class));
    }

    @Test
    @DisplayName("lists saved searches owned by the authenticated user")
    void listSavedSearchesSuccess() {
        UUID userUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");
        SavedSearchEntity saved = savedSearch(10L, UUID.randomUUID(), 1L, "Books", "{\"q\":\"books\"}");
        PageImpl<SavedSearchEntity> page = new PageImpl<>(
                List.of(saved),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                1);
        SavedSearchResponse summary = new SavedSearchResponse()
                .uuid(saved.getUuid())
                .name("Books")
                .criteria(new SavedSearchCriteria().q("books"))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt());
        SavedSearchPagedResponse expected = new SavedSearchPagedResponse()
                .content(List.of(summary))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .sort("createdAt,desc");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(savedSearchRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(pageResponseMapper.toSavedSearchPagedResponse(eq(page), any(), eq("createdAt,desc"))).thenReturn(expected);

        SavedSearchPagedResponse result = service.listSavedSearches(userUuid, 0, 20, null);

        assertEquals(1L, result.getTotalElements());
        assertEquals("Books", result.getContent().get(0).getName());
        verify(savedSearchRepository).findByUserId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("deletes only a saved search owned by the authenticated user")
    void deleteSavedSearchSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID savedSearchUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");
        SavedSearchEntity saved = savedSearch(10L, savedSearchUuid, 1L, "Books", "{\"q\":\"books\"}");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(savedSearchRepository.findByUuidAndUserId(savedSearchUuid, 1L)).thenReturn(Optional.of(saved));

        service.deleteSavedSearch(userUuid, savedSearchUuid);

        verify(savedSearchRepository).delete(saved);
    }

    @Test
    @DisplayName("returns not found when deleting another user's saved search")
    void deleteSavedSearchRejectsNonOwner() {
        UUID userUuid = UUID.randomUUID();
        UUID savedSearchUuid = UUID.randomUUID();
        UserEntity user = user(1L, userUuid, "alice");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(savedSearchRepository.findByUuidAndUserId(savedSearchUuid, 1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.deleteSavedSearch(userUuid, savedSearchUuid));

        assertEquals(404, exception.getStatus().value());
    }

    private UserEntity user(Long id, UUID uuid, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUuid(uuid);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private SavedSearchEntity savedSearch(Long id, UUID uuid, Long userId, String name, String criteriaPayload) {
        SavedSearchEntity savedSearch = new SavedSearchEntity();
        savedSearch.setId(id);
        savedSearch.setUuid(uuid);
        savedSearch.setUserId(userId);
        savedSearch.setName(name);
        savedSearch.setCriteriaPayload(criteriaPayload);
        savedSearch.setCreatedAt(OffsetDateTime.now());
        savedSearch.setUpdatedAt(OffsetDateTime.now());
        return savedSearch;
    }
}

