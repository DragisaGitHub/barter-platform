package com.barterplatform.web.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.AdminRemoveListingRequest;
import com.barterplatform.api.model.AdminRestoreListingRequest;
import com.barterplatform.api.model.ItemStatus;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.application.catalog.service.ListingModerationService;
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

class AdminListingsControllerMvcTest {

    private static final UUID ADMIN_UUID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private MockMvc mockMvc;
    private AdminListingQueryService adminListingQueryService;
    private ListingModerationService listingModerationService;

    @BeforeEach
    void setUp() {
        adminListingQueryService = mock(AdminListingQueryService.class);
        listingModerationService = mock(ListingModerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminListingsController(adminListingQueryService, listingModerationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
        setAdminUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListAdminListingsUsingOwnerQueryFilter() throws Exception {
        when(adminListingQueryService.listListings(eq(0), eq(20), eq("createdAt,desc"), eq(null), eq("Alice"), any(), eq(null)))
                .thenReturn(new AdminListingPagedResponse().content(List.of()).page(0).size(20).totalElements(0L).totalPages(0).first(true).last(true));

        mockMvc.perform(apiGet("/admin/listings")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));

        verify(adminListingQueryService).listListings(eq(0), eq(20), eq("createdAt,desc"), eq(null), eq("Alice"), eq(null), eq(null));
    }

    @Test
    void shouldGetAdminListingDetail() throws Exception {
        UUID listingUuid = UUID.randomUUID();
        when(adminListingQueryService.getListing(listingUuid))
                .thenReturn(new AdminListingDetailResponse().uuid(listingUuid).title("Desk"));

        mockMvc.perform(apiGet("/admin/listings/" + listingUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Desk"));

        verify(adminListingQueryService).getListing(listingUuid);
    }

    @Test
    void shouldRemoveAdminListing() throws Exception {
        UUID listingUuid = UUID.randomUUID();
        when(listingModerationService.removeListing(eq(ADMIN_UUID), eq(listingUuid), any(AdminRemoveListingRequest.class)))
                .thenReturn(new AdminListingDetailResponse().uuid(listingUuid).status(ItemStatus.REMOVED));

        mockMvc.perform(apiPost("/admin/listings/" + listingUuid + "/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reasonCode\": \"POLICY_VIOLATION\",
                                  \"userMessage\": \"Removed\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));

        verify(listingModerationService).removeListing(eq(ADMIN_UUID), eq(listingUuid), any(AdminRemoveListingRequest.class));
    }

    @Test
    void shouldRestoreAdminListing() throws Exception {
        UUID listingUuid = UUID.randomUUID();
        when(listingModerationService.restoreListing(eq(ADMIN_UUID), eq(listingUuid), any(AdminRestoreListingRequest.class)))
                .thenReturn(new AdminListingDetailResponse().uuid(listingUuid).status(ItemStatus.ACTIVE));

        mockMvc.perform(apiPost("/admin/listings/" + listingUuid + "/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reasonCode\": \"OWNER_REQUEST\",
                                  \"userMessage\": \"Restored\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(listingModerationService).restoreListing(eq(ADMIN_UUID), eq(listingUuid), any(AdminRestoreListingRequest.class));
    }

    private void setAdminUser() {
        AuthenticatedUser principal = new AuthenticatedUser(ADMIN_UUID, "admin", List.of("ADMIN"));
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
}

