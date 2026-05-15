package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.application.catalog.service.TradeOfferInvalidationService;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeOfferInvalidationServiceImpl implements TradeOfferInvalidationService {

    private final TradeOfferRepository tradeOfferRepository;

    public TradeOfferInvalidationServiceImpl(TradeOfferRepository tradeOfferRepository) {
        this.tradeOfferRepository = tradeOfferRepository;
    }

    @Override
    public void invalidatePendingOffersForListing(Long itemId) {
        List<TradeOfferEntity> offers = tradeOfferRepository.findPendingOffersReferencingItem(itemId);
        for (TradeOfferEntity offer : offers) {
            offer.invalidate();
            tradeOfferRepository.save(offer);
        }
    }
}

