package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOfferMessageRepository extends JpaRepository<TradeOfferMessageEntity, Long> {

    Optional<TradeOfferMessageEntity> findByUuid(UUID uuid);

    List<TradeOfferMessageEntity> findByTradeOfferIdOrderByCreatedAtAsc(Long tradeOfferId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}