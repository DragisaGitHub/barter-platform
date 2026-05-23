package com.barterplatform.web.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import com.barterplatform.web.security.SecurityConfig;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = SwaggerEndpointTest.TestApplication.class,
        properties = "server.servlet.context-path=/api/v1"
)
@AutoConfigureMockMvc
class SwaggerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(apiGet("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void shouldExposeSwaggerUiWithoutAuthentication() throws Exception {
        // Swagger UI may respond with 200 or 302 redirect - both indicate it is accessible (not 401/403)
        mockMvc.perform(apiGet("/swagger-ui/index.html"))
                .andExpect(status().is(anyOf(is(200), is(302))));
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }
    }
}
