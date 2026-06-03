package com.barterplatform.web.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.AdminBetaFeedbackPagedResponse;
import com.barterplatform.api.model.AdminBetaFeedbackSummaryResponse;
import com.barterplatform.application.feedback.service.AdminBetaFeedbackService;
import com.barterplatform.web.admin.controller.AdminBetaFeedbackController;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
        classes = AdminBetaFeedbackAuthorizationMvcTest.TestApplication.class,
        properties = "server.servlet.context-path="
)
@AutoConfigureMockMvc
class AdminBetaFeedbackAuthorizationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedCannotListAdminBetaFeedback() throws Exception {
        mockMvc.perform(get("/admin/feedback/beta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotListAdminBetaFeedback() throws Exception {
        mockMvc.perform(get("/admin/feedback/beta"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorRoleCannotListAdminBetaFeedback() throws Exception {
        mockMvc.perform(get("/admin/feedback/beta"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanListAdminBetaFeedback() throws Exception {
        mockMvc.perform(get("/admin/feedback/beta"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotUpdateAdminBetaFeedback() throws Exception {
        mockMvc.perform(patch("/admin/feedback/beta/{feedbackUuid}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{" + "\"status\":\"REVIEWED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanUpdateAdminBetaFeedback() throws Exception {
        mockMvc.perform(patch("/admin/feedback/beta/{feedbackUuid}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{" + "\"status\":\"REVIEWED\"}"))
                .andExpect(status().isOk());
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, AdminBetaFeedbackController.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }

        @Bean
        AdminBetaFeedbackService adminBetaFeedbackService() {
            AdminBetaFeedbackService service = mock(AdminBetaFeedbackService.class);
            when(service.listFeedback(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(new AdminBetaFeedbackPagedResponse()
                            .content(List.of())
                            .page(0).size(20).totalElements(0L).totalPages(0)
                            .first(true).last(true).sort("createdAt,desc"));
            when(service.updateStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(new AdminBetaFeedbackSummaryResponse().uuid(UUID.randomUUID()));
            return service;
        }

        @Bean
        MockMvcBuilderCustomizer securityMockMvcCustomizer() {
            return builder -> builder.apply(springSecurity());
        }
    }
}

