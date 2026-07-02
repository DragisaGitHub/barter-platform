package com.barterplatform.web.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.catalog.repository.FavoriteItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.barterplatform.infrastructure.moderation.repository.ReportRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!",
                "barter.storage.type=local"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AdminOperationsIntegrationTest {

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
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired private FavoriteItemRepository favoriteItemRepository;
    @Autowired private ItemImageRepository itemImageRepository;
    @Autowired private ItemTagRepository itemTagRepository;
    @Autowired private ListingModerationActionRepository listingModerationActionRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private TradeReviewRepository tradeReviewRepository;
    @Autowired private TradeOfferMessageRepository tradeOfferMessageRepository;
    @Autowired private TradeOfferItemRepository tradeOfferItemRepository;
    @Autowired private TradeOfferRepository tradeOfferRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        reportRepository.deleteAllInBatch();
        tradeReviewRepository.deleteAllInBatch();
        tradeOfferMessageRepository.deleteAllInBatch();
        tradeOfferItemRepository.deleteAllInBatch();
        tradeOfferRepository.deleteAllInBatch();
        favoriteItemRepository.deleteAllInBatch();
        itemImageRepository.deleteAllInBatch();
        itemTagRepository.deleteAllInBatch();
        listingModerationActionRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ── /admin/system/sentry-test ────────────────────────────────────────────

    @Test
    void adminCanTriggerBackendSentryTest() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        mockMvc.perform(apiSentryTest().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void regularUserCannotTriggerBackendSentryTest() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiSentryTest().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotTriggerBackendSentryTest() throws Exception {
        mockMvc.perform(apiSentryTest())
                .andExpect(status().isUnauthorized());
    }

    // ── /admin/operations/overview ───────────────────────────────────────────

    @Test
    void adminCanAccessOperationalOverview() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        mockMvc.perform(apiGet().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.system.applicationName").value("barter-platform"))
                .andExpect(jsonPath("$.system.serverTime").exists())
                .andExpect(jsonPath("$.health.overallStatus").exists())
                .andExpect(jsonPath("$.health.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.users.totalUsers").value(1))
                .andExpect(jsonPath("$.users.activeUsers").value(1))
                .andExpect(jsonPath("$.marketplace.totalItems").value(0))
                .andExpect(jsonPath("$.moderation.openReports").value(0))
                .andExpect(jsonPath("$.storage.storageProviderType").value("local"))
                .andExpect(jsonPath("$.deployment.deploymentStateAvailability").value("unavailable"));
    }

    @Test
    void regularUserCannotAccessOperationalOverview() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiGet().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessOperationalOverview() throws Exception {
        mockMvc.perform(apiGet())
                .andExpect(status().isUnauthorized());
    }

    // ── /admin/operations/backups ────────────────────────────────────────────

    @Test
    void adminCanAccessOperationalBackups() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        // No BACKUP_AZURE_CONNECTION_STRING / BACKUP_AZURE_CONTAINER set in test context —
        // service must return a safe placeholder, never throw or expose config secrets.
        mockMvc.perform(apiBackupsGet().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("placeholder"))
                .andExpect(jsonPath("$.scheduledBackupEnabled").value(false))
                .andExpect(jsonPath("$.note").exists())
                // sensitive fields must be absent when no config is present
                .andExpect(jsonPath("$.blobName").doesNotExist());
    }

    @Test
    void regularUserCannotAccessOperationalBackups() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiBackupsGet().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessOperationalBackups() throws Exception {
        mockMvc.perform(apiBackupsGet())
                .andExpect(status().isUnauthorized());
    }

    // ── /admin/operations/deployments ────────────────────────────────────────

    @Test
    void adminCanAccessOperationalDeployments() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        mockMvc.perform(apiDeploymentsGet().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").exists())
                .andExpect(jsonPath("$.environment").exists());
    }

    @Test
    void regularUserCannotAccessOperationalDeployments() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiDeploymentsGet().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessOperationalDeployments() throws Exception {
        mockMvc.perform(apiDeploymentsGet())
                .andExpect(status().isUnauthorized());
    }

    // ── /admin/operations/costs ──────────────────────────────────────────────

    @Test
    void adminCanAccessOperationalCosts() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        // No Azure Cost Management config set in test context —
        // service must return a safe placeholder, never throw or expose secrets.
        mockMvc.perform(apiCostsGet().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("placeholder"))
                .andExpect(jsonPath("$.note").exists())
                // Sensitive cost fields must be absent when no config is present
                .andExpect(jsonPath("$.currentMonthCost").doesNotExist())
                .andExpect(jsonPath("$.previousMonthCost").doesNotExist())
                .andExpect(jsonPath("$.dailyTrend").doesNotExist())
                .andExpect(jsonPath("$.serviceBreakdown").doesNotExist());
    }

    @Test
    void regularUserCannotAccessOperationalCosts() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiCostsGet().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessOperationalCosts() throws Exception {
        mockMvc.perform(apiCostsGet())
                .andExpect(status().isUnauthorized());
    }

    // ── /admin/operations/monitoring ─────────────────────────────────────────

    @Test
    void adminCanAccessOperationalMonitoring() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        mockMvc.perform(apiMonitoringGet().header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").exists())
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.platform.backendStatus").value("UP"))
                .andExpect(jsonPath("$.platform.frontendStatus").value("NOT_PROBED"))
                .andExpect(jsonPath("$.platform.landingStatus").value("NOT_PROBED"))
                .andExpect(jsonPath("$.platform.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.application.javaVersion").exists())
                .andExpect(jsonPath("$.application.activeProfiles").isArray())
                .andExpect(jsonPath("$.application.serverTime").exists())
                .andExpect(jsonPath("$.jvm.processors").isNumber())
                .andExpect(jsonPath("$.jvm.heapUsedBytes").isNumber())
                .andExpect(jsonPath("$.http.readinessStatus").value("ACCEPTING_TRAFFIC"))
                .andExpect(jsonPath("$.http.livenessStatus").value("CORRECT"))
                .andExpect(jsonPath("$.database.datasourceValid").value(true))
                .andExpect(jsonPath("$.storage.storageProviderType").value("local"))
                // Backup not configured in test context — blob probe must be absent
                .andExpect(jsonPath("$.storage.blobStorageReachable").doesNotExist())
                .andExpect(jsonPath("$.storage.backupContainerReachable").doesNotExist());
    }

    @Test
    void regularUserCannotAccessOperationalMonitoring() throws Exception {
        String userToken = registerActivateAndLogin();

        mockMvc.perform(apiMonitoringGet().header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessOperationalMonitoring() throws Exception {
        mockMvc.perform(apiMonitoringGet())
                .andExpect(status().isUnauthorized());
    }

    private String registerActivateLoginAndAssignAdmin() throws Exception {
        UserEntity user = registerAndActivate("operations-admin", "operations-admin@example.com");
        var adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        UserRoleEntity userRole = new UserRoleEntity();
        UserRoleId id = new UserRoleId();
        id.setUserId(user.getId());
        id.setRoleId(adminRole.getId());
        userRole.setId(id);
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);
        return login("operations-admin@example.com");
    }

    private String registerActivateAndLogin() throws Exception {
        registerAndActivate("operations-user", "operations-user@example.com");
        return login("operations-user@example.com");
    }

    private UserEntity registerAndActivate(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .servletPath("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, "P@ssword123")))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);
        return user;
    }

    private String login(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, "P@ssword123")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asString();
    }

    private MockHttpServletRequestBuilder apiGet() {
        return get("/api/v1/admin/operations/overview")
                .contextPath("/api/v1")
                .servletPath("/admin/operations/overview")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiBackupsGet() {
        return get("/api/v1/admin/operations/backups")
                .contextPath("/api/v1")
                .servletPath("/admin/operations/backups")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiDeploymentsGet() {
        return get("/api/v1/admin/operations/deployments")
                .contextPath("/api/v1")
                .servletPath("/admin/operations/deployments")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiCostsGet() {
        return get("/api/v1/admin/operations/costs")
                .contextPath("/api/v1")
                .servletPath("/admin/operations/costs")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiMonitoringGet() {
        return get("/api/v1/admin/operations/monitoring")
                .contextPath("/api/v1")
                .servletPath("/admin/operations/monitoring")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiSentryTest() {
        return post("/api/v1/admin/system/sentry-test")
                .contextPath("/api/v1")
                .servletPath("/admin/system/sentry-test")
                .accept(MediaType.APPLICATION_JSON);
    }
}

