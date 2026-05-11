package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.application.identity.service.impl.EmailVerificationServiceImpl;
import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
                "spring.flyway.enabled=true"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!"
})
class EmailVerificationIntegrationTest {

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

    // ══════════════════════════════════════════════════════════════
    //  Registration creates a verification code
    // ══════════════════════════════════════════════════════════════

    @Test
    void registerShouldCreateVerificationCode() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        assertThat(user.getStatus())
                .isEqualTo(com.barterplatform.domain.identity.enums.UserStatus.PENDING_VERIFICATION);
        assertThat(user.isEmailVerified()).isFalse();

        var codes = emailVerificationCodeRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId());
        assertThat(codes).isPresent();
        assertThat(codes.get().getCodeHash()).isNotBlank();
        assertThat(codes.get().getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════
    //  Login blocked before verification
    // ══════════════════════════════════════════════════════════════

    @Test
    void loginShouldBeBlockedBeforeVerification() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Email verification required."));
    }

    // ══════════════════════════════════════════════════════════════
    //  Wrong code rejected
    // ══════════════════════════════════════════════════════════════

    @Test
    void wrongCodeShouldBeRejected() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(verifyEmailRequest("alex@example.com", "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code."));
    }

    // ══════════════════════════════════════════════════════════════
    //  Expired code rejected
    // ══════════════════════════════════════════════════════════════

    @Test
    void expiredCodeShouldBeRejected() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        // Force-expire the code
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        var codeEntity = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        codeEntity.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        emailVerificationCodeRepository.save(codeEntity);

        // Compute the correct code hash to find the raw code is impossible,
        // but use any code — it will hit the "expired" check first
        // We need to produce the correct hash. Instead, let's create a known code.
        // Replace the code with a known value.
        String knownCode = "123456";
        codeEntity.setCodeHash(EmailVerificationServiceImpl.hashCode(knownCode));
        emailVerificationCodeRepository.save(codeEntity);

        mockMvc.perform(verifyEmailRequest("alex@example.com", knownCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code has expired. Please request a new one."));
    }

    // ══════════════════════════════════════════════════════════════
    //  Correct code activates user
    // ══════════════════════════════════════════════════════════════

    @Test
    void correctCodeShouldActivateUser() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        // Replace the hash with a known code so we can verify
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        var codeEntity = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        String knownCode = "654321";
        codeEntity.setCodeHash(EmailVerificationServiceImpl.hashCode(knownCode));
        emailVerificationCodeRepository.save(codeEntity);

        mockMvc.perform(verifyEmailRequest("alex@example.com", knownCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully."));

        // Verify user is now active and email verified
        var updatedUser = userRepository.findByEmail("alex@example.com").orElseThrow();
        assertThat(updatedUser.isEmailVerified()).isTrue();
        assertThat(updatedUser.getStatus())
                .isEqualTo(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);

        // Code should be marked as used
        var usedCode = emailVerificationCodeRepository.findById(codeEntity.getId()).orElseThrow();
        assertThat(usedCode.getUsedAt()).isNotNull();

        // Login should now work
        mockMvc.perform(loginRequest("alex@example.com", "P@ssword123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════
    //  Resend creates a new code
    // ══════════════════════════════════════════════════════════════

    @Test
    void resendShouldCreateNewCode() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        var originalCode = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        String originalHash = originalCode.getCodeHash();

        // Resend
        mockMvc.perform(resendCodeRequest("alex@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent."));

        // A new code should exist (latest one should have a different hash)
        var newCode = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        // The new code is most likely different (extremely unlikely collision with 6-digit codes)
        assertThat(newCode.getId()).isNotEqualTo(originalCode.getId());
    }

    // ══════════════════════════════════════════════════════════════
    //  Already verified email returns early
    // ══════════════════════════════════════════════════════════════

    @Test
    void verifyAlreadyVerifiedEmailShouldReturnEarly() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        // Manually mark as verified
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        userRepository.save(user);

        mockMvc.perform(verifyEmailRequest("alex@example.com", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is already verified."));
    }

    @Test
    void resendForAlreadyVerifiedEmailShouldReturnEarly() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        // Manually mark as verified
        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        userRepository.save(user);

        mockMvc.perform(resendCodeRequest("alex@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is already verified."));
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private MockHttpServletRequestBuilder registerRequest(String username, String email, String password) {
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

    private MockHttpServletRequestBuilder verifyEmailRequest(String email, String code) {
        return post("/api/v1/auth/verify-email")
                .contextPath("/api/v1")
                .servletPath("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "%s"
                        }
                        """.formatted(email, code));
    }

    private MockHttpServletRequestBuilder resendCodeRequest(String email) {
        return post("/api/v1/auth/resend-verification-code")
                .contextPath("/api/v1")
                .servletPath("/auth/resend-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email));
    }
}

