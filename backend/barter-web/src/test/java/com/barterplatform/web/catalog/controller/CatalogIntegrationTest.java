package com.barterplatform.web.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.infrastructure.catalog.repository.FavoriteItemRepository;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String CATEGORY_ELECTRONICS_UUID = "c0a80101-0003-4000-8000-000000000003";
    private static final String CATEGORY_CLOTHES_UUID = "c0a80101-0004-4000-8000-000000000004";
    private static final String CATEGORY_HOME_UUID = "c0a80101-0005-4000-8000-000000000005";
    private static final String CATEGORY_SPORTS_UUID = "c0a80101-0006-4000-8000-000000000006";
    private static final String TAG_KIDS_UUID = "d0a80101-0001-4000-8000-000000000001";
    private static final String TAG_VINTAGE_UUID = "d0a80101-0003-4000-8000-000000000003";
    private static final Map<String, SeedCategoryState> SEED_CATEGORY_STATES = new LinkedHashMap<>();

    static {
        SEED_CATEGORY_STATES.put(CATEGORY_TOYS_UUID, new SeedCategoryState("Toys", "toys", 1));
        SEED_CATEGORY_STATES.put(CATEGORY_BOOKS_UUID, new SeedCategoryState("Books", "books", 2));
        SEED_CATEGORY_STATES.put(CATEGORY_ELECTRONICS_UUID, new SeedCategoryState("Electronics", "electronics", 3));
        SEED_CATEGORY_STATES.put(CATEGORY_CLOTHES_UUID, new SeedCategoryState("Clothes", "clothes", 4));
        SEED_CATEGORY_STATES.put(CATEGORY_HOME_UUID, new SeedCategoryState("Home", "home", 5));
        SEED_CATEGORY_STATES.put(CATEGORY_SPORTS_UUID, new SeedCategoryState("Sports", "sports", 6));
    }

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
    @Autowired private CategoryRepository categoryRepository;
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
        resetCategories();
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
    void shouldListPopularCategoriesUsingActiveItemCountsAndLimit() throws Exception {
        String token = registerActivateAndLogin("popular-user", "popular@example.com", "P@ssword123");

        createItem(token, "Toy Car", CATEGORY_TOYS_UUID);
        createItem(token, "Toy Train", CATEGORY_TOYS_UUID);
        createItem(token, "Toy Puzzle", CATEGORY_TOYS_UUID);
        createItem(token, "Book One", CATEGORY_BOOKS_UUID);
        createItem(token, "Book Two", CATEGORY_BOOKS_UUID);
        createItem(token, "Laptop", CATEGORY_ELECTRONICS_UUID);
        createItem(token, "Headphones", CATEGORY_ELECTRONICS_UUID);

        String deletedCategoryItemUuid = createItem(token, "Coat", CATEGORY_CLOTHES_UUID);
        String inactiveItemUuid = createItem(token, "Lamp", CATEGORY_HOME_UUID);
        String deletedItemUuid = createItem(token, "Bicycle", CATEGORY_SPORTS_UUID);

        CategoryEntity deletedCategory = categoryRepository.findByUuid(UUID.fromString(CATEGORY_CLOTHES_UUID)).orElseThrow();
        deletedCategory.setDeletedAt(OffsetDateTime.now());
        categoryRepository.save(deletedCategory);

        ItemEntity inactiveItem = itemRepository.findByUuid(UUID.fromString(inactiveItemUuid)).orElseThrow();
        inactiveItem.setStatus(ItemStatus.ARCHIVED);
        inactiveItem.setArchivedAt(OffsetDateTime.now());
        itemRepository.save(inactiveItem);

        ItemEntity deletedItem = itemRepository.findByUuid(UUID.fromString(deletedItemUuid)).orElseThrow();
        deletedItem.setDeletedAt(OffsetDateTime.now());
        itemRepository.save(deletedItem);

        assertThat(itemRepository.findByUuid(UUID.fromString(deletedCategoryItemUuid))).isPresent();

        mockMvc.perform(apiGet("/catalog/categories/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].uuid").value(CATEGORY_TOYS_UUID))
                .andExpect(jsonPath("$[0].activeItemCount").value(3))
                .andExpect(jsonPath("$[1].uuid").value(CATEGORY_BOOKS_UUID))
                .andExpect(jsonPath("$[1].activeItemCount").value(2))
                .andExpect(jsonPath("$[2].uuid").value(CATEGORY_ELECTRONICS_UUID))
                .andExpect(jsonPath("$[2].activeItemCount").value(2))
                .andExpect(jsonPath("$[*].slug", not(hasItem("clothes"))))
                .andExpect(jsonPath("$[*].slug", not(hasItem("home"))))
                .andExpect(jsonPath("$[*].slug", not(hasItem("sports"))));

        mockMvc.perform(apiGet("/catalog/categories/popular").queryParam("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uuid").value(CATEGORY_TOYS_UUID))
                .andExpect(jsonPath("$[1].uuid").value(CATEGORY_BOOKS_UUID));
    }

    @Test
    void shouldUseSortOrderAndNameFallbackForPopularCategoriesWithSameCounts() throws Exception {
        String token = registerActivateAndLogin("fallback-user", "fallback@example.com", "P@ssword123");

        CategoryEntity priorityCategory = createCategory("Priority Picks", "priority-picks", 10);
        CategoryEntity alphaCategory = createCategory("Alpha Finds", "alpha-finds", 20);
        CategoryEntity betaCategory = createCategory("Beta Finds", "beta-finds", 20);

        createItem(token, "Priority Item", priorityCategory.getUuid().toString());
        createItem(token, "Alpha Item", alphaCategory.getUuid().toString());
        createItem(token, "Beta Item", betaCategory.getUuid().toString());

        mockMvc.perform(apiGet("/catalog/categories/popular").queryParam("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Priority Picks"))
                .andExpect(jsonPath("$[1].name").value("Alpha Finds"))
                .andExpect(jsonPath("$[2].name").value("Beta Finds"));
    }

    @Test
    void shouldRejectPopularCategoryLimitAboveMaximum() throws Exception {
        mockMvc.perform(apiGet("/catalog/categories/popular").queryParam("limit", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("must be less than or equal to 20"));
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
                        .queryParam("size", "20"))
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

        // ── 8. Archived item is not visible in public search ─────
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
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

        // Public search is ACTIVE-only even without a status parameter
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Active Widget"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
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

    @Test
    void shouldCreateUpdateReturnAndSearchByApproximateExchangeLocation() throws Exception {
        String token = registerActivateAndLogin("location-user", "location@example.com", "P@ssword123");

        MvcResult createResult = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Board Game",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD",
                                  "status": "ACTIVE",
                                  "exchangeCity": "Belgrade",
                                  "exchangeArea": "Novi Beograd",
                                  "exchangeLocation": "Near the public library area"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exchangeCity").value("Belgrade"))
                .andExpect(jsonPath("$.exchangeArea").value("Novi Beograd"))
                .andExpect(jsonPath("$.exchangeLocation").value("Near the public library area"))
                .andReturn();

        String itemUuid = extractField(createResult, "uuid");
        ItemEntity savedItem = itemRepository.findByUuid(UUID.fromString(itemUuid)).orElseThrow();
        assertThat(savedItem.getExchangeCity()).isEqualTo("Belgrade");
        assertThat(savedItem.getExchangeArea()).isEqualTo("Novi Beograd");
        assertThat(savedItem.getExchangeLocation()).isEqualTo("Near the public library area");

        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("location", "beograd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(itemUuid))
                .andExpect(jsonPath("$.content[0].exchangeCity").value("Belgrade"))
                .andExpect(jsonPath("$.content[0].exchangeArea").value("Novi Beograd"))
                .andExpect(jsonPath("$.content[0].exchangeLocation").value("Near the public library area"));

        mockMvc.perform(apiPatch("/catalog/items/" + itemUuid)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exchangeCity": "Zemun",
                                  "exchangeArea": "Gardoš",
                                  "exchangeLocation": "Main pedestrian zone"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeCity").value("Zemun"))
                .andExpect(jsonPath("$.exchangeArea").value("Gardoš"))
                .andExpect(jsonPath("$.exchangeLocation").value("Main pedestrian zone"));

        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeCity").value("Zemun"))
                .andExpect(jsonPath("$.exchangeArea").value("Gardoš"))
                .andExpect(jsonPath("$.exchangeLocation").value("Main pedestrian zone"));

        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("location", "gardoš"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(itemUuid));

        mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No Location Required",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD",
                                  "status": "ACTIVE"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("No Location Required"));
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
        return createItem(token, title, CATEGORY_TOYS_UUID);
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

    private CategoryEntity createCategory(String name, String slug, int sortOrder) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setSlug(slug);
        category.setSortOrder(sortOrder);
        return categoryRepository.save(category);
    }

    private void resetCategories() {
        List<CategoryEntity> categories = categoryRepository.findAll();
        List<CategoryEntity> extraCategories = new ArrayList<>();
        List<CategoryEntity> seededCategories = new ArrayList<>();

        for (CategoryEntity category : categories) {
            SeedCategoryState seedCategoryState = SEED_CATEGORY_STATES.get(category.getUuid().toString());
            if (seedCategoryState == null) {
                extraCategories.add(category);
                continue;
            }

            category.setName(seedCategoryState.name());
            category.setSlug(seedCategoryState.slug());
            category.setDescription(null);
            category.setParentId(null);
            category.setSortOrder(seedCategoryState.sortOrder());
            category.setDeletedAt(null);
            seededCategories.add(category);
        }

        if (!extraCategories.isEmpty()) {
            categoryRepository.deleteAllInBatch(extraCategories);
        }

        categoryRepository.saveAll(seededCategories);
    }

    private record SeedCategoryState(String name, String slug, int sortOrder) {
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

