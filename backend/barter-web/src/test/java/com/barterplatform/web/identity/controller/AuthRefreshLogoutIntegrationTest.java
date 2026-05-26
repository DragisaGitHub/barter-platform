package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class AuthRefreshLogoutIntegrationTest {

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
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanMutableTables() {
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldRefreshTokenAfterLogin() throws Exception {
        registerAndActivateUser("alex99", "alex@example.com", "P@ssword123");

        // Login
        MvcResult loginResult = mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        // Refresh
        MvcResult refreshResult = mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.refreshExpiresIn").isNumber())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.preferredLanguage").value("EN"))
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("refreshToken").asText();

        // Old token should be rotated (revoked)
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Old refresh token should no longer work
        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutAndRevokeRefreshToken() throws Exception {
        registerAndActivateUser("alex99", "alex@example.com", "P@ssword123");

        // Login
        MvcResult loginResult = mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // Logout
        mockMvc.perform(logoutRequest(refreshToken))
                .andExpect(status().isNoContent());

        // Verify token is revoked
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).allSatisfy(token ->
                assertThat(token.getRevokedAt()).isNotNull());

        // Revoked token should not work for refresh
        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked."));
    }

    @Test
    void shouldRejectRefreshForSuspendedUser() throws Exception {
        registerAndActivateUser("alex99", "alex@example.com", "P@ssword123");

        // Login
        MvcResult loginResult = mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // Suspend user
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.SUSPENDED);
        userRepository.save(user);

        // Refresh should fail
        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is suspended."));

        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getRevokedAt()).isNotNull();
    }

    @Test
    void shouldRejectRefreshForBannedUserAndRevokeToken() throws Exception {
        registerAndActivateUser("alex99", "alex@example.com", "P@ssword123");

        MvcResult loginResult = mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.BANNED);
        userRepository.save(user);

        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is banned."));

        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getRevokedAt()).isNotNull();
    }

    @Test
    void shouldRejectRefreshWhenTokenIsMissingFromRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contextPath("/api/v1")
                        .servletPath("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required."));
    }

    @Test
    void shouldCompleteFullLoginRefreshLogoutFlow() throws Exception {
        registerAndActivateUser("alex99", "alex@example.com", "P@ssword123");

        // 1. Login
        MvcResult loginResult = mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // 2. Refresh
        MvcResult refreshResult = mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("refreshToken").asText();

        // 3. Logout with the new refresh token
        mockMvc.perform(logoutRequest(newRefreshToken))
                .andExpect(status().isNoContent());

        // 4. Verify new token can no longer be used
        mockMvc.perform(refreshRequest(newRefreshToken))
                .andExpect(status().isUnauthorized());
    }

    private void registerAndActivateUser(String username, String email, String password) throws Exception {
        mockMvc.perform(registerUserRequest(username, email, password))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPreferredLanguage(PreferredLanguage.EN);
        userRepository.save(user);
    }

    private MockHttpServletRequestBuilder registerUserRequest(String username, String email, String password) {
        return post("/api/v1/auth/register")
                .contextPath("/api/v1")
                .servletPath("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, email, password));
    }

    private MockHttpServletRequestBuilder loginRequest(String identifier, String password) {
        return post("/api/v1/auth/login")
                .contextPath("/api/v1")
                .servletPath("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "identifier": "%s",
                          "password": "%s"
                        }
                        """.formatted(identifier, password));
    }

    private MockHttpServletRequestBuilder refreshRequest(String refreshToken) {
        return post("/api/v1/auth/refresh")
                .contextPath("/api/v1")
                .servletPath("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshToken));
    }

    private MockHttpServletRequestBuilder logoutRequest(String refreshToken) {
        return post("/api/v1/auth/logout")
                .contextPath("/api/v1")
                .servletPath("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshToken));
    }
}

