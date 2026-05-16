package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UserPreferencesIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void migrationDefaultsPreferredLanguageToSrWhenColumnIsOmitted() {
        UUID userUuid = UUID.randomUUID();
        int updated = jdbcTemplate.update(
                """
                INSERT INTO users (uuid, username, email, password_hash, status, email_verified, mfa_enabled, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userUuid,
                "legacy-user",
                "legacy@example.com",
                "$2a$10$legacyhash",
                "ACTIVE",
                true,
                false,
                OffsetDateTime.now());

        assertThat(updated).isEqualTo(1);
        assertThat(userRepository.findByUuid(userUuid)).get()
                .extracting(user -> user.getPreferredLanguage())
                .isEqualTo(PreferredLanguage.SR);
    }

    @Test
    void shouldPersistUpdatedLanguageToEn() throws Exception {
        String accessToken = registerActivateAndLogin("alex99", "alex@example.com", "P@ssword123");

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"EN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("EN"));

        assertThat(userRepository.findByEmail("alex@example.com")).get()
                .extracting(user -> user.getPreferredLanguage())
                .isEqualTo(PreferredLanguage.EN);
    }

    @Test
    void authMeReflectsUpdatedLanguage() throws Exception {
        String accessToken = registerActivateAndLogin("alex99", "alex@example.com", "P@ssword123");

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"EN\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(apiGet("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("EN"));
    }

    @Test
    void invalidLanguageReturns400() throws Exception {
        String accessToken = registerActivateAndLogin("alex99", "alex@example.com", "P@ssword123");

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"DE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/preferences"));
    }

    @Test
    void userPreferenceEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(apiGet("/users/me/preferences"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(apiPatch("/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"EN\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String registerActivateAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .servletPath("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"username\": \"%s\",
                                  \"email\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preferredLanguage").value("SR"));

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"identifier\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

