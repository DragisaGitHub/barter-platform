package com.barterplatform.web.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.ArchiveItemRequest;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.CreateItemRequest;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.application.catalog.service.CatalogQueryService;
import com.barterplatform.application.catalog.service.FavoriteItemService;
import com.barterplatform.application.catalog.service.ItemCommandService;
import com.barterplatform.application.catalog.service.ItemImageService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
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

class CatalogControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private CatalogQueryService catalogQueryService;
    private FavoriteItemService favoriteItemService;
    private ItemCommandService itemCommandService;
    private ItemImageService itemImageService;

    @BeforeEach
    void setUp() {
        catalogQueryService = mock(CatalogQueryService.class);
        favoriteItemService = mock(FavoriteItemService.class);
        itemCommandService = mock(ItemCommandService.class);
        itemImageService = mock(ItemImageService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogController(
                        catalogQueryService, favoriteItemService, itemCommandService, itemImageService))
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

    // ── Public: searchItems ──────────────────────────────────────

    @Test
    void searchItemsShouldReturn200() throws Exception {
        ItemPagedResponse pagedResponse = new ItemPagedResponse()
                .content(List.of(new ItemSummaryResponse().uuid(UUID.randomUUID()).title("Widget")))
                .page(0).size(20).totalElements(1L).totalPages(1).first(true).last(true);
        when(catalogQueryService.searchItems(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Widget"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(catalogQueryService).searchItems(eq(0), eq(20), any(), any(), any(), any(), any(), any());
    }

    // ── Public: getItemByUuid ────────────────────────────────────

    @Test
    void getItemByUuidShouldReturn200() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        ItemDetailResponse detail = new ItemDetailResponse().uuid(itemUuid).title("My Book");
        when(catalogQueryService.getItemByUuid(itemUuid)).thenReturn(detail);

        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Book"));

        verify(catalogQueryService).getItemByUuid(itemUuid);
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

    // ── removeItem returns 501 ───────────────────────────────────

    @Test
    void removeItemShouldReturn501() throws Exception {
        setAuthenticatedUser();

        UUID itemUuid = UUID.randomUUID();

        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid))
                .andExpect(status().isNotImplemented());

        verifyNoInteractions(itemCommandService);
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

