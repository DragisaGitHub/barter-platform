package com.barterplatform.web.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.SavedSearchCriteria;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import com.barterplatform.application.catalog.service.SavedSearchService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SavedSearchesControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private SavedSearchService savedSearchService;

    @BeforeEach
    void setUp() {
        savedSearchService = mock(SavedSearchService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SavedSearchesController(savedSearchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSavedSearchShouldDelegateToService() throws Exception {
        setAuthenticatedUser();

        UUID savedSearchUuid = UUID.randomUUID();
        SavedSearchResponse response = new SavedSearchResponse()
                .uuid(savedSearchUuid)
                .name("Kids books")
                .criteria(new SavedSearchCriteria().q("books"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now());
        org.mockito.Mockito.when(savedSearchService.createSavedSearch(eq(USER_UUID), any()))
                .thenReturn(response);

        mockMvc.perform(apiPost("/saved-searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kids books",
                                  "criteria": { "q": "books" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(savedSearchUuid.toString()))
                .andExpect(jsonPath("$.criteria.q").value("books"));

        verify(savedSearchService).createSavedSearch(eq(USER_UUID), any());
    }

    @Test
    void listSavedSearchesShouldDelegateToService() throws Exception {
        setAuthenticatedUser();

        SavedSearchPagedResponse response = new SavedSearchPagedResponse()
                .content(List.of(new SavedSearchResponse()
                        .uuid(UUID.randomUUID())
                        .name("Bike search")
                        .criteria(new SavedSearchCriteria().q("bike"))
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .sort("createdAt,desc");
        org.mockito.Mockito.when(savedSearchService.listSavedSearches(eq(USER_UUID), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(apiGet("/saved-searches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bike search"));

        verify(savedSearchService).listSavedSearches(eq(USER_UUID), any(), any(), any());
    }

    @Test
    void deleteSavedSearchShouldDelegateToService() throws Exception {
        setAuthenticatedUser();

        UUID savedSearchUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/saved-searches/" + savedSearchUuid))
                .andExpect(status().isNoContent());

        verify(savedSearchService).deleteSavedSearch(USER_UUID, savedSearchUuid);
    }

    private void setAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_UUID, "alice", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

