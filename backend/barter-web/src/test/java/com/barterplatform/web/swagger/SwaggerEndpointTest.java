package com.barterplatform.web.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import com.barterplatform.web.security.SecurityConfig;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = SwaggerEndpointTest.TestApplication.class)
@AutoConfigureMockMvc
class SwaggerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeSwaggerUiWithoutAuthentication() throws Exception {
        // Swagger UI may respond with 200 or 302 redirect - both indicate it is accessible (not 401/403)
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(anyOf(is(200), is(302))));
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }
    }
}

