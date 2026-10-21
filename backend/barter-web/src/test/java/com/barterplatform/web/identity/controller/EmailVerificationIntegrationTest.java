package com.barterplatform.web.identity.controller;

import com.barterplatform.BarterApplication;
import com.barterplatform.application.identity.auth.VerificationCodeHasher;
import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!",
                "barter.verification.max-codes-per-window=3"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
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
    @Autowired private VerificationCodeHasher codeHasher;

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

    @Test
    void verificationShouldRejectProhibitedStatusesWithoutMutatingAccountOrCode() throws Exception {
        for (UserStatus prohibitedStatus : List.of(UserStatus.SUSPENDED, UserStatus.BANNED, UserStatus.DELETED)) {
            String username = "status_" + prohibitedStatus.name().toLowerCase();
            String email = username + "@example.com";
            mockMvc.perform(registerRequest(username, email, "P@ssword123"))
                    .andExpect(status().isCreated());

            var user = userRepository.findByEmail(email).orElseThrow();
            user.setStatus(prohibitedStatus);
            userRepository.save(user);
            var code = setKnownCode(email, "123456");
            String expectedMessage = switch (prohibitedStatus) {
                case SUSPENDED -> "Account is suspended.";
                case BANNED -> "Account is banned.";
                case DELETED -> "Account is deleted.";
                default -> throw new IllegalArgumentException("Unexpected status: " + prohibitedStatus);
            };

            mockMvc.perform(verifyEmailRequest(email, "123456"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());

            var unchangedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(prohibitedStatus);
            assertThat(unchangedUser.isEmailVerified()).isFalse();
            assertThat(emailVerificationCodeRepository.findById(code.getId()).orElseThrow().getUsedAt()).isNull();
            assertThat(refreshTokenRepository.findAll())
                    .noneMatch(token -> unchangedUser.getId().equals(token.getUserId()));
        }
    }

    @Test
    void deletedAccountCannotLoginOrRefresh() throws Exception {
        mockMvc.perform(registerRequest("deleteduser", "deleted@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        setKnownCode("deleted@example.com", "123456");
        MvcResult verification = mockMvc.perform(verifyEmailRequest("deleted@example.com", "123456"))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = new ObjectMapper().readTree(verification.getResponse().getContentAsString())
                .get("refreshToken").asText();

        var user = userRepository.findByEmail("deleted@example.com").orElseThrow();
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        mockMvc.perform(loginRequest("deleted@example.com", "P@ssword123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is deleted."));
        mockMvc.perform(refreshTokenRequest(refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is deleted."));

        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> user.getId().equals(token.getUserId()))
                .singleElement()
                .satisfies(token -> assertThat(token.getRevokedAt()).isNotNull());
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

    @Test
    void invalidCodeShouldNotAuthenticate() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        // Bad code → 400 error, no access token in response
        mockMvc.perform(verifyEmailRequest("alex@example.com", "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void unknownEmailUsesSameGenericErrorAsInvalidCode() throws Exception {
        mockMvc.perform(verifyEmailRequest("unknown@example.com", "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_INVALID"))
                .andExpect(jsonPath("$.message").value("Invalid verification code."));
    }

    @Test
    void invalidAttemptIsPersistedInPostgres() throws Exception {
        mockMvc.perform(registerRequest("attemptuser", "attempt@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(verifyEmailRequest("attempt@example.com", "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_INVALID"));

        var user = userRepository.findByEmail("attempt@example.com").orElseThrow();
        var code = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        assertThat(code.getFailedAttempts()).isEqualTo(1);
        assertThat(code.getInvalidatedAt()).isNull();
    }

    @Test
    void fifthInvalidAttemptLocksCodeAndCorrectAttemptAfterLockoutFails() throws Exception {
        mockMvc.perform(registerRequest("lockuser", "lock@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        var code = setKnownCode("lock@example.com", "654321");

        for (int attempt = 1; attempt <= 5; attempt++) {
            var result = mockMvc.perform(verifyEmailRequest("lock@example.com", "000000"))
                    .andExpect(status().isBadRequest());
            result.andExpect(jsonPath("$.code").value(attempt == 5
                    ? "VERIFICATION_CODE_LOCKED"
                    : "VERIFICATION_CODE_INVALID"));
        }

        var locked = emailVerificationCodeRepository.findById(code.getId()).orElseThrow();
        assertThat(locked.getFailedAttempts()).isEqualTo(5);
        assertThat(locked.getInvalidatedAt()).isNotNull();
        mockMvc.perform(verifyEmailRequest("lock@example.com", "654321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_LOCKED"));
        assertThat(userRepository.findByEmail("lock@example.com").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void resendAfterLockoutIssuesReplacementWithoutReactivatingOldCode() throws Exception {
        mockMvc.perform(registerRequest("resendlock", "resend-lock@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        var original = setKnownCode("resend-lock@example.com", "654321");
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(verifyEmailRequest("resend-lock@example.com", "000000"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(resendCodeRequest("resend-lock@example.com")).andExpect(status().isOk());
        var user = userRepository.findByEmail("resend-lock@example.com").orElseThrow();
        var replacement = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        assertThat(emailVerificationCodeRepository.findById(original.getId()).orElseThrow().getInvalidatedAt()).isNotNull();

        mockMvc.perform(verifyEmailRequest("resend-lock@example.com", "654321"))
                .andExpect(status().isBadRequest());
        setKnownCode("resend-lock@example.com", "123456");
        mockMvc.perform(verifyEmailRequest("resend-lock@example.com", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void resendCooldownAndPerAccountIssuanceCapReturn429WithRetryAfter() throws Exception {
        mockMvc.perform(registerRequest("resendcap", "resend-cap@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail("resend-cap@example.com").orElseThrow();
        var initial = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();

        mockMvc.perform(resendCodeRequest("resend-cap@example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        initial.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        emailVerificationCodeRepository.save(initial);
        mockMvc.perform(resendCodeRequest("resend-cap@example.com")).andExpect(status().isOk());
        var second = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        second.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        emailVerificationCodeRepository.save(second);
        mockMvc.perform(resendCodeRequest("resend-cap@example.com")).andExpect(status().isOk());
        var third = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        third.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        emailVerificationCodeRepository.save(third);
        mockMvc.perform(resendCodeRequest("resend-cap@example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void concurrentResendsCannotBypassAccountCooldown() throws Exception {
        mockMvc.perform(registerRequest("concurrentresend", "concurrent-resend@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail("concurrent-resend@example.com").orElseThrow();
        var original = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        original.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        emailVerificationCodeRepository.save(original);

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> requests = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                requests.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent resend test start.");
                    }
                    return mockMvc.perform(resendCodeRequest("concurrent-resend@example.com").with(request -> {
                        request.setRemoteAddr("198.51.100.203");
                        return request;
                    })).andReturn();
                }));
            }
            start.countDown();
            List<Integer> statuses = List.of(
                    requests.get(0).get(30, TimeUnit.SECONDS).getResponse().getStatus(),
                    requests.get(1).get(30, TimeUnit.SECONDS).getResponse().getStatus());

            assertThat(statuses).containsExactlyInAnyOrder(200, 429);
        }

        assertThat(emailVerificationCodeRepository.findAll().stream()
                .filter(code -> user.getId().equals(code.getUserId()))
                .count()).isEqualTo(2);
    }

    @Test
    void verifiedCodeIsSingleUse() throws Exception {
        mockMvc.perform(registerRequest("singleuse", "single-use@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        var code = setKnownCode("single-use@example.com", "123456");

        mockMvc.perform(verifyEmailRequest("single-use@example.com", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        mockMvc.perform(verifyEmailRequest("single-use@example.com", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is already verified."))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        assertThat(emailVerificationCodeRepository.findById(code.getId()).orElseThrow().getUsedAt()).isNotNull();
    }

    @Test
    void concurrentCorrectRequestsIssueOnlyOneAuthenticationSession() throws Exception {
        mockMvc.perform(registerRequest("concurrent", "concurrent@example.com", "P@ssword123"))
                .andExpect(status().isCreated());
        setKnownCode("concurrent@example.com", "123456");

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> requests = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                requests.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return mockMvc.perform(verifyEmailRequest("concurrent@example.com", "123456")).andReturn();
                }));
            }
            start.countDown();
            List<MvcResult> results = List.of(requests.get(0).get(30, TimeUnit.SECONDS),
                    requests.get(1).get(30, TimeUnit.SECONDS));
            assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
            long tokenResponses = 0;
            ObjectMapper objectMapper = new ObjectMapper();
            for (MvcResult result : results) {
                if (objectMapper.readTree(result.getResponse().getContentAsString()).hasNonNull("accessToken")) {
                    tokenResponses++;
                }
            }
            assertThat(tokenResponses).isEqualTo(1);
        }

        var user = userRepository.findByEmail("concurrent@example.com").orElseThrow();
        assertThat(refreshTokenRepository.findAll().stream().filter(token -> user.getId().equals(token.getUserId())).count())
                .isEqualTo(1);
    }

    @Test
    void concurrentWrongAttemptsCannotExceedOrLoseAttemptCount() throws Exception {
        mockMvc.perform(registerRequest("concurrentwrong", "concurrent-wrong@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<MvcResult>> requests = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                requests.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return mockMvc.perform(verifyEmailRequest("concurrent-wrong@example.com", "000000")).andReturn();
                }));
            }
            start.countDown();
            for (Future<MvcResult> request : requests) {
                assertThat(request.get(30, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(400);
            }
        }

        var user = userRepository.findByEmail("concurrent-wrong@example.com").orElseThrow();
        var code = emailVerificationCodeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        assertThat(code.getFailedAttempts()).isEqualTo(5);
        assertThat(code.getInvalidatedAt()).isNotNull();
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
        codeEntity.setCodeHash(codeHasher.hash(user.getId(), knownCode));
        emailVerificationCodeRepository.save(codeEntity);

        mockMvc.perform(verifyEmailRequest("alex@example.com", knownCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code has expired. Please request a new one."));
    }

    @Test
    void expiredCodeShouldNotAuthenticate() throws Exception {
        mockMvc.perform(registerRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("alex@example.com").orElseThrow();
        var codeEntity = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        codeEntity.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        String knownCode = "654321";
        codeEntity.setCodeHash(codeHasher.hash(user.getId(), knownCode));
        emailVerificationCodeRepository.save(codeEntity);

        // Expired code → 400 error, no access token
        mockMvc.perform(verifyEmailRequest("alex@example.com", knownCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        assertThat(emailVerificationCodeRepository.findById(codeEntity.getId()).orElseThrow().getFailedAttempts())
                .isZero();
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
        codeEntity.setCodeHash(codeHasher.hash(user.getId(), knownCode));
        emailVerificationCodeRepository.save(codeEntity);

        mockMvc.perform(verifyEmailRequest("alex@example.com", knownCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully."))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("alex@example.com"));

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
        originalCode.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        emailVerificationCodeRepository.save(originalCode);

        // Resend
        mockMvc.perform(resendCodeRequest("alex@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent."));

        // A new code should exist (latest one should have a different hash)
        var newCode = emailVerificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        assertThat(newCode.getId()).isNotEqualTo(originalCode.getId());
        assertThat(emailVerificationCodeRepository.findById(originalCode.getId()).orElseThrow().getInvalidatedAt())
                .isNotNull();
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
                .andExpect(jsonPath("$.message").value("Email is already verified."))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
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

    private EmailVerificationCodeEntity setKnownCode(String email, String knownCode) {
        var user = userRepository.findByEmail(email).orElseThrow();
        var code = emailVerificationCodeRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();
        code.setCodeHash(codeHasher.hash(user.getId(), knownCode));
        return emailVerificationCodeRepository.save(code);
    }

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

    private MockHttpServletRequestBuilder refreshTokenRequest(String refreshToken) {
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

