package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferRequestedEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOfferRequestedEntryRepository extends JpaRepository<TradeOfferRequestedEntryEntity, Long> {

    @Query("""
            SELECT toe.itemListingEntryId FROM TradeOfferRequestedEntryEntity toe
            WHERE toe.tradeOffer.id = :offerId
            ORDER BY toe.id ASC
            """)
    List<Long> findEntryIdsByTradeOfferId(@Param("offerId") Long offerId);
}

