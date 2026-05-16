package com.barterplatform.web.trade.controller;

import com.barterplatform.api.controller.TradeOffersApi;
import com.barterplatform.api.model.*;
import com.barterplatform.application.trade.service.TradeOfferMessageService;
import com.barterplatform.application.trade.service.TradeOfferService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
public class TradeOffersController implements TradeOffersApi {

    private final TradeOfferService tradeOfferService;
    private final TradeOfferMessageService tradeOfferMessageService;

    public TradeOffersController(
            TradeOfferService tradeOfferService,
            TradeOfferMessageService tradeOfferMessageService) {
        this.tradeOfferService = tradeOfferService;
        this.tradeOfferMessageService = tradeOfferMessageService;
    }

    @Override
    public ResponseEntity<TradeOfferResponse> createTradeOffer(
            CreateTradeOfferRequest createTradeOfferRequest) {
        UUID currentUserUuid = currentUserUuid();
        TradeOfferResponse response = tradeOfferService.createOffer(currentUserUuid, createTradeOfferRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<TradeOfferPagedResponse> listIncomingTradeOffers(
            Integer page, Integer size, String sort,
            TradeOfferStatus status) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.listIncoming(
                currentUserUuid, page, size, sort, mapStatusToDomain(status)));
    }

    @Override
    public ResponseEntity<TradeOfferPagedResponse> listSentTradeOffers(
            Integer page, Integer size, String sort,
            TradeOfferStatus status) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.listSent(
                currentUserUuid, page, size, sort, mapStatusToDomain(status)));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> getTradeOfferByUuid(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.getOffer(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> acceptTradeOffer(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.acceptOffer(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> confirmTradeOfferCompletion(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.confirmCompletion(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> rejectTradeOffer(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.rejectOffer(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> cancelTradeOffer(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.cancelOffer(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<List<TradeOfferMessageResponse>> listTradeOfferMessages(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferMessageService.listMessages(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferMessageResponse> sendTradeOfferMessage(
            UUID tradeOfferUuid,
            SendTradeOfferMessageRequest sendTradeOfferMessageRequest) {
        UUID currentUserUuid = currentUserUuid();
        TradeOfferMessageResponse response = tradeOfferMessageService.sendMessage(
                currentUserUuid,
                tradeOfferUuid,
                sendTradeOfferMessageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Private helpers ──────────────────────────────────────────

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;
        return principal.getUserUuid();
    }

    private com.barterplatform.domain.trade.enums.TradeOfferStatus mapStatusToDomain(
            TradeOfferStatus apiStatus) {
        return apiStatus == null ? null
                : com.barterplatform.domain.trade.enums.TradeOfferStatus.valueOf(apiStatus.name());
    }
}

