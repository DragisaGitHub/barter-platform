package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
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
class AuthLoginIntegrationTest {

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

    @BeforeEach
    void cleanMutableTables() {
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldLoginSuccessfullyAfterRegistration() throws Exception {
        String rawPassword = "P@ssword123";

        // First register a user
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", rawPassword))
                .andExpect(status().isCreated());

        // Activate the user (set status to ACTIVE)
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPreferredLanguage(PreferredLanguage.EN);
        userRepository.save(user);

        // Login with email
        MvcResult result = mockMvc.perform(loginRequest("alex@example.com", rawPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.uuid").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("alex99"))
                .andExpect(jsonPath("$.user.email").value("alex@example.com"))
                .andExpect(jsonPath("$.user.preferredLanguage").value("EN"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash");

        // Verify refresh token is persisted as hash
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getTokenHash()).isNotBlank();
        assertThat(tokens.getFirst().getUserId()).isEqualTo(user.getId());
        assertThat(tokens.getFirst().getExpiresAt()).isNotNull();

        // Verify last_login_at is updated
        var updatedUser = userRepository.findByEmail("alex@example.com").orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldLoginWithUsername() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        mockMvc.perform(loginRequest("alex99", "P@ssword123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("alex99"));
    }

    @Test
    void shouldReturnUnauthorizedForWrongPassword() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        mockMvc.perform(loginRequest("alex@example.com", "WrongPassword1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void shouldReturnUnauthorizedForNonExistentUser() throws Exception {
        mockMvc.perform(loginRequest("nonexistent@example.com", "P@ssword123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void shouldReturnForbiddenForSuspendedUser() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.SUSPENDED);
        userRepository.save(user);

        mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Account is suspended."));
    }

    @Test
    void shouldReturnForbiddenForBannedUser() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.BANNED);
        userRepository.save(user);

        mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Account is banned."));
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
}

