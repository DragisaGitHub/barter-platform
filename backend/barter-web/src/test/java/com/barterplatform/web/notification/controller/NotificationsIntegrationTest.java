package com.barterplatform.web.notification.controller;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.notification.entity.NotificationEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import com.barterplatform.infrastructure.notification.repository.NotificationRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class NotificationsIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemTagRepository itemTagRepository;
    @Autowired private TradeOfferRepository tradeOfferRepository;
    @Autowired private TradeOfferMessageRepository tradeOfferMessageRepository;
    @Autowired private TradeOfferItemRepository tradeOfferItemRepository;
    @Autowired private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        // Delete notifications before users (FK constraint)
        notificationRepository.deleteAllInBatch();
        tradeOfferMessageRepository.deleteAllInBatch();
        tradeOfferItemRepository.deleteAllInBatch();
        tradeOfferRepository.deleteAllInBatch();
        itemTagRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ══════════════════════════════════════════════════════════════
    //  Unauthenticated access is rejected
    // ══════════════════════════════════════════════════════════════

    @Test
    void listNotificationsRequiresAuthentication() throws Exception {
        mockMvc.perform(apiGet("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unreadCountRequiresAuthentication() throws Exception {
        mockMvc.perform(apiGet("/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markReadRequiresAuthentication() throws Exception {
        mockMvc.perform(apiPost("/notifications/" + CATEGORY_TOYS_UUID + "/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markAllReadRequiresAuthentication() throws Exception {
        mockMvc.perform(apiPost("/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════
    //  Trade offer received → notification created for receiver
    // ══════════════════════════════════════════════════════════════

    @Test
    void createTradeOfferCreatesReceiverNotification() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

        // Bob sends offer to Alice
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

        // Alice sees 1 unread notification
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // Alice lists notifications
        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_RECEIVED"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.content[0].referenceType").value("TRADE_OFFER"));

        // Bob (sender) has no notifications from creating the offer
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // ══════════════════════════════════════════════════════════════
    //  Accept → notification for sender
    // ══════════════════════════════════════════════════════════════

    @Test
    void acceptOfferCreatesSenderNotification() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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

        // Alice accepts
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        // Bob should now have 1 unread notification (TRADE_OFFER_ACCEPTED)
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_ACCEPTED"))
                .andExpect(jsonPath("$.content[0].referenceType").value("TRADE_OFFER"))
                .andExpect(jsonPath("$.content[0].referenceUuid").value(offerUuid));
    }

    @Test
    void firstCompletionConfirmationCreatesCounterpartyNotification() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/confirm-completion")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_COMPLETION_CONFIRMED"))
                .andExpect(jsonPath("$.content[0].referenceUuid").value(offerUuid));
    }

    @Test
    void finalCompletionCreatesNotificationsForBothParticipants() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/confirm-completion")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/confirm-completion")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_COMPLETED"))
                .andExpect(jsonPath("$.content[0].referenceUuid").value(offerUuid));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_COMPLETED"))
                .andExpect(jsonPath("$.content[0].referenceUuid").value(offerUuid));
    }

    // ══════════════════════════════════════════════════════════════
    //  Reject → notification for sender
    // ══════════════════════════════════════════════════════════════

    @Test
    void rejectOfferCreatesSenderNotification() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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
                .andExpect(status().isOk());

        // Bob should have 1 unread notification (TRADE_OFFER_REJECTED)
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_REJECTED"))
                .andExpect(jsonPath("$.content[0].referenceUuid").value(offerUuid));
    }

    // ══════════════════════════════════════════════════════════════
    //  Cancel → notification for receiver
    // ══════════════════════════════════════════════════════════════

    @Test
    void cancelOfferCreatesReceiverNotification() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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

        // Bob cancels their own offer
        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/cancel")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        // Alice should have 2 notifications: RECEIVED + CANCELLED
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void tradeMessageNotificationReturnsMetadata() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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
                                  "content": "Can we meet tomorrow?"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_MESSAGE_RECEIVED"))
                .andExpect(jsonPath("$.content[0].metadata.actorUsername").value("bob"))
                .andExpect(jsonPath("$.content[0].metadata.tradeOfferUuid").value(offerUuid))
                .andExpect(jsonPath("$.content[0].title").value("TRADE_MESSAGE_RECEIVED"))
                .andExpect(jsonPath("$.content[0].message").doesNotExist());
    }

    @Test
    void legacyNotificationWithoutMetadataStillReturnsStoredTitleAndMessage() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        Long aliceUserId = userRepository.findByEmail("alice@test.com").orElseThrow().getId();

        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientUserId(aliceUserId);
        notification.setType(NotificationType.LISTING_REMOVED);
        notification.setTitle("Legacy notification title");
        notification.setMessage("Legacy notification body");
        notificationRepository.save(notification);

        MvcResult listResult = mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Legacy notification title"))
                .andExpect(jsonPath("$.content[0].message").value("Legacy notification body"))
                .andReturn();

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode firstNotification = listJson.get("content").get(0);
        org.junit.jupiter.api.Assertions.assertTrue(
                !firstNotification.has("metadata") || firstNotification.get("metadata").isNull());
    }

    // ══════════════════════════════════════════════════════════════
    //  Mark single notification as read
    // ══════════════════════════════════════════════════════════════

    @Test
    void markSingleNotificationAsRead() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

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

        // Get Alice's notification UUID
        MvcResult listResult = mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String notifUuid = listJson.get("content").get(0).get("uuid").asString();

        // Mark as read
        mockMvc.perform(apiPost("/notifications/" + notifUuid + "/read")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Unread count should now be 0
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // ══════════════════════════════════════════════════════════════
    //  Mark all notifications as read
    // ══════════════════════════════════════════════════════════════

    @Test
    void markAllNotificationsAsRead() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String carolToken = registerActivateAndLogin("carol", "carol@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String carolItemUuid = createActiveItem(carolToken, "Carol's Hat");
        String bobItemUuid1 = createActiveItem(bobToken, "Bob's Gadget");
        String bobItemUuid2 = createActiveItem(bobToken, "Bob's Toy");

        // Bob sends 2 offers to Alice (creates 2 notifications for Alice)
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, bobItemUuid1)))
                .andExpect(status().isCreated());

        // Bob sends offer to Carol
        mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(carolItemUuid, bobItemUuid2)))
                .andExpect(status().isCreated());

        // Alice has 1 unread
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // Alice marks all as read
        mockMvc.perform(apiPost("/notifications/read-all")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // Alice's count is now 0
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // Carol's notification is unaffected (still unread)
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    // ══════════════════════════════════════════════════════════════
    //  User cannot access another user's notification
    // ══════════════════════════════════════════════════════════════

    @Test
    void userCannotMarkAnotherUsersNotificationAsRead() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");

        // Bob sends offer → creates notification for Alice
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

        // Get Alice's notification UUID
        MvcResult listResult = mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String aliceNotifUuid = listJson.get("content").get(0).get("uuid").asString();

        // Bob tries to mark Alice's notification as read → 404 (not found for Bob)
        mockMvc.perform(apiPost("/notifications/" + aliceNotifUuid + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════════
    //  Auto-rejected competing offers do NOT create notifications
    // ══════════════════════════════════════════════════════════════

    @Test
    void autoRejectedCompetingOffersDoNotCreateNotifications() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@test.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob", "bob@test.com", "P@ssword456");
        String carolToken = registerActivateAndLogin("carol", "carol@test.com", "P@ssword789");

        String aliceItemUuid = createActiveItem(aliceToken, "Alice's Book");
        String bobItemUuid = createActiveItem(bobToken, "Bob's Gadget");
        String carolItemUuid = createActiveItem(carolToken, "Carol's Hat");

        // Bob sends offer for Alice's Book (PENDING)
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

        // Carol also sends an offer for Alice's Book (competing PENDING offer)
        MvcResult carolOfferResult = mockMvc.perform(apiPost("/trade-offers")
                        .header("Authorization", "Bearer " + carolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "%s",
                                  "senderItemUuids": ["%s"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """.formatted(aliceItemUuid, carolItemUuid)))
                .andExpect(status().isCreated())
                .andReturn();

        String carolOfferUuid = extractField(carolOfferResult);

        // Alice accepts Carol's offer → Bob's competing offer is auto-rejected
        mockMvc.perform(apiPost("/trade-offers/" + carolOfferUuid + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        // Bob should only have the ACCEPTED notification for Carol's offer affecting Bob's offer.
        // Wait — Bob is the SENDER of AUTO-REJECTED offer. The spec says no notification for auto-reject.
        // Bob's offer was auto-rejected, but no TRADE_OFFER_REJECTED notification for Bob.
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // Carol should get 1 notification: ACCEPTED
        mockMvc.perform(apiGet("/notifications/unread-count")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(apiGet("/notifications")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRADE_OFFER_ACCEPTED"));
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
                .andReturn();

        return extractField(result);
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

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

