package com.barterplatform.web.identity.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!"
})
class AuthMeIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("barter_db")
            .withUsername("barter_user")
            .withPassword("barter_password");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanMutableTables() {
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserWithValidJwt() throws Exception {
        // Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .servletPath("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alex99",
                                  "email": "alex@example.com",
                                  "password": "P@ssword123"
                                }
                                """))
                .andExpect(status().isCreated());

        // Activate user
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        userRepository.save(user);

        // Login to get token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "alex@example.com",
                                  "password": "P@ssword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        // Call /auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .contextPath("/api/v1")
                        .servletPath("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alex99"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0].code").value("USER"));
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .contextPath("/api/v1")
                        .servletPath("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .contextPath("/api/v1")
                        .servletPath("/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAccessProtectedEndpointWithValidJwt() throws Exception {
        // Register + activate + login
        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .servletPath("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bob42",
                                  "email": "bob@example.com",
                                  "password": "P@ssword456"
                                }
                                """))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("bob@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "bob@example.com",
                                  "password": "P@ssword456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        // /auth/me works with token
        mockMvc.perform(get("/api/v1/auth/me")
                        .contextPath("/api/v1")
                        .servletPath("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob42"));
    }
}

