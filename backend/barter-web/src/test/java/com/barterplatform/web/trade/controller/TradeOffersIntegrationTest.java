package com.barterplatform.web.trade.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.barterplatform.infrastructure.notification.repository.NotificationRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
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
class TradeOffersIntegrationTest {

    private static final String CATEGORY_TOYS_UUID = "c0a80101-0001-4000-8000-000000000001";

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
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemTagRepository itemTagRepository;
    @Autowired
    private TradeOfferRepository tradeOfferRepository;
    @Autowired
    private TradeOfferItemRepository tradeOfferItemRepository;
    @Autowired
    private TradeOfferMessageRepository tradeOfferMessageRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        // Delete notifications before users (FK constraint)
        notificationRepository.deleteAllInBatch();
        tradeOfferItemRepository.deleteAllInBatch();
        tradeOfferMessageRepository.deleteAllInBatch();
        tradeOfferRepository.deleteAllInBatch();
        itemTagRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ══════════════════════════════════════════════════════════════
    //  Full trade offer lifecycle (ITEM_EXCHANGE – backward compat)
    // ══════════════════════════════════════════════════════════════

    @Test
    void fullTradeOfferLifecycle() throws Exception {
        // ── 1. Register & login Alice and Bob ─────────────────────
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        // ── 2. Alice creates ACTIVE item ──────────────────────────
        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");

        // ── 3. Bob creates ACTIVE item ────────────────────────────
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

        // ── 4. Bob sends trade offer to Alice (ITEM_EXCHANGE) ─────
        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE",
                                  "message": "Want to trade?"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.mode").value("ITEM_EXCHANGE"))
                .andExpect(jsonPath("$.sender.username").value("bob"))
                .andExpect(jsonPath("$.receiver.username").value("alice"))
                .andExpect(jsonPath("$.senderItem.uuid").value(bobItemUuid))
                .andExpect(jsonPath("$.receiverItem.uuid").value(aliceItemUuid))
                .andExpect(jsonPath("$.offeredItems", hasSize(1)))
                .andExpect(jsonPath("$.offeredItems[0].uuid").value(bobItemUuid))
                .andExpect(jsonPath("$.message").value("Want to trade?"))
                .andReturn();

        String offerUuid = extractField(offerResult);

        // ── 5. Alice sees incoming offer ──────────────────────────
        mockMvc.perform(apiGet("/trade-offers/incoming")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(offerUuid))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].mode").value("ITEM_EXCHANGE"));

        // ── 6. Bob sees sent offer ────────────────────────────────
        mockMvc.perform(apiGet("/trade-offers/sent")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(offerUuid));

        // ── 7. Alice accepts offer ────────────────────────────────
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty());

        // ── 8. Both items become ARCHIVED ─────────────────────────
        mockMvc.perform(apiGet("/catalog/items/" + aliceItemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(apiGet("/catalog/items/" + bobItemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        // ── 9. Archived items disappear from public ACTIVE search ─
        mockMvc.perform(apiGet("/catalog/items")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // ── 10. Archived items remain visible in owner my-items ───
        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(aliceItemUuid));

        mockMvc.perform(apiGet("/catalog/items/mine")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].uuid").value(bobItemUuid));

        // ── 11. Accepted offer detail has respondedAt set ─────────
        mockMvc.perform(apiGet("/trade-offers/" + offerUuid)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════
    //  ITEM_EXCHANGE with multiple offered items
    // ══════════════════════════════════════════════════════════════

    @Test
    void itemExchangeWithMultipleOfferedItems() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItem1Uuid = createActiveItem(bobToken, "Bob's Item 1");
        String bobItem2Uuid = createActiveItem(bobToken, "Bob's Item 2");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s", "%s"],
                                  "mode": "ITEM_EXCHANGE",
                                  "message": "Two for one deal!"
                                }
                                """.formatted(aliceItemUuid, bobItem1Uuid, bobItem2Uuid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("ITEM_EXCHANGE"))
                .andExpect(jsonPath("$.offeredItems", hasSize(2)))
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Accept: all 3 items should be archived
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(apiGet("/catalog/items/" + aliceItemUuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(apiGet("/catalog/items/" + bobItem1Uuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(apiGet("/catalog/items/" + bobItem2Uuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    // ══════════════════════════════════════════════════════════════
    //  GIFT mode
    // ══════════════════════════════════════════════════════════════

    @Test
    void giftModeWithNoOfferedItems() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "mode": "GIFT",
                                  "message": "I would love this as a gift!"
                                }
                                """.formatted(aliceItemUuid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("GIFT"))
                .andExpect(jsonPath("$.offeredItems", hasSize(0)))
                .andExpect(jsonPath("$.senderItem").doesNotExist())
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Accept: only requested item should be archived
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(apiGet("/catalog/items/" + aliceItemUuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void giftModeWithOfferedItemsIsRejected() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "GIFT",
                                  "message": "This should fail"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  NEGOTIABLE mode
    // ══════════════════════════════════════════════════════════════

    @Test
    void negotiableModeWithNoOfferedItemsAndMessage() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "mode": "NEGOTIABLE",
                                  "message": "I can offer $50 cash for this item"
                                }
                                """.formatted(aliceItemUuid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("NEGOTIABLE"))
                .andExpect(jsonPath("$.offeredItems", hasSize(0)))
                .andExpect(jsonPath("$.message").value("I can offer $50 cash for this item"))
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Accept: only requested item archived (no offered items)
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(apiGet("/catalog/items/" + aliceItemUuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Duplicate senderItemUuids rejected
    // ══════════════════════════════════════════════════════════════

    @Test
    void duplicateSenderItemUuidsRejected() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s", "%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid, bobItemUuid)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Accept archives all offered items + requested item and rejects competing
    // ══════════════════════════════════════════════════════════════

    @Test
    void acceptArchivesAllOfferedAndRequestedAndRejectsCompeting() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String charlieToken = registerActivateAndLogin("charlie", "charlie@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItem1Uuid = createActiveItem(bobToken, "Bob's Item 1");
        String bobItem2Uuid = createActiveItem(bobToken, "Bob's Item 2");
        String charlieItemUuid = createActiveItem(charlieToken, "Charlie's Item");

        // Bob offers two items for Alice's item
        MvcResult bobOfferResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s", "%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItem1Uuid, bobItem2Uuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String bobOfferUuid = extractField(bobOfferResult);

        // Charlie sends a competing offer for Alice's item
        MvcResult charlieOfferResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, charlieItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String charlieOfferUuid = extractField(charlieOfferResult);

        // Alice accepts Bob's offer
        mockMvc.perform(apiPost("/trade-offers/" + bobOfferUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // All 3 items archived
        mockMvc.perform(apiGet("/catalog/items/" + aliceItemUuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(apiGet("/catalog/items/" + bobItem1Uuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(apiGet("/catalog/items/" + bobItem2Uuid))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        // Charlie's item NOT archived
        mockMvc.perform(apiGet("/catalog/items/" + charlieItemUuid))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Charlie's competing offer auto-rejected
        mockMvc.perform(apiGet("/trade-offers/" + charlieOfferUuid)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Competing offers auto-rejection (backward compat)
    // ══════════════════════════════════════════════════════════════

    @Test
    void competingOffersAreAutoRejectedOnAccept() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String charlieToken = registerActivateAndLogin("charlie", "charlie@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");
        String charlieItemUuid = createActiveItem(charlieToken, "Charlie's Item");

        // Bob sends offer to Alice
        MvcResult bobOfferResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String bobOfferUuid = extractField(bobOfferResult);

        // Charlie sends competing offer to Alice (for the same receiver item)
        MvcResult charlieOfferResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, charlieItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String charlieOfferUuid = extractField(charlieOfferResult);

        // Alice accepts Bob's offer
        mockMvc.perform(apiPost("/trade-offers/" + bobOfferUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Charlie's competing offer is automatically REJECTED
        mockMvc.perform(apiGet("/trade-offers/" + charlieOfferUuid)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Status filter on list endpoints
    // ══════════════════════════════════════════════════════════════

    @Test
    void listIncomingWithStatusFilter() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        // Bob sends offer
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated());

        // Alice filters incoming by PENDING → 1 result
        mockMvc.perform(apiGet("/trade-offers/incoming")
                        .header("Authorization", "Bearer " + aliceToken)
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Alice filters incoming by ACCEPTED → 0 results
        mockMvc.perform(apiGet("/trade-offers/incoming")
                        .header("Authorization", "Bearer " + aliceToken)
                        .queryParam("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listSentWithStatusFilter() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        // Bob sends offer
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated());

        // Bob filters sent by PENDING → 1 result
        mockMvc.perform(apiGet("/trade-offers/sent")
                        .header("Authorization", "Bearer " + bobToken)
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Bob filters sent by REJECTED → 0 results
        mockMvc.perform(apiGet("/trade-offers/sent")
                        .header("Authorization", "Bearer " + bobToken)
                        .queryParam("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ══════════════════════════════════════════════════════════════
    //  Authorization & permission checks
    // ══════════════════════════════════════════════════════════════

    @Test
    void shouldReturn401ForCreateWithoutToken() throws Exception {
        mockMvc.perform(apiPost("/trade-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "bbbb2222-2222-2222-2222-222222222222",
                                  "senderItemUuids": ["aaaa1111-1111-1111-1111-111111111111"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void senderCannotAcceptOwnOffer() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Bob (sender) tries to accept → 403
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonParticipantCannotAccessOffer() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String charlieToken = registerActivateAndLogin("charlie", "charlie@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Charlie (non-participant) cannot view
        mockMvc.perform(apiGet("/trade-offers/" + offerUuid)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════
    //  Business rule enforcement
    // ══════════════════════════════════════════════════════════════

    @Test
    void cannotOfferInactiveItem() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        // Alice creates ACTIVE item, then archives it
        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        mockMvc.perform(apiPost("/catalog/items/" + aliceItemUuid + "/archive")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        // Bob tries to offer for archived item → 409
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isConflict());
    }

    @Test
    void selfOfferForbidden() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");

        String item1Uuid = createActiveItem(aliceToken, "Alice Item 1");
        String item2Uuid = createActiveItem(aliceToken, "Alice Item 2");

        // Alice tries to trade with herself → 403
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(item2Uuid, item1Uuid)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelledOfferCannotBeAccepted() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Bob cancels
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/cancel")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Alice tries to accept cancelled offer → 409
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectedOfferCannotBeAccepted() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        // Alice rejects
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/reject")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Alice tries to accept rejected offer → 409
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isConflict());
    }

    // ══════════════════════════════════════════════════════════════
    //  Mode validation: ITEM_EXCHANGE requires sender items
    // ══════════════════════════════════════════════════════════════

    @Test
    void itemExchangeWithoutSenderItemsRejected() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");

        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Mode validation: GIFT/NEGOTIABLE require message
    // ══════════════════════════════════════════════════════════════

    @Test
    void giftWithoutMessageRejected() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");

        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "mode": "GIFT"
                                }
                                """.formatted(aliceItemUuid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negotiableWithoutMessageRejected() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Item");

        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "mode": "NEGOTIABLE"
                                }
                                """.formatted(aliceItemUuid)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Trade offer messaging
    // ══════════════════════════════════════════════════════════════

    @Test
    void participantsCanExchangeMessages() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "receiverItemUuid": "%s",
                              "senderItemUuids": ["%s"],
                              "mode": "ITEM_EXCHANGE"
                            }
                            """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "Hello Alice"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello Alice"));

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "Hello Bob"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello Bob"));

        mockMvc.perform(apiGet("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("Hello Alice"))
                .andExpect(jsonPath("$[1].content").value("Hello Bob"));
    }

    @Test
    void nonParticipantCannotAccessMessages() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String charlieToken = registerActivateAndLogin("charlie", "charlie@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "receiverItemUuid": "%s",
                              "senderItemUuids": ["%s"],
                              "mode": "ITEM_EXCHANGE"
                            }
                            """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        mockMvc.perform(apiGet("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotSendMessageForNonPendingOffer() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "receiverItemUuid": "%s",
                              "senderItemUuids": ["%s"],
                              "mode": "ITEM_EXCHANGE"
                            }
                            """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "Too late"
                            }
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    void messagesRemainReadableAfterAccept() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice Item");
        String bobItemUuid = createActiveItem(bobToken, "Bob Item");

        MvcResult offerResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "receiverItemUuid": "%s",
                              "senderItemUuids": ["%s"],
                              "mode": "ITEM_EXCHANGE"
                            }
                            """.formatted(aliceItemUuid, bobItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String offerUuid = extractField(offerResult);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "Initial message"
                            }
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(apiGet("/trade-offers/" + offerUuid + "/messages")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Initial message"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private String registerActivateAndLogin(String username, String email, String password)
            throws Exception {
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

    private String createActiveItem(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(apiPost("/catalog/items")
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
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        return extractField(result);
    }

    private String extractField(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("uuid").asText();
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