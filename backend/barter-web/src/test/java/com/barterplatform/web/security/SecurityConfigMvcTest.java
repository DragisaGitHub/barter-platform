package com.barterplatform.web.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = SecurityConfigMvcTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    })
    void shouldAllowPublicAuthEndpointsWithoutAuthentication(String path) throws Exception {
        mockMvc.perform(post(path))
                .andExpect(status().isOk())
                .andExpect(content().string("public-ok"));
    }

    @Test
    void shouldAllowPingWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/profiles/11111111-1111-1111-1111-111111111111",
            "/api/v1/profiles/11111111-1111-1111-1111-111111111111/items"
    })
    void shouldAllowPublicProfileEndpointsWithoutAuthentication(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string("profile-ok"));
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "security-test-user")
    void shouldAllowAuthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected-ok"));
    }

    @RestController
    static class TestSecurityController {

        @PostMapping({"/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh"})
        ResponseEntity<String> auth() {
            return ResponseEntity.ok("public-ok");
        }

        @GetMapping("/api/v1/ping")
        ResponseEntity<String> ping() {
            return ResponseEntity.ok("pong");
        }

        @GetMapping({"/api/v1/profiles/{userUuid}", "/api/v1/profiles/{userUuid}/items"})
        ResponseEntity<String> profiles() {
            return ResponseEntity.ok("profile-ok");
        }

        @GetMapping("/api/v1/protected")
        ResponseEntity<String> protectedEndpoint() {
            return ResponseEntity.ok("protected-ok");
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfig.class, TestSecurityController.class, JwtAuthenticationFilter.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }
    }
}
