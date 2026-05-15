package com.barterplatform.application.catalog.service;

public interface TradeOfferInvalidationService {

    void invalidatePendingOffersForListing(Long itemId);
}

