package com.barterplatform.web.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import com.barterplatform.application.profile.service.PublicProfileService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProfilesControllerMvcTest {

    private MockMvc mockMvc;
    private PublicProfileService publicProfileService;

    @BeforeEach
    void setUp() {
        publicProfileService = mock(PublicProfileService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProfilesController(publicProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── getPublicProfile ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /profiles/{userUuid}")
    class GetPublicProfile {

        @Test
        @DisplayName("returns 200 with profile for ACTIVE user")
        void returnsProfileForActiveUser() throws Exception {
            UUID uuid = UUID.randomUUID();
            PublicProfileResponse response = new PublicProfileResponse()
                    .uuid(uuid)
                    .username("alice")
                    .joinedAt(OffsetDateTime.now())
                    .activeItemCount(5)
                    .completedTradeCount(3)
                    .cancelledTradeCount(1)
                    .averageRating(null);

            when(publicProfileService.getPublicProfile(uuid)).thenReturn(response);

            mockMvc.perform(apiGet("/profiles/" + uuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.activeItemCount").value(5))
                    .andExpect(jsonPath("$.completedTradeCount").value(3))
                    .andExpect(jsonPath("$.cancelledTradeCount").value(1))
                    .andExpect(jsonPath("$.averageRating").doesNotExist())
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.roles").doesNotExist());

            verify(publicProfileService).getPublicProfile(uuid);
        }

        @Test
        @DisplayName("returns 404 for non-existing user")
        void returns404ForNonExistingUser() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(publicProfileService.getPublicProfile(uuid))
                    .thenThrow(new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found"));

            mockMvc.perform(apiGet("/profiles/" + uuid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("response does not contain email or roles")
        void responseDoesNotExposePrivateData() throws Exception {
            UUID uuid = UUID.randomUUID();
            PublicProfileResponse response = new PublicProfileResponse()
                    .uuid(uuid)
                    .username("alice")
                    .joinedAt(OffsetDateTime.now())
                    .activeItemCount(0)
                    .completedTradeCount(0)
                    .cancelledTradeCount(0);

            when(publicProfileService.getPublicProfile(uuid)).thenReturn(response);

            mockMvc.perform(apiGet("/profiles/" + uuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.roles").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.emailVerified").doesNotExist())
                    .andExpect(jsonPath("$.mfaEnabled").doesNotExist())
                    .andExpect(jsonPath("$.lastLoginAt").doesNotExist());
        }
    }

    // ── listPublicItems ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /profiles/{userUuid}/items")
    class ListPublicItems {

        @Test
        @DisplayName("returns 200 with items for ACTIVE user")
        void returnsItemsForActiveUser() throws Exception {
            UUID userUuid = UUID.randomUUID();
            ItemPagedResponse pagedResponse = new ItemPagedResponse()
                    .content(List.of(new ItemSummaryResponse().uuid(UUID.randomUUID()).title("Widget")))
                    .page(0).size(20).totalElements(1L).totalPages(1).first(true).last(true);

            when(publicProfileService.listPublicItems(eq(userUuid), any(), any(), any()))
                    .thenReturn(pagedResponse);

            mockMvc.perform(apiGet("/profiles/" + userUuid + "/items")
                            .queryParam("page", "0")
                            .queryParam("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Widget"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(publicProfileService).listPublicItems(eq(userUuid), eq(0), eq(20), any());
        }

        @Test
        @DisplayName("returns 404 for non-active user")
        void returns404ForNonActiveUser() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(publicProfileService.listPublicItems(eq(uuid), any(), any(), any()))
                    .thenThrow(new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found"));

            mockMvc.perform(apiGet("/profiles/" + uuid + "/items"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

