package com.barterplatform.web.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class AdminCategoriesIntegrationTest {

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
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanMutableTables() {
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void adminCrudLifecycleKeepsPublicCategoriesClean() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin();

        MvcResult createResult = mockMvc.perform(apiPost()
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Board Games & Puzzles",
                                  "description": "Family fun",
                                  "sortOrder": 25
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("board-games-puzzles"))
                .andReturn();

        String categoryUuid = extractField(createResult);

        mockMvc.perform(apiGet("/admin/categories/" + categoryUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Board Games & Puzzles"));

        mockMvc.perform(apiPatch("/admin/categories/" + categoryUuid)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Retro Board Games",
                                  "slug": "retro-board-games",
                                  "sortOrder": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Retro Board Games"))
                .andExpect(jsonPath("$.slug").value("retro-board-games"));

        mockMvc.perform(apiDelete("/admin/categories/" + categoryUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findByUuid(UUID.fromString(categoryUuid)))
                .get()
                .extracting(CategoryEntity::getDeletedAt)
                .isNotNull();

        mockMvc.perform(apiGet("/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].uuid", not(hasItem(categoryUuid))));

        mockMvc.perform(apiGet("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("includeDeleted", "true")
                        .queryParam("q", "retro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].uuid").value(categoryUuid))
                .andExpect(jsonPath("$.content[0].deleted").value(true));
    }

    private String registerActivateLoginAndAssignAdmin() throws Exception {
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
                                """.formatted("admincat", "admincat@example.com", "P@ssword123")))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("admincat@example.com").orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        var adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        UserRoleEntity userRole = new UserRoleEntity();
        UserRoleId id = new UserRoleId();
        id.setUserId(user.getId());
        id.setRoleId(adminRole.getId());
        userRole.setId(id);
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "password": "%s"
                                }
                                """.formatted("admincat@example.com", "P@ssword123")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asString();
    }

    private String extractField(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("uuid").asString();
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPost() {
        return post("/api/v1" + "/admin/categories")
                .contextPath("/api/v1")
                .servletPath("/admin/categories")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

