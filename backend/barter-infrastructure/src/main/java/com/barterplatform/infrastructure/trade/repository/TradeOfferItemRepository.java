package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferItemEntity;
import com.barterplatform.domain.trade.enums.TradeOfferItemSide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOfferItemRepository extends JpaRepository<TradeOfferItemEntity, Long> {

    List<TradeOfferItemEntity> findByTradeOfferIdAndSide(Long tradeOfferId, TradeOfferItemSide side);

    @Query("""
            SELECT toi.itemId FROM TradeOfferItemEntity toi
            WHERE toi.tradeOffer.id = :offerId
              AND toi.side = :side
            """)
    List<Long> findItemIdsByTradeOfferIdAndSide(
            @Param("offerId") Long offerId,
            @Param("side") TradeOfferItemSide side);
}

