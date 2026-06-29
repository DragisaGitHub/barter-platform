package com.barterplatform.web.admin.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
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
                "barter.seed.demo-content=true"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AdminListingsIntegrationTest {

    private static final String CATEGORY_TOYS_UUID = "c0a80101-0001-4000-8000-000000000001";
    private static final String CATEGORY_BOOKS_UUID = "c0a80101-0002-4000-8000-000000000002";

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

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
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

    @Test
    void ownerQueryAliceReturnsListingsWithHttp200() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin("admin-listings", "admin-listings@example.com", "P@ssword123");
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String dragisaToken = registerActivateAndLogin("dragisa", "dragisa@example.com", "P@ssword123");

        createItem(aliceToken, "Alice Desk", CATEGORY_TOYS_UUID);
        createItem(dragisaToken, "Dragisa Bike", CATEGORY_BOOKS_UUID);

        mockMvc.perform(apiGet("/admin/listings")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ownerUsername").value("alice"))
                .andExpect(jsonPath("$.content[0].title").value("Alice Desk"));
    }

    @Test
    void ownerQueryDragisaReturnsListingsWithHttp200() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin("admin-dragisa", "admin-dragisa@example.com", "P@ssword123");
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String dragisaToken = registerActivateAndLogin("dragisa", "dragisa@example.com", "P@ssword123");

        createItem(aliceToken, "Alice Desk", CATEGORY_TOYS_UUID);
        createItem(dragisaToken, "Dragisa Bike", CATEGORY_BOOKS_UUID);

        mockMvc.perform(apiGet("/admin/listings")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "dragisa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ownerUsername").value("dragisa"))
                .andExpect(jsonPath("$.content[0].title").value("Dragisa Bike"));
    }

    @Test
    void ownerQueryUnknownReturnsHttp200WithEmptyPage() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin("admin-empty", "admin-empty@example.com", "P@ssword123");
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        createItem(aliceToken, "Alice Desk", CATEGORY_TOYS_UUID);

        mockMvc.perform(apiGet("/admin/listings")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void ownerQuerySupportsMultipleMatchingOwners() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin("admin-multi", "admin-multi@example.com", "P@ssword123");
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String aliceTwoToken = registerActivateAndLogin("alice2", "alice2@example.com", "P@ssword123");
        String dragisaToken = registerActivateAndLogin("dragisa", "dragisa@example.com", "P@ssword123");

        createItem(aliceToken, "Alice Desk", CATEGORY_TOYS_UUID);
        createItem(aliceTwoToken, "Alice Lamp", CATEGORY_BOOKS_UUID);
        createItem(dragisaToken, "Dragisa Bike", CATEGORY_TOYS_UUID);

        mockMvc.perform(apiGet("/admin/listings")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].ownerUsername", containsInAnyOrder("alice", "alice2")))
                .andExpect(jsonPath("$.content[*].ownerUsername", not(hasItem("dragisa"))));
    }

    @Test
    void ownerQueryStillWorksWithCategoryStatusAndTitleFilters() throws Exception {
        String adminToken = registerActivateLoginAndAssignAdmin("admin-filters", "admin-filters@example.com", "P@ssword123");
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        createItem(aliceToken, "Red Bike", CATEGORY_TOYS_UUID);
        createItem(aliceToken, "Red Reader", CATEGORY_BOOKS_UUID);
        String removedUuid = createItem(aliceToken, "Red Helmet", CATEGORY_TOYS_UUID);
        markItemRemoved(removedUuid);

        mockMvc.perform(apiGet("/admin/listings")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerQuery", "Alice")
                        .queryParam("q", "Red")
                        .queryParam("categoryUuid", CATEGORY_TOYS_UUID)
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ownerUsername").value("alice"))
                .andExpect(jsonPath("$.content[0].title").value("Red Bike"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    private void markItemRemoved(String itemUuid) {
        ItemEntity item = itemRepository.findByUuid(java.util.UUID.fromString(itemUuid)).orElseThrow();
        item.setStatus(ItemStatus.REMOVED);
        item.setRemovedAt(OffsetDateTime.now());
        itemRepository.save(item);
    }

    private String registerActivateLoginAndAssignAdmin(String username, String email, String password) throws Exception {
        UserEntity user = registerAndActivate(username, email, password);
        assignAdminRole(user);
        return login(email, password);
    }

    private String registerActivateAndLogin(String username, String email, String password) throws Exception {
        registerAndActivate(username, email, password);
        return login(email, password);
    }

    private UserEntity registerAndActivate(String username, String email, String password) throws Exception {
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
                                """.formatted(username, email, password)))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        return user;
    }

    private String login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").textValue();
    }

    private void assignAdminRole(UserEntity user) {
        var adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        UserRoleEntity userRole = new UserRoleEntity();
        UserRoleId id = new UserRoleId();
        id.setUserId(user.getId());
        id.setRoleId(adminRole.getId());
        userRole.setId(id);
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);
    }

    private String createItem(String token, String title, String categoryUuid) throws Exception {
        MvcResult createResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD",
                                  "status": "ACTIVE"
                                }
                                """.formatted(title, categoryUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractField(createResult, "uuid");
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(field).textValue();
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

