package com.barterplatform.web.catalog.controller;

import com.barterplatform.api.model.*;
import com.barterplatform.application.catalog.service.*;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private CatalogQueryService catalogQueryService;
    private FavoriteItemService favoriteItemService;
    private ItemCommandService itemCommandService;
    private ItemImageService itemImageService;
    private RecommendationService recommendationService;
    private WishlistMatchService wishlistMatchService;

    @BeforeEach
    void setUp() {
        catalogQueryService = mock(CatalogQueryService.class);
        favoriteItemService = mock(FavoriteItemService.class);
        itemCommandService = mock(ItemCommandService.class);
        itemImageService = mock(ItemImageService.class);
        recommendationService = mock(RecommendationService.class);
        wishlistMatchService = mock(WishlistMatchService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogController(
                        catalogQueryService, favoriteItemService, itemCommandService, itemImageService, recommendationService, wishlistMatchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Public: listCategories ────────────────────────────────────

    @Test
    void listCategoriesShouldReturn200() throws Exception {
        CategoryResponse cat = new CategoryResponse()
                .uuid(UUID.randomUUID()).name("Books").slug("books").sortOrder(1);
        when(catalogQueryService.listCategories()).thenReturn(List.of(cat));

        mockMvc.perform(apiGet("/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Books"));

        verify(catalogQueryService).listCategories();
    }

    @Test
    void listPopularCategoriesShouldReturn200() throws Exception {
        PopularCategoryResponse category = new PopularCategoryResponse()
                .uuid(UUID.randomUUID())
                .name("Books")
                .slug("books")
                .sortOrder(1)
                .activeItemCount(12L);
        when(catalogQueryService.listPopularCategories(6)).thenReturn(List.of(category));

        mockMvc.perform(apiGet("/catalog/categories/popular").queryParam("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Books"))
                .andExpect(jsonPath("$[0].activeItemCount").value(12));

        verify(catalogQueryService).listPopularCategories(6);
    }

    // ── Public: listTags ─────────────────────────────────────────

    @Test
    void listTagsShouldReturn200() throws Exception {
        TagResponse tag = new TagResponse()
                .uuid(UUID.randomUUID()).name("Vintage").slug("vintage");
        when(catalogQueryService.listTags()).thenReturn(List.of(tag));

        mockMvc.perform(apiGet("/catalog/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vintage"));

        verify(catalogQueryService).listTags();
    }

    // ── Public: getCategoryFormSchema ─────────────────────────────

    @Test
    void getCategoryFormSchemaShouldReturn200WithoutAuthentication() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        CategoryFormFieldOptionResponse option = new CategoryFormFieldOptionResponse()
                .optionUuid(UUID.randomUUID()).value("new").label("New").displayOrder(0);
        CategoryFormFieldResponse field = new CategoryFormFieldResponse()
                .fieldUuid(UUID.randomUUID())
                .key("condition")
                .label("Condition")
                .fieldType(CategorySchemaFieldType.SINGLE_SELECT)
                .required(true)
                .searchable(true)
                .filterable(true)
                .sortable(false)
                .displayOrder(0)
                .options(List.of(option));
        CategoryFormSchemaResponse response = new CategoryFormSchemaResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(UUID.randomUUID())
                .schemaVersion(1)
                .schemaStatus(CategorySchemaStatus.ACTIVE)
                .fields(List.of(field));
        when(catalogQueryService.getCategoryFormSchema(categoryUuid)).thenReturn(response);

        // No authentication is set up (SecurityContextHolder is cleared in setUp), confirming the endpoint is public.
        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/form-schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryUuid").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.fields[0].key").value("condition"))
                .andExpect(jsonPath("$.fields[0].options[0].value").value("new"));

        verify(catalogQueryService).getCategoryFormSchema(categoryUuid);
    }

    @Test
    void getCategoryFormSchemaShouldReturn200WithEmptyFieldsWhenNoActiveSchema() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        CategoryFormSchemaResponse response = new CategoryFormSchemaResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(null)
                .schemaVersion(null)
                .schemaStatus(null)
                .fields(List.of());
        when(catalogQueryService.getCategoryFormSchema(categoryUuid)).thenReturn(response);

        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/form-schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryUuid").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.schemaUuid").doesNotExist())
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void getCategoryFormSchemaShouldReturn404WhenCategoryUnknown() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        when(catalogQueryService.getCategoryFormSchema(categoryUuid))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                        com.barterplatform.common.exception.ErrorCode.NOT_FOUND,
                        "Category with uuid '%s' was not found.".formatted(categoryUuid)));

        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/form-schema"))
                .andExpect(status().isNotFound());
    }

    // ── Public: searchItems ──────────────────────────────────────

    @Test
    void searchItemsShouldReturn200() throws Exception {
        ItemPagedResponse pagedResponse = new ItemPagedResponse()
                .content(List.of(new ItemSummaryResponse().uuid(UUID.randomUUID()).title("Widget")))
                .page(0).size(20).totalElements(1L).totalPages(1).first(true).last(true);
        when(catalogQueryService.searchItems(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("location", "Belgrade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Widget"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(catalogQueryService).searchItems(eq(0), eq(20), any(), any(), any(), any(), any(), eq("Belgrade"), any());
    }

    @Test
    void searchItemsShouldExtractFieldFiltersFromQueryString() throws Exception {
        ItemPagedResponse pagedResponse = new ItemPagedResponse()
                .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0).first(true).last(true);
        when(catalogQueryService.searchItems(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("field.brand", "Samsung")
                        .queryParam("field.has5g", "true"))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Map<String, java.util.List<String>>> captor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(catalogQueryService).searchItems(any(), any(), any(), any(), any(), any(), any(), any(), captor.capture());
        java.util.Map<String, java.util.List<String>> fieldFilters = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(List.of("Samsung"), fieldFilters.get("brand"));
        org.junit.jupiter.api.Assertions.assertEquals(List.of("true"), fieldFilters.get("has5g"));
    }

    // ── Public: getCategoryFilters ───────────────────────────────

    @Test
    void getCategoryFiltersShouldReturn200WithoutAuthentication() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        CategoryFormFieldOptionResponse option = new CategoryFormFieldOptionResponse()
                .optionUuid(UUID.randomUUID()).value("samsung").label("Samsung").displayOrder(0);
        CategoryFilterFieldResponse filterField = new CategoryFilterFieldResponse()
                .fieldUuid(UUID.randomUUID())
                .key("brand")
                .label("Brand")
                .fieldType(CategorySchemaFieldType.SINGLE_SELECT)
                .displayOrder(0)
                .options(List.of(option));
        CategoryFiltersResponse response = new CategoryFiltersResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(UUID.randomUUID())
                .schemaVersion(1)
                .filters(List.of(filterField));
        when(catalogQueryService.getCategoryFilters(categoryUuid)).thenReturn(response);

        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryUuid").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.filters[0].key").value("brand"))
                .andExpect(jsonPath("$.filters[0].options[0].value").value("samsung"));

        verify(catalogQueryService).getCategoryFilters(categoryUuid);
    }

    @Test
    void getCategoryFiltersShouldReturn200WithEmptyFiltersWhenNoActiveSchema() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        CategoryFiltersResponse response = new CategoryFiltersResponse()
                .categoryUuid(categoryUuid)
                .schemaUuid(null)
                .schemaVersion(null)
                .filters(List.of());
        when(catalogQueryService.getCategoryFilters(categoryUuid)).thenReturn(response);

        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryUuid").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.filters").isArray())
                .andExpect(jsonPath("$.filters").isEmpty());
    }

    @Test
    void getCategoryFiltersShouldReturn404WhenCategoryUnknown() throws Exception {
        UUID categoryUuid = UUID.randomUUID();
        when(catalogQueryService.getCategoryFilters(categoryUuid))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                        com.barterplatform.common.exception.ErrorCode.NOT_FOUND,
                        "Category with uuid '%s' was not found.".formatted(categoryUuid)));

        mockMvc.perform(apiGet("/categories/" + categoryUuid + "/filters"))
                .andExpect(status().isNotFound());
    }

    // ── Public: getItemByUuid ────────────────────────────────────

    @Test
    void getItemByUuidShouldReturn200() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        ItemDetailResponse detail = new ItemDetailResponse().uuid(itemUuid).title("My Book");
        when(catalogQueryService.getItemByUuid(itemUuid, null, false)).thenReturn(detail);

        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Book"));

        verify(catalogQueryService).getItemByUuid(itemUuid, null, false);
    }

    @Test
    void listRecommendationsShouldReturnAnonymousFallback() throws Exception {
        ItemSummaryResponse item = new ItemSummaryResponse().uuid(UUID.randomUUID()).title("Recommended Book");
        RecommendationPagedResponse response = new RecommendationPagedResponse()
                .content(List.of(new RecommendationItemResponse()
                        .item(item)
                        .reason(RecommendationReason.POPULAR_RECENTLY)))
                .page(0).size(12).totalElements(1L).totalPages(1).first(true).last(true);
        when(recommendationService.listRecommendations(null, 0, 12, "recommendationScore,desc")).thenReturn(response);

        mockMvc.perform(apiGet("/catalog/recommendations")
                        .queryParam("page", "0")
                        .queryParam("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].item.title").value("Recommended Book"))
                .andExpect(jsonPath("$.content[0].reason").value("POPULAR_RECENTLY"));

        verify(recommendationService).listRecommendations(null, 0, 12, "recommendationScore,desc");
    }

    // ── Authenticated: createItem ────────────────────────────────

    @Test
    void createItemUnauthenticatedShouldFail() throws Exception {
        // No SecurityContext set → principal is null → NullPointerException or 500
        // In real app with security filter it would be 401, but in standalone MockMvc
        // without security filter we can only verify the controller requires the principal
        mockMvc.perform(apiPost("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test",
                                  "categoryUuid": "c0a80101-0001-4000-8000-000000000001",
                                  "condition": "GOOD"
                                }
                                """))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(itemCommandService);
    }

    @Test
    void createItemAuthenticatedShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID catUuid = UUID.fromString("c0a80101-0001-4000-8000-000000000001");
        ItemDetailResponse response = new ItemDetailResponse()
                .uuid(UUID.randomUUID()).title("Test");
        when(itemCommandService.createItem(eq(USER_UUID), any(CreateItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(apiPost("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test",
                                  "categoryUuid": "c0a80101-0001-4000-8000-000000000001",
                                  "condition": "GOOD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test"));

        verify(itemCommandService).createItem(eq(USER_UUID), any(CreateItemRequest.class));
    }

    // ── Authenticated: listMyItems ───────────────────────────────

    @Test
    void listMyItemsShouldDelegateToCatalogQueryService() throws Exception {
        setAuthenticatedUser();

        ItemPagedResponse pagedResponse = new ItemPagedResponse()
                .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0)
                .first(true).last(true);
        when(catalogQueryService.listMyItems(eq(USER_UUID), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/catalog/items/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(catalogQueryService).listMyItems(eq(USER_UUID), any(), any(), any(), any());
    }

    @Test
    void listFavoriteItemsShouldDelegateToFavoriteItemService() throws Exception {
        setAuthenticatedUser();

        ItemPagedResponse pagedResponse = new ItemPagedResponse()
                .content(List.of(new ItemSummaryResponse().uuid(UUID.randomUUID()).title("Wishlisted Item")))
                .page(0).size(20).totalElements(1L).totalPages(1).first(true).last(true);
        when(favoriteItemService.listFavoriteItems(eq(USER_UUID), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/catalog/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Wishlisted Item"));

        verify(favoriteItemService).listFavoriteItems(eq(USER_UUID), any(), any(), any());
    }

    @Test
    void favoriteItemShouldDelegateToFavoriteItemService() throws Exception {
        setAuthenticatedUser();

        UUID itemUuid = UUID.randomUUID();
        when(favoriteItemService.favoriteItem(USER_UUID, itemUuid))
                .thenReturn(new MessageResponse().message("Item favorited successfully."));

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item favorited successfully."));

        verify(favoriteItemService).favoriteItem(USER_UUID, itemUuid);
    }

    @Test
    void unfavoriteItemShouldDelegateToFavoriteItemService() throws Exception {
        setAuthenticatedUser();

        UUID itemUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/favorite"))
                .andExpect(status().isNoContent());

        verify(favoriteItemService).unfavoriteItem(USER_UUID, itemUuid);
    }

    // ── Authenticated: archiveItem ───────────────────────────────

    @Test
    void archiveItemShouldDelegateToItemCommandService() throws Exception {
        setAuthenticatedUser();

        UUID itemUuid = UUID.randomUUID();
        ItemDetailResponse response = new ItemDetailResponse()
                .uuid(itemUuid).title("Archived Item")
                .status(com.barterplatform.api.model.ItemStatus.ARCHIVED);
        when(itemCommandService.archiveItem(eq(USER_UUID), eq(itemUuid), any()))
                .thenReturn(response);

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(itemCommandService).archiveItem(eq(USER_UUID), eq(itemUuid), any(ArchiveItemRequest.class));
    }

    // ── Authenticated: updateItem ────────────────────────────────

    @Test
    void updateItemShouldDelegateToItemCommandService() throws Exception {
        setAuthenticatedUser();

        UUID itemUuid = UUID.randomUUID();
        ItemDetailResponse response = new ItemDetailResponse()
                .uuid(itemUuid).title("Updated Title");
        when(itemCommandService.updateItem(eq(USER_UUID), eq(itemUuid), any()))
                .thenReturn(response);

        mockMvc.perform(apiPatch("/catalog/items/" + itemUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        verify(itemCommandService).updateItem(eq(USER_UUID), eq(itemUuid), any());
    }


    // ── Helpers ──────────────────────────────────────────────────

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

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
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

