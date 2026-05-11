package com.barterplatform.web.trade.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.*;
import com.barterplatform.application.trade.service.TradeOfferMessageService;
import com.barterplatform.application.trade.service.TradeOfferService;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TradeOffersControllerMvcTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private TradeOfferService tradeOfferService;
    private TradeOfferMessageService tradeOfferMessageService;

    @BeforeEach
    void setUp() {
        tradeOfferService = mock(TradeOfferService.class);
        tradeOfferMessageService = mock(TradeOfferMessageService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TradeOffersController(tradeOfferService,  tradeOfferMessageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createTradeOffer ──────────────────────────────────────────

    @Test
    void createTradeOfferUnauthenticatedShouldFail() throws Exception {
        mockMvc.perform(apiPost("/trade-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "bbbb2222-2222-2222-2222-222222222222",
                                  "senderItemUuids": ["aaaa1111-1111-1111-1111-111111111111"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(tradeOfferService);
    }

    @Test
    void createTradeOfferAuthenticatedShouldDelegate() throws Exception {
        setAuthenticatedUser();

        TradeOfferResponse response = new TradeOfferResponse()
                .uuid(UUID.randomUUID())
                .status(com.barterplatform.api.model.TradeOfferStatus.PENDING)
                .mode(com.barterplatform.api.model.TradeOfferMode.ITEM_EXCHANGE)
                .offeredItems(List.of());
        when(tradeOfferService.createOffer(eq(USER_UUID), any(CreateTradeOfferRequest.class)))
                .thenReturn(response);

        mockMvc.perform(apiPost("/trade-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverItemUuid": "bbbb2222-2222-2222-2222-222222222222",
                                  "senderItemUuids": ["aaaa1111-1111-1111-1111-111111111111"],
                                  "mode": "ITEM_EXCHANGE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.mode").value("ITEM_EXCHANGE"));

        verify(tradeOfferService).createOffer(eq(USER_UUID), any(CreateTradeOfferRequest.class));
    }

    // ── listIncomingTradeOffers ────────────────────────────────────

    @Test
    void listIncomingTradeOffersShouldDelegateWithStatusFilter() throws Exception {
        setAuthenticatedUser();

        TradeOfferPagedResponse pagedResponse = new TradeOfferPagedResponse()
                .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0)
                .first(true).last(true);
        when(tradeOfferService.listIncoming(eq(USER_UUID), any(), any(), any(),
                eq(TradeOfferStatus.PENDING)))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/trade-offers/incoming")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(tradeOfferService).listIncoming(eq(USER_UUID), eq(0), eq(20), any(),
                eq(TradeOfferStatus.PENDING));
    }

    // ── listSentTradeOffers ───────────────────────────────────────

    @Test
    void listSentTradeOffersShouldDelegateWithStatusFilter() throws Exception {
        setAuthenticatedUser();

        TradeOfferPagedResponse pagedResponse = new TradeOfferPagedResponse()
                .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0)
                .first(true).last(true);
        when(tradeOfferService.listSent(eq(USER_UUID), any(), any(), any(),
                eq(TradeOfferStatus.ACCEPTED)))
                .thenReturn(pagedResponse);

        mockMvc.perform(apiGet("/trade-offers/sent")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(tradeOfferService).listSent(eq(USER_UUID), eq(0), eq(20), any(),
                eq(TradeOfferStatus.ACCEPTED));
    }

    // ── getTradeOfferByUuid ───────────────────────────────────────

    @Test
    void getTradeOfferByUuidShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        TradeOfferResponse response = new TradeOfferResponse()
                .uuid(offerUuid)
                .status(com.barterplatform.api.model.TradeOfferStatus.PENDING);
        when(tradeOfferService.getOffer(USER_UUID, offerUuid)).thenReturn(response);

        mockMvc.perform(apiGet("/trade-offers/" + offerUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(offerUuid.toString()));

        verify(tradeOfferService).getOffer(USER_UUID, offerUuid);
    }

    // ── acceptTradeOffer ──────────────────────────────────────────

    @Test
    void acceptTradeOfferShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        TradeOfferResponse response = new TradeOfferResponse()
                .uuid(offerUuid)
                .status(com.barterplatform.api.model.TradeOfferStatus.ACCEPTED);
        when(tradeOfferService.acceptOffer(USER_UUID, offerUuid)).thenReturn(response);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(tradeOfferService).acceptOffer(USER_UUID, offerUuid);
    }

    // ── rejectTradeOffer ──────────────────────────────────────────

    @Test
    void rejectTradeOfferShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        TradeOfferResponse response = new TradeOfferResponse()
                .uuid(offerUuid)
                .status(com.barterplatform.api.model.TradeOfferStatus.REJECTED);
        when(tradeOfferService.rejectOffer(USER_UUID, offerUuid)).thenReturn(response);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(tradeOfferService).rejectOffer(USER_UUID, offerUuid);
    }

    // ── cancelTradeOffer ──────────────────────────────────────────

    @Test
    void cancelTradeOfferShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        TradeOfferResponse response = new TradeOfferResponse()
                .uuid(offerUuid)
                .status(com.barterplatform.api.model.TradeOfferStatus.CANCELLED);
        when(tradeOfferService.cancelOffer(USER_UUID, offerUuid)).thenReturn(response);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(tradeOfferService).cancelOffer(USER_UUID, offerUuid);
    }

    @Test
    void listTradeOfferMessagesShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();

        TradeOfferMessageResponse response = new TradeOfferMessageResponse()
                .uuid(messageUuid)
                .tradeOfferUuid(offerUuid)
                .senderUserUuid(USER_UUID)
                .senderUsername("alice")
                .recipientUserUuid(UUID.randomUUID())
                .recipientUsername("bob")
                .content("Hello")
                .isRead(false);

        when(tradeOfferMessageService.listMessages(USER_UUID, offerUuid))
                .thenReturn(List.of(response));

        mockMvc.perform(apiGet("/trade-offers/" + offerUuid + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(messageUuid.toString()))
                .andExpect(jsonPath("$[0].content").value("Hello"));

        verify(tradeOfferMessageService).listMessages(USER_UUID, offerUuid);
    }

    @Test
    void sendTradeOfferMessageShouldDelegate() throws Exception {
        setAuthenticatedUser();

        UUID offerUuid = UUID.randomUUID();
        UUID messageUuid = UUID.randomUUID();

        TradeOfferMessageResponse response = new TradeOfferMessageResponse()
                .uuid(messageUuid)
                .tradeOfferUuid(offerUuid)
                .senderUserUuid(USER_UUID)
                .senderUsername("alice")
                .recipientUserUuid(UUID.randomUUID())
                .recipientUsername("bob")
                .content("Hello")
                .isRead(false);

        when(tradeOfferMessageService.sendMessage(eq(USER_UUID), eq(offerUuid), any(SendTradeOfferMessageRequest.class)))
                .thenReturn(response);

        mockMvc.perform(apiPost("/trade-offers/" + offerUuid + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "Hello"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(messageUuid.toString()))
                .andExpect(jsonPath("$.content").value("Hello"));

        verify(tradeOfferMessageService).sendMessage(eq(USER_UUID), eq(offerUuid), any(SendTradeOfferMessageRequest.class));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void setAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_UUID, "alice", List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
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

