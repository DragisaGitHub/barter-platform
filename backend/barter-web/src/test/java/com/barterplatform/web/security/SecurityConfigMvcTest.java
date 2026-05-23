package com.barterplatform.web.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.barterplatform.web.ratelimit.RateLimitService;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = SecurityConfigMvcTest.TestApplication.class,
        properties = "server.servlet.context-path="
)
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
            "/actuator/health",
            "/actuator/health/readiness"
    })
    void shouldAllowActuatorHealthEndpointsWithoutAuthentication(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
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
    void shouldRequireAuthenticationForNonHealthActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldWriteSecurityHeadersOnPublicResponses() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.STRICT_CONTENT_SECURITY_POLICY))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), geolocation=(), microphone=()"));
    }

    @Test
    void shouldKeepStrictCspOnHealthResponses() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.STRICT_CONTENT_SECURITY_POLICY))
                .andExpect(jsonPath("$.status").value("UP"));
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
        ResponseEntity<String> profiles(@PathVariable String userUuid) {
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
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, TestSecurityController.class, JwtAuthenticationFilter.class, RateLimitService.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
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
