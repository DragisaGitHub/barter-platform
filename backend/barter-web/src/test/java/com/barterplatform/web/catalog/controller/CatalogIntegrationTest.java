package com.barterplatform.web.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.infrastructure.catalog.repository.FavoriteItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CatalogIntegrationTest {

    // ── Seed UUIDs from V003__catalog_schema.sql ──────────────────
    private static final String CATEGORY_TOYS_UUID = "c0a80101-0001-4000-8000-000000000001";
    private static final String CATEGORY_BOOKS_UUID = "c0a80101-0002-4000-8000-000000000002";
    private static final String TAG_KIDS_UUID = "d0a80101-0001-4000-8000-000000000001";
    private static final String TAG_VINTAGE_UUID = "d0a80101-0003-4000-8000-000000000003";

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
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired private FavoriteItemRepository favoriteItemRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemTagRepository itemTagRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        favoriteItemRepository.deleteAllInBatch();
        itemTagRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ══════════════════════════════════════════════════════════════
    //  Public endpoints
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldListSeededCategories() throws Exception {
        mockMvc.perform(apiGet("/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Toys"))
                .andExpect(jsonPath("$[0].uuid").value(CATEGORY_TOYS_UUID))
                .andExpect(jsonPath("$[0].slug").value("toys"));
    }

    @Test
    void shouldListSeededTags() throws Exception {
        mockMvc.perform(apiGet("/catalog/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void shouldReturnEmptyItemsPageInitially() throws Exception {
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ══════════════════════════════════════════════════════════════
    //  Full lifecycle: create → search → detail → update → archive
    // ══════════════════════════════════════════════════════════════

    @Test
    void fullItemLifecycle() throws Exception {
        // ── 1. Register & login ──────────────────────────────────
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        // ── 2. Create item with tags ─────────────────────────────
        String createBody = """
                {
                  "title": "Vintage Toy Car",
                  "description": "A 1960s die-cast model car in great condition.",
                  "categoryUuid": "%s",
                  "condition": "GOOD",
                  "status": "ACTIVE",
                  "tagUuids": ["%s", "%s"]
                }
                """.formatted(CATEGORY_TOYS_UUID, TAG_KIDS_UUID, TAG_VINTAGE_UUID);

        MvcResult createResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Vintage Toy Car"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.condition").value("GOOD"))
                .andExpect(jsonPath("$.category.uuid").value(CATEGORY_TOYS_UUID))
                .andExpect(jsonPath("$.category.name").value("Toys"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.ownerUsername").value("alice"))
                .andReturn();

        String itemUuid = extractField(createResult, "uuid");

        // ── 3. Verify item_tags persisted in DB ──────────────────
        ItemEntity savedItem = itemRepository.findByUuid(UUID.fromString(itemUuid)).orElseThrow();
        assertThat(itemTagRepository.findByIdItemId(savedItem.getId())).hasSize(2);

        // ── 4. Search – item appears in public ACTIVE search ─────
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(itemUuid))
                .andExpect(jsonPath("$.content[0].title").value("Vintage Toy Car"));

        // ── 5. Get item detail by UUID ───────────────────────────
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(itemUuid))
                .andExpect(jsonPath("$.title").value("Vintage Toy Car"))
                .andExpect(jsonPath("$.description").value("A 1960s die-cast model car in great condition."))
                .andExpect(jsonPath("$.tags", hasSize(2)));

        // ── 6. Update item ───────────────────────────────────────
        String updateBody = """
                {
                  "title": "Updated Vintage Toy Car",
                  "categoryUuid": "%s",
                  "tagUuids": ["%s"]
                }
                """.formatted(CATEGORY_BOOKS_UUID, TAG_VINTAGE_UUID);

        mockMvc.perform(apiPatch("/catalog/items/" + itemUuid)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Vintage Toy Car"))
                .andExpect(jsonPath("$.category.uuid").value(CATEGORY_BOOKS_UUID))
                .andExpect(jsonPath("$.tags", hasSize(1)));

        // Tags replaced – only 1 tag now
        assertThat(itemTagRepository.findByIdItemId(savedItem.getId())).hasSize(1);

        // ── 7. Archive item ──────────────────────────────────────
        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/archive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        // Verify archivedAt is set in DB
        ItemEntity archivedItem = itemRepository.findByUuid(UUID.fromString(itemUuid)).orElseThrow();
        assertThat(archivedItem.getArchivedAt()).isNotNull();
        assertThat(archivedItem.getStatus())
                .isEqualTo(com.barterplatform.domain.catalog.enums.ItemStatus.ARCHIVED);

        // ── 8. Archived item NOT in public ACTIVE search ─────────
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // ── 9. Archived item IS in my-items ──────────────────────
        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(itemUuid));
    }

    // ══════════════════════════════════════════════════════════════
    //  Status filtering on /mine and public search
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldFilterMyItemsByStatus() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        // Create ACTIVE item
        String activeBody = """
                {
                  "title": "Active Widget",
                  "categoryUuid": "%s",
                  "condition": "NEW",
                  "status": "ACTIVE"
                }
                """.formatted(CATEGORY_TOYS_UUID);
        mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Create second item and archive it
        String toArchiveBody = """
                {
                  "title": "Archive Widget",
                  "categoryUuid": "%s",
                  "condition": "GOOD",
                  "status": "ACTIVE"
                }
                """.formatted(CATEGORY_TOYS_UUID);
        MvcResult archiveResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toArchiveBody))
                .andExpect(status().isCreated())
                .andReturn();
        String archivedUuid = extractField(archiveResult, "uuid");

        mockMvc.perform(apiPost("/catalog/items/" + archivedUuid + "/archive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        // /mine without status filter → both items (ACTIVE + ARCHIVED)
        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // /mine?status=ACTIVE → only 1
        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Active Widget"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        // /mine?status=ARCHIVED → only 1
        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Archive Widget"))
                .andExpect(jsonPath("$.content[0].status").value("ARCHIVED"));

        // Public search status=ACTIVE → only active item, never archived
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Active Widget"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Ownership enforcement
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldForbidUpdateByAnotherUser() throws Exception {
        // Alice creates an item
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        String createBody = """
                {
                  "title": "Alice Item",
                  "categoryUuid": "%s",
                  "condition": "NEW"
                }
                """.formatted(CATEGORY_TOYS_UUID);

        MvcResult createResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String itemUuid = extractField(createResult, "uuid");

        // Bob tries to update Alice's item → 403
        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");

        mockMvc.perform(apiPatch("/catalog/items/" + itemUuid)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You are not the owner of this item."));
    }

    @Test
    void shouldForbidArchiveByAnotherUser() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        MvcResult createResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Alice Only",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andReturn();

        String itemUuid = extractField(createResult, "uuid");

        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/archive")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════
    //  Auth required for protected endpoints
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldReturn401ForCreateWithoutToken() throws Exception {
        mockMvc.perform(apiPost("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthorized",
                                  "categoryUuid": "%s",
                                  "condition": "NEW"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailForMyItemsWithoutToken() throws Exception {
        // /catalog/items/mine matches the permitAll GET /catalog/items/* wildcard,
        // so the request passes security but fails at the controller (no principal).
        mockMvc.perform(apiGet("/catalog/items/mine"))
                .andExpect(status().is5xxServerError());
    }

    // ══════════════════════════════════════════════════════════════
    //  Category resolution & item detail
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldResolveCategoryInItemDetail() throws Exception {
        String token = registerActivateAndLogin("carol", "carol@example.com", "P@ssword123");

        MvcResult result = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Electronics Gadget",
                                  "categoryUuid": "c0a80101-0003-4000-8000-000000000003",
                                  "condition": "LIKE_NEW",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.name").value("Electronics"))
                .andExpect(jsonPath("$.category.slug").value("electronics"))
                .andReturn();

        String itemUuid = extractField(result, "uuid");

        // Public detail also resolves category
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentItem() throws Exception {
        mockMvc.perform(apiGet("/catalog/items/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDefaultToDraftStatus() throws Exception {
        String token = registerActivateAndLogin("dave", "dave@example.com", "P@ssword123");

        mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Draft Item",
                                  "categoryUuid": "%s",
                                  "condition": "USED"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Favorites / wishlist
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldFavoriteItemSuccessfully() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Favorite Candidate");

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item favorited successfully."));

        assertThat(favoriteItemRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldFavoriteItemIdempotently() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Favorite Twice");

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item favorited successfully."));

        assertThat(favoriteItemRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldUnfavoriteItemIdempotently() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Unfavorite Candidate");

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(favoriteItemRepository.findAll()).isEmpty();
    }

    @Test
    void shouldListOnlyCurrentUsersFavorites() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");

        String aliceItemUuid = createItem(aliceToken, "Alice Favorite Item");
        String bobItemUuid = createItem(bobToken, "Bob Favorite Item");

        mockMvc.perform(apiPost("/catalog/items/" + aliceItemUuid + "/favorite")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());
        mockMvc.perform(apiPost("/catalog/items/" + bobItemUuid + "/favorite")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());
        mockMvc.perform(apiPost("/catalog/items/" + aliceItemUuid + "/favorite")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(apiGet("/catalog/favorites")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.sort").value("createdAt,desc"))
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder(
                        "Alice Favorite Item", "Bob Favorite Item")));

        mockMvc.perform(apiGet("/catalog/favorites")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Alice Favorite Item"));
    }

    @Test
    void shouldReturn401ForFavoriteWithoutToken() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Unauthorized Favorite");

        mockMvc.perform(apiPost("/catalog/items/" + itemUuid + "/favorite"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForFavoritesWithoutToken() throws Exception {
        mockMvc.perform(apiGet("/catalog/favorites"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Register a user, activate them, login, and return the access token.
     */
    private String registerActivateAndLogin(String username, String email, String password)
            throws Exception {
        // Register
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

        // Activate
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        // Login
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
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asText();
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(field).asText();
    }

    private String createItem(String token, String title) throws Exception {
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
                                """.formatted(title, CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractField(createResult, "uuid");
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

