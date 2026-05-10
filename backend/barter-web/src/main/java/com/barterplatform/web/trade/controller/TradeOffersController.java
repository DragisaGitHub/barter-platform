package com.barterplatform.web.trade.controller;

import com.barterplatform.api.controller.TradeOffersApi;
import com.barterplatform.api.model.CreateTradeOfferRequest;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.api.model.TradeOfferStatus;
import com.barterplatform.application.trade.service.TradeOfferService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradeOffersController implements TradeOffersApi {

    private final TradeOfferService tradeOfferService;

    public TradeOffersController(TradeOfferService tradeOfferService) {
        this.tradeOfferService = tradeOfferService;
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
            Integer page, Integer size, @Nullable String sort,
            @Nullable TradeOfferStatus status) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.listIncoming(
                currentUserUuid, page, size, sort, mapStatusToDomain(status)));
    }

    @Override
    public ResponseEntity<TradeOfferPagedResponse> listSentTradeOffers(
            Integer page, Integer size, @Nullable String sort,
            @Nullable TradeOfferStatus status) {
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
    public ResponseEntity<TradeOfferResponse> rejectTradeOffer(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.rejectOffer(currentUserUuid, tradeOfferUuid));
    }

    @Override
    public ResponseEntity<TradeOfferResponse> cancelTradeOffer(UUID tradeOfferUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(tradeOfferService.cancelOffer(currentUserUuid, tradeOfferUuid));
    }

    // ── Private helpers ──────────────────────────────────────────

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUserUuid();
    }

    private com.barterplatform.domain.trade.enums.TradeOfferStatus mapStatusToDomain(
            @Nullable TradeOfferStatus apiStatus) {
        return apiStatus == null ? null
                : com.barterplatform.domain.trade.enums.TradeOfferStatus.valueOf(apiStatus.name());
    }
}

