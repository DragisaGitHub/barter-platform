package com.barterplatform.web.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.PermissionCode;
import com.barterplatform.api.model.PermissionResponse;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.application.identity.service.PermissionService;
import com.barterplatform.application.identity.service.UserManagementService;
import com.barterplatform.application.identity.service.UserQueryService;
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
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class,
            UsersController.class, PermissionsController.class})
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
    }
}

