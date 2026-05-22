package com.barterplatform.web.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.ReportPagedResponse;
import com.barterplatform.api.model.PermissionCode;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.application.catalog.service.AdminCategoryService;
import com.barterplatform.web.admin.controller.AdminListingsController;
import com.barterplatform.web.admin.controller.AdminReportsController;
import com.barterplatform.application.catalog.service.ListingModerationService;
import com.barterplatform.web.admin.controller.AdminCategoriesController;
import com.barterplatform.application.identity.service.PermissionService;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.application.identity.service.UserPreferenceService;
import com.barterplatform.application.identity.service.UserQueryService;
import com.barterplatform.application.moderation.service.ReportService;
import com.barterplatform.web.identity.controller.PermissionsController;
import com.barterplatform.web.identity.controller.UsersController;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = RoleBasedAuthorizationMvcTest.TestApplication.class,
        properties = "server.servlet.context-path="
)
@AutoConfigureMockMvc
class RoleBasedAuthorizationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Users endpoint ---

    @Test
    void unauthenticatedCannotListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleIsForbiddenFromListingUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorRoleCanListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    // --- Permissions endpoint ---

    @Test
    void unauthenticatedCannotListPermissions() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotListPermissions() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListPermissions() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isOk());
    }

    // --- Admin categories endpoint ---

    @Test
    void unauthenticatedCannotListAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotListAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorRoleCannotListAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk());
    }

    // --- Admin listings endpoint ---

    @Test
    void unauthenticatedCannotListAdminListings() throws Exception {
        mockMvc.perform(get("/admin/listings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotListAdminListings() throws Exception {
        mockMvc.perform(get("/admin/listings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListAdminListings() throws Exception {
        mockMvc.perform(get("/admin/listings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorRoleCanListAdminListings() throws Exception {
        mockMvc.perform(get("/admin/listings"))
                .andExpect(status().isOk());
    }

    // --- Admin reports endpoint ---

    @Test
    void unauthenticatedCannotListAdminReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotListAdminReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListAdminReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorRoleCanListAdminReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk());
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class,
            UsersController.class, PermissionsController.class, AdminCategoriesController.class,
            AdminListingsController.class, AdminReportsController.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }

        @Bean
        UserQueryService userQueryService() {
            UserQueryService service = mock(UserQueryService.class);
            when(service.listUsers(any(), any(), any())).thenReturn(
                    new UserPagedResponse()
                            .content(List.of())
                            .page(0).size(20).totalElements(0L).totalPages(0)
                            .first(true).last(true));
            return service;
        }

        @Bean
        UserManagementService userManagementService() {
            return mock(UserManagementService.class);
        }

        @Bean
        UserPreferenceService userPreferenceService() {
            return mock(UserPreferenceService.class);
        }

        @Bean
        PermissionService permissionService() {
            PermissionService service = mock(PermissionService.class);
            when(service.listPermissions()).thenReturn(List.of(
                    new PermissionResponse()
                            .uuid(UUID.randomUUID())
                            .code(PermissionCode.USER_VIEW)
                            .name("User View")
                            .description("Allows viewing users")
                            .createdAt(OffsetDateTime.now())));
            return service;
        }

        @Bean
        AdminCategoryService adminCategoryService() {
            AdminCategoryService service = mock(AdminCategoryService.class);
            when(service.searchCategories(any(), any(), any(), any(), any())).thenReturn(
                    new AdminCategoryPagedResponse()
                            .content(List.of())
                            .page(0).size(20).totalElements(0L).totalPages(0)
                            .first(true).last(true).sort("sortOrder,asc"));
            return service;
        }

        @Bean
        AdminListingQueryService adminListingQueryService() {
            AdminListingQueryService service = mock(AdminListingQueryService.class);
            when(service.listListings(any(), any(), any(), any(), any(), any(), any())).thenReturn(
                    new AdminListingPagedResponse()
                            .content(List.of())
                            .page(0).size(20).totalElements(0L).totalPages(0)
                            .first(true).last(true).sort("createdAt,desc"));
            return service;
        }

        @Bean
        ListingModerationService listingModerationService() {
            return mock(ListingModerationService.class);
        }

        @Bean
        ReportService reportService() {
            ReportService service = mock(ReportService.class);
            when(service.listReports(any(), any(), any(), any(), any(), any())).thenReturn(
                    new ReportPagedResponse()
                            .content(List.of())
                            .page(0).size(20).totalElements(0L).totalPages(0)
                            .first(true).last(true).sort("createdAt,desc"));
            return service;
        }

        /**
         * Spring Boot 4 no longer auto-registers SecurityMockMvcConfigurer with @AutoConfigureMockMvc.
         * This bean ensures @WithMockUser wires the security context into MockMvc correctly.
         */
        @Bean
        MockMvcBuilderCustomizer securityMockMvcCustomizer() {
            return builder -> builder.apply(springSecurity());
        }
    }
}

