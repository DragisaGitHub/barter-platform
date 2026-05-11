package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOfferMessageRepository extends JpaRepository<TradeOfferMessageEntity, Long> {

    List<TradeOfferMessageEntity> findByTradeOfferIdOrderByCreatedAtAsc(Long tradeOfferId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}