package com.barterplatform.application.catalog.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeOfferInvalidationServiceImplTest {

    @Mock
    private TradeOfferRepository tradeOfferRepository;

    private TradeOfferInvalidationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TradeOfferInvalidationServiceImpl(tradeOfferRepository);
    }

    @Test
    void invalidatePendingOffersForListingInvalidatesEachPendingOffer() {
        TradeOfferEntity first = pendingOffer(1L);
        TradeOfferEntity second = pendingOffer(2L);
        when(tradeOfferRepository.findPendingOffersReferencingItem(55L)).thenReturn(List.of(first, second));

        service.invalidatePendingOffersForListing(55L);

        verify(tradeOfferRepository).findPendingOffersReferencingItem(55L);
        verify(tradeOfferRepository).save(first);
        verify(tradeOfferRepository).save(second);
    }

    @Test
    void invalidatePendingOffersForListingDoesNothingWhenNoOffersReferenceListing() {
        when(tradeOfferRepository.findPendingOffersReferencingItem(55L)).thenReturn(List.of());

        service.invalidatePendingOffersForListing(55L);

        verify(tradeOfferRepository).findPendingOffersReferencingItem(55L);
        verify(tradeOfferRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private TradeOfferEntity pendingOffer(Long id) {
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setId(id);
        offer.setUuid(UUID.randomUUID());
        offer.setSenderUserId(10L);
        offer.setReceiverUserId(20L);
        offer.setSenderItemId(30L);
        offer.setReceiverItemId(40L);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setCreatedAt(OffsetDateTime.now());
        return offer;
    }
}

